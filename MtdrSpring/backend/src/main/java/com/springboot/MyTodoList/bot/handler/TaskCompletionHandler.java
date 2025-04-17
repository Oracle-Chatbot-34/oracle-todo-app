package com.springboot.MyTodoList.bot.handler;

import com.springboot.MyTodoList.bot.keyboard.KeyboardFactory;
import com.springboot.MyTodoList.bot.service.BotService;
import com.springboot.MyTodoList.bot.util.BotLogger;
import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.model.bot.UserBotState;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Handler for task completion workflow
 */
public class TaskCompletionHandler {
    private final BotLogger logger = new BotLogger(TaskCompletionHandler.class);
    private final BotService botService;
    private final TelegramLongPollingBot bot;

    // Animation frames for loading indicators
    private static final String[] LOADING_FRAMES = { "⬜⬜⬜", "⬛⬜⬜", "⬛⬛⬜", "⬛⬛⬛", "✅" };

    public TaskCompletionHandler(BotService botService, TelegramLongPollingBot bot) {
        this.botService = botService;
        this.bot = bot;
    }

    /**
     * Start the task completion flow
     */
    public void startTaskCompletion(long chatId, UserBotState state) {
        logger.info(chatId, "Starting task completion flow for user: {}", state.getUser().getFullName());
        try {
            // Show loading animation
            SendMessage loadingMessage = new SendMessage();
            loadingMessage.setChatId(chatId);
            loadingMessage.setText("Fetching your tasks...\n⬜⬜⬜");
            loadingMessage.enableHtml(true);

            Message sentMessage = bot.execute(loadingMessage);

            // Animation frames
            for (int i = 1; i < LOADING_FRAMES.length - 1; i++) {
                Thread.sleep(300);
                EditMessageText updateMessage = new EditMessageText();
                updateMessage.setChatId(chatId);
                updateMessage.setMessageId(sentMessage.getMessageId());
                updateMessage.setText("Fetching your tasks...\n" + LOADING_FRAMES[i]);
                updateMessage.enableHtml(true);
                bot.execute(updateMessage);
            }

            logger.debug(chatId, "Fetching active tasks for user ID: {}", state.getUser().getId());
            List<ToDoItem> activeTasks = botService.findActiveTasksByAssigneeId(state.getUser().getId());
            logger.debug(chatId, "Found {} active tasks for user", activeTasks.size());

            if (activeTasks.isEmpty()) {
                logger.info(chatId, "No active tasks found for user");
                EditMessageText noTasksMessage = new EditMessageText();
                noTasksMessage.setChatId(chatId);
                noTasksMessage.setMessageId(sentMessage.getMessageId());
                noTasksMessage.setText("✅ You don't have any active tasks to complete.");
                bot.execute(noTasksMessage);
                return;
            }

            state.setTaskCompletionMode(true);
            state.setTaskCompletionStage("SELECT_TASK");
            logger.debug(chatId, "Set user state: taskCompletionMode=true, taskCompletionStage=SELECT_TASK");

            // Create task selection message with inline keyboard
            StringBuilder messageText = new StringBuilder();
            messageText.append("<b>Select a Task to Complete</b>\n\n");
            messageText.append("Please select one of your active tasks to mark as complete:");

            // Create inline keyboard with task options
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            // Add each task as a button
            for (ToDoItem task : activeTasks) {
                List<InlineKeyboardButton> row = new ArrayList<>();
                InlineKeyboardButton taskButton = new InlineKeyboardButton();

                String taskLabel = "ID " + task.getID() + ": " + task.getTitle();
                if (taskLabel.length() > 30) {
                    taskLabel = taskLabel.substring(0, 27) + "...";
                }

                taskButton.setText(taskLabel);
                taskButton.setCallbackData("task_complete_" + task.getID());
                row.add(taskButton);
                rows.add(row);
            }

            // Add cancel button
            List<InlineKeyboardButton> cancelRow = new ArrayList<>();
            InlineKeyboardButton cancelButton = new InlineKeyboardButton();
            cancelButton.setText("❌ Cancel");
            cancelButton.setCallbackData("task_back_to_menu");
            cancelRow.add(cancelButton);
            rows.add(cancelRow);

            markup.setKeyboard(rows);

            EditMessageText taskSelectionMessage = new EditMessageText();
            taskSelectionMessage.setChatId(chatId);
            taskSelectionMessage.setMessageId(sentMessage.getMessageId());
            taskSelectionMessage.setText(messageText.toString());
            taskSelectionMessage.enableHtml(true);
            taskSelectionMessage.setReplyMarkup(markup);

            bot.execute(taskSelectionMessage);
            logger.info(chatId, "Task selection prompt sent with {} tasks", activeTasks.size());
        } catch (Exception e) {
            logger.error(chatId, "Error starting task completion", e);
            MessageHandler.sendErrorMessage(chatId,
                    "There was an error starting the task completion process. Please try again later.", bot);
            state.resetTaskCompletion();
        }
    }

    /**
     * Process task completion stages
     */
    public void processTaskCompletion(long chatId, String messageText, UserBotState state) {
        logger.info(chatId, "Processing task completion, stage: {}", state.getTaskCompletionStage());
        logger.debug(chatId, "Task completion input: '{}'", messageText);
        try {
            String stage = state.getTaskCompletionStage();

            switch (stage) {
                case "SELECT_TASK":
                    processTaskSelection(chatId, messageText, state);
                    break;
                case "ACTUAL_HOURS":
                    processActualHours(chatId, messageText, state);
                    break;
                case "COMMENTS":
                    processComments(chatId, messageText, state);
                    break;
                default:
                    logger.warn(chatId, "Unknown task completion stage: {}", stage);
                    MessageHandler.sendErrorMessage(chatId,
                            "An error occurred in the task completion process. Please try again.", bot);
                    state.resetTaskCompletion();
            }
        } catch (Exception e) {
            logger.error(chatId, "Error in task completion process", e);
            MessageHandler.sendErrorMessage(chatId,
                    "There was an error in the task completion process. Please try again.", bot);

            // Reset task completion state
            state.resetTaskCompletion();
        }
    }

    /**
     * Process task selection
     */
    private void processTaskSelection(long chatId, String messageText, UserBotState state) {
        if (messageText.equals("Cancel")) {
            logger.info(chatId, "User cancelled task completion");
            state.resetTaskCompletion();
            logger.debug(chatId, "Reset task completion state");
            MessageHandler.showDeveloperTaskMenu(chatId, state, bot);
            return;
        }

        // Extract task ID from message
        int taskId;
        if (messageText.startsWith("ID: ")) {
            // Parse from "ID: X - Title"
            String idPart = messageText.substring(4, messageText.indexOf(" - "));
            try {
                taskId = Integer.parseInt(idPart);
                logger.debug(chatId, "Extracted task ID {} from selection", taskId);
            } catch (NumberFormatException e) {
                logger.warn(chatId, "Invalid task ID format: '{}'", idPart);
                MessageHandler.sendErrorMessage(chatId, "Invalid task ID format. Please select a valid task.", bot);
                return;
            }
        } else {
            // Try to parse as a direct ID
            try {
                taskId = Integer.parseInt(messageText);
                logger.debug(chatId, "Parsed task ID {} directly from input", taskId);
            } catch (NumberFormatException e) {
                logger.warn(chatId, "Invalid task ID format: '{}'", messageText);
                MessageHandler.sendErrorMessage(chatId, "Please enter a valid task ID or select from the list.", bot);
                return;
            }
        }

        // Check if task exists and belongs to user
        logger.debug(chatId, "Fetching task with ID {} for validation", taskId);
        Optional<ToDoItem> taskOpt = botService.getToDoItemById(taskId);
        if (taskOpt.isEmpty()) {
            logger.warn(chatId, "Task not found with ID {}", taskId);
            MessageHandler.sendErrorMessage(chatId, "Task not found. Please enter a valid task ID.", bot);
            return;
        }

        ToDoItem task = taskOpt.get();
        if (!task.getAssigneeId().equals(state.getUser().getId())) {
            logger.warn(chatId, "Task {} is not assigned to user {}", taskId, state.getUser().getId());
            MessageHandler.sendErrorMessage(chatId, "This task is not assigned to you. Please enter a valid task ID.",
                    bot);
            return;
        }

        // Store task ID and move to actual hours stage
        state.setTempTaskId(taskId);
        state.setTaskCompletionStage("ACTUAL_HOURS");
        logger.debug(chatId, "Updated stage to ACTUAL_HOURS, stored task ID: {}", taskId);

        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Please enter the actual hours spent on this task <b>" + task.getTitle() + "</b>:");
            message.enableHtml(true);

            // Hide keyboard
            message.setReplyMarkup(KeyboardFactory.createEmptyKeyboard());
            logger.debug(chatId, "Created actual hours prompt with keyboard removed");

            bot.execute(message);
            logger.info(chatId, "Actual hours prompt sent");
        } catch (TelegramApiException e) {
            logger.error(chatId, "Error sending actual hours prompt", e);
            throw new RuntimeException("Failed to send message", e);
        }
    }

    /**
     * Process actual hours
     */
    private void processActualHours(long chatId, String messageText, UserBotState state) {
        try {
            logger.debug(chatId, "Parsing actual hours from input: '{}'", messageText);
            double actualHours = Double.parseDouble(messageText);
            logger.debug(chatId, "Parsed actual hours: {}", actualHours);

            if (actualHours <= 0) {
                logger.warn(chatId, "Invalid actual hours (<=0) entered: {}", actualHours);
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Actual hours must be greater than 0. Please enter a valid number:");
                bot.execute(message);
                return;
            }

            // Store actual hours and ask for comments
            state.setTempActualHours(actualHours);
            state.setTaskCompletionStage("COMMENTS");
            logger.debug(chatId, "Updated stage to COMMENTS, stored actual hours: {}", actualHours);

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Please enter any comments or notes about the completed task (or type 'skip' to skip):");
            bot.execute(message);
            logger.info(chatId, "Comments prompt sent");
        } catch (NumberFormatException e) {
            logger.warn(chatId, "Invalid actual hours format entered: '{}'", messageText);
            try {
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Please enter a valid number for actual hours:");
                bot.execute(message);
            } catch (TelegramApiException ex) {
                logger.error(chatId, "Error sending format error message", ex);
            }
        } catch (TelegramApiException e) {
            logger.error(chatId, "Error sending comments prompt", e);
            throw new RuntimeException("Failed to send message", e);
        }
    }

    /**
     * Process comments and complete the task
     */
    private void processComments(long chatId, String messageText, UserBotState state) {
        // Store comments and complete task
        String comments = messageText.equals("skip") ? "" : messageText;
        logger.debug(chatId, "Processing comments: '{}'", comments);

        try {
            // Show loading animation
            SendMessage loadingMessage = new SendMessage();
            loadingMessage.setChatId(chatId);
            loadingMessage.setText("Completing task...\n⬜⬜⬜");
            loadingMessage.enableHtml(true);

            Message sentMessage = bot.execute(loadingMessage);

            // Animation frames
            for (int i = 1; i < LOADING_FRAMES.length - 1; i++) {
                Thread.sleep(300);
                EditMessageText updateMessage = new EditMessageText();
                updateMessage.setChatId(chatId);
                updateMessage.setMessageId(sentMessage.getMessageId());
                updateMessage.setText("Completing task...\n" + LOADING_FRAMES[i]);
                updateMessage.enableHtml(true);
                bot.execute(updateMessage);
            }

            // Complete the task
            logger.debug(chatId, "Completing task ID {} with actual hours {} and comments",
                    state.getTempTaskId(), state.getTempActualHours());
            ToDoItem task = botService.completeTask(state.getTempTaskId(), state.getTempActualHours(), comments);
            logger.info(chatId, "Task ID {} successfully completed", task.getID());

            // Reset state
            state.resetTaskCompletion();
            logger.debug(chatId, "Reset task completion state");

            // Show success message
            EditMessageText successMessage = new EditMessageText();
            successMessage.setChatId(chatId);
            successMessage.setMessageId(sentMessage.getMessageId());
            successMessage.setText("✅ Task " + task.getID() + " marked as completed successfully!" +
                    "\n\nTitle: " + task.getTitle() +
                    "\nActual Hours: " + task.getActualHours());
            successMessage.enableHtml(true);

            bot.execute(successMessage);
            logger.info(chatId, "Task completion success message sent");

            // Show updated task list after a short delay
            Thread.sleep(1000);
            MessageHandler.showActiveTasksList(chatId, botService.getActiveToDoItems(state.getUser().getId()), state,
                    bot);
        } catch (Exception e) {
            logger.error(chatId, "Error completing task", e);
            MessageHandler.sendErrorMessage(chatId, "Failed to complete the task. Please try again later.", bot);
            state.resetTaskCompletion();
        }
    }

    /**
     * Handle task status updates (mark as done/undone/delete)
     */
    public void handleTaskStatusUpdate(long chatId, UserBotState state, String command, int taskId) {
        logger.info(chatId, "Handling task status update: {} for task ID: {}", command, taskId);
        try {
            // Show loading animation
            SendMessage loadingMessage = new SendMessage();
            loadingMessage.setChatId(chatId);
            loadingMessage.setText("Processing task...\n⬜⬜⬜");
            loadingMessage.enableHtml(true);

            Message sentMessage = bot.execute(loadingMessage);

            // Animation frames
            for (int i = 1; i < LOADING_FRAMES.length - 1; i++) {
                Thread.sleep(300);
                EditMessageText updateMessage = new EditMessageText();
                updateMessage.setChatId(chatId);
                updateMessage.setMessageId(sentMessage.getMessageId());
                updateMessage.setText("Processing task...\n" + LOADING_FRAMES[i]);
                updateMessage.enableHtml(true);
                bot.execute(updateMessage);
                Thread.sleep(300);
            }

            Optional<ToDoItem> taskOpt = botService.getToDoItemById(taskId);
            if (taskOpt.isEmpty()) {
                logger.warn(chatId, "Task not found with ID {}", taskId);
                EditMessageText errorMessage = new EditMessageText();
                errorMessage.setChatId(chatId);
                errorMessage.setMessageId(sentMessage.getMessageId());
                errorMessage.setText("❌ Task not found. Please try again.");
                bot.execute(errorMessage);
                return;
            }

            ToDoItem task = taskOpt.get();
            String message = "";
            String actionEmoji = "";

            switch (command) {
                case "DONE":
                    // For DONE, we need actual hours, so we'll start the full completion flow
                    if (command.equals("DONE") && !task.isDone()) {
                        // Set up state for completion flow
                        state.setTaskCompletionMode(true);
                        state.setTaskCompletionStage("ACTUAL_HOURS");
                        state.setTempTaskId(taskId);

                        // Show actual hours prompt
                        EditMessageText hoursPrompt = new EditMessageText();
                        hoursPrompt.setChatId(chatId);
                        hoursPrompt.setMessageId(sentMessage.getMessageId());
                        hoursPrompt.setText(
                                "Please enter the actual hours spent on this task <b>" + task.getTitle() + "</b>:");
                        hoursPrompt.enableHtml(true);
                        bot.execute(hoursPrompt);
                        return;
                    }
                    task.setDone(true);
                    botService.updateToDoItem(task);
                    message = "Task marked as done!";
                    actionEmoji = "✅";
                    break;
                case "UNDO":
                    task.setDone(false);
                    botService.updateToDoItem(task);
                    message = "Task marked as not done!";
                    actionEmoji = "↩️";
                    break;
                case "DELETE":
                    botService.deleteToDoItem(taskId);
                    message = "Task deleted!";
                    actionEmoji = "🗑️";
                    break;
                default:
                    logger.warn(chatId, "Unknown task command: {}", command);
                    EditMessageText invalidMessage = new EditMessageText();
                    invalidMessage.setChatId(chatId);
                    invalidMessage.setMessageId(sentMessage.getMessageId());
                    invalidMessage.setText("❌ Invalid command. Please try again.");
                    bot.execute(invalidMessage);
                    return;
            }

            // Send confirmation with animation
            EditMessageText confirmationMessage = new EditMessageText();
            confirmationMessage.setChatId(chatId);
            confirmationMessage.setMessageId(sentMessage.getMessageId());
            confirmationMessage.setText(actionEmoji + " " + message);
            confirmationMessage.enableHtml(true);
            bot.execute(confirmationMessage);

            // Update task list after a short delay
            Thread.sleep(1000);
            List<ToDoItem> updatedTasks = botService.getAllToDoItems(state.getUser().getId());
            MessageHandler.showTaskList(chatId, updatedTasks, state, bot);
        } catch (Exception e) {
            logger.error(chatId, "Error handling task status update", e);
            MessageHandler.sendErrorMessage(chatId, "Failed to update task. Please try again later.", bot);
        }
    }
}