package ru.alerto.proxyback.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.alerto.proxyback.entity.FileSystemEntry;

import java.util.List;
import java.util.Optional;

public interface FileSystemRepository extends JpaRepository<FileSystemEntry, Long> {
    Optional<FileSystemEntry> findByPath(String path);

    @Query("SELECT f FROM FileSystemEntry f WHERE f.path LIKE CONCAT(:parentPath, '/%') AND f.path NOT LIKE CONCAT(:parentPath, '/%/%')")
    List<FileSystemEntry> findDirectChildren(@Param("parentPath") String parentPath);

    boolean existsByPath(String path);
}