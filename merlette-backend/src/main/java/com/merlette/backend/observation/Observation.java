package com.merlette.backend.observation;

import com.merlette.backend.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.*;

@Entity @Table(name = "observations") @Getter @NoArgsConstructor
public class Observation {
    public enum Status { NEEDS_ID, IDENTIFIED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") private User author;
    @ManyToOne(fetch = FetchType.LAZY) private Category category;
    private String title;
    @Column(columnDefinition = "text") private String description;
    private LocalDate observedAt;
    private String locationName;
    @Column(precision = 9, scale = 6) private BigDecimal latitude;
    @Column(precision = 9, scale = 6) private BigDecimal longitude;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "observation_status") private Status status = Status.NEEDS_ID;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = createdAt;
    public Observation(User author, Category category, ObservationService.Creation input) {
        this.author = author; this.category = category; this.title = input.getTitle();
        this.description = input.getDescription(); this.observedAt = input.getObservedAt();
        this.locationName = input.getLocationName(); this.latitude = input.getLatitude(); this.longitude = input.getLongitude();
    }
}
