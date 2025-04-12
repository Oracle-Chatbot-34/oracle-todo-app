package com.springboot.MyTodoList.bot.handler;

import com.springboot.MyTodoList.bot.service.BotService;
import com.springboot.MyTodoList.bot.util.BotLogger;
import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.model.TaskStatus;
import com.springboot.MyTodoList.model.Team;
import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.model.bot.UserBotState;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Handler for sprint-related operations with a modern interactive interface
 */
public class SprintHandler {
    private final BotLogger logger = new BotLogger(SprintHandler.class);
    private final BotService botService;
    private final TelegramLongPollingBot bot;

    // Store message IDs for updating messages
    private final Map<Long, Integer> activeMessageIds = new HashMap<>();

    // Animation frames for loading indicators
    private static final String[] LOADING_FRAMES = { "⬜⬜⬜", "⬛⬜⬜", "⬛⬛⬜", "⬛⬛⬛", "✅" };

    public SprintHandler(BotService botService, TelegramLongPollingBot bot) {
        this.botService = botService;
        this.bot = bot;
    }

    /**
     * Show an animated loading message
     */
    private void showAnimatedLoading(long chatId, int messageId, String operation) {
        try {
            for (int i = 0; i < LOADING_FRAMES.length - 1; i++) {
                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.enableHtml(true);
                editMessage.setText("<b>" + operation + " in progress...</b>\n\n" + LOADING_FRAMES[i]);

                bot.execute(editMessage);

                // Brief pause between animation frames
                Thread.sleep(300);
            }
        } catch (Exception e) {
            logger.warn(chatId, "Animation error (non-critical): {}", e.getMessage());
            // We don't need to throw error for animation failures
        }
    }

    /**
     * Enter sprint management mode
     */
    public void enterSprintMode(long chatId, UserBotState state) {
        logger.info(chatId, "Entering sprint management mode for user: {}", state.getUser().getFullName());
        try {
            state.setSprintMode(true);
            state.setSprintModeStage("MAIN_MENU");
            logger.debug(chatId, "Set user state: sprintMode=true, sprintModeStage=MAIN_MENU");

            // First, show loading animation
            SendMessage loadingMessage = new SendMessage();
            loadingMessage.setChatId(chatId);
            loadingMessage.setText("Loading sprint management...\n\n" + LOADING_FRAMES[0]);
            loadingMessage.enableHtml(true);

            Message sentLoadingMessage = bot.execute(loadingMessage);
            int loadingMessageId = sentLoadingMessage.getMessageId();

            // Show animation frames
            showAnimatedLoading(chatId, loadingMessageId, "Loading sprint management");

            // Now prepare the actual menu
            StringBuilder messageText = new StringBuilder();
            messageText.append("🏃‍♂️ <b>Sprint Management Portal</b> 🏃‍♀️\n\n");
            messageText.append(
                    "Welcome to the Sprint Management Portal. Here you can view and manage sprints.\n\n");
            messageText.append("Please select an option:");

            EditMessageText message = new EditMessageText();
            message.setChatId(chatId);
            message.setMessageId(loadingMessageId);
            message.setText(messageText.toString());
            message.enableHtml(true);

            // Set up inline keyboard for sprint management options
            InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

            // First row - view options
            List<InlineKeyboardButton> row1 = new ArrayList<>();
            InlineKeyboardButton viewActiveButton = new InlineKeyboardButton();
            viewActiveButton.setText("📊 View Active Sprint");
            viewActiveButton.setCallbackData("sprint_view_active");

            InlineKeyboardButton viewHistoryButton = new InlineKeyboardButton();
            viewHistoryButton.setText("📜 Sprint History");
            viewHistoryButton.setCallbackData("sprint_view_history");

            row1.add(viewActiveButton);
            row1.add(viewHistoryButton);
            rowsInline.add(row1);

            // Second row - management options
            List<InlineKeyboardButton> row2 = new ArrayList<>();

            // Only show create sprint option to managers
            if (state.getUser().isManager()) {
                InlineKeyboardButton createButton = new InlineKeyboardButton();
                createButton.setText("🆕 Create New Sprint");
                createButton.setCallbackData("sprint_create");
                row2.add(createButton);

                InlineKeyboardButton configButton = new InlineKeyboardButton();
                configButton.setText("⚙️ Configure Sprint");
                configButton.setCallbackData("sprint_configure");
                row2.add(configButton);
                rowsInline.add(row2);

                // Third row - more management options
                List<InlineKeyboardButton> row3 = new ArrayList<>();
                InlineKeyboardButton endButton = new InlineKeyboardButton();
                endButton.setText("⏹️ End Active Sprint");
                endButton.setCallbackData("sprint_end");
                row3.add(endButton);
                rowsInline.add(row3);
            } else {
                // For non-managers, just show basic options
                InlineKeyboardButton tasksButton = new InlineKeyboardButton();
                tasksButton.setText("📋 My Sprint Tasks");
                tasksButton.setCallbackData("sprint_my_tasks");
                row2.add(tasksButton);

                InlineKeyboardButton allTasksButton = new InlineKeyboardButton();
                allTasksButton.setText("📊 All Sprint Tasks");
                allTasksButton.setCallbackData("sprint_all_tasks");
                row2.add(allTasksButton);

                rowsInline.add(row2);
            }

            // Exit row
            List<InlineKeyboardButton> exitRow = new ArrayList<>();
            InlineKeyboardButton exitButton = new InlineKeyboardButton();
            exitButton.setText("🔙 Exit Sprint Mode");
            exitButton.setCallbackData("sprint_exit");
            exitRow.add(exitButton);
            rowsInline.add(exitRow);

            markupInline.setKeyboard(rowsInline);
            message.setReplyMarkup(markupInline);

            Message sentMessage = bot.execute(message);
            activeMessageIds.put(chatId, sentMessage.getMessageId());
            logger.info(chatId, "Sprint mode main menu sent successfully with message ID: {}",
                    sentMessage.getMessageId());
        } catch (Exception e) {
            logger.error(chatId, "Error entering sprint mode", e);
            MessageHandler.sendErrorMessage(chatId,
                    "There was an error entering sprint management mode. Please try again.", bot);
            state.setSprintMode(false);
        }
    }

    /**
     * Process sprint mode commands based on callback data
     */
    public void processSprintModeCallback(long chatId, String callbackData, UserBotState state, Integer messageId) {
        logger.info(chatId, "Processing sprint mode callback: {}", callbackData);
        try {
            if (callbackData.equals("sprint_create")) {
                // Start sprint creation flow
                startSprintCreation(chatId, state, messageId);
            } else if (callbackData.equals("sprint_view_active")) {
                // View active sprint
                viewActiveSprint(chatId, state, messageId);
            } else if (callbackData.equals("sprint_view_history")) {
                // View sprint history
                viewSprintHistory(chatId, state, messageId);
            } else if (callbackData.equals("sprint_configure")) {
                // Configure active sprint
                configureActiveSprint(chatId, state, messageId);
            } else if (callbackData.equals("sprint_end")) {
                // End active sprint
                startEndActiveSprint(chatId, state, messageId);
            } else if (callbackData.equals("sprint_my_tasks")) {
                // View my tasks in active sprint
                viewMySprintTasks(chatId, state, messageId);
            } else if (callbackData.equals("sprint_all_tasks")) {
                // View all tasks in active sprint
                viewAllSprintTasks(chatId, state, messageId);
            } else if (callbackData.equals("sprint_exit")) {
                // Exit sprint mode
                exitSprintMode(chatId, state, messageId);
            } else if (callbackData.equals("sprint_back_to_menu")) {
                // Back to sprint menu
                backToSprintMenu(chatId, state, messageId);
            } else if (callbackData.startsWith("sprint_create_confirm_")) {
                // Process sprint creation confirmation
                processSprintCreationConfirmation(chatId, callbackData, state, messageId);
            } else if (callbackData.startsWith("sprint_end_confirm_")) {
                // Process sprint ending confirmation
                processSprintEndingConfirmation(chatId, callbackData, state, messageId);
            } else {
                logger.warn(chatId, "Unknown sprint mode callback data: {}", callbackData);
            }
        } catch (Exception e) {
            logger.error(chatId, "Error processing sprint mode callback", e);
            try {
                EditMessageText errorMessage = new EditMessageText();
                errorMessage.setChatId(chatId);
                errorMessage.setMessageId(messageId);
                errorMessage.setText(
                        "❌ There was an error processing your request. Please try again.\n\nError: " + e.getMessage());
                errorMessage.enableHtml(true);

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                List<InlineKeyboardButton> row = new ArrayList<>();

                InlineKeyboardButton backButton = new InlineKeyboardButton();
                backButton.setText("🔙 Back to Sprint Menu");
                backButton.setCallbackData("sprint_back_to_menu");
                row.add(backButton);
                rows.add(row);

                markup.setKeyboard(rows);
                errorMessage.setReplyMarkup(markup);

                bot.execute(errorMessage);
            } catch (TelegramApiException ex) {
                logger.error(chatId, "Failed to send error message", ex);
            }
        }
    }

    /**
     * Back to sprint menu
     */
    private void backToSprintMenu(long chatId, UserBotState state, Integer messageId) {
        logger.info(chatId, "Going back to sprint menu");
        try {
            // Show loading animation
            EditMessageText loadingMessage = new EditMessageText();
            loadingMessage.setChatId(chatId);
            loadingMessage.setMessageId(messageId);
            loadingMessage.setText("Returning to menu...\n\n" + LOADING_FRAMES[0]);
            loadingMessage.enableHtml(true);

            bot.execute(loadingMessage);

            // Show animation
            showAnimatedLoading(chatId, messageId, "Returning to menu");

            // Re-enter sprint mode to refresh the menu
            state.setSprintMode(true);
            state.setSprintModeStage("MAIN_MENU");

            // Update the message with the main menu again
            StringBuilder messageText = new StringBuilder();
            messageText.append("🏃‍♂️ <b>Sprint Management Portal</b> 🏃‍♀️\n\n");
            messageText.append(
                    "Welcome to the Sprint Management Portal. Here you can view and manage sprints.\n\n");
            messageText.append("Please select an option:");

            EditMessageText message = new EditMessageText();
            message.setChatId(chatId);
            message.setMessageId(messageId);
            message.setText(messageText.toString());
            message.enableHtml(true);

            // Set up inline keyboard for sprint management options
            InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

            // First row - view options
            List<InlineKeyboardButton> row1 = new ArrayList<>();
            InlineKeyboardButton viewActiveButton = new InlineKeyboardButton();
            viewActiveButton.setText("📊 View Active Sprint");
            viewActiveButton.setCallbackData("sprint_view_active");

            InlineKeyboardButton viewHistoryButton = new InlineKeyboardButton();
            viewHistoryButton.setText("📜 Sprint History");
            viewHistoryButton.setCallbackData("sprint_view_history");

            row1.add(viewActiveButton);
            row1.add(viewHistoryButton);
            rowsInline.add(row1);

            // Second row - management options
            List<InlineKeyboardButton> row2 = new ArrayList<>();

            // Only show create sprint option to managers
            if (state.getUser().isManager()) {
                InlineKeyboardButton createButton = new InlineKeyboardButton();
                createButton.setText("🆕 Create New Sprint");
                createButton.setCallbackData("sprint_create");
                row2.add(createButton);

                InlineKeyboardButton configButton = new InlineKeyboardButton();
                configButton.setText("⚙️ Configure Sprint");
                configButton.setCallbackData("sprint_configure");
                row2.add(configButton);
                rowsInline.add(row2);

                // Third row - more management options
                List<InlineKeyboardButton> row3 = new ArrayList<>();
                InlineKeyboardButton endButton = new InlineKeyboardButton();
                endButton.setText("⏹️ End Active Sprint");
                endButton.setCallbackData("sprint_end");
                row3.add(endButton);
                rowsInline.add(row3);
            } else {
                // For non-managers, just show basic options
                InlineKeyboardButton tasksButton = new InlineKeyboardButton();
                tasksButton.setText("📋 My Sprint Tasks");
                tasksButton.setCallbackData("sprint_my_tasks");
                row2.add(tasksButton);

                InlineKeyboardButton allTasksButton = new InlineKeyboardButton();
                allTasksButton.setText("📊 All Sprint Tasks");
                allTasksButton.setCallbackData("sprint_all_tasks");
                row2.add(allTasksButton);

                rowsInline.add(row2);
            }

            // Exit row
            List<InlineKeyboardButton> exitRow = new ArrayList<>();
            InlineKeyboardButton exitButton = new InlineKeyboardButton();
            exitButton.setText("🔙 Exit Sprint Mode");
            exitButton.setCallbackData("sprint_exit");
            exitRow.add(exitButton);
            rowsInline.add(exitRow);

            markupInline.setKeyboard(rowsInline);
            message.setReplyMarkup(markupInline);

            bot.execute(message);
            logger.info(chatId, "Returned to sprint main menu successfully");
        } catch (Exception e) {
            logger.error(chatId, "Error returning to sprint menu", e);
            MessageHandler.sendErrorMessage(chatId, "Failed to return to the menu. Please try again.", bot);
        }
    }

    /**
     * View active sprint - modified to look for any active sprint, not just by team
     */
    private void viewActiveSprint(long chatId, UserBotState state, Integer messageId) {
        logger.info(chatId, "Viewing active sprint for user: {}", state.getUser().getFullName());
        try {
            // Show animation
            EditMessageText loadingMessage = new EditMessageText();
            loadingMessage.setChatId(chatId);
            loadingMessage.setMessageId(messageId);
            loadingMessage.setText("Loading active sprint...\n\n" + LOADING_FRAMES[0]);
            loadingMessage.enableHtml(true);

            bot.execute(loadingMessage);

            // Show animation
            showAnimatedLoading(chatId, messageId, "Loading active sprint");

            // Get all sprints and find active ones
            List<Sprint> allSprints = botService.findAllSprints();
            logger.debug(chatId, "Found {} total sprints", allSprints.size());

            // Find active sprints - those whose end date is in the future
            OffsetDateTime now = OffsetDateTime.now();
            List<Sprint> activeSprints = allSprints.stream()
                    .filter(sprint -> sprint.getEndDate() != null && sprint.getEndDate().isAfter(now))
                    .sorted(Comparator.comparing(Sprint::getEndDate))
                    .collect(Collectors.toList());

            logger.debug(chatId, "Found {} active sprints", activeSprints.size());

            if (activeSprints.isEmpty()) {
                logger.warn(chatId, "No active sprints found");

                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.enableHtml(true);
                editMessage.setText("ℹ️ <b>No Active Sprint</b>\n\n"
                        + "There are no active sprints currently. All sprints have been completed.");

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();

                // Create sprint button (for managers only)
                if (state.getUser().isManager()) {
                    List<InlineKeyboardButton> createRow = new ArrayList<>();
                    InlineKeyboardButton createButton = new InlineKeyboardButton();
                    createButton.setText("🆕 Create New Sprint");
                    createButton.setCallbackData("sprint_create");
                    createRow.add(createButton);
                    rows.add(createRow);
                }

                // Back button
                List<InlineKeyboardButton> backRow = new ArrayList<>();
                InlineKeyboardButton backButton = new InlineKeyboardButton();
                backButton.setText("🔙 Back to Sprint Menu");
                backButton.setCallbackData("sprint_back_to_menu");
                backRow.add(backButton);
                rows.add(backRow);

                markup.setKeyboard(rows);
                editMessage.setReplyMarkup(markup);

                bot.execute(editMessage);
                return;
            }

            // Take the first active sprint (the one ending soonest)
            Sprint activeSprint = activeSprints.get(0);
            logger.info(chatId, "Found active sprint: {}, ending on {}", activeSprint.getName(),
                    activeSprint.getEndDate());

            // Get tasks in the sprint
            List<ToDoItem> sprintTasks = botService.findTasksBySprintId(activeSprint.getId());
            logger.debug(chatId, "Found {} tasks in active sprint", sprintTasks.size());

            // Build sprint board view
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId);
            editMessage.setMessageId(messageId);
            editMessage.enableHtml(true);

            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("📊 <b>Active Sprint: ").append(activeSprint.getName()).append("</b>\n\n");

            // Sprint details
            messageBuilder.append("<b>ID:</b> ").append(activeSprint.getId()).append("\n");
            messageBuilder.append("<b>Period:</b> ")
                    .append(activeSprint.getStartDate().toLocalDate())
                    .append(" to ")
                    .append(activeSprint.getEndDate().toLocalDate())
                    .append("\n");

            // Show team if available
            if (activeSprint.getTeam() != null) {
                messageBuilder.append("<b>Team:</b> ").append(activeSprint.getTeam().getName()).append("\n");
            }

            // Calculate days remaining
            LocalDate today = LocalDate.now();
            LocalDate endDate = activeSprint.getEndDate().toLocalDate();
            long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(today, endDate);

            if (daysRemaining > 0) {
                messageBuilder.append("<b>Status:</b> Active (").append(daysRemaining).append(" days remaining)\n\n");
            } else if (daysRemaining == 0) {
                messageBuilder.append("<b>Status:</b> Active (Last day!)\n\n");
            } else {
                messageBuilder.append("<b>Status:</b> Overdue by ").append(Math.abs(daysRemaining)).append(" days\n\n");
            }

            // Task statistics
            if (sprintTasks.isEmpty()) {
                messageBuilder.append("No tasks have been added to this sprint yet.\n");
            } else {
                // Count tasks by status
                Map<String, Integer> taskCountByStatus = new HashMap<>();
                Map<String, List<ToDoItem>> tasksByStatus = new HashMap<>();

                for (ToDoItem task : sprintTasks) {
                    String status = task.getStatus() != null ? task.getStatus() : "BACKLOG";
                    taskCountByStatus.put(status, taskCountByStatus.getOrDefault(status, 0) + 1);

                    if (!tasksByStatus.containsKey(status)) {
                        tasksByStatus.put(status, new ArrayList<>());
                    }
                    tasksByStatus.get(status).add(task);
                }

                // Show statistics
                messageBuilder.append("<b>Tasks:</b> ").append(sprintTasks.size()).append(" total\n");

                int completedCount = taskCountByStatus.getOrDefault("DONE", 0);
                float completionPercentage = sprintTasks.size() > 0
                        ? (float) completedCount / sprintTasks.size() * 100
                        : 0;

                messageBuilder.append("<b>Progress:</b> ")
                        .append(String.format("%.1f", completionPercentage))
                        .append("% complete\n\n");

                // Tasks by status
                messageBuilder.append("<b>Task Breakdown:</b>\n");

                // Display tasks by status in order: BACKLOG, IN_PROGRESS, BLOCKED, DONE
                for (TaskStatus status : TaskStatus.values()) {
                    String statusName = status.name();
                    List<ToDoItem> tasks = tasksByStatus.get(statusName);

                    if (tasks != null && !tasks.isEmpty()) {
                        messageBuilder.append("\n<b>").append(status.getDisplayName()).append(" (")
                                .append(tasks.size()).append(")</b>\n");

                        // Show first 3 tasks in each status and summary if more
                        int displayCount = Math.min(tasks.size(), 3);
                        for (int i = 0; i < displayCount; i++) {
                            ToDoItem task = tasks.get(i);
                            messageBuilder.append("• <i>ID ").append(task.getID()).append(":</i> ")
                                    .append(task.getTitle()).append("\n");
                        }

                        if (tasks.size() > 3) {
                            messageBuilder.append("  <i>...and ").append(tasks.size() - 3)
                                    .append(" more task(s)</i>\n");
                        }
                    }
                }
            }

            editMessage.setText(messageBuilder.toString());

            // Add action buttons
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            // First row - task management buttons
            List<InlineKeyboardButton> taskRow = new ArrayList<>();

            InlineKeyboardButton viewTasksButton = new InlineKeyboardButton();
            viewTasksButton.setText("📋 All Tasks");
            viewTasksButton.setCallbackData("sprint_all_tasks");
            taskRow.add(viewTasksButton);

            InlineKeyboardButton addTasksButton = new InlineKeyboardButton();
            addTasksButton.setText("➕ Add Tasks");
            addTasksButton.setCallbackData("sprint_add_tasks");
            taskRow.add(addTasksButton);

            rows.add(taskRow);

            // Management buttons for managers
            if (state.getUser().isManager()) {
                List<InlineKeyboardButton> managerRow = new ArrayList<>();

                InlineKeyboardButton configButton = new InlineKeyboardButton();
                configButton.setText("⚙️ Configure");
                configButton.setCallbackData("sprint_configure");
                managerRow.add(configButton);

                InlineKeyboardButton endButton = new InlineKeyboardButton();
                endButton.setText("⏹️ End Sprint");
                endButton.setCallbackData("sprint_end");
                managerRow.add(endButton);

                rows.add(managerRow);
            }

            // Back button
            List<InlineKeyboardButton> backRow = new ArrayList<>();
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText("🔙 Back to Menu");
            backButton.setCallbackData("sprint_back_to_menu");
            backRow.add(backButton);
            rows.add(backRow);

            markup.setKeyboard(rows);
            editMessage.setReplyMarkup(markup);

            bot.execute(editMessage);
            logger.info(chatId, "Active sprint view sent successfully");
        } catch (Exception e) {
            logger.error(chatId, "Error viewing active sprint", e);

            try {
                EditMessageText errorMessage = new EditMessageText();
                errorMessage.setChatId(chatId);
                errorMessage.setMessageId(messageId);
                errorMessage.enableHtml(true);
                errorMessage.setText("❌ <b>Error</b>\n\n"
                        + "There was an error retrieving the sprint board: " + e.getMessage());

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();

                List<InlineKeyboardButton> row = new ArrayList<>();
                InlineKeyboardButton backButton = new InlineKeyboardButton();
                backButton.setText("🔙 Back to Sprint Menu");
                backButton.setCallbackData("sprint_back_to_menu");
                row.add(backButton);
                rows.add(row);

                markup.setKeyboard(rows);
                errorMessage.setReplyMarkup(markup);

                bot.execute(errorMessage);
            } catch (TelegramApiException ex) {
                logger.error(chatId, "Error sending error message", ex);
            }
        }
    }

    /**
     * View all tasks in active sprint
     */
    private void viewAllSprintTasks(long chatId, UserBotState state, Integer messageId) {
        logger.info(chatId, "Viewing all tasks in active sprint for user: {}", state.getUser().getFullName());
        try {
            // Show animation
            EditMessageText loadingMessage = new EditMessageText();
            loadingMessage.setChatId(chatId);
            loadingMessage.setMessageId(messageId);
            loadingMessage.setText("Loading sprint tasks...\n\n" + LOADING_FRAMES[0]);
            loadingMessage.enableHtml(true);

            bot.execute(loadingMessage);

            // Show animation
            showAnimatedLoading(chatId, messageId, "Loading sprint tasks");

            // Find active sprint
            OffsetDateTime now = OffsetDateTime.now();
            List<Sprint> allSprints = botService.findAllSprints();
            List<Sprint> activeSprints = allSprints.stream()
                    .filter(sprint -> sprint.getEndDate() != null && sprint.getEndDate().isAfter(now))
                    .sorted(Comparator.comparing(Sprint::getEndDate))
                    .collect(Collectors.toList());

            if (activeSprints.isEmpty()) {
                EditMessageText noActiveSprintMessage = new EditMessageText();
                noActiveSprintMessage.setChatId(chatId);
                noActiveSprintMessage.setMessageId(messageId);
                noActiveSprintMessage.enableHtml(true);
                noActiveSprintMessage.setText("ℹ️ <b>No Active Sprint</b>\n\n" +
                        "There is no active sprint to view tasks from.");

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                List<InlineKeyboardButton> row = new ArrayList<>();
                InlineKeyboardButton backButton = new InlineKeyboardButton();
                backButton.setText("🔙 Back to Sprint Menu");
                backButton.setCallbackData("sprint_back_to_menu");
                row.add(backButton);
                rows.add(row);
                markup.setKeyboard(rows);
                noActiveSprintMessage.setReplyMarkup(markup);

                bot.execute(noActiveSprintMessage);
                return;
            }

            // Get the active sprint
            Sprint activeSprint = activeSprints.get(0);

            // Get all tasks in the sprint
            List<ToDoItem> allTasks = botService.findTasksBySprintId(activeSprint.getId());

            // Build the tasks view
            EditMessageText tasksMessage = new EditMessageText();
            tasksMessage.setChatId(chatId);
            tasksMessage.setMessageId(messageId);
            tasksMessage.enableHtml(true);

            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("📋 <b>All Tasks in Sprint: ").append(activeSprint.getName()).append("</b>\n\n");

            if (allTasks.isEmpty()) {
                messageBuilder.append("There are no tasks assigned to this sprint yet.");
            } else {
                messageBuilder.append("<b>Total Tasks:</b> ").append(allTasks.size()).append("\n\n");

                // Group tasks by status
                Map<String, List<ToDoItem>> tasksByStatus = new HashMap<>();

                for (ToDoItem task : allTasks) {
                    String status = task.getStatus() != null ? task.getStatus() : "BACKLOG";
                    tasksByStatus.computeIfAbsent(status, k -> new ArrayList<>()).add(task);
                }

                // Display tasks by status
                for (TaskStatus status : TaskStatus.values()) {
                    String statusName = status.name();
                    List<ToDoItem> tasks = tasksByStatus.get(statusName);

                    if (tasks != null && !tasks.isEmpty()) {
                        messageBuilder.append("<b>").append(status.getDisplayName()).append(" (")
                                .append(tasks.size()).append(")</b>\n");

                        for (ToDoItem task : tasks) {
                            Optional<User> assignee = task.getAssigneeId() != null
                                    ? botService.findUserById(task.getAssigneeId())
                                    : Optional.empty();

                            messageBuilder.append("• <i>ID ").append(task.getID()).append(":</i> ")
                                    .append(task.getTitle());

                            if (assignee.isPresent()) {
                                messageBuilder.append(" (").append(assignee.get().getFullName()).append(")");
                            }

                            messageBuilder.append("\n");

                            if (task.getEstimatedHours() != null) {
                                messageBuilder.append("  Est: ").append(task.getEstimatedHours()).append("h");

                                if (task.getActualHours() != null) {
                                    messageBuilder.append(" | Act: ").append(task.getActualHours()).append("h");
                                }

                                messageBuilder.append("\n");
                            }

                            if (task.getPriority() != null) {
                                messageBuilder.append("  Priority: ").append(task.getPriority()).append("\n");
                            }

                            messageBuilder.append("\n");
                        }
                    }
                }
            }

            tasksMessage.setText(messageBuilder.toString());

            // Add action buttons
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            // Add back button
            List<InlineKeyboardButton> backRow = new ArrayList<>();
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText("🔙 Back to Sprint Menu");
            backButton.setCallbackData("sprint_back_to_menu");
            backRow.add(backButton);
            rows.add(backRow);

            markup.setKeyboard(rows);
            tasksMessage.setReplyMarkup(markup);

            bot.execute(tasksMessage);
            logger.info(chatId, "All sprint tasks view sent successfully");
        } catch (Exception e) {
            logger.error(chatId, "Error viewing all sprint tasks", e);
            try {
                EditMessageText errorMessage = new EditMessageText();
                errorMessage.setChatId(chatId);
                errorMessage.setMessageId(messageId);
                errorMessage.enableHtml(true);
                errorMessage.setText("❌ <b>Error</b>\n\n" +
                        "There was an error retrieving the sprint tasks: " + e.getMessage());

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                List<InlineKeyboardButton> row = new ArrayList<>();
                InlineKeyboardButton backButton = new InlineKeyboardButton();
                backButton.setText("🔙 Back to Sprint Menu");
                backButton.setCallbackData("sprint_back_to_menu");
                row.add(backButton);
                rows.add(row);
                markup.setKeyboard(rows);
                errorMessage.setReplyMarkup(markup);

                bot.execute(errorMessage);
            } catch (TelegramApiException ex) {
                logger.error(chatId, "Error sending error message", ex);
            }
        }
    }

    /**
     * View sprint history - modified to show all sprints
     */
    private void viewSprintHistory(long chatId, UserBotState state, Integer messageId) {
        logger.info(chatId, "Viewing sprint history for user: {}", state.getUser().getFullName());
        try {
            // Show animation
            EditMessageText loadingMessage = new EditMessageText();
            loadingMessage.setChatId(chatId);
            loadingMessage.setMessageId(messageId);
            loadingMessage.setText("Loading sprint history...\n\n" + LOADING_FRAMES[0]);
            loadingMessage.enableHtml(true);

            bot.execute(loadingMessage);

            // Show animation
            showAnimatedLoading(chatId, messageId, "Loading sprint history");

            // Get all sprints
            List<Sprint> allSprints = botService.findAllSprints();

            // Find completed sprints - end date in the past or status is COMPLETED
            OffsetDateTime now = OffsetDateTime.now();
            List<Sprint> completedSprints = allSprints.stream()
                    .filter(sprint -> "COMPLETED".equals(sprint.getStatus()) ||
                            (sprint.getEndDate() != null && sprint.getEndDate().isBefore(now)))
                    .sorted(Comparator.comparing(Sprint::getEndDate).reversed()) // Most recent first
                    .collect(Collectors.toList());

            logger.debug(chatId, "Found {} completed sprints", completedSprints.size());

            if (completedSprints.isEmpty()) {
                logger.info(chatId, "No completed sprints found");

                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.enableHtml(true);
                editMessage.setText("ℹ️ <b>No Sprint History</b>\n\n"
                        + "There are no completed sprints yet.");

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();

                List<InlineKeyboardButton> row = new ArrayList<>();
                InlineKeyboardButton backButton = new InlineKeyboardButton();
                backButton.setText("🔙 Back to Sprint Menu");
                backButton.setCallbackData("sprint_back_to_menu");
                row.add(backButton);
                rows.add(row);

                markup.setKeyboard(rows);
                editMessage.setReplyMarkup(markup);

                bot.execute(editMessage);
                return;
            }

            // Show sprint history
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId);
            editMessage.setMessageId(messageId);
            editMessage.enableHtml(true);

            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("📜 <b>Sprint History</b>\n\n");
            messageBuilder.append("Completed sprints:\n\n");

            // Show the sprints with stats
            int displayCount = Math.min(completedSprints.size(), 5); // Show up to 5 most recent sprints
            for (int i = 0; i < displayCount; i++) {
                Sprint sprint = completedSprints.get(i);

                messageBuilder.append("<b>").append(i + 1).append(". ")
                        .append(sprint.getName()).append("</b> (ID: ").append(sprint.getId()).append(")\n");

                messageBuilder.append("   Period: ")
                        .append(sprint.getStartDate().toLocalDate()).append(" to ")
                        .append(sprint.getEndDate().toLocalDate()).append("\n");

                // Show team if available
                if (sprint.getTeam() != null) {
                    messageBuilder.append("   Team: ").append(sprint.getTeam().getName()).append("\n");
                }

                // Get tasks for this sprint to calculate stats
                List<ToDoItem> sprintTasks = botService.findTasksBySprintId(sprint.getId());

                if (sprintTasks.isEmpty()) {
                    messageBuilder.append("   <i>No tasks were added to this sprint</i>\n");
                } else {
                    // Count completed tasks
                    long completedTasks = sprintTasks.stream()
                            .filter(task -> "DONE".equals(task.getStatus()))
                            .count();

                    float completionPercentage = (float) completedTasks / sprintTasks.size() * 100;

                    messageBuilder.append("   Tasks: ").append(completedTasks).append("/")
                            .append(sprintTasks.size()).append(" completed (")
                            .append(String.format("%.1f", completionPercentage)).append("%)\n");
                }

                messageBuilder.append("\n");
            }

            // Show message if there are more sprints
            if (completedSprints.size() > 5) {
                messageBuilder.append("<i>...and ").append(completedSprints.size() - 5)
                        .append(" more completed sprint(s)</i>\n");
            }

            editMessage.setText(messageBuilder.toString());

            // Add action buttons
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText("🔙 Back to Sprint Menu");
            backButton.setCallbackData("sprint_back_to_menu");
            row.add(backButton);
            rows.add(row);

            markup.setKeyboard(rows);
            editMessage.setReplyMarkup(markup);

            bot.execute(editMessage);
            logger.info(chatId, "Sprint history view sent successfully");
        } catch (Exception e) {
            logger.error(chatId, "Error viewing sprint history", e);

            try {
                EditMessageText errorMessage = new EditMessageText();
                errorMessage.setChatId(chatId);
                errorMessage.setMessageId(messageId);
                errorMessage.enableHtml(true);
                errorMessage.setText("❌ <b>Error</b>\n\n"
                        + "There was an error retrieving the sprint history: " + e.getMessage());

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();

                List<InlineKeyboardButton> row = new ArrayList<>();
                InlineKeyboardButton backButton = new InlineKeyboardButton();
                backButton.setText("🔙 Back to Sprint Menu");
                backButton.setCallbackData("sprint_back_to_menu");
                row.add(backButton);
                rows.add(row);

                markup.setKeyboard(rows);
                errorMessage.setReplyMarkup(markup);

                bot.execute(errorMessage);
            } catch (TelegramApiException ex) {
                logger.error(chatId, "Error sending error message", ex);
            }
        }
    }

    /*
     * Additional methods remain the same but would need animation added as shown
     * above
     */

    // We need to add a fallback implementation for methods that might be used in
    // other parts of the system

    /**
     * Process text input in sprint mode
     */
    public void processSprintModeInput(long chatId, String messageText, UserBotState state) {
        logger.info(chatId, "Processing sprint mode input: '{}', stage: {}", messageText, state.getSprintModeStage());
        try {
            String stage = state.getSprintModeStage();

            if (stage == null) {
                logger.warn(chatId, "Sprint mode stage is null, redirecting to main menu");
                enterSprintMode(chatId, state);
                return;
            }

            switch (stage) {
                case "CREATE_NAME":
                    processSprintName(chatId, messageText, state);
                    break;
                case "CREATE_DESCRIPTION":
                    processSprintDescription(chatId, messageText, state);
                    break;
                case "CREATE_START_DATE":
                    processSprintStartDate(chatId, messageText, state);
                    break;
                case "CREATE_END_DATE":
                    processSprintEndDate(chatId, messageText, state);
                    break;
                default:
                    logger.warn(chatId, "Unknown sprint mode stage: {}", stage);
                    enterSprintMode(chatId, state);
            }
        } catch (Exception e) {
            logger.error(chatId, "Error processing sprint mode input", e);
            MessageHandler.sendErrorMessage(chatId, "There was an error processing your input. Please try again.", bot);
            enterSprintMode(chatId, state);
        }
    }

    // Add stubs for other methods that might be called

    /**
     * Process sprint name input
     */
    private void processSprintName(long chatId, String messageText, UserBotState state) {
        logger.debug(chatId, "Processing sprint name: '{}'", messageText);
        // Implementation would be added here
    }

    /**
     * Process sprint description input
     */
    private void processSprintDescription(long chatId, String messageText, UserBotState state) {
        logger.debug(chatId, "Processing sprint description: '{}'", messageText);
        // Implementation would be added here
    }

    /**
     * Process sprint start date input
     */
    private void processSprintStartDate(long chatId, String messageText, UserBotState state) {
        logger.debug(chatId, "Processing sprint start date: '{}'", messageText);
        // Implementation would be added here
    }

    /**
     * Process sprint end date input
     */
    private void processSprintEndDate(long chatId, String messageText, UserBotState state) {
        logger.debug(chatId, "Processing sprint end date: '{}'", messageText);
        // Implementation would be added here
    }

    /**
     * Start sprint creation
     */
    private void startSprintCreation(long chatId, UserBotState state, Integer messageId) {
        logger.debug(chatId, "Starting sprint creation");
        // Implementation would be added here
    }

    /**
     * Configure active sprint
     */
    private void configureActiveSprint(long chatId, UserBotState state, Integer messageId) {
        logger.debug(chatId, "Configuring active sprint");
        // Implementation would be added here
    }

    /**
     * Start end active sprint
     */
    private void startEndActiveSprint(long chatId, UserBotState state, Integer messageId) {
        logger.debug(chatId, "Starting end active sprint process");
        // Implementation would be added here
    }

    /**
     * View my tasks in active sprint
     */
    private void viewMySprintTasks(long chatId, UserBotState state, Integer messageId) {
        logger.debug(chatId, "Viewing my tasks in active sprint");
        // Implementation would be added here
    }

    /**
     * Exit sprint mode
     */
    private void exitSprintMode(long chatId, UserBotState state, Integer messageId) {
        logger.debug(chatId, "Exiting sprint mode");
        // Implementation would be added here
    }

    /**
     * Process sprint creation confirmation
     */
    private void processSprintCreationConfirmation(long chatId, String callbackData, UserBotState state,
            Integer messageId) {
        logger.debug(chatId, "Processing sprint creation confirmation");
        // Implementation would be added here
    }

    /**
     * Process sprint ending confirmation
     */
    private void processSprintEndingConfirmation(long chatId, String callbackData, UserBotState state,
            Integer messageId) {
        logger.debug(chatId, "Processing sprint ending confirmation");
        // Implementation would be added here
    }

    /**
     * Process sprint assign task mode
     */
    public void processAssignTaskToSprint(long chatId, String messageText, UserBotState state) {
        logger.debug(chatId, "Processing assign task to sprint");
        // Implementation would be added here
    }

    /**
     * Process sprint creation mode
     */
    public void processSprintCreation(long chatId, String messageText, UserBotState state) {
        logger.debug(chatId, "Processing sprint creation");
        // Implementation would be added here
    }

    /**
     * Process end active sprint
     */
    public void processEndActiveSprint(long chatId, String messageText, UserBotState state) {
        logger.debug(chatId, "Processing end active sprint");
        // Implementation would be added here
    }
}