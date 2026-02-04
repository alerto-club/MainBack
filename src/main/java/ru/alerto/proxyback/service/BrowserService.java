package ru.alerto.proxyback.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import ru.alerto.proxyback.entity.AppUser;
import ru.alerto.proxyback.entity.FileSystemEntry;
import ru.alerto.proxyback.repository.AppUserRepository;
import ru.alerto.proxyback.repository.FileSystemRepository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrowserService {

    private final AppUserRepository userRepository;
    private final FileSystemRepository fileRepository;

    @Value("${app.files.root}")
    private String globalRootPath;

    @Value("${app.files.mode:SHARED}")
    private String storageMode;

    private AppUser getInitializedUser(Long telegramId, String username) {
        AppUser user = userRepository.findByTelegramId(telegramId).orElseThrow();
        FileSystemEntry correctUserRoot = getOrCreateUserRoot(username);

        boolean needReset = false;

        if (user.getCurrentDirectoryId() == null) {
            needReset = true;
        } else if (!fileRepository.existsById(user.getCurrentDirectoryId())) {
            needReset = true;
        } else {
            FileSystemEntry current = fileRepository.findById(user.getCurrentDirectoryId()).get();
            // Улучшенная проверка на вложенность (игнорирует регистр букв для Windows)
            if (!isInside(current.getPath(), correctUserRoot.getPath())) {
                log.warn("Security Reset: User tried to escape root! Current: {}, Root: {}", current.getPath(), correctUserRoot.getPath());
                needReset = true;
            }
        }

        if (needReset) {
            user.setCurrentDirectoryId(correctUserRoot.getId());
            userRepository.save(user);
        }
        return user;
    }

    @Transactional
    public FileSystemEntry getOrCreateUserRoot(String username) {
        // 1. Формируем сырой путь
        String targetPath = globalRootPath;
        if ("PRIVATE".equalsIgnoreCase(storageMode)) {
            String safeUsername = (username != null) ? username : "unknown_user";
            targetPath = targetPath + File.separator + safeUsername;
        }

        File physicalDir = new File(targetPath);
        if (!physicalDir.exists()) {
            physicalDir.mkdirs();
            try {
                new File(physicalDir, "Welcome_" + username + ".txt").createNewFile();
            } catch (IOException e) {
                log.error("Error creating welcome file", e);
            }
        }

        // 2. ВАЖНО: Превращаем в канонический путь (убираем ./ и ../)
        String finalPath = normalizePath(getSafeCanonicalPath(physicalDir));

        return fileRepository.findByPath(finalPath)
                .orElseGet(() -> {
                    FileSystemEntry newRoot = FileSystemEntry.builder()
                            .path(finalPath)
                            .name("PRIVATE".equalsIgnoreCase(storageMode) ? username : "Главная")
                            .isDirectory(true)
                            .build();
                    return fileRepository.save(newRoot);
                });
    }

    @Transactional
    public InlineKeyboardMarkup getKeyboardForCurrentDir(Long telegramId) {
        String savedUsername = userRepository.findByTelegramId(telegramId).map(AppUser::getUsername).orElse("unknown");
        AppUser user = getInitializedUser(telegramId, savedUsername);

        FileSystemEntry currentDirEntry = fileRepository.findById(user.getCurrentDirectoryId()).orElseThrow();
        FileSystemEntry userRoot = getOrCreateUserRoot(savedUsername);

        File physicalDir = new File(currentDirEntry.getPath());
        File[] filesOnDisk = physicalDir.listFiles();

        List<FileSystemEntry> activeEntries = new ArrayList<>();

        if (filesOnDisk != null) {
            for (File file : filesOnDisk) {
                // Всегда используем канонический путь
                String normalizedPath = normalizePath(getSafeCanonicalPath(file));

                FileSystemEntry entry = fileRepository.findByPath(normalizedPath)
                        .orElseGet(() -> {
                            FileSystemEntry newEntry = FileSystemEntry.builder()
                                    .path(normalizedPath)
                                    .name(file.getName())
                                    .isDirectory(file.isDirectory())
                                    .build();
                            return fileRepository.save(newEntry);
                        });
                activeEntries.add(entry);
            }
        }

        activeEntries.sort(Comparator.comparing(FileSystemEntry::isDirectory).reversed()
                .thenComparing(FileSystemEntry::getName));

        List<InlineKeyboardRow> rows = new ArrayList<>();

        // Кнопка Назад
        // Сравниваем пути через equalsIgnoreCase на всякий случай
        if (!currentDirEntry.getPath().equalsIgnoreCase(userRoot.getPath())) {
            rows.add(new InlineKeyboardRow(createButton("⬅️ Назад", "UP")));
        }

        for (FileSystemEntry entry : activeEntries) {
            String icon = entry.isDirectory() ? "📁 " : "📄 ";
            String callbackData = entry.isDirectory() ? "DIR:" + entry.getId() : "FILE:" + entry.getId();
            rows.add(new InlineKeyboardRow(createButton(icon + entry.getName(), callbackData)));
        }

        return new InlineKeyboardMarkup(rows);
    }

    public String getCurrentPathText(Long telegramId) {
        String savedUsername = userRepository.findByTelegramId(telegramId).map(AppUser::getUsername).orElse("unknown");
        AppUser user = getInitializedUser(telegramId, savedUsername);

        FileSystemEntry dir = fileRepository.findById(user.getCurrentDirectoryId()).orElseThrow();
        FileSystemEntry userRoot = getOrCreateUserRoot(savedUsername);

        String fullPath = dir.getPath();
        String rootPath = userRoot.getPath();

        // Отрезаем корень (нечувствительно к регистру)
        String relativePath = "";
        if (fullPath.toLowerCase().startsWith(rootPath.toLowerCase())) {
            relativePath = fullPath.substring(rootPath.length());
        }

        if (relativePath.startsWith("/")) relativePath = relativePath.substring(1);

        String rootName = userRoot.getName();

        if (relativePath.isEmpty()) {
            return "📁 <b>" + rootName + "</b>";
        } else {
            String breadcrumbs = Arrays.stream(relativePath.split("/"))
                    .collect(Collectors.joining(" → "));
            return "📁 <b>" + rootName + "</b> → <code>" + breadcrumbs + "</code>";
        }
    }

    @Transactional
    public File processClick(Long telegramId, String data) {
        String savedUsername = userRepository.findByTelegramId(telegramId).map(AppUser::getUsername).orElse("unknown");
        AppUser user = userRepository.findByTelegramId(telegramId).orElseThrow();
        FileSystemEntry userRoot = getOrCreateUserRoot(savedUsername);

        if ("UP".equals(data)) {
            FileSystemEntry current = fileRepository.findById(user.getCurrentDirectoryId()).orElseThrow();

            // Если мы уже в корне
            if (current.getPath().equalsIgnoreCase(userRoot.getPath())) {
                return null;
            }

            // Ищем родителя
            String parentPath = current.getPath().substring(0, current.getPath().lastIndexOf('/'));

            // Если родитель короче корня юзера - не пускаем
            if (parentPath.length() < userRoot.getPath().length()) {
                return null;
            }

            fileRepository.findByPath(parentPath).ifPresent(parent -> {
                user.setCurrentDirectoryId(parent.getId());
                userRepository.save(user);
            });
            return null;
        }

        if (data.startsWith("DIR:")) {
            Long dirId = Long.parseLong(data.split(":")[1]);
            fileRepository.findById(dirId).ifPresent(dir -> {
                user.setCurrentDirectoryId(dirId);
                userRepository.save(user);
            });
            return null;
        }

        if (data.startsWith("FILE:")) {
            Long fileId = Long.parseLong(data.split(":")[1]);
            FileSystemEntry fileEntry = fileRepository.findById(fileId).orElseThrow();
            return new File(fileEntry.getPath());
        }

        return null;
    }

    private InlineKeyboardButton createButton(String text, String callbackData) {
        return InlineKeyboardButton.builder().text(text).callbackData(callbackData).build();
    }

    private String normalizePath(String path) {
        if (path == null) return "";
        String p = path.replace("\\", "/");
        if (p.endsWith("/")) p = p.substring(0, p.length() - 1);
        return p;
    }

    private String getSafeCanonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }

    // Проверка, что child внутри parent (или равен ему)
    private boolean isInside(String childPath, String parentPath) {
        String child = childPath.toLowerCase();
        String parent = parentPath.toLowerCase();
        return child.equals(parent) || child.startsWith(parent + "/");
    }
}