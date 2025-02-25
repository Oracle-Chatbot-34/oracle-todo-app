package com.springboot.MyTodoList;

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
import com.springboot.MyTodoList.service.ToDoItemService;
import com.springboot.MyTodoList.util.BotMessages;

@SpringBootApplication
public class MyTodoListApplication implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(MyTodoListApplication.class);

	@Autowired
	private ToDoItemService toDoItemService;

	@Value("${telegram.bot.token:disabled}")
	private String telegramBotToken;

	@Value("${telegram.bot.name:disabled}")
	private String botName;

	public static void main(String[] args) {
		SpringApplication.run(MyTodoListApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		// Only start the Telegram bot if not disabled
		if (!"disabled".equals(telegramBotToken) && !"disabled".equals(botName)) {
			try {
				TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
				telegramBotsApi.registerBot(new ToDoItemBotController(telegramBotToken, botName, toDoItemService));
				logger.info(BotMessages.BOT_REGISTERED_STARTED.getMessage());
			} catch (TelegramApiException e) {
				logger.error("Failed to start Telegram bot", e);
			}
		} else {
			logger.info("Telegram bot is disabled");
		}
	}
}
