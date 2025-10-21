package com.example.rental.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "capacities", uniqueConstraints = @UniqueConstraint(columnNames = "carType"))
public class Capacity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CarType carType;

    @Column(nullable = false)
    private int quantity;
}
