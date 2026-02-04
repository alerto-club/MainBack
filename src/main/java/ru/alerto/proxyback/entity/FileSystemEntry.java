package ru.alerto.proxyback.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "file_system_entries", indexes = {
        @Index(name = "idx_path", columnList = "path")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileSystemEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String path;

    @Column(nullable = false)
    private String name;

    private boolean isDirectory;
}