package ru.alerto.proxyback.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Collection;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileWatcherService {

    private final FileSystemService fileSystemService; // Инжектим сервис вместо репозитория

    @Value("${app.files.root}")
    private String rootPath;

    private FileAlterationMonitor monitor;

    @PostConstruct
    public void init() {
        startWatching();
    }

    @Async("taskExecutor")
    public void startWatching() {
        File rootDir = new File(rootPath);
        if (!rootDir.exists()) {
            log.error("Root directory not found: {}", rootPath);
            return;
        }

        // 1. Начальное сканирование
        log.info("Starting initial scan...");
        Collection<File> files = FileUtils.listFilesAndDirs(rootDir,
                org.apache.commons.io.filefilter.TrueFileFilter.INSTANCE,
                org.apache.commons.io.filefilter.TrueFileFilter.INSTANCE);

        for (File file : files) {
            if (file.getAbsolutePath().equals(rootDir.getAbsolutePath())) continue;
            // Внешний вызов сервиса -> Транзакция работает!
            fileSystemService.syncFile(file);
        }

        // 2. Настройка наблюдателя
        FileAlterationObserver observer = new FileAlterationObserver(rootDir);
        observer.addListener(new FileAlterationListenerAdaptor() {
            @Override
            public void onFileCreate(File file) {
                fileSystemService.syncFile(file);
            }

            @Override
            public void onFileDelete(File file) {
                fileSystemService.deleteFile(file);
            }

            @Override
            public void onDirectoryCreate(File directory) {
                fileSystemService.syncFile(directory);
            }

            @Override
            public void onDirectoryDelete(File directory) {
                fileSystemService.deleteFile(directory);
            }
        });

        monitor = new FileAlterationMonitor(5000, observer);
        try {
            monitor.start();
        } catch (Exception e) {
            log.error("Monitor start error", e);
        }
    }

    @PreDestroy
    public void stop() throws Exception {
        if (monitor != null) {
            monitor.stop();
        }
    }
}