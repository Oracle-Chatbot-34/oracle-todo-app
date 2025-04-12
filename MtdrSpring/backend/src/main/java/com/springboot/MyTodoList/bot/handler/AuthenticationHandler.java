package com.springboot.MyTodoList.bot.handler;

import com.springboot.MyTodoList.bot.service.BotService;
import com.springboot.MyTodoList.bot.util.BotLogger;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.model.bot.UserBotState;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Optional;

/**
 * Handler for user authentication with the Telegram bot
 */
public class AuthenticationHandler {
    private final BotLogger logger = new BotLogger(AuthenticationHandler.class);
    private final BotService botService;
    private final TelegramLongPollingBot bot;

    public AuthenticationHandler(BotService botService, TelegramLongPollingBot bot) {
        this.botService = botService;
        this.bot = bot;
    }

    /**
     * Handle authentication via employee ID
     */
    public void handleAuthentication(long chatId, String employeeId, UserBotState state) {
        logger.info(chatId, "Handling authentication for employee ID: {}", employeeId);
        try {
            // First, check if this Telegram user is already registered
            Optional<User> userByTelegramId = botService.findUserByTelegramId(chatId);
            logger.debug(chatId, "User lookup by Telegram ID: present={}", userByTelegramId.isPresent());

            if (userByTelegramId.isPresent()) {
                // User already registered with this Telegram ID
                handleExistingUser(chatId, userByTelegramId.get(), state);
            } else {
                // Try to find user by employee ID
                handleNewAuthentication(chatId, employeeId, state);
            }
        } catch (Exception e) {
            logger.error(chatId, "Unexpected error during authentication", e);
            sendErrorMessage(chatId, "Authentication system is currently unavailable. Please try again later.");
        }
    }

    /**
     * Handle existing user (already authenticated by Telegram ID)
     */
    private void handleExistingUser(long chatId, User user, UserBotState state) {
        logger.info(chatId, "User already registered: {}, {}, role={}", user.getFullName(), user.getEmployeeId(),
                user.getRole());
        state.setAuthenticated(true);
        state.setUser(user);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Welcome back, " + user.getFullName() + "!");
        message.enableHtml(true);

        try {
            bot.execute(message);
            logger.info(chatId, "Welcome back message sent");
            MessageHandler.showMainScreen(chatId, state, bot);
        } catch (TelegramApiException e) {
            logger.error(chatId, "Error sending welcome back message", e);
            sendErrorMessage(chatId,
                    "Authentication successful, but there was an error displaying the menu. Please type /start to continue.");
        }
    }

    /**
     * Handle new authentication via employee ID
     */
    private void handleNewAuthentication(long chatId, String employeeId, UserBotState state) {
        logger.debug(chatId, "Looking up user by employee ID: {}", employeeId);
        Optional<User> userOpt = botService.findUserByEmployeeId(employeeId);
        logger.debug(chatId, "User lookup by employee ID: present={}", userOpt.isPresent());

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            logger.info(chatId, "User found: {}, role: employee={}, developer={}, manager={}",
                    user.getFullName(), user.isEmployee(), user.isDeveloper(), user.isManager());

            // Print detailed role information for debugging
            logger.debug(chatId, "User role details - ID: {}, Username: {}, Role: {}, isManager(): {}",
                    user.getId(), user.getUsername(), user.getRole(), user.isManager());

            // Associate this Telegram ID with the user
            logger.debug(chatId, "Associating Telegram ID with user {}", user.getFullName());
            try {
                user = botService.updateUserTelegramId(user, chatId);
                logger.info(chatId, "Telegram ID successfully associated with user {}", user.getFullName());
            } catch (Exception e) {
                logger.error(chatId, "Failed to update user's Telegram ID", e);
                sendErrorMessage(chatId, "Authentication failed. Please try again later.");
                return;
            }

            // Set authenticated state
            state.setAuthenticated(true);
            state.setUser(user);
            logger.debug(chatId, "User state updated: authenticated=true, role={}", user.getRole());

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Authentication successful! Welcome, " + user.getFullName()
                    + ". Your Telegram account is now linked to your DashMaster profile."
                    + "\n\nYou are logged in as: <b>" + user.getRole() + "</b>");
            message.enableHtml(true);

            try {
                bot.execute(message);
                logger.info(chatId, "Authentication success message sent");
                MessageHandler.showMainScreen(chatId, state, bot);
            } catch (TelegramApiException e) {
                logger.error(chatId, "Error sending authentication success message", e);
                sendErrorMessage(chatId,
                        "Authentication successful, but there was an error displaying the menu. Please type /start to continue.");
            }
        } else {
            logger.warn(chatId, "Authentication failed - user not found or not authorized");
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Authentication failed. Please enter a valid Employee ID:");

            try {
                bot.execute(message);
                logger.info(chatId, "Authentication failure message sent");
            } catch (TelegramApiException e) {
                logger.error(chatId, "Error sending authentication failure message", e);
                sendErrorMessage(chatId, "Communication error. Please try again.");
            }
        }
    }

    /**
     * Send error message to user
     */
    private void sendErrorMessage(long chatId, String errorText) {
        logger.error(chatId, "Sending error message: {}", errorText);
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("❌ " + errorText);

        try {
            bot.execute(message);
            logger.info(chatId, "Error message successfully sent");
        } catch (TelegramApiException e) {
            logger.error(chatId, "Failed to send error message", e);
        }
    }

    /**
     * Handle the initial greeting for new users
     */
    public void handleInitialGreeting(long chatId) {
        logger.info(chatId, "Sending initial greeting");
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.enableHtml(true);

            StringBuilder msgText = new StringBuilder();
            msgText.append("<b>Welcome to the DashMaster Task Management Bot!</b>\n\n");
            msgText.append("This bot helps you manage your tasks and sprints with the DashMaster system.\n\n");
            msgText.append("To get started, please provide your employee ID for authentication.");

            message.setText(msgText.toString());

            bot.execute(message);
            logger.info(chatId, "Initial greeting sent successfully");
        } catch (Exception e) {
            logger.error(chatId, "Error sending initial greeting", e);
            MessageHandler.sendErrorMessage(chatId, "Failed to send welcome message. Please try again.", bot);
        }
    }
}