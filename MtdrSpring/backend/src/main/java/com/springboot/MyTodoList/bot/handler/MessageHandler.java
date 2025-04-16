package com.springboot.MyTodoList.bot.handler;

import com.springboot.MyTodoList.bot.keyboard.KeyboardFactory;
import com.springboot.MyTodoList.bot.util.BotLogger;
import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.model.bot.UserBotState;
import com.springboot.MyTodoList.util.BotMessages;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler for general message sending and UI display
 */
public class MessageHandler {
    private static final BotLogger logger = new BotLogger(MessageHandler.class);

    /**
     * Shows the main menu screen to the user
     */
    public static void showMainScreen(long chatId, UserBotState state, TelegramLongPollingBot bot) {
        logger.info(chatId, "Showing main screen to user: {}", state.getUser().getFullName());
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);

            // Personalized welcome message using the user's name
            String welcomeMessage = "Hello, " + state.getUser().getFullName() + "! " +
                    BotMessages.HELLO_MYTODO_BOT.getMessage();
            message.setText(welcomeMessage);
            logger.debug(chatId, "Welcome message: {}", welcomeMessage);

            // Create keyboard with options based on user role
            ReplyKeyboardMarkup keyboardMarkup = KeyboardFactory.createMainMenuKeyboard(state.getUser());
            message.setReplyMarkup(keyboardMarkup);
            logger.debug(chatId, "Main screen keyboard created");

            bot.execute(message);
            logger.info(chatId, "Main screen successfully shown");
        } catch (TelegramApiException e) {
            logger.error(chatId, "Error showing main screen", e);
            sendErrorMessage(chatId,
                    "There was a problem displaying the main menu. Please try again by typing /start.", bot);
        }
    }

    /**
     * Shows the task list for a user
     */
    public static void showTaskList(long chatId, List<ToDoItem> tasks, UserBotState state, TelegramLongPollingBot bot) {
        logger.info(chatId, "Showing task list with {} tasks", tasks.size());
        try {
            if (tasks.isEmpty()) {
                logger.info(chatId, "No tasks found");
                sendMessage(chatId, "Your todo list is empty. Add new items using the 'Add New Task' button.", bot);
                return;
            }

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("MY TASK LIST");

            // Create keyboard with tasks
            ReplyKeyboardMarkup keyboardMarkup = KeyboardFactory.createTaskListKeyboard(tasks);
            message.setReplyMarkup(keyboardMarkup);
            logger.debug(chatId, "Task list keyboard created with {} tasks", tasks.size());

            bot.execute(message);
            logger.info(chatId, "Task list successfully displayed");
        } catch (Exception e) {
            logger.error(chatId, "Error displaying task list", e);
            sendErrorMessage(chatId, "Failed to load your todo list. Please try again later.", bot);
        }
    }

    /**
     * Show the developer task menu
     */
    public static void showDeveloperTaskMenu(long chatId, UserBotState state, TelegramLongPollingBot bot) {
        logger.info(chatId, "Showing developer task menu for user: {}", state.getUser().getFullName());
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);

            String welcomeMessage = "Task Management for " + state.getUser().getFullName() + "\n" +
                    "Select an option:";
            message.setText(welcomeMessage);
            logger.debug(chatId, "Welcome message for developer task menu: {}", welcomeMessage);

            // Create keyboard with task management options
            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            message.setReplyMarkup(keyboardMarkup);

            // Create task management keyboard by role
            if (state.getUser().isManager()) {
                message.setReplyMarkup(createManagerTaskMenu());
            } else {
                message.setReplyMarkup(createDeveloperTaskMenu());
            }

            bot.execute(message);
            logger.info(chatId, "Developer task menu successfully shown");
        } catch (TelegramApiException e) {
            logger.error(chatId, "Error showing developer task menu", e);
            sendErrorMessage(chatId, "There was a problem displaying the task menu. Please try again.", bot);
        }
    }

    /**
     * Create task management menu for developers
     */
    private static ReplyKeyboardMarkup createDeveloperTaskMenu() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        return keyboardMarkup;
    }

    /**
     * Create task management menu for managers
     */
    private static ReplyKeyboardMarkup createManagerTaskMenu() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        return keyboardMarkup;
    }

    /**
     * Remove the keyboard
     */
    public static void hideKeyboard(long chatId, TelegramLongPollingBot bot) {
        logger.info(chatId, "Hiding keyboard");
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(BotMessages.BYE.getMessage());

            ReplyKeyboardRemove keyboardRemove = new ReplyKeyboardRemove(true);
            message.setReplyMarkup(keyboardRemove);

            bot.execute(message);
            logger.info(chatId, "Keyboard hidden successfully");
        } catch (TelegramApiException e) {
            logger.error(chatId, "Error hiding keyboard", e);
        }
    }

    /**
     * Show help information
     */
    public static void showHelpInformation(long chatId, TelegramLongPollingBot bot) {
        logger.info(chatId, "Showing help information");
        try {
            StringBuilder helpText = new StringBuilder();
            helpText.append("📋 *DashMaster Bot Commands*\n\n");
            helpText.append("• */start* - Show the main menu\n");
            helpText.append("• */todolist* - View your task list\n");
            helpText.append("• */additem* - Add a new task\n");
            helpText.append("• */hide* - Hide the keyboard\n");
            helpText.append("• */help* - Show this help message\n\n");
            helpText.append("You can also use the buttons on the keyboard for easier navigation.");

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(helpText.toString());
            message.enableMarkdown(true);

            bot.execute(message);
            logger.info(chatId, "Help information sent successfully");
        } catch (TelegramApiException e) {
            logger.error(chatId, "Error sending help information", e);
            sendErrorMessage(chatId, "Failed to send help information. Please try again later.", bot);
        }
    }

    /**
     * Send regular message to user
     */
    public static void sendMessage(long chatId, String text, TelegramLongPollingBot bot) {
        logger.info(chatId, "Sending message: {}", text);
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(text);
            bot.execute(message);
            logger.info(chatId, "Message successfully sent");
        } catch (TelegramApiException e) {
            logger.error(chatId, "Failed to send message", e);
        }
    }

    /**
     * Send error message to user
     */
    public static void sendErrorMessage(long chatId, String errorText, TelegramLongPollingBot bot) {
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
     * Show active tasks list with proper keyboard
     */
    public static void showActiveTasksList(long chatId, List<ToDoItem> tasks, UserBotState state,
            TelegramLongPollingBot bot) {
        logger.info(chatId, "Displaying active tasks list, count: {}", tasks.size());
        try {
            if (tasks.isEmpty()) {
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("You don't have any active tasks at the moment.");
                message.enableHtml(true);

                // Create keyboard with options
                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                keyboardMarkup.setResizeKeyboard(true);
                List<KeyboardRow> keyboard = new ArrayList<>();

                KeyboardRow row1 = new KeyboardRow();
                row1.add("📝 Create New Task");
                row1.add("🏃‍♂️ Sprint Management");
                keyboard.add(row1);

                KeyboardRow row2 = new KeyboardRow();
                row2.add("🏠 Main Menu");
                keyboard.add(row2);

                keyboardMarkup.setKeyboard(keyboard);
                message.setReplyMarkup(keyboardMarkup);

                bot.execute(message);
                logger.info(chatId, "No active tasks message sent");
                return;
            }

            StringBuilder tasksText = new StringBuilder();
            tasksText.append("<b>Your Active Tasks:</b>\n\n");

            for (ToDoItem task : tasks) {
                tasksText.append("<b>ID:</b> ").append(task.getID()).append("\n");
                tasksText.append("<b>Title:</b> ").append(task.getTitle()).append("\n");

                if (task.getStatus() != null) {
                    tasksText.append("<b>Status:</b> ").append(task.getStatus()).append("\n");
                }

                if (task.getEstimatedHours() != null) {
                    tasksText.append("<b>Estimated Hours:</b> ").append(task.getEstimatedHours()).append("\n");
                }

                if (task.getPriority() != null) {
                    tasksText.append("<b>Priority:</b> ").append(task.getPriority()).append("\n");
                }

                // Show sprint info if task is in a sprint
                if (task.getSprintId() != null) {
                    tasksText.append("<b>Sprint:</b> ID ").append(task.getSprintId()).append("\n");
                }

                tasksText.append("\n");
            }

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(tasksText.toString());
            message.enableHtml(true);

            // Create keyboard with options for task management
            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            keyboardMarkup.setResizeKeyboard(true);
            List<KeyboardRow> keyboard = new ArrayList<>();

            KeyboardRow row1 = new KeyboardRow();
            row1.add("✅ Mark Task Complete");
            row1.add("📝 Create New Task");
            keyboard.add(row1);

            KeyboardRow row2 = new KeyboardRow();
            row2.add("🏃‍♂️ Sprint Management");
            row2.add("📝 List All Tasks");
            keyboard.add(row2);

            KeyboardRow row3 = new KeyboardRow();
            row3.add("🏠 Main Menu");
            keyboard.add(row3);

            keyboardMarkup.setKeyboard(keyboard);
            message.setReplyMarkup(keyboardMarkup);

            bot.execute(message);
            logger.info(chatId, "Active tasks list sent successfully");
        } catch (Exception e) {
            logger.error(chatId, "Error showing active tasks list", e);
            sendErrorMessage(chatId, "Failed to display active tasks. Please try again later.", bot);
        }
    }
}