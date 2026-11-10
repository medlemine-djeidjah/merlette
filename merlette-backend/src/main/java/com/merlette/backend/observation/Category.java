package com.merlette.backend.observation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity @Table(name = "categories") @Getter @NoArgsConstructor
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    private String name;
    private String slug;
}
