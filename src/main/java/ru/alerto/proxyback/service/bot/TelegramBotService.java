package ru.alerto.proxyback.service.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.alerto.proxyback.entity.AppUser;
import ru.alerto.proxyback.repository.AppUserRepository;
import ru.alerto.proxyback.service.BrowserService;

import java.io.File;
import java.time.LocalDateTime;

@Slf4j
@Component
public class TelegramBotService implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final TelegramClient telegramClient;
    private final String botToken;
    private final AppUserRepository userRepository;
    private final BrowserService browserService;

    public TelegramBotService(@Value("${telegram.bot.token}") String botToken,
                              AppUserRepository userRepository,
                              BrowserService browserService) {
        this.botToken = botToken;
        this.userRepository = userRepository;
        this.browserService = browserService;
        this.telegramClient = new OkHttpTelegramClient(botToken);
    }

    @Override
    public String getBotToken() { return botToken; }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() { return this; }

    @Override
    public void consume(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                Long chatId = update.getMessage().getChatId();
                if ("/start".equals(update.getMessage().getText())) {
                    registerUser(update.getMessage().getFrom());
                    sendNavigationMenu(chatId, "Добро пожаловать в файловую систему!");
                }
            } else if (update.hasCallbackQuery()) {
                String callbackData = update.getCallbackQuery().getData();
                Long chatId = update.getCallbackQuery().getMessage().getChatId();
                Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
                Long userId = update.getCallbackQuery().getFrom().getId();

                File fileToSend = browserService.processClick(userId, callbackData);

                if (fileToSend != null) {
                    sendFile(chatId, fileToSend);
                } else {
                    updateNavigationMenu(chatId, messageId, userId);
                }
            }
        } catch (Exception e) {
            log.error("Global error in bot consumer", e);
        }
    }

    private void sendNavigationMenu(Long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text + "\n" + browserService.getCurrentPathText(chatId))
                .parseMode("HTML") // <--- ВКЛЮЧАЕМ HTML
                .replyMarkup(browserService.getKeyboardForCurrentDir(chatId))
                .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send message", e);
        }
    }

    private void updateNavigationMenu(Long chatId, Integer messageId, Long userId) {
        EditMessageText message = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(browserService.getCurrentPathText(userId))
                .parseMode("HTML") // <--- ВКЛЮЧАЕМ HTML
                .replyMarkup(browserService.getKeyboardForCurrentDir(userId))
                .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to edit message", e);
        }
    }

    private void sendFile(Long chatId, File file) {
        SendDocument doc = SendDocument.builder()
                .chatId(chatId)
                .document(new InputFile(file))
                .build();
        try {
            telegramClient.execute(doc);
        } catch (TelegramApiException e) {
            log.error("Failed to send file", e);
        }
    }

    private void registerUser(org.telegram.telegrambots.meta.api.objects.User tgUser) {
        if (!userRepository.existsByTelegramId(tgUser.getId())) {
            AppUser newUser = AppUser.builder()
                    .telegramId(tgUser.getId())
                    .username(tgUser.getUserName())
                    .lastSeen(LocalDateTime.now())
                    .build();
            userRepository.save(newUser);
        }
    }
}