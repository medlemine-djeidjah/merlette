package com.merlette.backend.service;

import com.merlette.backend.dto.CommentDto;
import com.merlette.backend.entity.Comment;
import com.merlette.backend.entity.Observation;
import com.merlette.backend.entity.User;
import com.merlette.backend.exception.ApiException;
import com.merlette.backend.repository.CommentRepository;
import com.merlette.backend.repository.ObservationRepository;
import com.merlette.backend.repository.UserRepository;
import com.merlette.backend.security.CurrentUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final ObservationRepository observationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public List<CommentDto> list(Integer observationId) {
        findObservation(observationId);
        return commentRepository.findForObservation(observationId).stream().map(CommentDto::from).toList();
    }

    @Transactional
    public CommentDto create(Integer observationId, String content) {
        User me = userRepository.findById(currentUser.id()).orElseThrow(ApiException::unauthorized);
        Observation observation = findObservation(observationId);

        // notifier avant d'enregistrer, pour ne pas compter le nouveau commentateur
        notificationService.notifyNewComment(observation, me);

        Comment comment = new Comment();
        comment.setObservation(observation);
        comment.setAuthor(me);
        comment.setContent(content.trim());
        return CommentDto.from(commentRepository.save(comment));
    }

    @Transactional
    public CommentDto update(Integer commentId, String content) {
        Comment comment = findComment(commentId);
        currentUser.requireOwner(comment.getAuthor().getId());
        comment.setContent(content.trim());
        commentRepository.flush(); // déclenche @PreUpdate → updatedAt
        return CommentDto.from(comment);
    }

    @Transactional
    public void delete(Integer commentId) {
        Comment comment = findComment(commentId);
        currentUser.requireOwnerOrAdmin(comment.getAuthor().getId());
        commentRepository.delete(comment);
    }

    private Comment findComment(Integer id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Commentaire introuvable."));
    }

    private Observation findObservation(Integer id) {
        return observationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Observation introuvable."));
    }
}
