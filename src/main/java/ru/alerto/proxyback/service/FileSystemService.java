package ru.alerto.proxyback.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.alerto.proxyback.entity.FileSystemEntry;
import ru.alerto.proxyback.repository.FileSystemRepository;

import java.io.File;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileSystemService {

    private final FileSystemRepository repository;

    @Transactional
    public void syncFile(File file) {
        String normalizedPath = file.getAbsolutePath().replace("\\", "/");

        // Логика "Проверь и Сохрани" должна быть в одной транзакции
        if (repository.findByPath(normalizedPath).isEmpty()) {
            FileSystemEntry entry = FileSystemEntry.builder()
                    .path(normalizedPath)
                    .name(file.getName())
                    .isDirectory(file.isDirectory())
                    .build();
            repository.save(entry);
            log.info("DB: New entry saved: {}", normalizedPath);
        }
    }

    @Transactional
    public void deleteFile(File file) {
        String normalizedPath = file.getAbsolutePath().replace("\\", "/");
        repository.findByPath(normalizedPath).ifPresent(entry -> {
            repository.delete(entry);
            log.info("DB: Entry removed: {}", normalizedPath);
        });
    }
}