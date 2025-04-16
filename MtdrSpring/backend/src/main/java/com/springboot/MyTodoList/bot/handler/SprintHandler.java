package com.springboot.MyTodoList.bot.handler;

import com.springboot.MyTodoList.bot.keyboard.KeyboardFactory;
import com.springboot.MyTodoList.bot.service.BotService;
import com.springboot.MyTodoList.bot.util.BotLogger;
import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.model.TaskStatus;
import com.springboot.MyTodoList.model.Team;
import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.model.bot.UserBotState;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handler for sprint-related operations
 */
public class SprintHandler {
    private final BotLogger logger = new BotLogger(SprintHandler.class);
    private final BotService botService;
    private final TelegramLongPollingBot bot;

    public SprintHandler(BotService botService, TelegramLongPollingBot bot) {
        this.botService = botService;
        this.bot = bot;
    }

    /**
     * Show active sprint board
     */
    public void showSprintBoard(long chatId, UserBotState state) {
        logger.info(chatId, "Showing sprint board for user: {}", state.getUser().getFullName());
        try {
            // Try to find the active sprint for the user's team
            Long teamId = state.getUser().getTeam() != null ? state.getUser().getTeam().getId() : null;
            logger.debug(chatId, "User team ID: {}", teamId);

            if (teamId == null) {
                logger.warn(chatId, "User is not associated with any team");
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("You are not associated with any team.");

                // Create keyboard for next actions - ensure keyboard is properly initialized
                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                keyboardMarkup.setResizeKeyboard(true);
                List<KeyboardRow> keyboard = new ArrayList<>();

                KeyboardRow row = new KeyboardRow();
                row.add("🏠 Main Menu");
                keyboard.add(row);

                keyboardMarkup.setKeyboard(keyboard); // Set the keyboard rows
                message.setReplyMarkup(keyboardMarkup);

                bot.execute(message);
                logger.info(chatId, "No team message sent");
                return;
            }

            // Get the active sprint
            logger.debug(chatId, "Fetching active sprint for team ID: {}", teamId);
            Optional<Sprint> activeSprint = botService.findActiveSprintByTeamId(teamId);
            logger.debug(chatId, "Active sprint found: {}", activeSprint.isPresent());

            if (activeSprint.isEmpty()) {
                logger.warn(chatId, "No active sprint found for team ID {}", teamId);
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("There is no active sprint for your team.");

                // Create keyboard with appropriate options for when no sprint exists
                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                keyboardMarkup.setResizeKeyboard(true);
                List<KeyboardRow> keyboard = new ArrayList<>();

                KeyboardRow row = new KeyboardRow();
                if (state.getUser().isManager()) {
                    row.add("🆕 Create New Sprint");
                }
                row.add("🏠 Main Menu");
                keyboard.add(row);

                keyboardMarkup.setKeyboard(keyboard); // Set the keyboard rows
                message.setReplyMarkup(keyboardMarkup);

                bot.execute(message);
                logger.info(chatId, "No active sprint message sent");
                return;
            }

            // Get tasks in the sprint
            logger.debug(chatId, "Fetching tasks for sprint ID: {}", activeSprint.get().getId());
            List<ToDoItem> sprintTasks = botService.findTasksBySprintId(activeSprint.get().getId());
            logger.debug(chatId, "Found {} tasks in the sprint", sprintTasks.size());

            if (sprintTasks.isEmpty()) {
                logger.info(chatId, "No tasks found in sprint ID {} ({})",
                        activeSprint.get().getId(), activeSprint.get().getName());
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("There are no tasks in the current sprint: " + activeSprint.get().getName());

                // Create keyboard for next actions when sprint exists but has no tasks
                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                keyboardMarkup.setResizeKeyboard(true);
                List<KeyboardRow> keyboard = new ArrayList<>();

                KeyboardRow row = new KeyboardRow();
                row.add("📝 Create New Task");
                row.add("➕ Assign Task to Sprint");
                keyboard.add(row);

                row = new KeyboardRow();
                row.add("🏠 Main Menu");
                keyboard.add(row);

                keyboardMarkup.setKeyboard(keyboard); // Set the keyboard rows
                message.setReplyMarkup(keyboardMarkup);

                bot.execute(message);
                logger.info(chatId, "Empty sprint message sent");
                return;
            }

            // Build board text
            String boardText = buildSprintBoardText(activeSprint.get(), sprintTasks);
            logger.debug(chatId, "Sprint board text created");

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(boardText);

            // Create keyboard with appropriate options for sprint board view
            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            keyboardMarkup.setResizeKeyboard(true);
            List<KeyboardRow> keyboard = new ArrayList<>();

            KeyboardRow row = new KeyboardRow();
            row.add("🔄 My Active Tasks");
            row.add("📝 Create New Task");
            keyboard.add(row);

            row = new KeyboardRow();
            row.add("✅ Mark Task Complete");
            row.add("➕ Assign Task to Sprint");
            keyboard.add(row);

            if (state.getUser().isManager()) {
                row = new KeyboardRow();
                row.add("⏹️ End Active Sprint");
                keyboard.add(row);
            }

            row = new KeyboardRow();
            row.add("🏠 Main Menu");
            keyboard.add(row);

            keyboardMarkup.setKeyboard(keyboard); // Set the keyboard rows
            message.setReplyMarkup(keyboardMarkup);

            bot.execute(message);
            logger.info(chatId, "Sprint board sent");
        } catch (Exception e) {
            logger.error(chatId, "Error showing sprint board", e);

            try {
                // Create a safe error message with a valid keyboard
                SendMessage errorMessage = new SendMessage();
                errorMessage.setChatId(chatId);
                errorMessage.setText("❌ There was an error retrieving the sprint board. Please try again later.");

                // Always provide a valid keyboard for error messages
                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                keyboardMarkup.setResizeKeyboard(true);
                List<KeyboardRow> keyboard = new ArrayList<>();

                KeyboardRow row = new KeyboardRow();
                row.add("🏠 Main Menu");
                keyboard.add(row);

                keyboardMarkup.setKeyboard(keyboard); // Set the keyboard rows
                errorMessage.setReplyMarkup(keyboardMarkup);

                bot.execute(errorMessage);
                logger.info(chatId, "Error message with safe keyboard sent");
            } catch (Exception ex) {
                // If even the error handling fails, log it but don't throw
                logger.error(chatId, "Failed to send error message", ex);
            }
        }
    }

    /**
     * Build sprint board text with tasks grouped by status
     */
    private String buildSprintBoardText(Sprint sprint, List<ToDoItem> tasks) {
        StringBuilder boardText = new StringBuilder();
        boardText.append("📊 Sprint Board: ").append(sprint.getName()).append("\n\n");

        // Group tasks by status
        Map<String, List<ToDoItem>> tasksByStatus = new HashMap<>();

        for (ToDoItem task : tasks) {
            String status = task.getStatus() != null ? task.getStatus() : "BACKLOG";
            tasksByStatus.computeIfAbsent(status, k -> new ArrayList<>()).add(task);
        }

        // Add tasks by status
        for (TaskStatus status : TaskStatus.values()) {
            String statusName = status.name();
            if (tasksByStatus.containsKey(statusName)) {
                boardText.append("✦ ").append(status.getDisplayName()).append(" ✦\n");

                for (ToDoItem task : tasksByStatus.get(statusName)) {
                    boardText.append("- ID: ").append(task.getID())
                            .append(" | ").append(task.getTitle())
                            .append(" | Est: ").append(task.getEstimatedHours()).append("h");

                    if (task.getActualHours() != null) {
                        boardText.append(" | Act: ").append(task.getActualHours()).append("h");
                    }

                    boardText.append("\n");
                }

                boardText.append("\n");
            }
        }

        return boardText.toString();
    }

    /**
     * Start the process of creating a new sprint
     */
    public void startSprintCreation(long chatId, UserBotState state) {
        logger.info(chatId, "Starting sprint creation process for user: {}", state.getUser().getFullName());
        try {
            // Check if user is a manager
            if (!state.getUser().isManager()) {
                logger.warn(chatId, "User with ID {} is not a manager, cannot create sprints",
                        state.getUser().getId());
                MessageHandler.sendErrorMessage(chatId, "Only managers can create new sprints.", bot);
                return;
            }

            // Set state for sprint creation mode
            state.setSprintCreationMode(true);
            state.setSprintCreationStage("NAME");
            logger.debug(chatId, "Set user state: sprintCreationMode=true, sprintCreationStage=NAME");

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Let's create a new Sprint. First, please enter the Sprint name:");

            // Hide keyboard for text input
            message.setReplyMarkup(KeyboardFactory.createEmptyKeyboard());
            logger.debug(chatId, "Created sprint name prompt with keyboard removed");

            bot.execute(message);
            logger.info(chatId, "Sprint name prompt sent");
        } catch (Exception e) {
            logger.error(chatId, "Error starting sprint creation", e);
            MessageHandler.sendErrorMessage(chatId,
                    "There was an error starting sprint creation. Please try again later.", bot);
        }
    }

    /**
     * Process sprint creation stages
     */
    public void processSprintCreation(long chatId, String messageText, UserBotState state) {
        logger.info(chatId, "Processing sprint creation, stage: {}", state.getSprintCreationStage());
        logger.debug(chatId, "Sprint creation input: '{}'", messageText);
        try {
            String stage = state.getSprintCreationStage();

            if (stage == null) {
                logger.warn(chatId, "Sprint creation stage is null, assuming NAME stage");
                stage = "NAME";
                state.setSprintCreationStage(stage);
            }

            switch (stage) {
                case "NAME":
                    processSprintName(chatId, messageText, state);
                    break;
                case "DESCRIPTION":
                    processSprintDescription(chatId, messageText, state);
                    break;
                case "START_DATE":
                    processSprintStartDate(chatId, messageText, state);
                    break;
                case "END_DATE":
                    processSprintEndDate(chatId, messageText, state);
                    break;
                case "CONFIRMATION":
                    processSprintConfirmation(chatId, messageText, state);
                    break;
                default:
                    logger.warn(chatId, "Unknown sprint creation stage: {}", stage);
                    MessageHandler.sendErrorMessage(chatId,
                            "An error occurred in the sprint creation process. Please try again.", bot);
                    state.resetSprintCreation();
            }
        } catch (Exception e) {
            logger.error(chatId, "Error in sprint creation process", e);
            MessageHandler.sendErrorMessage(chatId,
                    "There was an error in the sprint creation process. Please try again.", bot);

            // Reset sprint creation state
            state.resetSprintCreation();
        }
    }

    /**
     * Process sprint name
     */
    private void processSprintName(long chatId, String messageText, UserBotState state) {
        // Store sprint name and ask for description
        logger.debug(chatId, "Storing sprint name: '{}'", messageText);
        state.setTempSprintName(messageText);
        state.setSprintCreationStage("DESCRIPTION");
        logger.debug(chatId, "Updated stage to DESCRIPTION");

        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Great! Now please provide a description for the sprint:");
            bot.execute(message);
            logger.info(chatId, "Description prompt sent");
        } catch (TelegramApiException e) {
            logger.error(chatId, "Error sending description prompt", e);
            throw new RuntimeException("Failed to send message", e);
        }
    }

    /**
     * Process sprint description
     */
    private void processSprintDescription(long chatId, String messageText, UserBotState state) {
        // Store description and ask for start date
        logger.debug(chatId, "Storing sprint description: '{}'", messageText);
        state.setTempSprintDescription(messageText);
        state.setSprintCreationStage("START_DATE");
        logger.debug(chatId, "Updated stage to START_DATE");

        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Please enter the start date (YYYY-MM-DD format):");
            bot.execute(message);
            logger.info(chatId, "Start date prompt sent");
        } catch (TelegramApiException e) {
            logger.error(chatId, "Error sending start date prompt", e);
            throw new RuntimeException("Failed to send message", e);
        }
    }

    /**
     * Process sprint start date
     */
    private void processSprintStartDate(long chatId, String messageText, UserBotState state) {
        // Validate and store start date, then ask for end date
        try {
            // Simple date validation
            if (!messageText.matches("\\d{4}-\\d{2}-\\d{2}")) {
                throw new IllegalArgumentException("Invalid date format");
            }

            logger.debug(chatId, "Storing sprint start date: '{}'", messageText);
            state.setTempSprintStartDate(messageText + "T00:00:00Z"); // Add time component for OffsetDateTime parsing
            state.setSprintCreationStage("END_DATE");
            logger.debug(chatId, "Updated stage to END_DATE");

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Please enter the end date (YYYY-MM-DD format):");
            bot.execute(message);
            logger.info(chatId, "End date prompt sent");
        } catch (Exception e) {
            logger.warn(chatId, "Invalid date format: '{}'", messageText, e);
            try {
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Please enter a valid date in YYYY-MM-DD format (e.g., 2025-04-30):");
                bot.execute(message);
                logger.info(chatId, "Date format error message sent");
            } catch (TelegramApiException ex) {
                logger.error(chatId, "Error sending date format error message", ex);
            }
        }
    }

    /**
     * Process sprint end date
     */
    private void processSprintEndDate(long chatId, String messageText, UserBotState state) {
        // Validate end date and move to confirmation
        try {
            // Simple date validation
            if (!messageText.matches("\\d{4}-\\d{2}-\\d{2}")) {
                throw new IllegalArgumentException("Invalid date format");
            }

            // Simple validation to ensure end date is after start date
            String startDate = state.getTempSprintStartDate();
            String endDateWithTime = messageText + "T00:00:00Z"; // Add time component for comparison
            if (startDate != null && startDate.compareTo(endDateWithTime) >= 0) {
                logger.warn(chatId, "End date must be after start date: start='{}', end='{}'",
                        startDate, endDateWithTime);
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("End date must be after the start date. Please enter a valid end date:");
                bot.execute(message);
                logger.info(chatId, "End date validation error message sent");
                return;
            }

            logger.debug(chatId, "Storing sprint end date: '{}'", messageText);
            state.setTempSprintEndDate(endDateWithTime);
            state.setSprintCreationStage("CONFIRMATION");
            logger.debug(chatId, "Updated stage to CONFIRMATION");

            // Build confirmation message
            StringBuilder summary = new StringBuilder();
            summary.append("Please confirm the sprint details:\n\n");
            summary.append("Name: ").append(state.getTempSprintName()).append("\n");
            summary.append("Description: ").append(state.getTempSprintDescription()).append("\n");
            summary.append("Start Date: ").append(state.getTempSprintStartDate().substring(0, 10)).append("\n");
            summary.append("End Date: ").append(state.getTempSprintEndDate().substring(0, 10)).append("\n\n");
            summary.append("Is this correct?");
            logger.debug(chatId, "Sprint confirmation summary created");

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(summary.toString());
            message.setReplyMarkup(KeyboardFactory.createSprintConfirmationKeyboard());

            bot.execute(message);
            logger.info(chatId, "Confirmation prompt sent");
        } catch (Exception e) {
            logger.warn(chatId, "Invalid date format or error creating confirmation: '{}'", messageText, e);
            try {
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Please enter a valid date in YYYY-MM-DD format (e.g., 2025-04-30):");
                bot.execute(message);
                logger.info(chatId, "Date format error message sent");
            } catch (TelegramApiException ex) {
                logger.error(chatId, "Error sending date format error message", ex);
            }
        }
    }

    /**
     * Process sprint confirmation
     */
    private void processSprintConfirmation(long chatId, String messageText, UserBotState state) {
        logger.debug(chatId, "Processing confirmation response: '{}'", messageText);
        if (messageText.equals("Yes, create sprint")) {
            logger.info(chatId, "User confirmed sprint creation");
            createSprint(chatId, state);
        } else {
            // Cancel sprint creation
            logger.info(chatId, "User cancelled sprint creation");
            state.resetSprintCreation();
            logger.debug(chatId, "Reset sprint creation state");

            try {
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Sprint creation cancelled. What would you like to do next?");

                // Create keyboard with options
                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                keyboardMarkup.setResizeKeyboard(true);
                // Add keyboard rows and options
                message.setReplyMarkup(keyboardMarkup);

                bot.execute(message);
                logger.info(chatId, "Sprint creation cancellation message sent");

                // Return to main screen
                MessageHandler.showMainScreen(chatId, state, bot);
            } catch (TelegramApiException e) {
                logger.error(chatId, "Error sending cancellation message", e);
            }
        }
    }

    /**
     * Create a sprint from the collected information
     */
    private void createSprint(long chatId, UserBotState state) {
        try {
            // Create the sprint
            Sprint sprint = new Sprint();
            sprint.setName(state.getTempSprintName());
            sprint.setDescription(state.getTempSprintDescription());

            // Parse dates from the stored strings
            sprint.setStartDate(OffsetDateTime.parse(state.getTempSprintStartDate()));
            sprint.setEndDate(OffsetDateTime.parse(state.getTempSprintEndDate()));

            // Set the team
            Team team = state.getUser().getTeam();
            if (team == null) {
                logger.warn(chatId, "User with ID {} is not associated with any team", state.getUser().getId());
                MessageHandler.sendErrorMessage(chatId, "You are not associated with any team. Cannot create sprint.",
                        bot);
                return;
            }
            sprint.setTeam(team);
            sprint.setStatus("ACTIVE");

            logger.debug(chatId, "Creating sprint: name='{}', teamId={}, startDate='{}', endDate='{}'",
                    sprint.getName(), sprint.getTeam() != null ? sprint.getTeam().getId() : "N/A",
                    sprint.getStartDate(), sprint.getEndDate());

            // Save the sprint
            Sprint savedSprint = botService.createSprint(sprint);
            logger.info(chatId, "Sprint created successfully with ID {}", savedSprint.getId());

            // Reset state
            state.resetSprintCreation();
            logger.debug(chatId, "Reset sprint creation state");

            // Show success message
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("✅ Sprint created successfully with ID: " + savedSprint.getId());

            // Create keyboard for next actions
            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            keyboardMarkup.setResizeKeyboard(true);
            // Add keyboard rows and options
            message.setReplyMarkup(keyboardMarkup);

            bot.execute(message);
            logger.info(chatId, "Sprint creation success message sent");

            // Return to main screen
            MessageHandler.showMainScreen(chatId, state, bot);
        } catch (Exception e) {
            logger.error(chatId, "Error creating sprint", e);
            MessageHandler.sendErrorMessage(chatId, "There was an error creating the sprint. Please try again.", bot);
            state.resetSprintCreation();
        }
    }

    /**
     * Start the process of ending the active sprint
     */
    public void startEndActiveSprint(long chatId, UserBotState state) {
        logger.info(chatId, "Starting end active sprint process for user: {}", state.getUser().getFullName());
        try {
            // Check if user is a manager
            if (!state.getUser().isManager()) {
                logger.warn(chatId, "User with ID {} is not a manager, cannot end sprints", state.getUser().getId());
                MessageHandler.sendErrorMessage(chatId, "Only managers can end sprints.", bot);
                return;
            }

            // Check if user is in a team
            Long teamId = state.getUser().getTeam() != null ? state.getUser().getTeam().getId() : null;
            logger.debug(chatId, "User team ID: {}", teamId);

            if (teamId == null) {
                logger.warn(chatId, "User with ID {} is not associated with any team", state.getUser().getId());
                MessageHandler.sendErrorMessage(chatId, "You are not associated with any team.", bot);
                return;
            }

            // Get the active sprint
            logger.debug(chatId, "Fetching active sprint for team ID: {}", teamId);
            Optional<Sprint> activeSprint = botService.findActiveSprintByTeamId(teamId);
            logger.debug(chatId, "Active sprint found: {}", activeSprint.isPresent());

            if (activeSprint.isEmpty()) {
                logger.warn(chatId, "No active sprint found for team ID {}", teamId);
                MessageHandler.sendErrorMessage(chatId, "There is no active sprint for your team.", bot);
                return;
            }

            // Set state for ending active sprint
            state.setEndSprintMode(true);
            logger.debug(chatId, "Set user state: endSprintMode=true");

            // Ask for confirmation
            StringBuilder confirmationText = new StringBuilder();
            confirmationText.append("Are you sure you want to end the active sprint?\n\n");
            confirmationText.append("Sprint: ").append(activeSprint.get().getName()).append("\n");
            confirmationText.append("ID: ").append(activeSprint.get().getId()).append("\n");
            confirmationText.append("Period: ").append(activeSprint.get().getStartDate()).append(" to ")
                    .append(activeSprint.get().getEndDate()).append("\n\n");
            confirmationText.append("This will set the sprint as inactive and cannot be undone.");

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(confirmationText.toString());

            // Create keyboard with confirmation options
            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            keyboardMarkup.setResizeKeyboard(true);
            // Add keyboard rows and options
            message.setReplyMarkup(keyboardMarkup);

            bot.execute(message);
            logger.info(chatId, "End sprint confirmation prompt sent");
        } catch (Exception e) {
            logger.error(chatId, "Error starting end active sprint process", e);
            MessageHandler.sendErrorMessage(chatId, "There was an error in the process. Please try again later.", bot);
        }
    }

    /**
     * Process ending active sprint
     */
    public void processEndActiveSprint(long chatId, String messageText, UserBotState state) {
        logger.info(chatId, "Processing end active sprint");
        logger.debug(chatId, "Input message: '{}'", messageText);
        try {
            if (messageText.equals("Yes, end sprint")) {
                // Get the active sprint
                Long teamId = state.getUser().getTeam().getId();
                logger.debug(chatId, "Finding active sprint for team ID: {}", teamId);

                Optional<Sprint> activeSprint = botService.findActiveSprintByTeamId(teamId);
                if (activeSprint.isPresent()) {
                    // End the sprint
                    logger.info(chatId, "Ending active sprint ID {} for team ID {}",
                            activeSprint.get().getId(), teamId);
                    Sprint completedSprint = botService.completeSprint(activeSprint.get().getId());

                    // Reset state
                    state.setEndSprintMode(false);
                    logger.debug(chatId, "Reset end sprint mode");

                    // Send success message
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText("✅ Sprint \"" + completedSprint.getName() + "\" has been ended successfully.");

                    // Create keyboard for next actions
                    ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                    keyboardMarkup.setResizeKeyboard(true);
                    // Add keyboard rows and options
                    message.setReplyMarkup(keyboardMarkup);

                    bot.execute(message);
                    logger.info(chatId, "Sprint end success message sent");
                } else {
                    logger.warn(chatId, "No active sprint found for team ID {} when trying to end it", teamId);
                    MessageHandler.sendErrorMessage(chatId,
                            "Could not find the active sprint. It may have already been ended.", bot);

                    // Reset state
                    state.setEndSprintMode(false);
                    logger.debug(chatId, "Reset end sprint mode");
                }
            } else if (messageText.equals("No, cancel")) {
                // Cancel ending sprint
                logger.info(chatId, "User cancelled ending sprint");
                state.setEndSprintMode(false);
                logger.debug(chatId, "Reset end sprint mode");

                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Sprint end process cancelled.");

                // Create keyboard with options
                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                keyboardMarkup.setResizeKeyboard(true);
                // Add keyboard rows and options
                message.setReplyMarkup(keyboardMarkup);

                bot.execute(message);
                logger.info(chatId, "Sprint end cancellation message sent");
            } else {
                logger.warn(chatId, "Unexpected message in end sprint mode: '{}'", messageText);
                MessageHandler.sendErrorMessage(chatId, "Please select one of the options.", bot);
            }
        } catch (Exception e) {
            logger.error(chatId, "Error processing end active sprint", e);
            MessageHandler.sendErrorMessage(chatId,
                    "There was an error ending the sprint. Please try again later.", bot);

            // Reset state on error
            state.setEndSprintMode(false);
            logger.debug(chatId, "Reset end sprint mode after error");
        }
    }

    /**
     * Start the process of assigning a task to a sprint
     */
    public void startAssignTaskToSprint(long chatId, UserBotState state) {
        logger.info(chatId, "Starting assign task to sprint process for user: {}", state.getUser().getFullName());
        try {
            // Check if user is a developer or manager
            if (!state.getUser().isDeveloper() && !state.getUser().isManager()) {
                logger.warn(chatId, "User with ID {} is not a developer or manager", state.getUser().getId());
                MessageHandler.sendErrorMessage(chatId, "Only developers and managers can assign tasks to sprints.",
                        bot);
                return;
            }

            // Get user's active tasks not yet in a sprint
            logger.debug(chatId, "Fetching active tasks not in sprint for user ID: {}", state.getUser().getId());
            List<ToDoItem> backlogTasks = botService.findActiveTasksByAssigneeId(state.getUser().getId()).stream()
                    .filter(task -> task.getSprintId() == null)
                    .toList();
            logger.debug(chatId, "Found {} backlog tasks for user", backlogTasks.size());

            if (backlogTasks.isEmpty()) {
                logger.info(chatId, "No backlog tasks found for user with ID {}", state.getUser().getId());
                MessageHandler.sendErrorMessage(chatId, "You don't have any backlog tasks to assign to a sprint.", bot);
                return;
            }

            // Try to find the active sprint for the user's team
            Long teamId = state.getUser().getTeam() != null ? state.getUser().getTeam().getId() : null;
            logger.debug(chatId, "User team ID: {}", teamId);

            if (teamId == null) {
                logger.warn(chatId, "User with ID {} is not associated with any team", state.getUser().getId());
                MessageHandler.sendErrorMessage(chatId, "You are not associated with any team.", bot);
                return;
            }

            // Get the active sprint
            logger.debug(chatId, "Fetching active sprint for team ID: {}", teamId);
            Optional<Sprint> activeSprint = botService.findActiveSprintByTeamId(teamId);
            logger.debug(chatId, "Active sprint found: {}", activeSprint.isPresent());

            if (activeSprint.isEmpty()) {
                logger.warn(chatId, "No active sprint found for team ID {}", teamId);
                MessageHandler.sendErrorMessage(chatId, "There is no active sprint for your team.", bot);
                return;
            }

            // Set state for assigning task to sprint
            state.setAssignToSprintMode(true);
            state.setAssignToSprintStage("SELECT_TASK");
            state.setTempSprintId(activeSprint.get().getId());
            logger.debug(chatId,
                    "Set user state: assignToSprintMode=true, assignToSprintStage=SELECT_TASK, sprintId={}",
                    activeSprint.get().getId());

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Please select a task to add to the sprint \"" + activeSprint.get().getName() + "\":");

            // Create keyboard with task selection
            message.setReplyMarkup(KeyboardFactory.createTaskSelectionForSprintKeyboard(backlogTasks));
            logger.debug(chatId, "Created task selection keyboard with {} tasks", backlogTasks.size());

            bot.execute(message);
            logger.info(chatId, "Task selection prompt sent");
        } catch (Exception e) {
            logger.error(chatId, "Error starting assign task to sprint process", e);
            MessageHandler.sendErrorMessage(chatId, "There was an error in the process. Please try again later.", bot);
        }
    }

    /**
     * Process task assignment to sprint
     */
    public void processAssignTaskToSprint(long chatId, String messageText, UserBotState state) {
        logger.info(chatId, "Processing assign task to sprint");
        logger.debug(chatId, "Input message: '{}'", messageText);
        try {
            if (messageText.equals("Cancel")) {
                logger.info(chatId, "User cancelled assign task to sprint");
                state.resetAssignToSprint();
                logger.debug(chatId, "Reset assign to sprint state");
                MessageHandler.showDeveloperTaskMenu(chatId, state, bot);
                return;
            }

            // Extract task ID from message
            int taskId;
            if (messageText.startsWith("ID: ")) {
                // Parse from "ID: X - Title"
                String idPart = messageText.substring(4, messageText.indexOf(" - "));
                taskId = Integer.parseInt(idPart);
                logger.debug(chatId, "Extracted task ID {} from selection", taskId);
            } else {
                // Try to parse as a direct ID
                try {
                    taskId = Integer.parseInt(messageText);
                    logger.debug(chatId, "Parsed task ID {} directly from input", taskId);
                } catch (NumberFormatException e) {
                    logger.warn(chatId, "Invalid task ID format: '{}'", messageText);
                    MessageHandler.sendErrorMessage(chatId, "Please enter a valid task ID or select from the list.",
                            bot);
                    return;
                }
            }

            // Assign task to sprint
            logger.debug(chatId, "Assigning task ID {} to sprint ID {}", taskId, state.getTempSprintId());
            ToDoItem task = botService.assignTaskToSprint(taskId, state.getTempSprintId());
            logger.info(chatId, "Task ID {} successfully assigned to sprint ID {}", taskId, state.getTempSprintId());

            // Reset state
            state.resetAssignToSprint();
            logger.debug(chatId, "Reset assign to sprint state");

            // Show success message
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("✅ Task \"" + task.getTitle() + "\" has been added to the sprint successfully!");

            // Create keyboard for next actions
            message.setReplyMarkup(KeyboardFactory.createAfterTaskCompletionKeyboard());
            logger.debug(chatId, "Created success keyboard");

            bot.execute(message);
            logger.info(chatId, "Success message sent");
        } catch (Exception e) {
            logger.error(chatId, "Error assigning task to sprint", e);
            MessageHandler.sendErrorMessage(chatId,
                    "There was an error assigning the task to the sprint. Please try again later.", bot);

            // Reset state
            state.resetAssignToSprint();
            logger.debug(chatId, "Reset assign to sprint state after error");
        }
    }
}