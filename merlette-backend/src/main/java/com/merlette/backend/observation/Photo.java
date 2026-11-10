package com.merlette.backend.observation;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "photos") @Getter @NoArgsConstructor
public class Photo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) private Observation observation;
    private String url;
    private short position;
    private Instant createdAt = Instant.now();
    public Photo(Observation observation, String url, int position) {
        this.observation = observation; this.url = url; this.position = (short) position;
    }
}
