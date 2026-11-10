package com.merlette.backend.service;

import com.merlette.backend.dto.ConversationDto;
import com.merlette.backend.dto.MeDto;
import com.merlette.backend.dto.UpdateProfileRequest;
import com.merlette.backend.dto.UserProfileDto;
import com.merlette.backend.entity.ObservationStatus;
import com.merlette.backend.entity.User;
import com.merlette.backend.exception.ApiException;
import com.merlette.backend.repository.ObservationRepository;
import com.merlette.backend.repository.ProposalRepository;
import com.merlette.backend.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ObservationRepository observationRepository;
    private final ProposalRepository proposalRepository;
    private final JdbcClient jdbcClient;

    @Transactional(readOnly = true)
    public MeDto me(Integer userId) {
        return MeDto.from(findMe(userId));
    }

    @Transactional(readOnly = true)
    public UserProfileDto profile(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable."));
        UserProfileDto.Stats stats = new UserProfileDto.Stats(
                observationRepository.countByAuthorId(id),
                proposalRepository.countByAuthorId(id),
                observationRepository.countByAuthorIdAndStatus(id, ObservationStatus.IDENTIFIED));
        return new UserProfileDto(user.getId(), user.getUsername(), user.getBio(), user.getAvatarUrl(),
                user.getCreatedAt(), stats);
    }

    @Transactional
    public MeDto updateMe(Integer userId, UpdateProfileRequest request) {
        User user = findMe(userId);
        if (request.bio() != null) {
            user.setBio(request.bio());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }
        return MeDto.from(user);
    }

    /** « Mes discussions » : observations publiées, commentées ou avec une proposition de l'utilisateur. */
    @Transactional(readOnly = true)
    public List<ConversationDto> conversations(Integer userId) {
        String sql = """
                SELECT o.id, o.title, o.status::text AS status,
                       (SELECT p.url FROM photos p WHERE p.observation_id = o.id
                        ORDER BY p.position LIMIT 1) AS cover_url,
                       GREATEST(o.updated_at,
                                (SELECT MAX(c.created_at) FROM comments c WHERE c.observation_id = o.id),
                                (SELECT MAX(p.created_at) FROM proposals p WHERE p.observation_id = o.id)
                       ) AS last_activity,
                       o.user_id = :uid AS is_author,
                       EXISTS (SELECT 1 FROM comments c WHERE c.observation_id = o.id AND c.user_id = :uid) AS is_commenter,
                       EXISTS (SELECT 1 FROM proposals p WHERE p.observation_id = o.id AND p.user_id = :uid) AS is_proposer
                FROM observations o
                WHERE o.user_id = :uid
                   OR EXISTS (SELECT 1 FROM comments c WHERE c.observation_id = o.id AND c.user_id = :uid)
                   OR EXISTS (SELECT 1 FROM proposals p WHERE p.observation_id = o.id AND p.user_id = :uid)
                ORDER BY last_activity DESC
                LIMIT 100
                """;
        return jdbcClient.sql(sql).param("uid", userId).query((rs, rowNum) -> {
            List<String> roles = new ArrayList<>();
            if (rs.getBoolean("is_author")) roles.add("AUTHOR");
            if (rs.getBoolean("is_commenter")) roles.add("COMMENTER");
            if (rs.getBoolean("is_proposer")) roles.add("PROPOSER");
            return new ConversationDto(rs.getInt("id"), rs.getString("title"), rs.getString("status"),
                    rs.getString("cover_url"), rs.getTimestamp("last_activity").toInstant(), roles);
        }).list();
    }

    private User findMe(Integer userId) {
        // token valide mais compte supprimé entre-temps
        return userRepository.findById(userId).orElseThrow(ApiException::unauthorized);
    }
}
