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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handler for sprint-related operations with a modern interactive interface
 */
public class SprintHandler {
    private final BotLogger logger = new BotLogger(SprintHandler.class);
    private final BotService botService;
    private final TelegramLongPollingBot bot;

    // Store message IDs for updating messages
    private final Map<Long, Integer> activeMessageIds = new HashMap<>();

    public SprintHandler(BotService botService, TelegramLongPollingBot bot) {
        this.botService = botService;
        this.bot = bot;
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

            StringBuilder messageText = new StringBuilder();
            messageText.append("🏃‍♂️ <b>Sprint Management Portal</b> 🏃‍♀️\n\n");
            messageText.append(
                    "Welcome to the Sprint Management Portal. Here you can create and manage sprints for your team.\n\n");
            messageText.append("Please select an option:");

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
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
            } else if (callbackData.equals("sprint_exit")) {
                // Exit sprint mode
                exitSprintMode(chatId, state, messageId);
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
                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.setText(
                        "❌ There was an error processing your request. Please try again.\n\nError: " + e.getMessage());
                editMessage.enableHtml(true);

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
            } catch (TelegramApiException ex) {
                logger.error(chatId, "Failed to send error message", ex);
            }
        }
    }

    /**
     * Process text input in sprint mode
     */
    public void processSprintModeInput(long chatId, String messageText, UserBotState state) {
        logger.info(chatId, "Processing sprint mode input: '{}', stage: {}", messageText, state.getSprintModeStage());
        try {
            String stage = state.getSprintModeStage();
            Integer messageId = activeMessageIds.get(chatId);

            if (stage == null) {
                logger.warn(chatId, "Sprint mode stage is null, redirecting to main menu");
                enterSprintMode(chatId, state);
                return;
            }

            switch (stage) {
                case "CREATE_NAME":
                    processSprintName(chatId, messageText, state, messageId);
                    break;
                case "CREATE_DESCRIPTION":
                    processSprintDescription(chatId, messageText, state, messageId);
                    break;
                case "CREATE_START_DATE":
                    processSprintStartDate(chatId, messageText, state, messageId);
                    break;
                case "CREATE_END_DATE":
                    processSprintEndDate(chatId, messageText, state, messageId);
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

    /**
     * Start the process of creating a new sprint with single-message interface
     */
    private void startSprintCreation(long chatId, UserBotState state, Integer messageId) {
        logger.info(chatId, "Starting sprint creation process for user: {}", state.getUser().getFullName());
        try {
            // Check if user is a manager
            if (!state.getUser().isManager()) {
                logger.warn(chatId, "User with ID {} is not a manager, cannot create sprints",
                        state.getUser().getId());

                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.setText("❌ Only managers can create new sprints.");
                editMessage.enableHtml(true);

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                List<InlineKeyboardButton> row = new ArrayList<>();

                InlineKeyboardButton backButton = new InlineKeyboardButton();
                backButton.setText("🔙 Back to Main Menu");
                backButton.setCallbackData("sprint_back_to_menu");
                row.add(backButton);
                rows.add(row);

                markup.setKeyboard(rows);
                editMessage.setReplyMarkup(markup);

                bot.execute(editMessage);
                return;
            }

            // Set state for sprint creation
            state.setSprintModeStage("CREATE_NAME");
            logger.debug(chatId, "Set sprint mode stage to CREATE_NAME");

            // Initialize temporary sprint data
            state.setTempSprintName("");
            state.setTempSprintDescription("");
            state.setTempSprintStartDate("");
            state.setTempSprintEndDate("");

            // Update the existing message with the sprint creation form
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId);
            editMessage.setMessageId(messageId);
            editMessage.enableHtml(true);

            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("🆕 <b>Create New Sprint</b>\n\n");
            messageBuilder.append("Please enter information for the new sprint.\n\n");
            messageBuilder.append("<b>Step 1/4:</b> Enter the Sprint name:");

            editMessage.setText(messageBuilder.toString());

            // Add cancel button
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            List<InlineKeyboardButton> row = new ArrayList<>();

            InlineKeyboardButton cancelButton = new InlineKeyboardButton();
            cancelButton.setText("❌ Cancel");
            cancelButton.setCallbackData("sprint_back_to_menu");
            row.add(cancelButton);
            rows.add(row);

            markup.setKeyboard(rows);
            editMessage.setReplyMarkup(markup);

            bot.execute(editMessage);
            logger.info(chatId, "Sprint creation name prompt sent successfully");
        } catch (Exception e) {
            logger.error(chatId, "Error starting sprint creation", e);
            MessageHandler.sendErrorMessage(chatId,
                    "There was an error starting sprint creation. Please try again later.", bot);
        }
    }

    /**
     * Process sprint name input and ask for description
     */
    private void processSprintName(long chatId, String sprintName, UserBotState state, Integer messageId) {
        logger.debug(chatId, "Processing sprint name: '{}'", sprintName);
        try {
            // Validate name
            if (sprintName.trim().isEmpty() || sprintName.length() > 50) {
                EditMessageText errorMessage = new EditMessageText();
                errorMessage.setChatId(chatId);
                errorMessage.setMessageId(messageId);
                errorMessage.enableHtml(true);
                errorMessage.setText("❌ Sprint name cannot be empty and must be less than 50 characters.\n\n" +
                        "Please enter a valid sprint name:");

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                List<InlineKeyboardButton> row = new ArrayList<>();

                InlineKeyboardButton cancelButton = new InlineKeyboardButton();
                cancelButton.setText("❌ Cancel");
                cancelButton.setCallbackData("sprint_back_to_menu");
                row.add(cancelButton);
                rows.add(row);

                markup.setKeyboard(rows);
                errorMessage.setReplyMarkup(markup);

                bot.execute(errorMessage);
                return;
            }

            // Store name and move to description stage
            state.setTempSprintName(sprintName.trim());
            state.setSprintModeStage("CREATE_DESCRIPTION");
            logger.debug(chatId, "Updated stage to CREATE_DESCRIPTION, stored name: {}", sprintName);

            // Update message with description prompt
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId);
            editMessage.setMessageId(messageId);
            editMessage.enableHtml(true);

            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("🆕 <b>Create New Sprint</b>\n\n");
            messageBuilder.append("<b>Sprint Name:</b> ").append(state.getTempSprintName()).append("\n\n");
            messageBuilder.append("<b>Step 2/4:</b> Enter the Sprint description:");

            editMessage.setText(messageBuilder.toString());

            // Add cancel button
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            List<InlineKeyboardButton> row = new ArrayList<>();

            InlineKeyboardButton cancelButton = new InlineKeyboardButton();
            cancelButton.setText("❌ Cancel");
            cancelButton.setCallbackData("sprint_back_to_menu");
            row.add(cancelButton);
            rows.add(row);

            markup.setKeyboard(rows);
            editMessage.setReplyMarkup(markup);

            bot.execute(editMessage);
            logger.info(chatId, "Sprint creation description prompt sent successfully");
        } catch (Exception e) {
            logger.error(chatId, "Error processing sprint name", e);
            MessageHandler.sendErrorMessage(chatId,
                    "There was an error processing the sprint name. Please try again.", bot);
            enterSprintMode(chatId, state);
        }
    }

    /**
     * Process sprint description input and ask for start date
     */
    private void processSprintDescription(long chatId, String description, UserBotState state, Integer messageId) {
        logger.debug(chatId, "Processing sprint description: '{}'", description);
        try {
            // Store description (allow empty description)
            state.setTempSprintDescription(description.trim());
            state.setSprintModeStage("CREATE_START_DATE");
            logger.debug(chatId, "Updated stage to CREATE_START_DATE, stored description");

            // Update message with start date prompt
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId);
            editMessage.setMessageId(messageId);
            editMessage.enableHtml(true);

            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("🆕 <b>Create New Sprint</b>\n\n");
            messageBuilder.append("<b>Sprint Name:</b> ").append(state.getTempSprintName()).append("\n");
            messageBuilder.append("<b>Description:</b> ").append(state.getTempSprintDescription()).append("\n\n");
            messageBuilder.append("<b>Step 3/4:</b> Enter the start date (YYYY-MM-DD):");

            editMessage.setText(messageBuilder.toString());

            // Add calendar suggestion buttons for today and next week
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            // Date suggestion buttons
            List<InlineKeyboardButton> dateRow = new ArrayList<>();

            InlineKeyboardButton todayButton = new InlineKeyboardButton();
            String todayDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            todayButton.setText("Today (" + todayDate + ")");
            todayButton.setCallbackData("date_" + todayDate);
            dateRow.add(todayButton);

            InlineKeyboardButton nextMondayButton = new InlineKeyboardButton();
            LocalDate nextMonday = LocalDate.now().plusDays(1);
            while (nextMonday.getDayOfWeek().getValue() != 1) {
                nextMonday = nextMonday.plusDays(1);
            }
            String nextMondayDate = nextMonday.format(DateTimeFormatter.ISO_LOCAL_DATE);
            nextMondayButton.setText("Next Monday (" + nextMondayDate + ")");
            nextMondayButton.setCallbackData("date_" + nextMondayDate);
            dateRow.add(nextMondayButton);

            rows.add(dateRow);

            // Cancel button
            List<InlineKeyboardButton> controlRow = new ArrayList<>();
            InlineKeyboardButton cancelButton = new InlineKeyboardButton();
            cancelButton.setText("❌ Cancel");
            cancelButton.setCallbackData("sprint_back_to_menu");
            controlRow.add(cancelButton);
            rows.add(controlRow);

            markup.setKeyboard(rows);
            editMessage.setReplyMarkup(markup);

            bot.execute(editMessage);
            logger.info(chatId, "Sprint creation start date prompt sent successfully");
        } catch (Exception e) {
            logger.error(chatId, "Error processing sprint description", e);
            MessageHandler.sendErrorMessage(chatId,
                    "There was an error processing the sprint description. Please try again.", bot);
            enterSprintMode(chatId, state);
        }
    }

    /**
     * Process sprint start date input and ask for end date
     */
    private void processSprintStartDate(long chatId, String startDate, UserBotState state, Integer messageId) {
        logger.debug(chatId, "Processing sprint start date: '{}'", startDate);
        try {
            // Handle callback data for date buttons
            if (startDate.startsWith("date_")) {
                startDate = startDate.substring(5);
            }

            // Validate date format
            LocalDate parsedDate;
            try {
                parsedDate = LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE);

                // Check that date is not in the past
                if (parsedDate.isBefore(LocalDate.now())) {
                    throw new IllegalArgumentException("Start date cannot be in the past");
                }
            } catch (DateTimeParseException e) {
                EditMessageText errorMessage = new EditMessageText();
                errorMessage.setChatId(chatId);
                errorMessage.setMessageId(messageId);
                errorMessage.enableHtml(true);
                errorMessage.setText("❌ Invalid date format. Please use YYYY-MM-DD format (e.g., 2025-05-15).");

                // Add date suggestion buttons again
                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();

                // Date suggestion buttons
                List<InlineKeyboardButton> dateRow = new ArrayList<>();

                InlineKeyboardButton todayButton = new InlineKeyboardButton();
                String todayDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
                todayButton.setText("Today (" + todayDate + ")");
                todayButton.setCallbackData("date_" + todayDate);
                dateRow.add(todayButton);

                InlineKeyboardButton nextMondayButton = new InlineKeyboardButton();
                LocalDate nextMonday = LocalDate.now().plusDays(1);
                while (nextMonday.getDayOfWeek().getValue() != 1) {
                    nextMonday = nextMonday.plusDays(1);
                }
                String nextMondayDate = nextMonday.format(DateTimeFormatter.ISO_LOCAL_DATE);
                nextMondayButton.setText("Next Monday (" + nextMondayDate + ")");
                nextMondayButton.setCallbackData("date_" + nextMondayDate);
                dateRow.add(nextMondayButton);

                rows.add(dateRow);

                // Cancel button
                List<InlineKeyboardButton> controlRow = new ArrayList<>();
                InlineKeyboardButton cancelButton = new InlineKeyboardButton();
                cancelButton.setText("❌ Cancel");
                cancelButton.setCallbackData("sprint_back_to_menu");
                controlRow.add(cancelButton);
                rows.add(controlRow);

                markup.setKeyboard(rows);
                errorMessage.setReplyMarkup(markup);

                bot.execute(errorMessage);
                return;
            } catch (IllegalArgumentException e) {
                EditMessageText errorMessage = new EditMessageText();
                errorMessage.setChatId(chatId);
                errorMessage.setMessageId(messageId);
                errorMessage.enableHtml(true);
                errorMessage.setText("❌ " + e.getMessage() + ". Please select a date today or in the future.");

                // Add date suggestion buttons again
                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();

                // Date suggestion buttons
                List<InlineKeyboardButton> dateRow = new ArrayList<>();

                InlineKeyboardButton todayButton = new InlineKeyboardButton();
                String todayDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
                todayButton.setText("Today (" + todayDate + ")");
                todayButton.setCallbackData("date_" + todayDate);
                dateRow.add(todayButton);

                InlineKeyboardButton nextMondayButton = new InlineKeyboardButton();
                LocalDate nextMonday = LocalDate.now().plusDays(1);
                while (nextMonday.getDayOfWeek().getValue() != 1) {
                    nextMonday = nextMonday.plusDays(1);
                }
                String nextMondayDate = nextMonday.format(DateTimeFormatter.ISO_LOCAL_DATE);
                nextMondayButton.setText("Next Monday (" + nextMondayDate + ")");
                nextMondayButton.setCallbackData("date_" + nextMondayDate);
                dateRow.add(nextMondayButton);

                rows.add(dateRow);

                // Cancel button
                List<InlineKeyboardButton> controlRow = new ArrayList<>();
                InlineKeyboardButton cancelButton = new InlineKeyboardButton();
                cancelButton.setText("❌ Cancel");
                cancelButton.setCallbackData("sprint_back_to_menu");
                controlRow.add(cancelButton);
                rows.add(controlRow);

                markup.setKeyboard(rows);
                errorMessage.setReplyMarkup(markup);

                bot.execute(errorMessage);
                return;
            }

            // Store the start date with time component and move to end date stage
            state.setTempSprintStartDate(startDate + "T00:00:00Z");
            state.setSprintModeStage("CREATE_END_DATE");
            logger.debug(chatId, "Updated stage to CREATE_END_DATE, stored start date: {}", startDate);

            // Update message with end date prompt
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId);
            editMessage.setMessageId(messageId);
            editMessage.enableHtml(true);

            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("🆕 <b>Create New Sprint</b>\n\n");
            messageBuilder.append("<b>Sprint Name:</b> ").append(state.getTempSprintName()).append("\n");
            messageBuilder.append("<b>Description:</b> ").append(state.getTempSprintDescription()).append("\n");
            messageBuilder.append("<b>Start Date:</b> ").append(startDate).append("\n\n");
            messageBuilder.append("<b>Step 4/4:</b> Enter the end date (YYYY-MM-DD):");

            editMessage.setText(messageBuilder.toString());

            // Add suggested end dates (typical sprint durations)
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            // Date suggestion buttons
            List<InlineKeyboardButton> twoWeekRow = new ArrayList<>();

            InlineKeyboardButton twoWeekButton = new InlineKeyboardButton();
            LocalDate twoWeeksLater = parsedDate.plusDays(14);
            String twoWeeksDate = twoWeeksLater.format(DateTimeFormatter.ISO_LOCAL_DATE);
            twoWeekButton.setText("2 Weeks (" + twoWeeksDate + ")");
            twoWeekButton.setCallbackData("date_" + twoWeeksDate);
            twoWeekRow.add(twoWeekButton);

            rows.add(twoWeekRow);

            List<InlineKeyboardButton> otherDurationsRow = new ArrayList<>();

            InlineKeyboardButton oneWeekButton = new InlineKeyboardButton();
            LocalDate oneWeekLater = parsedDate.plusDays(7);
            String oneWeekDate = oneWeekLater.format(DateTimeFormatter.ISO_LOCAL_DATE);
            oneWeekButton.setText("1 Week");
            oneWeekButton.setCallbackData("date_" + oneWeekDate);
            otherDurationsRow.add(oneWeekButton);

            InlineKeyboardButton fourWeekButton = new InlineKeyboardButton();
            LocalDate fourWeeksLater = parsedDate.plusDays(28);
            String fourWeeksDate = fourWeeksLater.format(DateTimeFormatter.ISO_LOCAL_DATE);
            fourWeekButton.setText("4 Weeks");
            fourWeekButton.setCallbackData("date_" + fourWeeksDate);
            otherDurationsRow.add(fourWeekButton);

            rows.add(otherDurationsRow);

            // Cancel button
            List<InlineKeyboardButton> controlRow = new ArrayList<>();
            InlineKeyboardButton cancelButton = new InlineKeyboardButton();
            cancelButton.setText("❌ Cancel");
            cancelButton.setCallbackData("sprint_back_to_menu");
            controlRow.add(cancelButton);
            rows.add(controlRow);

            markup.setKeyboard(rows);
            editMessage.setReplyMarkup(markup);

            bot.execute(editMessage);
            logger.info(chatId, "Sprint creation end date prompt sent successfully");
        } catch (Exception e) {
            logger.error(chatId, "Error processing sprint start date", e);
            MessageHandler.sendErrorMessage(chatId,
                    "There was an error processing the sprint start date. Please try again.", bot);
            enterSprintMode(chatId, state);
        }
    }

    /**
     * Process sprint end date input and show confirmation
     */
    private void processSprintEndDate(long chatId, String endDate, UserBotState state, Integer messageId) {
        logger.debug(chatId, "Processing sprint end date: '{}'", endDate);
        try {
            // Handle callback data for date buttons
            if (endDate.startsWith("date_")) {
                endDate = endDate.substring(5);
            }

            // Parse start date for comparison
            String startDateStr = state.getTempSprintStartDate().substring(0, 10);
            LocalDate startDate = LocalDate.parse(startDateStr, DateTimeFormatter.ISO_LOCAL_DATE);

            // Validate date format and logic
            LocalDate parsedEndDate;
            try {
                parsedEndDate = LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE);

                // Validate end date is after start date
                if (parsedEndDate.isBefore(startDate) || parsedEndDate.isEqual(startDate)) {
                    throw new IllegalArgumentException("End date must be after the start date");
                }
            } catch (DateTimeParseException e) {
                EditMessageText errorMessage = new EditMessageText();
                errorMessage.setChatId(chatId);
                errorMessage.setMessageId(messageId);
                errorMessage.enableHtml(true);
                errorMessage.setText("❌ Invalid date format. Please use YYYY-MM-DD format (e.g., 2025-05-29).");

                // Add suggested end dates again
                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();

                // Date suggestion buttons
                List<InlineKeyboardButton> twoWeekRow = new ArrayList<>();

                InlineKeyboardButton twoWeekButton = new InlineKeyboardButton();
                LocalDate twoWeeksLater = startDate.plusDays(14);
                String twoWeeksDate = twoWeeksLater.format(DateTimeFormatter.ISO_LOCAL_DATE);
                twoWeekButton.setText("2 Weeks (" + twoWeeksDate + ")");
                twoWeekButton.setCallbackData("date_" + twoWeeksDate);
                twoWeekRow.add(twoWeekButton);

                rows.add(twoWeekRow);

                List<InlineKeyboardButton> otherDurationsRow = new ArrayList<>();

                InlineKeyboardButton oneWeekButton = new InlineKeyboardButton();
                LocalDate oneWeekLater = startDate.plusDays(7);
                String oneWeekDate = oneWeekLater.format(DateTimeFormatter.ISO_LOCAL_DATE);
                oneWeekButton.setText("1 Week");
                oneWeekButton.setCallbackData("date_" + oneWeekDate);
                otherDurationsRow.add(oneWeekButton);

                InlineKeyboardButton fourWeekButton = new InlineKeyboardButton();
                LocalDate fourWeeksLater = startDate.plusDays(28);
                String fourWeeksDate = fourWeeksLater.format(DateTimeFormatter.ISO_LOCAL_DATE);
                fourWeekButton.setText("4 Weeks");
                fourWeekButton.setCallbackData("date_" + fourWeeksDate);
                otherDurationsRow.add(fourWeekButton);

                rows.add(otherDurationsRow);

                // Cancel button
                List<InlineKeyboardButton> controlRow = new ArrayList<>();
                InlineKeyboardButton cancelButton = new InlineKeyboardButton();
                cancelButton.setText("❌ Cancel");
                cancelButton.setCallbackData("sprint_back_to_menu");
                controlRow.add(cancelButton);
                rows.add(controlRow);

                markup.setKeyboard(rows);
                errorMessage.setReplyMarkup(markup);

                bot.execute(errorMessage);
                return;
            } catch (IllegalArgumentException e) {
                EditMessageText errorMessage = new EditMessageText();
                errorMessage.setChatId(chatId);
                errorMessage.setMessageId(messageId);
                errorMessage.enableHtml(true);
                errorMessage.setText("❌ " + e.getMessage() + ". Please enter a date after " + startDateStr + ".");

                // Add suggested end dates again
                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();

                // Date suggestion buttons
                List<InlineKeyboardButton> twoWeekRow = new ArrayList<>();

                InlineKeyboardButton twoWeekButton = new InlineKeyboardButton();
                LocalDate twoWeeksLater = startDate.plusDays(14);
                String twoWeeksDate = twoWeeksLater.format(DateTimeFormatter.ISO_LOCAL_DATE);
                twoWeekButton.setText("2 Weeks (" + twoWeeksDate + ")");
                twoWeekButton.setCallbackData("date_" + twoWeeksDate);
                twoWeekRow.add(twoWeekButton);

                rows.add(twoWeekRow);

                List<InlineKeyboardButton> otherDurationsRow = new ArrayList<>();

                InlineKeyboardButton oneWeekButton = new InlineKeyboardButton();
                LocalDate oneWeekLater = startDate.plusDays(7);
                String oneWeekDate = oneWeekLater.format(DateTimeFormatter.ISO_LOCAL_DATE);
                oneWeekButton.setText("1 Week");
                oneWeekButton.setCallbackData("date_" + oneWeekDate);
                otherDurationsRow.add(oneWeekButton);

                InlineKeyboardButton fourWeekButton = new InlineKeyboardButton();
                LocalDate fourWeeksLater = startDate.plusDays(28);
                String fourWeeksDate = fourWeeksLater.format(DateTimeFormatter.ISO_LOCAL_DATE);
                fourWeekButton.setText("4 Weeks");
                fourWeekButton.setCallbackData("date_" + fourWeeksDate);
                otherDurationsRow.add(fourWeekButton);

                rows.add(otherDurationsRow);

                // Cancel button
                List<InlineKeyboardButton> controlRow = new ArrayList<>();
                InlineKeyboardButton cancelButton = new InlineKeyboardButton();
                cancelButton.setText("❌ Cancel");
                cancelButton.setCallbackData("sprint_back_to_menu");
                controlRow.add(cancelButton);
                rows.add(controlRow);

                markup.setKeyboard(rows);
                errorMessage.setReplyMarkup(markup);

                bot.execute(errorMessage);
                return;
            }

            // Store the end date with time component
            state.setTempSprintEndDate(endDate + "T23:59:59Z");

            // Show confirmation message
            showSprintCreationConfirmation(chatId, state, messageId);
        } catch (Exception e) {
            logger.error(chatId, "Error processing sprint end date", e);
            MessageHandler.sendErrorMessage(chatId,
                    "There was an error processing the sprint end date. Please try again.", bot);
            enterSprintMode(chatId, state);
        }
    }

    /**
     * Show a confirmation screen for sprint creation
     */
    private void showSprintCreationConfirmation(long chatId, UserBotState state, Integer messageId) {
        logger.debug(chatId, "Showing sprint creation confirmation");
        try {
            // Update message with confirmation details
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId);
            editMessage.setMessageId(messageId);
            editMessage.enableHtml(true);

            // Format dates for display
            String startDateStr = state.getTempSprintStartDate().substring(0, 10);
            String endDateStr = state.getTempSprintEndDate().substring(0, 10);

            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("🆕 <b>Create New Sprint - Confirmation</b>\n\n");
            messageBuilder.append("Please confirm the following sprint details:\n\n");
            messageBuilder.append("<b>Sprint Name:</b> ").append(state.getTempSprintName()).append("\n");
            messageBuilder.append("<b>Description:</b> ").append(state.getTempSprintDescription()).append("\n");
            messageBuilder.append("<b>Start Date:</b> ").append(startDateStr).append("\n");
            messageBuilder.append("<b>End Date:</b> ").append(endDateStr).append("\n\n");
            messageBuilder.append("Is this information correct?");

            editMessage.setText(messageBuilder.toString());

            // Add confirmation buttons
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            List<InlineKeyboardButton> confirmRow = new ArrayList<>();

            InlineKeyboardButton confirmButton = new InlineKeyboardButton();
            confirmButton.setText("✅ Confirm & Create");
            confirmButton.setCallbackData("sprint_create_confirm_yes");
            confirmRow.add(confirmButton);

            InlineKeyboardButton editButton = new InlineKeyboardButton();
            editButton.setText("✏️ Edit Details");
            editButton.setCallbackData("sprint_create_confirm_edit");
            confirmRow.add(editButton);

            rows.add(confirmRow);

            List<InlineKeyboardButton> cancelRow = new ArrayList<>();
            InlineKeyboardButton cancelButton = new InlineKeyboardButton();
            cancelButton.setText("❌ Cancel");
            cancelButton.setCallbackData("sprint_create_confirm_cancel");
            cancelRow.add(cancelButton);
            rows.add(cancelRow);

            markup.setKeyboard(rows);
            editMessage.setReplyMarkup(markup);

            bot.execute(editMessage);
            logger.info(chatId, "Sprint creation confirmation sent successfully");
        } catch (Exception e) {
            logger.error(chatId, "Error showing sprint creation confirmation", e);
            MessageHandler.sendErrorMessage(chatId,
                    "There was an error preparing the confirmation. Please try again.", bot);
            enterSprintMode(chatId, state);
        }
    }

    /**
     * Process sprint creation confirmation callback
     */
    private void processSprintCreationConfirmation(long chatId, String callbackData, UserBotState state,
            Integer messageId) {
        logger.info(chatId, "Processing sprint creation confirmation: {}", callbackData);
        try {
            if (callbackData.equals("sprint_create_confirm_yes")) {
                // Create the sprint
                createSprint(chatId, state, messageId);
            } else if (callbackData.equals("sprint_create_confirm_edit")) {
                // Go back to start of creation process
                startSprintCreation(chatId, state, messageId);
            } else if (callbackData.equals("sprint_create_confirm_cancel")) {
                // Cancel sprint creation
                logger.info(chatId, "Sprint creation cancelled by user");

                EditMessageText cancelMessage = new EditMessageText();
                cancelMessage.setChatId(chatId);
                cancelMessage.setMessageId(messageId);
                cancelMessage.enableHtml(true);
                cancelMessage.setText("❌ Sprint creation has been cancelled.");

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();

                List<InlineKeyboardButton> row = new ArrayList<>();
                InlineKeyboardButton backButton = new InlineKeyboardButton();
                backButton.setText("🔙 Back to Sprint Menu");
                backButton.setCallbackData("sprint_back_to_menu");
                row.add(backButton);
                rows.add(row);

                markup.setKeyboard(rows);
                cancelMessage.setReplyMarkup(markup);

                bot.execute(cancelMessage);
            } else {
                logger.warn(chatId, "Unknown confirmation callback: {}", callbackData);
            }
        } catch (Exception e) {
            logger.error(chatId, "Error processing sprint creation confirmation", e);
            MessageHandler.sendErrorMessage(chatId,
                    "There was an error processing your confirmation. Please try again.", bot);
        }
    }

    /**
     * Create a sprint from the collected information
     */
    private void createSprint(long chatId, UserBotState state, Integer messageId) {
        logger.info(chatId, "Creating sprint with name: {}", state.getTempSprintName());
        try {
            // Create the sprint entity
            Sprint sprint = new Sprint();
            sprint.setName(state.getTempSprintName());
            sprint.setDescription(state.getTempSprintDescription());

            // Parse dates
            sprint.setStartDate(OffsetDateTime.parse(state.getTempSprintStartDate()));
            sprint.setEndDate(OffsetDateTime.parse(state.getTempSprintEndDate()));

            // Set team from user's team
            Team team = state.getUser().getTeam();
            if (team == null) {
                logger.warn(chatId, "User with ID {} is not associated with any team", state.getUser().getId());
                MessageHandler.sendErrorMessage(chatId, "You are not associated with any team. Cannot create sprint.",
                        bot);
                return;
            }
            sprint.setTeam(team);
            sprint.setStatus("ACTIVE");

            // Check for existing active sprints
            Optional<Sprint> existingActive = botService.findActiveSprintByTeamId(team.getId());
            if (existingActive.isPresent()) {
                logger.warn(chatId, "Team ID {} already has an active sprint: {}",
                        team.getId(), existingActive.get().getName());

                // Show warning message
                EditMessageText warningMessage = new EditMessageText();
                warningMessage.setChatId(chatId);
                warningMessage.setMessageId(messageId);
                warningMessage.enableHtml(true);
                warningMessage.setText("⚠️ <b>Warning</b>: Your team already has an active sprint \""
                        + existingActive.get().getName() + "\".\n\n"
                        + "Would you like to end the current active sprint and create this new one?");

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();

                List<InlineKeyboardButton> actionRow = new ArrayList<>();

                InlineKeyboardButton endAndCreateButton = new InlineKeyboardButton();
                endAndCreateButton.setText("End & Create New");
                endAndCreateButton.setCallbackData("sprint_end_and_create");
                actionRow.add(endAndCreateButton);

                InlineKeyboardButton cancelButton = new InlineKeyboardButton();
                cancelButton.setText("Cancel");
                cancelButton.setCallbackData("sprint_back_to_menu");
                actionRow.add(cancelButton);

                rows.add(actionRow);
                markup.setKeyboard(rows);
                warningMessage.setReplyMarkup(markup);

                bot.execute(warningMessage);
                return;
            }

            // Save the sprint
            Sprint savedSprint = botService.createSprint(sprint);
            logger.info(chatId, "Sprint created successfully with ID {}", savedSprint.getId());

            // Show success message
            EditMessageText successMessage = new EditMessageText();
            successMessage.setChatId(chatId);
            successMessage.setMessageId(messageId);
            successMessage.enableHtml(true);
            successMessage.setText("✅ <b>Sprint Created Successfully</b>\n\n"
                    + "Sprint \"" + savedSprint.getName() + "\" has been created with ID: " + savedSprint.getId()
                    + "\n\n"
                    + "You can now start adding tasks to this sprint.");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            List<InlineKeyboardButton> actionRow = new ArrayList<>();

            InlineKeyboardButton viewButton = new InlineKeyboardButton();
            viewButton.setText("📊 View Sprint");
            viewButton.setCallbackData("sprint_view_active");
            actionRow.add(viewButton);

            InlineKeyboardButton addTasksButton = new InlineKeyboardButton();
            addTasksButton.setText("➕ Add Tasks");
            addTasksButton.setCallbackData("sprint_add_tasks");
            actionRow.add(addTasksButton);

            rows.add(actionRow);

            List<InlineKeyboardButton> backRow = new ArrayList<>();
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText("🔙 Back to Menu");
            backButton.setCallbackData("sprint_back_to_menu");
            backRow.add(backButton);
            rows.add(backRow);

            markup.setKeyboard(rows);
            successMessage.setReplyMarkup(markup);

            bot.execute(successMessage);

            // Also send a separate success notification that will remain in chat history
            SendMessage notification = new SendMessage();
            notification.setChatId(chatId);
            notification.enableHtml(true);
            notification.setText("🎉 <b>New Sprint Created</b>\n\n"
                    + "Sprint \"" + savedSprint.getName() + "\" (ID: " + savedSprint.getId() + ")\n"
                    + "Period: " + savedSprint.getStartDate().toLocalDate() + " to "
                    + savedSprint.getEndDate().toLocalDate() + "\n"
                    + "Team: " + team.getName());

            bot.execute(notification);

            // Reset creation state
            state.setSprintModeStage("MAIN_MENU");
        } catch (Exception e) {
            logger.error(chatId, "Error creating sprint", e);

            try {
                EditMessageText errorMessage = new EditMessageText();
                errorMessage.setChatId(chatId);
                errorMessage.setMessageId(messageId);
                errorMessage.enableHtml(true);
                errorMessage.setText("❌ <b>Error Creating Sprint</b>\n\n"
                        + "There was an error creating the sprint: " + e.getMessage() + "\n\n"
                        + "Please try again.");

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();

                List<InlineKeyboardButton> row = new ArrayList<>();
                InlineKeyboardButton retryButton = new InlineKeyboardButton();
                retryButton.setText("🔄 Try Again");
                retryButton.setCallbackData("sprint_create");
                row.add(retryButton);

                InlineKeyboardButton backButton = new InlineKeyboardButton();
                backButton.setText("🔙 Back to Menu");
                backButton.setCallbackData("sprint_back_to_menu");
                row.add(backButton);

                rows.add(row);
                markup.setKeyboard(rows);
                errorMessage.setReplyMarkup(markup);

                bot.execute(errorMessage);
            } catch (TelegramApiException ex) {
                logger.error(chatId, "Error sending error message", ex);
                MessageHandler.sendErrorMessage(chatId,
                        "There was an error creating the sprint. Please try again later.", bot);
            }
        }
    }

    /**
     * View active sprint board
     */
    private void viewActiveSprint(long chatId, UserBotState state, Integer messageId) {
        logger.info(chatId, "Viewing active sprint for user: {}", state.getUser().getFullName());
        try {
            // Get team ID
            Long teamId = state.getUser().getTeam() != null ? state.getUser().getTeam().getId() : null;
            logger.debug(chatId, "User team ID: {}", teamId);

            if (teamId == null) {
                logger.warn(chatId, "User is not associated with any team");

                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.enableHtml(true);
                editMessage.setText("❌ <b>No Team Association</b>\n\n"
                        + "You are not associated with any team. Please contact your administrator.");

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

            // Get active sprint
            Optional<Sprint> activeSprint = botService.findActiveSprintByTeamId(teamId);

            if (activeSprint.isEmpty()) {
                logger.warn(chatId, "No active sprint found for team ID {}", teamId);

                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.enableHtml(true);
                editMessage.setText("ℹ️ <b>No Active Sprint</b>\n\n"
                        + "There is no active sprint for your team.");

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

            // Get tasks in the sprint
            List<ToDoItem> sprintTasks = botService.findTasksBySprintId(activeSprint.get().getId());

            // Build sprint board view
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId);
            editMessage.setMessageId(messageId);
            editMessage.enableHtml(true);

            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("📊 <b>Sprint Board: ").append(activeSprint.get().getName()).append("</b>\n\n");

            // Sprint details
            messageBuilder.append("<b>ID:</b> ").append(activeSprint.get().getId()).append("\n");
            messageBuilder.append("<b>Period:</b> ")
                    .append(activeSprint.get().getStartDate().toLocalDate())
                    .append(" to ")
                    .append(activeSprint.get().getEndDate().toLocalDate())
                    .append("\n");

            // Calculate days remaining
            LocalDate today = LocalDate.now();
            LocalDate endDate = activeSprint.get().getEndDate().toLocalDate();
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
                // int progressCount = taskCountByStatus.getOrDefault("IN_PROGRESS", 0);
                // int backlogCount = taskCountByStatus.getOrDefault("BACKLOG", 0);
                // int blockedCount = taskCountByStatus.getOrDefault("BLOCKED", 0);

                float completionPercentage = (float) completedCount / sprintTasks.size() * 100;

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
            viewTasksButton.setCallbackData("sprint_view_all_tasks");
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
                MessageHandler.sendErrorMessage(chatId,
                        "There was an error retrieving the sprint board. Please try again later.", bot);
            }
        }
    }

    /**
     * View sprint history (past sprints)
     */
    private void viewSprintHistory(long chatId, UserBotState state, Integer messageId) {
        logger.info(chatId, "Viewing sprint history for user: {}", state.getUser().getFullName());
        try {
            // Get team ID
            Long teamId = state.getUser().getTeam() != null ? state.getUser().getTeam().getId() : null;
            logger.debug(chatId, "User team ID: {}", teamId);

            if (teamId == null) {
                logger.warn(chatId, "User is not associated with any team");

                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.enableHtml(true);
                editMessage.setText("❌ <b>No Team Association</b>\n\n"
                        + "You are not associated with any team. Please contact your administrator.");

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

            // Get completed sprints (not active ones)
            List<Sprint> completedSprints = botService.findCompletedSprintsByTeamId(teamId);

            if (completedSprints.isEmpty()) {
                logger.info(chatId, "No completed sprints found for team ID {}", teamId);

                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.enableHtml(true);
                editMessage.setText("ℹ️ <b>No Sprint History</b>\n\n"
                        + "Your team doesn't have any completed sprints yet.");

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
            messageBuilder.append("Completed sprints for your team:\n\n");

            // Sort sprints by end date, most recent first
            completedSprints.sort((s1, s2) -> s2.getEndDate().compareTo(s1.getEndDate()));

            // Show the sprints with stats
            int displayCount = Math.min(completedSprints.size(), 5); // Show up to 5 most recent sprints
            for (int i = 0; i < displayCount; i++) {
                Sprint sprint = completedSprints.get(i);

                messageBuilder.append("<b>").append(i + 1).append(". ")
                        .append(sprint.getName()).append("</b> (ID: ").append(sprint.getId()).append(")\n");

                messageBuilder.append("   Period: ")
                        .append(sprint.getStartDate().toLocalDate()).append(" to ")
                        .append(sprint.getEndDate().toLocalDate()).append("\n");

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
                MessageHandler.sendErrorMessage(chatId,
                        "There was an error retrieving the sprint history. Please try again later.", bot);
            }
        }
    }

    /**
     * Show my tasks in the active sprint
     */
    private void viewMySprintTasks(long chatId, UserBotState state, Integer messageId) {
        logger.info(chatId, "Viewing my sprint tasks for user: {}", state.getUser().getFullName());
        try {
            // Get team ID
            Long teamId = state.getUser().getTeam() != null ? state.getUser().getTeam().getId() : null;
            logger.debug(chatId, "User team ID: {}", teamId);

            if (teamId == null) {
                logger.warn(chatId, "User is not associated with any team");

                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.enableHtml(true);
                editMessage.setText("❌ <b>No Team Association</b>\n\n"
                        + "You are not associated with any team. Please contact your administrator.");

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

            // Get active sprint
            Optional<Sprint> activeSprint = botService.findActiveSprintByTeamId(teamId);

            if (activeSprint.isEmpty()) {
                logger.warn(chatId, "No active sprint found for team ID {}", teamId);

                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.enableHtml(true);
                editMessage.setText("ℹ️ <b>No Active Sprint</b>\n\n"
                        + "There is no active sprint for your team.");

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

            // Get my tasks in the sprint
            List<ToDoItem> myTasks = botService.findTasksBySprintIdAndAssigneeId(
                    activeSprint.get().getId(), state.getUser().getId());

            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId);
            editMessage.setMessageId(messageId);
            editMessage.enableHtml(true);

            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("📋 <b>My Tasks in Sprint</b>\n\n");
            messageBuilder.append("Sprint: <b>").append(activeSprint.get().getName()).append("</b>\n");
            messageBuilder.append("Period: ").append(activeSprint.get().getStartDate().toLocalDate())
                    .append(" to ").append(activeSprint.get().getEndDate().toLocalDate()).append("\n\n");

            if (myTasks.isEmpty()) {
                messageBuilder.append("You don't have any tasks assigned to you in this sprint.\n");
            } else {
                messageBuilder.append("Your tasks in this sprint:\n\n");

                // Group tasks by status
                Map<String, List<ToDoItem>> tasksByStatus = new HashMap<>();

                for (ToDoItem task : myTasks) {
                    String status = task.getStatus() != null ? task.getStatus() : "BACKLOG";
                    if (!tasksByStatus.containsKey(status)) {
                        tasksByStatus.put(status, new ArrayList<>());
                    }
                    tasksByStatus.get(status).add(task);
                }

                // Display tasks by status
                for (TaskStatus status : TaskStatus.values()) {
                    String statusName = status.name();
                    List<ToDoItem> tasks = tasksByStatus.get(statusName);

                    if (tasks != null && !tasks.isEmpty()) {
                        messageBuilder.append("<b>").append(status.getDisplayName()).append(" (")
                                .append(tasks.size()).append(")</b>\n");

                        for (ToDoItem task : tasks) {
                            messageBuilder.append("• <i>ID ").append(task.getID()).append(":</i> ")
                                    .append(task.getTitle()).append("\n");

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

                // Task statistics
                long completedTasks = myTasks.stream()
                        .filter(task -> "DONE".equals(task.getStatus()))
                        .count();

                float completionPercentage = (float) completedTasks / myTasks.size() * 100;

                messageBuilder.append("<b>Your Progress:</b> ").append(completedTasks).append("/")
                        .append(myTasks.size()).append(" tasks completed (")
                        .append(String.format("%.1f", completionPercentage)).append("%)\n");
            }

            editMessage.setText(messageBuilder.toString());

            // Add action buttons
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            // Task actions
            if (!myTasks.isEmpty()) {
                List<InlineKeyboardButton> actionRow = new ArrayList<>();

                InlineKeyboardButton updateButton = new InlineKeyboardButton();
                updateButton.setText("✏️ Update Task Status");
                updateButton.setCallbackData("sprint_update_task_status");
                actionRow.add(updateButton);

                InlineKeyboardButton completeButton = new InlineKeyboardButton();
                completeButton.setText("✅ Mark Task Complete");
                completeButton.setCallbackData("sprint_mark_task_complete");
                actionRow.add(completeButton);

                rows.add(actionRow);
            }

            // View button
            List<InlineKeyboardButton> viewRow = new ArrayList<>();
            InlineKeyboardButton viewSprintButton = new InlineKeyboardButton();
            viewSprintButton.setText("📊 View Full Sprint");
            viewSprintButton.setCallbackData("sprint_view_active");
            viewRow.add(viewSprintButton);
            rows.add(viewRow);

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
            logger.info(chatId, "My sprint tasks view sent successfully");
        } catch (Exception e) {
            logger.error(chatId, "Error viewing my sprint tasks", e);

            try {
                EditMessageText errorMessage = new EditMessageText();
                errorMessage.setChatId(chatId);
                errorMessage.setMessageId(messageId);
                errorMessage.enableHtml(true);
                errorMessage.setText("❌ <b>Error</b>\n\n"
                        + "There was an error retrieving your sprint tasks: " + e.getMessage());

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
                MessageHandler.sendErrorMessage(chatId,
                        "There was an error retrieving your sprint tasks. Please try again later.", bot);
            }
        }
    }

    /**
     * Configure settings for active sprint
     */
    private void configureActiveSprint(long chatId, UserBotState state, Integer messageId) {
        logger.info(chatId, "Configuring active sprint for user: {}", state.getUser().getFullName());
        try {
            // Check if user is a manager
            if (!state.getUser().isManager()) {
                logger.warn(chatId, "User with ID {} is not a manager, cannot configure sprints",
                        state.getUser().getId());

                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.enableHtml(true);
                editMessage.setText("❌ <b>Permission Denied</b>\n\n"
                        + "Only managers can configure sprints.");

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

            // Get team ID
            Long teamId = state.getUser().getTeam() != null ? state.getUser().getTeam().getId() : null;
            logger.debug(chatId, "User team ID: {}", teamId);

            if (teamId == null) {
                logger.warn(chatId, "User is not associated with any team");

                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.enableHtml(true);
                editMessage.setText("❌ <b>No Team Association</b>\n\n"
                        + "You are not associated with any team. Please contact your administrator.");

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

            // Get active sprint
            Optional<Sprint> activeSprint = botService.findActiveSprintByTeamId(teamId);

            if (activeSprint.isEmpty()) {
                logger.warn(chatId, "No active sprint found for team ID {}", teamId);

                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.enableHtml(true);
                editMessage.setText("ℹ️ <b>No Active Sprint</b>\n\n"
                        + "There is no active sprint to configure.");

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();

                List<InlineKeyboardButton> createRow = new ArrayList<>();
                InlineKeyboardButton createButton = new InlineKeyboardButton();
                createButton.setText("🆕 Create New Sprint");
                createButton.setCallbackData("sprint_create");
                createRow.add(createButton);
                rows.add(createRow);

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

            // Show sprint configuration options
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId);
            editMessage.setMessageId(messageId);
            editMessage.enableHtml(true);

            Sprint sprint = activeSprint.get();

            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("⚙️ <b>Configure Sprint: ").append(sprint.getName()).append("</b>\n\n");
            messageBuilder.append("Sprint ID: ").append(sprint.getId()).append("\n");
            messageBuilder.append("Description: ").append(sprint.getDescription()).append("\n");
            messageBuilder.append("Start Date: ").append(sprint.getStartDate().toLocalDate()).append("\n");
            messageBuilder.append("End Date: ").append(sprint.getEndDate().toLocalDate()).append("\n");
            messageBuilder.append("Status: ").append(sprint.getStatus()).append("\n\n");
            messageBuilder.append("Select an option to configure this sprint:");

            editMessage.setText(messageBuilder.toString());

            // Configuration options
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            // First row - basic info
            List<InlineKeyboardButton> basicRow = new ArrayList<>();

            InlineKeyboardButton editNameButton = new InlineKeyboardButton();
            editNameButton.setText("Edit Name");
            editNameButton.setCallbackData("sprint_edit_name");
            basicRow.add(editNameButton);

            InlineKeyboardButton editDescButton = new InlineKeyboardButton();
            editDescButton.setText("Edit Description");
            editDescButton.setCallbackData("sprint_edit_description");
            basicRow.add(editDescButton);

            rows.add(basicRow);

            // Second row - dates
            List<InlineKeyboardButton> datesRow = new ArrayList<>();

            InlineKeyboardButton editStartButton = new InlineKeyboardButton();
            editStartButton.setText("Edit Start Date");
            editStartButton.setCallbackData("sprint_edit_start_date");
            datesRow.add(editStartButton);

            InlineKeyboardButton editEndButton = new InlineKeyboardButton();
            editEndButton.setText("Edit End Date");
            editEndButton.setCallbackData("sprint_edit_end_date");
            datesRow.add(editEndButton);

            rows.add(datesRow);

            // Third row - extended options
            List<InlineKeyboardButton> extendedRow = new ArrayList<>();

            InlineKeyboardButton extendButton = new InlineKeyboardButton();
            extendButton.setText("Extend Sprint");
            extendButton.setCallbackData("sprint_extend");
            extendedRow.add(extendButton);

            InlineKeyboardButton endButton = new InlineKeyboardButton();
            endButton.setText("End Sprint");
            endButton.setCallbackData("sprint_end");
            extendedRow.add(endButton);

            rows.add(extendedRow);

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
            logger.info(chatId, "Sprint configuration options sent successfully");
        } catch (Exception e) {
            logger.error(chatId, "Error configuring active sprint", e);

            try {
                EditMessageText errorMessage = new EditMessageText();
                errorMessage.setChatId(chatId);
                errorMessage.setMessageId(messageId);
                errorMessage.enableHtml(true);
                errorMessage.setText("❌ <b>Error</b>\n\n"
                        + "There was an error configuring the sprint: " + e.getMessage());

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
                MessageHandler.sendErrorMessage(chatId,
                        "There was an error configuring the sprint. Please try again later.", bot);
            }
        }
    }

    /**
     * Start the process of ending an active sprint
     */
    private void startEndActiveSprint(long chatId, UserBotState state, Integer messageId) {
        logger.info(chatId, "Starting end active sprint process for user: {}", state.getUser().getFullName());
        try {
            // Check if user is a manager
            if (!state.getUser().isManager()) {
                logger.warn(chatId, "User with ID {} is not a manager, cannot end sprints",
                        state.getUser().getId());

                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.enableHtml(true);
                editMessage.setText("❌ <b>Permission Denied</b>\n\n"
                        + "Only managers can end sprints.");

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

            // Get team ID
            Long teamId = state.getUser().getTeam() != null ? state.getUser().getTeam().getId() : null;
            logger.debug(chatId, "User team ID: {}", teamId);

            if (teamId == null) {
                logger.warn(chatId, "User with ID {} is not associated with any team", state.getUser().getId());

                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.enableHtml(true);
                editMessage.setText("❌ <b>No Team Association</b>\n\n"
                        + "You are not associated with any team. Please contact your administrator.");

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

            // Get active sprint
            Optional<Sprint> activeSprint = botService.findActiveSprintByTeamId(teamId);

            if (activeSprint.isEmpty()) {
                logger.warn(chatId, "No active sprint found for team ID {}", teamId);

                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.enableHtml(true);
                editMessage.setText("ℹ️ <b>No Active Sprint</b>\n\n"
                        + "There is no active sprint to end.");

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();

                List<InlineKeyboardButton> createRow = new ArrayList<>();
                InlineKeyboardButton createButton = new InlineKeyboardButton();
                createButton.setText("🆕 Create New Sprint");
                createButton.setCallbackData("sprint_create");
                createRow.add(createButton);
                rows.add(createRow);

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

            // Get sprint tasks to show statistics
            List<ToDoItem> sprintTasks = botService.findTasksBySprintId(activeSprint.get().getId());

            // Count tasks by status
            int totalTasks = sprintTasks.size();
            long completedTasks = sprintTasks.stream()
                    .filter(task -> "DONE".equals(task.getStatus()))
                    .count();
            long inProgressTasks = sprintTasks.stream()
                    .filter(task -> "IN_PROGRESS".equals(task.getStatus()))
                    .count();

            float completionPercentage = totalTasks > 0 ? (float) completedTasks / totalTasks * 100 : 0;

            // Show confirmation for ending sprint
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId);
            editMessage.setMessageId(messageId);
            editMessage.enableHtml(true);

            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("⏹️ <b>End Active Sprint</b>\n\n");
            messageBuilder.append("Are you sure you want to end the current active sprint?\n\n");
            messageBuilder.append("<b>Sprint:</b> ").append(activeSprint.get().getName())
                    .append(" (ID: ").append(activeSprint.get().getId()).append(")\n");
            messageBuilder.append("<b>Period:</b> ")
                    .append(activeSprint.get().getStartDate().toLocalDate())
                    .append(" to ")
                    .append(activeSprint.get().getEndDate().toLocalDate())
                    .append("\n\n");

            // Sprint progress
            messageBuilder.append("<b>Current Progress:</b>\n");

            if (totalTasks > 0) {
                messageBuilder.append("• ").append(completedTasks).append("/").append(totalTasks)
                        .append(" tasks completed (").append(String.format("%.1f", completionPercentage))
                        .append("%)\n");

                if (inProgressTasks > 0) {
                    messageBuilder.append("• ").append(inProgressTasks).append(" tasks still in progress\n");
                }

                long pendingTasks = totalTasks - completedTasks - inProgressTasks;
                if (pendingTasks > 0) {
                    messageBuilder.append("• ").append(pendingTasks).append(" tasks not started\n");
                }
            } else {
                messageBuilder.append("No tasks were added to this sprint.\n");
            }

            messageBuilder.append(
                    "\n⚠️ <b>Warning:</b> This action cannot be undone. All uncompleted tasks will remain in their current state.\n\n");
            messageBuilder.append("Are you sure you want to end this sprint?");

            editMessage.setText(messageBuilder.toString());

            // Add confirmation buttons
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            List<InlineKeyboardButton> confirmRow = new ArrayList<>();

            InlineKeyboardButton confirmButton = new InlineKeyboardButton();
            confirmButton.setText("✅ Yes, End Sprint");
            confirmButton.setCallbackData("sprint_end_confirm_yes");
            confirmRow.add(confirmButton);

            InlineKeyboardButton cancelButton = new InlineKeyboardButton();
            cancelButton.setText("❌ No, Cancel");
            cancelButton.setCallbackData("sprint_end_confirm_no");
            confirmRow.add(cancelButton);

            rows.add(confirmRow);

            markup.setKeyboard(rows);
            editMessage.setReplyMarkup(markup);

            bot.execute(editMessage);
            logger.info(chatId, "End sprint confirmation sent successfully");
        } catch (Exception e) {
            logger.error(chatId, "Error starting end active sprint process", e);

            try {
                EditMessageText errorMessage = new EditMessageText();
                errorMessage.setChatId(chatId);
                errorMessage.setMessageId(messageId);
                errorMessage.enableHtml(true);
                errorMessage.setText("❌ <b>Error</b>\n\n"
                        + "There was an error preparing to end the sprint: " + e.getMessage());

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
                MessageHandler.sendErrorMessage(chatId,
                        "There was an error in the process. Please try again later.", bot);
            }
        }
    }

    /**
     * Process sprint ending confirmation callback
     */
    private void processSprintEndingConfirmation(long chatId, String callbackData, UserBotState state,
            Integer messageId) {
        logger.info(chatId, "Processing sprint ending confirmation: {}", callbackData);
        try {
            if (callbackData.equals("sprint_end_confirm_yes")) {
                // End the sprint
                endSprint(chatId, state, messageId);
            } else if (callbackData.equals("sprint_end_confirm_no")) {
                // Cancel ending sprint
                logger.info(chatId, "Sprint ending cancelled by user");

                EditMessageText cancelMessage = new EditMessageText();
                cancelMessage.setChatId(chatId);
                cancelMessage.setMessageId(messageId);
                cancelMessage.enableHtml(true);
                cancelMessage.setText("ℹ️ Sprint ending has been cancelled.");

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();

                List<InlineKeyboardButton> row = new ArrayList<>();
                InlineKeyboardButton backButton = new InlineKeyboardButton();
                backButton.setText("🔙 Back to Sprint Menu");
                backButton.setCallbackData("sprint_back_to_menu");
                row.add(backButton);
                rows.add(row);

                markup.setKeyboard(rows);
                cancelMessage.setReplyMarkup(markup);

                bot.execute(cancelMessage);
            } else {
                logger.warn(chatId, "Unknown confirmation callback: {}", callbackData);
            }
        } catch (Exception e) {
            logger.error(chatId, "Error processing sprint ending confirmation", e);
            MessageHandler.sendErrorMessage(chatId,
                    "There was an error processing your confirmation. Please try again.", bot);
        }
    }

    /**
     * End the active sprint
     */
    private void endSprint(long chatId, UserBotState state, Integer messageId) {
        logger.info(chatId, "Ending active sprint for user: {}", state.getUser().getFullName());
        try {
            // Get team ID
            Long teamId = state.getUser().getTeam().getId();
            logger.debug(chatId, "Finding active sprint for team ID: {}", teamId);

            Optional<Sprint> activeSprint = botService.findActiveSprintByTeamId(teamId);
            if (activeSprint.isEmpty()) {
                logger.warn(chatId, "No active sprint found for team ID {} when trying to end it", teamId);

                EditMessageText errorMessage = new EditMessageText();
                errorMessage.setChatId(chatId);
                errorMessage.setMessageId(messageId);
                errorMessage.enableHtml(true);
                errorMessage.setText("❌ <b>Error</b>\n\n"
                        + "Could not find the active sprint. It may have already been ended.");

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
                return;
            }

            // End the sprint
            Sprint completedSprint = botService.completeSprint(activeSprint.get().getId());
            logger.info(chatId, "Sprint ID {} successfully ended", completedSprint.getId());

            // Get final statistics
            List<ToDoItem> sprintTasks = botService.findTasksBySprintId(completedSprint.getId());

            int totalTasks = sprintTasks.size();
            long completedTasks = sprintTasks.stream()
                    .filter(task -> "DONE".equals(task.getStatus()))
                    .count();

            float completionPercentage = totalTasks > 0 ? (float) completedTasks / totalTasks * 100 : 0;

            // Show success message
            EditMessageText successMessage = new EditMessageText();
            successMessage.setChatId(chatId);
            successMessage.setMessageId(messageId);
            successMessage.enableHtml(true);

            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("✅ <b>Sprint Ended Successfully</b>\n\n");
            messageBuilder.append("Sprint \"").append(completedSprint.getName())
                    .append("\" has been marked as completed.\n\n");

            messageBuilder.append("<b>Final Statistics:</b>\n");

            if (totalTasks > 0) {
                messageBuilder.append("• ").append(completedTasks).append("/").append(totalTasks)
                        .append(" tasks completed (").append(String.format("%.1f", completionPercentage))
                        .append("%)\n");
            } else {
                messageBuilder.append("No tasks were added to this sprint.\n");
            }

            messageBuilder.append("\nYou can now create a new sprint for your team.");

            successMessage.setText(messageBuilder.toString());

            // Add buttons for next actions
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            List<InlineKeyboardButton> actionRow = new ArrayList<>();

            InlineKeyboardButton createButton = new InlineKeyboardButton();
            createButton.setText("🆕 Create New Sprint");
            createButton.setCallbackData("sprint_create");
            actionRow.add(createButton);

            InlineKeyboardButton historyButton = new InlineKeyboardButton();
            historyButton.setText("📜 View Sprint History");
            historyButton.setCallbackData("sprint_view_history");
            actionRow.add(historyButton);

            rows.add(actionRow);

            List<InlineKeyboardButton> backRow = new ArrayList<>();
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText("🔙 Back to Menu");
            backButton.setCallbackData("sprint_back_to_menu");
            backRow.add(backButton);
            rows.add(backRow);

            markup.setKeyboard(rows);
            successMessage.setReplyMarkup(markup);

            bot.execute(successMessage);

            // Also send a separate success notification that will remain in chat history
            SendMessage notification = new SendMessage();
            notification.setChatId(chatId);
            notification.enableHtml(true);
            notification.setText("🏁 <b>Sprint Completed</b>\n\n"
                    + "Sprint \"" + completedSprint.getName() + "\" (ID: " + completedSprint.getId()
                    + ") has been completed.\n"
                    + "Final completion rate: " + String.format("%.1f", completionPercentage) + "%");

            bot.execute(notification);
            logger.info(chatId, "Sprint end success messages sent");
        } catch (Exception e) {
            logger.error(chatId, "Error ending sprint", e);

            try {
                EditMessageText errorMessage = new EditMessageText();
                errorMessage.setChatId(chatId);
                errorMessage.setMessageId(messageId);
                errorMessage.enableHtml(true);
                errorMessage.setText("❌ <b>Error</b>\n\n"
                        + "There was an error ending the sprint: " + e.getMessage());

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
                MessageHandler.sendErrorMessage(chatId,
                        "There was an error ending the sprint. Please try again later.", bot);
            }
        }
    }

    /**
     * Exit sprint management mode
     */
    private void exitSprintMode(long chatId, UserBotState state, Integer messageId) {
        logger.info(chatId, "Exiting sprint mode for user: {}", state.getUser().getFullName());
        try {
            state.setSprintMode(false);
            state.setSprintModeStage(null);
            logger.debug(chatId, "Reset sprint mode state");

            // Send message confirming exit
            EditMessageText exitMessage = new EditMessageText();
            exitMessage.setChatId(chatId);
            exitMessage.setMessageId(messageId);
            exitMessage.enableHtml(true);
            exitMessage.setText("You've exited the Sprint Management Portal. What would you like to do next?");

            // Create buttons for main options
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            List<InlineKeyboardButton> row1 = new ArrayList<>();

            InlineKeyboardButton tasksButton = new InlineKeyboardButton();
            tasksButton.setText("📝 Task Management");
            tasksButton.setCallbackData("main_tasks");
            row1.add(tasksButton);

            InlineKeyboardButton sprintButton = new InlineKeyboardButton();
            sprintButton.setText("🔄 Re-enter Sprint Mode");
            sprintButton.setCallbackData("main_sprint");
            row1.add(sprintButton);

            rows.add(row1);

            List<InlineKeyboardButton> row2 = new ArrayList<>();

            InlineKeyboardButton helpButton = new InlineKeyboardButton();
            helpButton.setText("❓ Help");
            helpButton.setCallbackData("main_help");
            row2.add(helpButton);

            rows.add(row2);

            markup.setKeyboard(rows);
            exitMessage.setReplyMarkup(markup);

            bot.execute(exitMessage);
            logger.info(chatId, "Sprint mode exit message sent successfully");

            // Remove from active message tracking
            activeMessageIds.remove(chatId);
        } catch (Exception e) {
            logger.error(chatId, "Error exiting sprint mode", e);
            MessageHandler.sendErrorMessage(chatId,
                    "There was an error exiting sprint mode. Please try again.", bot);

            // Ensure state is reset even if error occurs
            state.setSprintMode(false);
            state.setSprintModeStage(null);
        }
    }

    /**
     * Process assigning a task to a sprint
     */
    public void processAssignTaskToSprint(long chatId, String messageText, UserBotState state) {
        // If this functionality already exists under a different name, rename it
        // Otherwise, implement the assignment logic here
        logger.info(chatId, "Processing assign task to sprint: {}", messageText);

        // For now, we'll just show a message explaining this isn't implemented yet
        MessageHandler.sendMessage(chatId,
                "Task-to-sprint assignment is not implemented yet. Please use the sprint management menu.", bot);

        // Reset the state
        state.setAssignToSprintMode(false);
        state.setAssignToSprintStage(null);

        // Return to sprint mode main menu
        enterSprintMode(chatId, state);
    }

    /**
     * Process legacy sprint creation mode
     */
    public void processSprintCreation(long chatId, String messageText, UserBotState state) {
        // This is likely a legacy method that's been replaced by the new sprint portal
        // For backwards compatibility, redirect to the new implementation
        logger.info(chatId, "Redirecting legacy sprint creation to new sprint portal");

        // Enter the new sprint mode
        enterSprintMode(chatId, state);

        // Reset legacy state
        state.setSprintCreationMode(false);
        state.setSprintCreationStage(null);
    }

    /**
     * Process legacy end sprint mode
     */
    public void processEndActiveSprint(long chatId, String messageText, UserBotState state) {
        // This is likely a legacy method that's been replaced by the new sprint portal
        // For backwards compatibility, redirect to the new implementation
        logger.info(chatId, "Redirecting legacy end sprint to new sprint portal");

        // Enter the new sprint mode
        enterSprintMode(chatId, state);

        // Reset legacy state
        state.setEndSprintMode(false);
    }
}