package com.springboot.MyTodoList.bot.config;

import com.springboot.MyTodoList.bot.controller.ToDoItemBotController;
import com.springboot.MyTodoList.service.SprintService;
import com.springboot.MyTodoList.service.ToDoItemService;
import com.springboot.MyTodoList.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class TelegramBotConfig {
    private static final Logger logger = LoggerFactory.getLogger(TelegramBotConfig.class);

    @Value("${telegram.bot.token:disabled}")
    private String telegramBotToken;

    @Value("${telegram.bot.name:DashMasterBot}")
    private String botName;

    @Bean
    public TelegramBotsApi telegramBotsApi(
            ToDoItemService toDoItemService,
            UserService userService,
            SprintService sprintService) throws TelegramApiException {

        // Verify Telegram bot configuration
        if (telegramBotToken == null || telegramBotToken.isEmpty() || "disabled".equals(telegramBotToken)) {
            logger.warn("Telegram bot token is not set. Bot functionality will be disabled.");
            return null;
        }

        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);

        ToDoItemBotController bot = new ToDoItemBotController(
                telegramBotToken,
                botName,
                toDoItemService,
                userService,
                sprintService);

        telegramBotsApi.registerBot(bot);
        logger.info("Telegram bot registered and started successfully!");

        return telegramBotsApi;
    }
}