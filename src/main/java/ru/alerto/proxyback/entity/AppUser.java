package ru.alerto.proxyback.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_users", indexes = {
        @Index(name = "idx_telegram_id", columnList = "telegramId")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long telegramId;

    private String username;

    private LocalDateTime lastSeen;

    private Long currentDirectoryId;
}