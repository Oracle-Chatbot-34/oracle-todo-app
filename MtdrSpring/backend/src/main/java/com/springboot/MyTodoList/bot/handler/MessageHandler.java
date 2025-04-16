package com.springboot.MyTodoList.bot.handler;

import com.springboot.MyTodoList.bot.keyboard.KeyboardFactory;
import com.springboot.MyTodoList.bot.util.BotLogger;
import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.model.bot.UserBotState;
import com.springboot.MyTodoList.util.BotMessages;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
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
            message.enableHtml(true);

            // Personalized welcome message using the user's name
            String welcomeMessage = "Hello, <b>" + state.getUser().getFullName() + "</b>! " +
                    "\n\nWelcome to the <b>DashMaster Task Management</b> system. " +
                    "Please select an option from the menu below:";
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
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.enableHtml(true);
                message.setText("Your todo list is empty. Add new items using the <b>Create New Task</b> button.");

                // Create keyboard with options for empty task list
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
                return;
            }

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.enableHtml(true);

            StringBuilder messageText = new StringBuilder();
            messageText.append("<b>MY TASK LIST</b>\n\n");

            // Group tasks by status
            messageText.append("<b>Active Tasks:</b>\n");
            boolean hasActiveTasks = false;

            for (ToDoItem task : tasks) {
                if (!task.isDone()) {
                    hasActiveTasks = true;
                    messageText.append("• ID <code>").append(task.getID()).append("</code>: ")
                            .append(task.getTitle());

                    if (task.getPriority() != null) {
                        messageText.append(" [").append(task.getPriority()).append("]");
                    }

                    messageText.append("\n");

                    if (task.getStatus() != null) {
                        messageText.append("  Status: ").append(task.getStatus()).append("\n");
                    }

                    if (task.getEstimatedHours() != null) {
                        messageText.append("  Est. Hours: ").append(task.getEstimatedHours()).append("\n");
                    }

                    messageText.append("\n");
                }
            }

            if (!hasActiveTasks) {
                messageText.append("No active tasks.\n\n");
            }

            messageText.append("<b>Completed Tasks:</b>\n");
            boolean hasCompletedTasks = false;

            for (ToDoItem task : tasks) {
                if (task.isDone()) {
                    hasCompletedTasks = true;
                    messageText.append("• ID <code>").append(task.getID()).append("</code>: ")
                            .append(task.getTitle()).append(" ✅\n");

                    if (task.getActualHours() != null) {
                        messageText.append("  Actual Hours: ").append(task.getActualHours()).append("\n");
                    }

                    messageText.append("\n");
                }
            }

            if (!hasCompletedTasks) {
                messageText.append("No completed tasks.\n");
            }

            message.setText(messageText.toString());

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
            message.enableHtml(true);

            String welcomeMessage = "<b>Task Management for " + state.getUser().getFullName() + "</b>\n\n" +
                    "Select an option:";
            message.setText(welcomeMessage);
            logger.debug(chatId, "Welcome message for developer task menu: {}", welcomeMessage);

            // Create keyboard with task management options based on role
            if (state.getUser().isManager()) {
                message.setReplyMarkup(KeyboardFactory.createManagerTaskMenu());
                logger.debug(chatId, "Created manager task menu keyboard");
            } else if (state.getUser().isDeveloper()) {
                message.setReplyMarkup(KeyboardFactory.createDeveloperTaskMenu());
                logger.debug(chatId, "Created developer task menu keyboard");
            } else {
                message.setReplyMarkup(KeyboardFactory.createEmployeeTaskMenu());
                logger.debug(chatId, "Created employee task menu keyboard");
            }

            bot.execute(message);
            logger.info(chatId, "Task menu successfully shown");
        } catch (TelegramApiException e) {
            logger.error(chatId, "Error showing task menu", e);
            sendErrorMessage(chatId, "There was a problem displaying the task menu. Please try again.", bot);
        }
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
            helpText.append("📋 <b>DashMaster Bot Commands</b>\n\n");
            helpText.append("• <code>/start</code> - Show the main menu\n");
            helpText.append("• <code>/todolist</code> - View your task list\n");
            helpText.append("• <code>/additem</code> - Add a new task\n");
            helpText.append("• <code>/sprint</code> - Access sprint management\n");
            helpText.append("• <code>/hide</code> - Hide the keyboard\n");
            helpText.append("• <code>/help</code> - Show this help message\n\n");
            helpText.append("<b>Task Management:</b>\n");
            helpText.append("• To mark a task as done: <code>[ID]-DONE</code>\n");
            helpText.append("• To undo a task: <code>[ID]-UNDO</code>\n");
            helpText.append("• To delete a task: <code>[ID]-DELETE</code>\n\n");
            helpText.append("You can also use the buttons on the keyboard for easier navigation.");

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(helpText.toString());
            message.enableHtml(true);

            // Add basic keyboard
            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            keyboardMarkup.setResizeKeyboard(true);
            List<KeyboardRow> keyboard = new ArrayList<>();

            KeyboardRow row = new KeyboardRow();
            row.add("🏠 Main Menu");
            keyboard.add(row);

            keyboardMarkup.setKeyboard(keyboard);
            message.setReplyMarkup(keyboardMarkup);

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
            message.enableHtml(true);
            bot.execute(message);
            logger.info(chatId, "Message successfully sent");
        } catch (TelegramApiException e) {
            logger.error(chatId, "Failed to send message", e);
        }
    }

    /**
     * Send interactive message with inline keyboard
     */
    public static void sendInlineKeyboardMessage(long chatId, String text, InlineKeyboardMarkup keyboard,
            TelegramLongPollingBot bot) {
        logger.info(chatId, "Sending inline keyboard message: {}", text);
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(text);
            message.enableHtml(true);
            message.setReplyMarkup(keyboard);
            bot.execute(message);
            logger.info(chatId, "Inline keyboard message successfully sent");
        } catch (TelegramApiException e) {
            logger.error(chatId, "Failed to send inline keyboard message", e);
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
        message.enableHtml(true);

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
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.enableHtml(true);

            if (tasks.isEmpty()) {
                message.setText("You don't have any active tasks at the moment.");

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
                tasksText.append("<b>ID:</b> <code>").append(task.getID()).append("</code>\n");
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

            message.setText(tasksText.toString());

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

    /**
     * Show success message with animation
     */
    public static void showSuccessMessage(long chatId, String message, TelegramLongPollingBot bot) {
        logger.info(chatId, "Showing success message: {}", message);
        try {
            // Show animation frames
            String[] frames = { "⬜⬜⬜", "⬛⬜⬜", "⬛⬛⬜", "⬛⬛⬛", "✅" };

            SendMessage initialMessage = new SendMessage();
            initialMessage.setChatId(chatId);
            initialMessage.setText("Processing...\n\n" + frames[0]);
            initialMessage.enableHtml(true);

            org.telegram.telegrambots.meta.api.objects.Message sentMessage = bot.execute(initialMessage);
            int messageId = sentMessage.getMessageId();

            // Create animation
            for (int i = 1; i < frames.length; i++) {
                try {
                    org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText editMessage = new org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText();
                    editMessage.setChatId(chatId);
                    editMessage.setMessageId(messageId);
                    editMessage.setText("Processing...\n\n" + frames[i]);
                    editMessage.enableHtml(true);

                    bot.execute(editMessage);
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // Show final success message
            org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText finalMessage = new org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText();
            finalMessage.setChatId(chatId);
            finalMessage.setMessageId(messageId);
            finalMessage.setText("✅ " + message);
            finalMessage.enableHtml(true);

            bot.execute(finalMessage);
            logger.info(chatId, "Success message animation completed");
        } catch (TelegramApiException e) {
            logger.error(chatId, "Error showing success message animation", e);
            // Fallback to simple message
            sendMessage(chatId, "✅ " + message, bot);
        }
    }
}