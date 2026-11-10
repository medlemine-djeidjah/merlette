package com.merlette.backend.service;

import com.merlette.backend.dto.NotificationDto;
import com.merlette.backend.dto.PageResponse;
import com.merlette.backend.entity.Notification;
import com.merlette.backend.entity.NotificationType;
import com.merlette.backend.entity.Observation;
import com.merlette.backend.entity.Proposal;
import com.merlette.backend.entity.User;
import com.merlette.backend.exception.ApiException;
import com.merlette.backend.repository.CommentRepository;
import com.merlette.backend.repository.NotificationRepository;
import com.merlette.backend.repository.UserRepository;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Notifications in-app (PRD §6.2). Jamais de notification à l'auteur de l'action. */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int PAGE_SIZE = 20;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    // --- Création ----------------------------------------------------------

    /** Nouvelle proposition → auteur de l'observation. */
    public void notifyNewProposal(Observation observation, Proposal proposal, User actor) {
        String message = actor.getUsername() + " propose « " + proposal.getSpecies().getCommonName()
                + " » pour votre observation " + label(observation);
        send(observation.getAuthor().getId(), NotificationType.NEW_PROPOSAL, observation, actor, message);
    }

    /** Nouveau commentaire → auteur de l'observation + personnes ayant déjà commenté. */
    public void notifyNewComment(Observation observation, User actor) {
        Set<Integer> recipients = new LinkedHashSet<>();
        recipients.add(observation.getAuthor().getId());
        recipients.addAll(commentRepository.findCommenterIds(observation.getId()));
        String message = actor.getUsername() + " a commenté l'observation " + label(observation);
        recipients.forEach(id -> send(id, NotificationType.NEW_COMMENT, observation, actor, message));
    }

    /** Consensus atteint → auteur de l'observation + proposeur de l'espèce retenue. */
    public void notifyIdentified(Observation observation, Proposal winner, User actor) {
        String species = winner.getSpecies().getCommonName();
        Map<Integer, String> messages = new LinkedHashMap<>();
        messages.put(winner.getAuthor().getId(), "Votre proposition « " + species + " » a été retenue pour l'observation "
                + label(observation));
        // si l'auteur est aussi le proposeur, c'est ce message qui est gardé
        messages.put(observation.getAuthor().getId(), "Votre observation a été identifiée : " + species);
        messages.forEach((id, message) ->
                send(id, NotificationType.OBSERVATION_IDENTIFIED, observation, actor, message));
    }

    private void send(Integer recipientId, NotificationType type, Observation observation, User actor, String message) {
        if (recipientId.equals(actor.getId())) {
            return;
        }
        Notification n = new Notification();
        n.setRecipient(userRepository.getReferenceById(recipientId));
        n.setActor(actor);
        n.setObservation(observation);
        n.setType(type);
        n.setMessage(message.length() > 300 ? message.substring(0, 300) : message);
        notificationRepository.save(n);
    }

    private static String label(Observation o) {
        return o.getTitle() == null || o.getTitle().isBlank() ? "#" + o.getId() : "« " + o.getTitle() + " »";
    }

    // --- Lecture -----------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<NotificationDto> list(Integer userId, boolean onlyUnread, int page) {
        PageRequest pageable = PageRequest.of(Math.max(page, 1) - 1, PAGE_SIZE,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<Notification> result = notificationRepository.findForUser(userId, onlyUnread, pageable);
        return PageResponse.of(result.getContent().stream().map(NotificationDto::from).toList(), result);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Integer userId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }

    @Transactional
    public NotificationDto markAsRead(Integer id, Integer userId) {
        // 404 aussi si la notification appartient à quelqu'un d'autre
        Notification n = notificationRepository.findByIdAndRecipientId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Notification introuvable."));
        n.setRead(true);
        return NotificationDto.from(n);
    }

    @Transactional
    public int markAllAsRead(Integer userId) {
        return notificationRepository.markAllAsRead(userId);
    }
}
