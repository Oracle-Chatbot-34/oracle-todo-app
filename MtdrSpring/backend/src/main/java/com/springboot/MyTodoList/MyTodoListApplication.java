package com.springboot.MyTodoList;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import com.springboot.MyTodoList.controller.ToDoItemBotController;
import com.springboot.MyTodoList.service.SprintService;
import com.springboot.MyTodoList.service.ToDoItemService;
import com.springboot.MyTodoList.service.UserService;
import com.springboot.MyTodoList.util.BotMessages;

@SpringBootApplication
public class MyTodoListApplication implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(MyTodoListApplication.class);

    @Autowired
    private ToDoItemService toDoItemService;

    @Autowired
    private UserService userService;

    @Autowired
    private SprintService sprintService;

    @Value("${telegram.bot.token:disabled}")
    private String telegramBotToken;

    @Value("${telegram.bot.name:DashMasterBot}")
    private String botName;

    public static void main(String[] args) {
        SpringApplication.run(MyTodoListApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        boolean skipBot = Arrays.asList(args).contains("--skip-bot");
        
        // Verify Telegram bot configuration
        if (telegramBotToken == null || telegramBotToken.isEmpty()) {
            logger.error("Telegram bot token is not set. Please set the TELEGRAM_BOT_TOKEN environment variable.");
            throw new IllegalStateException(
                    "Telegram bot token is required. Please set the TELEGRAM_BOT_TOKEN environment variable.");
        }

        if (skipBot == true) {
            logger.warn("Skipping Telegram bot registration as per configuration.");
            return;
        } else {
            try {
                TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
                telegramBotsApi.registerBot(new ToDoItemBotController(
                        telegramBotToken,
                        botName,
                        toDoItemService,
                        userService,
                        sprintService));
                logger.info(BotMessages.BOT_REGISTERED_STARTED.getMessage());
            } catch (TelegramApiException e) {
                logger.error("Failed to start Telegram bot", e);
                logger.warn("Application will continue without Telegram bot functionality.");
            }
        }
    }
}