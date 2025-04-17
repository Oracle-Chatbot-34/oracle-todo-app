package com.springboot.MyTodoList.bot.controller;

import com.springboot.MyTodoList.bot.handler.*;
import com.springboot.MyTodoList.bot.service.BotService;
import com.springboot.MyTodoList.bot.util.BotLogger;
import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.model.bot.UserBotState;
import com.springboot.MyTodoList.service.SprintService;
import com.springboot.MyTodoList.service.ToDoItemService;
import com.springboot.MyTodoList.service.UserService;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main controller for the Telegram bot interactions
 * Handles incoming messages and delegates to specific handlers
 */
public class ToDoItemBotController extends TelegramLongPollingBot {
    private static final BotLogger logger = new BotLogger(ToDoItemBotController.class);

    private final String botName;
    private final BotService botService;
    private final AuthenticationHandler authHandler;
    private final TaskCreationHandler taskCreationHandler;
    private final TaskCompletionHandler taskCompletionHandler;
    private final SprintHandler sprintHandler;

    // Store user states
    private final ConcurrentHashMap<Long, UserBotState> userStates = new ConcurrentHashMap<>();

    public ToDoItemBotController(
            String botToken,
            String botName,
            ToDoItemService toDoItemService,
            UserService userService,
            SprintService sprintService) {
        super(botToken);
        logger.info("Initializing ToDoItemBotController with token: {}, name: {}",
                botToken.substring(0, 5) + "...", botName);
        this.botName = botName;

        // Initialize the service and handlers
        this.botService = new BotService(toDoItemService, userService, sprintService);
        this.authHandler = new AuthenticationHandler(botService, this);
        this.taskCreationHandler = new TaskCreationHandler(botService, this);
        this.taskCompletionHandler = new TaskCompletionHandler(botService, this);
        this.sprintHandler = new SprintHandler(botService, this);

        logger.info("ToDoItemBotController successfully initialized");
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public void onUpdateReceived(Update update) {
        long chatId = 0;

        try {
            // Handle callback queries (inline keyboard buttons)
            if (update.hasCallbackQuery()) {
                CallbackQuery callbackQuery = update.getCallbackQuery();
                String callbackData = callbackQuery.getData();
                Message message = callbackQuery.getMessage();
                chatId = message.getChatId();

                logger.info(chatId, "Received callback query: '{}'", callbackData);

                // Get or initialize user state
                UserBotState state = userStates.getOrDefault(chatId, new UserBotState());
                userStates.put(chatId, state);

                // Make sure user is authenticated
                if (!state.isAuthenticated()) {
                    logger.warn(chatId, "Received callback from unauthenticated user, ignoring");
                    return;
                }

                // Handle callback based on prefix
                if (callbackData.startsWith("sprint_")) {
                    // Sprint management callbacks
                    sprintHandler.processSprintModeCallback(chatId, callbackData, state, message.getMessageId());
                } else if (callbackData.startsWith("main_")) {
                    // Main menu callbacks
                    processMainMenuCallback(chatId, callbackData, state);
                } else if (callbackData.startsWith("date_")) {
                    // Date selection for sprint creation
                    if (state.isSprintMode() && state.getSprintModeStage() != null) {
                        if (state.getSprintModeStage().equals("CREATE_START_DATE")) {
                            sprintHandler.processSprintModeInput(chatId, callbackData, state);
                        } else if (state.getSprintModeStage().equals("CREATE_END_DATE")) {
                            sprintHandler.processSprintModeInput(chatId, callbackData, state);
                        }
                    }
                } else if (callbackData.startsWith("task_")) {
                    // Task management callbacks
                    processTaskCallback(chatId, callbackData, state, message.getMessageId());
                } else if (callbackData.startsWith("team_")) {
                    // Team management callbacks
                    processTeamCallback(chatId, callbackData, state, message.getMessageId());
                } else if (callbackData.startsWith("kpi_")) {
                    // KPI Dashboard callbacks
                    processKpiCallback(chatId, callbackData, state, message.getMessageId());
                } else {
                    logger.warn(chatId, "Unknown callback data: {}", callbackData);
                }

                return;
            }

            // Process message if it exists and has text
            if (update.hasMessage() && update.getMessage().hasText()) {
                String messageText = update.getMessage().getText();
                chatId = update.getMessage().getChatId();
                logger.info(chatId, "Received message: '{}'", messageText);

                // Get or initialize user state
                UserBotState state = userStates.getOrDefault(chatId, new UserBotState());
                userStates.put(chatId, state);
                logger.debug(chatId,
                        "User state: authenticated={}, sprintMode={}, newTaskMode={}, taskCompletionMode={}",
                        state.isAuthenticated(), state.isSprintMode(), state.isNewTaskMode(),
                        state.isTaskCompletionMode());

                // Handle authentication flow
                if (!state.isAuthenticated()) {
                    handleUnauthenticatedUser(chatId, messageText, state);
                    return;
                }

                // Handle active modes
                if (state.isSprintMode()) {
                    sprintHandler.processSprintModeInput(chatId, messageText, state);
                    return;
                }

                if (state.isNewTaskMode()) {
                    taskCreationHandler.processTaskCreation(chatId, messageText, state);
                    return;
                }

                if (state.isTaskCompletionMode()) {
                    taskCompletionHandler.processTaskCompletion(chatId, messageText, state);
                    return;
                }

                if (state.isAssignToSprintMode()) {
                    sprintHandler.processAssignTaskToSprint(chatId, messageText, state);
                    return;
                }

                if (state.isSprintCreationMode()) {
                    sprintHandler.processSprintCreation(chatId, messageText, state);
                    return;
                }

                if (state.isEndSprintMode()) {
                    sprintHandler.processEndActiveSprint(chatId, messageText, state);
                    return;
                }

                // Handle standard commands
                handleCommands(chatId, messageText, state);

                // Update user state in map
                userStates.put(chatId, state);
            } else {
                // Handle non-text messages
                if (update.hasMessage()) {
                    chatId = update.getMessage().getChatId();
                    logger.warn(chatId, "Received non-text message");
                    MessageHandler.sendMessage(chatId, "I can only process text messages.", this);
                } else {
                    logger.warn("Received update without message: {}", update.getUpdateId());
                }
            }
        } catch (Exception e) {
            // Global error handler
            logger.error(chatId, "Unexpected error in bot operation", e);
            if (chatId != 0) {
                MessageHandler.sendErrorMessage(chatId, "An unexpected error occurred. Please try again later.", this);
            }
        }
    }

    /**
     * Handle messages from unauthenticated users
     */
    private void handleUnauthenticatedUser(long chatId, String messageText, UserBotState state) {
        logger.info(chatId, "Processing unauthenticated user message");
        if (messageText.equals("/start")) {
            logger.info(chatId, "Processing start command for unauthenticated user");
            authHandler.handleInitialGreeting(chatId);
        } else {
            logger.info(chatId, "Processing authentication attempt with employee ID: {}", messageText);
            authHandler.handleAuthentication(chatId, messageText, state);
        }
    }

    /**
     * Process main menu callbacks
     */
    private void processMainMenuCallback(long chatId, String callbackData, UserBotState state) {
        logger.info(chatId, "Processing main menu callback: {}", callbackData);
        try {
            switch (callbackData) {
                case "main_sprint":
                    // Enter sprint management mode
                    sprintHandler.enterSprintMode(chatId, state);
                    break;
                case "main_tasks":
                    // Show tasks menu
                    MessageHandler.showDeveloperTaskMenu(chatId, state, this);
                    break;
                case "main_help":
                    // Show help information
                    MessageHandler.showHelpInformation(chatId, this);
                    break;
                case "main_team":
                    // Show team management options (for managers)
                    if (state.getUser().isManager()) {
                        showTeamManagement(chatId, state);
                    } else {
                        MessageHandler.sendErrorMessage(chatId, "You don't have access to team management features.",
                                this);
                        MessageHandler.showMainScreen(chatId, state, this);
                    }
                    break;
                case "main_kpi":
                    // Show KPI dashboard
                    showKpiDashboard(chatId, state);
                    break;
                default:
                    logger.warn(chatId, "Unknown main menu callback: {}", callbackData);
                    MessageHandler.showMainScreen(chatId, state, this);
            }
        } catch (Exception e) {
            logger.error(chatId, "Error processing main menu callback", e);
            MessageHandler.sendErrorMessage(chatId, "Failed to process your request. Please try again.", this);
            MessageHandler.showMainScreen(chatId, state, this);
        }
    }

    /**
     * Process task callbacks
     */
    private void processTaskCallback(long chatId, String callbackData, UserBotState state, Integer messageId) {
        logger.info(chatId, "Processing task callback: {}", callbackData);
        try {
            if (callbackData.startsWith("task_complete_")) {
                // Extract task ID from callback data
                int taskId = Integer.parseInt(callbackData.substring("task_complete_".length()));
                taskCompletionHandler.handleTaskStatusUpdate(chatId, state, "DONE", taskId);
            } else if (callbackData.startsWith("task_undo_")) {
                int taskId = Integer.parseInt(callbackData.substring("task_undo_".length()));
                taskCompletionHandler.handleTaskStatusUpdate(chatId, state, "UNDO", taskId);
            } else if (callbackData.startsWith("task_delete_")) {
                int taskId = Integer.parseInt(callbackData.substring("task_delete_".length()));
                taskCompletionHandler.handleTaskStatusUpdate(chatId, state, "DELETE", taskId);
            } else if (callbackData.equals("task_create_new")) {
                taskCreationHandler.startTaskCreation(chatId, state);
            } else if (callbackData.equals("task_view_active")) {
                showActiveTasksForUser(chatId, state);
            } else if (callbackData.equals("task_view_all")) {
                MessageHandler.showTaskList(chatId, botService.getAllToDoItems(state.getUser().getId()), state, this);
            } else if (callbackData.equals("task_back_to_menu")) {
                MessageHandler.showMainScreen(chatId, state, this);
            } else if (callbackData.startsWith("task_assign_to_sprint_")) {
                int taskId = Integer.parseInt(callbackData.substring("task_assign_to_sprint_".length()));
                startAssignTaskToSprint(chatId, taskId, state);
            } else {
                logger.warn(chatId, "Unknown task callback: {}", callbackData);
                MessageHandler.showDeveloperTaskMenu(chatId, state, this);
            }
        } catch (Exception e) {
            logger.error(chatId, "Error processing task callback", e);
            MessageHandler.sendErrorMessage(chatId, "Failed to process task action. Please try again.", this);
            MessageHandler.showDeveloperTaskMenu(chatId, state, this);
        }
    }

    /**
     * Start process to assign a task to a sprint
     */
    private void startAssignTaskToSprint(long chatId, int taskId, UserBotState state) {
        logger.info(chatId, "Starting process to assign task {} to a sprint", taskId);
        try {
            // Get active sprints
            List<Sprint> activeSprints = botService.findAllSprints().stream()
                    .filter(sprint -> "ACTIVE".equals(sprint.getStatus()))
                    .toList();

            if (activeSprints.isEmpty()) {
                MessageHandler.sendMessage(chatId, "No active sprints found. Please create a sprint first.", this);
                return;
            }

            // Save task ID in state
            state.setTempTaskId(taskId);
            state.setAssignToSprintMode(true);
            state.setAssignToSprintStage("SELECT_SPRINT");

            // Show sprint options
            StringBuilder message = new StringBuilder();
            message.append("Please select a sprint to assign task #").append(taskId).append(" to:\n\n");

            for (Sprint sprint : activeSprints) {
                message.append("• Sprint ID: ").append(sprint.getId())
                        .append(" - ").append(sprint.getName()).append("\n");
            }

            message.append("\nReply with the Sprint ID to assign the task.");

            MessageHandler.sendMessage(chatId, message.toString(), this);
        } catch (Exception e) {
            logger.error(chatId, "Error starting task assignment to sprint", e);
            MessageHandler.sendErrorMessage(chatId, "Failed to start task assignment process. Please try again.", this);
            state.resetAssignToSprint();
        }
    }

    /**
     * Process team management callbacks
     */
    private void processTeamCallback(long chatId, String callbackData, UserBotState state, Integer messageId) {
        logger.info(chatId, "Processing team callback: {}", callbackData);
        try {
            if (callbackData.equals("team_members")) {
                showTeamMembers(chatId, state, messageId);
            } else if (callbackData.equals("team_add_member")) {
                // Start process to add team member
                MessageHandler.sendAnimatedMessage(chatId, messageId,
                        "This feature is available in the web interface. Team membership management requires additional inputs and permissions.",
                        this, true);
            } else if (callbackData.equals("team_back_to_menu")) {
                MessageHandler.showMainScreen(chatId, state, this);
            } else {
                logger.warn(chatId, "Unknown team callback: {}", callbackData);
                showTeamManagement(chatId, state);
            }
        } catch (Exception e) {
            logger.error(chatId, "Error processing team callback", e);
            MessageHandler.sendErrorMessage(chatId, "Failed to process team action. Please try again.", this);
            MessageHandler.showMainScreen(chatId, state, this);
        }
    }

    /**
     * Process KPI dashboard callbacks
     */
    private void processKpiCallback(long chatId, String callbackData, UserBotState state, Integer messageId) {
        logger.info(chatId, "Processing KPI callback: {}", callbackData);
        try {
            if (callbackData.equals("kpi_view")) {
                // Show basic KPI information
                showKpiSummary(chatId, state, messageId);
            } else if (callbackData.equals("kpi_back_to_menu")) {
                MessageHandler.showMainScreen(chatId, state, this);
            } else {
                logger.warn(chatId, "Unknown KPI callback: {}", callbackData);
                showKpiDashboard(chatId, state);
            }
        } catch (Exception e) {
            logger.error(chatId, "Error processing KPI callback", e);
            MessageHandler.sendErrorMessage(chatId, "Failed to process KPI action. Please try again.", this);
            MessageHandler.showMainScreen(chatId, state, this);
        }
    }

    /**
     * Show KPI summary information with animation
     */
    private void showKpiSummary(long chatId, UserBotState state, Integer messageId) {
        logger.info(chatId, "Showing KPI summary for user: {}", state.getUser().getFullName());
        try {
            // Show animated loading
            EditMessageText loadingMessage = new EditMessageText();
            loadingMessage.setChatId(chatId);
            loadingMessage.setMessageId(messageId);
            loadingMessage.setText("Loading KPI data...\n⬜⬜⬜");
            loadingMessage.enableHtml(true);
            execute(loadingMessage);

            // Animation frames
            String[] frames = { "⬛⬜⬜", "⬛⬛⬜", "⬛⬛⬛", "✅" };
            for (String frame : frames) {
                Thread.sleep(300);
                EditMessageText updateMessage = new EditMessageText();
                updateMessage.setChatId(chatId);
                updateMessage.setMessageId(messageId);
                updateMessage.setText("Loading KPI data...\n" + frame);
                updateMessage.enableHtml(true);
                execute(updateMessage);
            }

            // Show final message
            StringBuilder kpiText = new StringBuilder();
            kpiText.append("<b>KPI Dashboard</b>\n\n");
            kpiText.append(
                    "The KPI dashboard is best viewed in the web interface where charts and detailed metrics are available.\n\n");
            kpiText.append("<b>Basic Statistics:</b>\n");

            // Get active sprint if any
            List<Sprint> activeSprintList = botService.findAllSprints().stream()
                    .filter(sprint -> "ACTIVE".equals(sprint.getStatus()))
                    .toList();

            if (!activeSprintList.isEmpty()) {
                Sprint activeSprint = activeSprintList.get(0);
                kpiText.append("• Active Sprint: ").append(activeSprint.getName()).append("\n");
                kpiText.append("• Sprint End Date: ").append(formatDate(activeSprint.getEndDate())).append("\n\n");
            } else {
                kpiText.append("• No active sprint\n\n");
            }

            EditMessageText kpiMessage = new EditMessageText();
            kpiMessage.setChatId(chatId);
            kpiMessage.setMessageId(messageId);
            kpiMessage.setText(kpiText.toString());
            kpiMessage.enableHtml(true);

            // Add back button
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            List<InlineKeyboardButton> row = new ArrayList<>();

            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText("🔙 Back to Menu");
            backButton.setCallbackData("kpi_back_to_menu");
            row.add(backButton);
            rows.add(row);

            markup.setKeyboard(rows);
            kpiMessage.setReplyMarkup(markup);

            execute(kpiMessage);
        } catch (Exception e) {
            logger.error(chatId, "Error showing KPI summary", e);
            try {
                EditMessageText errorMessage = new EditMessageText();
                errorMessage.setChatId(chatId);
                errorMessage.setMessageId(messageId);
                errorMessage.setText("There was an error accessing the KPI information. Please try again later.");
                errorMessage.enableHtml(true);
                execute(errorMessage);
            } catch (TelegramApiException ex) {
                logger.error(chatId, "Failed to send error message", ex);
            }
        }
    }

    /**
     * Format date for display
     */
    private String formatDate(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
    }

    /**
     * Show team members with animation
     */
    private void showTeamMembers(long chatId, UserBotState state, Integer messageId) {
        logger.info(chatId, "Showing team members for user: {}", state.getUser().getFullName());
        try {
            // Show animated loading
            EditMessageText loadingMessage = new EditMessageText();
            loadingMessage.setChatId(chatId);
            loadingMessage.setMessageId(messageId);
            loadingMessage.setText("Loading team members...\n⬜⬜⬜");
            loadingMessage.enableHtml(true);
            execute(loadingMessage);

            // Animation frames
            String[] frames = { "⬛⬜⬜", "⬛⬛⬜", "⬛⬛⬛", "✅" };
            for (String frame : frames) {
                Thread.sleep(300);
                EditMessageText updateMessage = new EditMessageText();
                updateMessage.setChatId(chatId);
                updateMessage.setMessageId(messageId);
                updateMessage.setText("Loading team members...\n" + frame);
                updateMessage.enableHtml(true);
                execute(updateMessage);
            }

            // Check if user has a team
            if (state.getUser().getTeam() == null) {
                EditMessageText noTeamMessage = new EditMessageText();
                noTeamMessage.setChatId(chatId);
                noTeamMessage.setMessageId(messageId);
                noTeamMessage.setText("You are not currently associated with any team.");
                noTeamMessage.enableHtml(true);
                execute(noTeamMessage);
                return;
            }

            // Get team members
            List<User> teamMembers = botService.findUsersByTeamId(state.getUser().getTeam().getId());

            StringBuilder message = new StringBuilder();
            message.append("<b>Team: ").append(state.getUser().getTeam().getName()).append("</b>\n\n");
            message.append("<b>Team Members:</b>\n");

            for (User member : teamMembers) {
                message.append("• ")
                        .append(member.getFullName())
                        .append(" (").append(member.getRole()).append(")\n");
            }

            // Add back button
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            List<InlineKeyboardButton> row = new ArrayList<>();

            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText("🔙 Back to Team Management");
            backButton.setCallbackData("team_back_to_menu");
            row.add(backButton);
            rows.add(row);

            markup.setKeyboard(rows);

            EditMessageText teamMessage = new EditMessageText();
            teamMessage.setChatId(chatId);
            teamMessage.setMessageId(messageId);
            teamMessage.setText(message.toString());
            teamMessage.enableHtml(true);
            teamMessage.setReplyMarkup(markup);

            execute(teamMessage);
        } catch (Exception e) {
            logger.error(chatId, "Error showing team members", e);
            try {
                EditMessageText errorMessage = new EditMessageText();
                errorMessage.setChatId(chatId);
                errorMessage.setMessageId(messageId);
                errorMessage.setText("There was an error retrieving team members. Please try again later.");
                errorMessage.enableHtml(true);
                execute(errorMessage);
            } catch (TelegramApiException ex) {
                logger.error(chatId, "Failed to send error message", ex);
            }
        }
    }

    /**
     * Show active tasks for the current user
     */
    private void showActiveTasksForUser(long chatId, UserBotState state) {
        logger.info(chatId, "Showing active tasks for user");
        try {
            // Show loading animation
            SendMessage loadingMessage = new SendMessage();
            loadingMessage.setChatId(chatId);
            loadingMessage.setText("Loading your active tasks...\n⬜⬜⬜");
            loadingMessage.enableHtml(true);

            Message sentMessage = execute(loadingMessage);

            // Animation frames
            String[] frames = { "⬛⬜⬜", "⬛⬛⬜", "⬛⬛⬛", "✅" };
            for (String frame : frames) {
                Thread.sleep(300);
                EditMessageText updateMessage = new EditMessageText();
                updateMessage.setChatId(chatId);
                updateMessage.setMessageId(sentMessage.getMessageId());
                updateMessage.setText("Loading your active tasks...\n" + frame);
                updateMessage.enableHtml(true);
                execute(updateMessage);
            }

            // Get active tasks for the user
            MessageHandler.showActiveTasksList(
                    chatId,
                    botService.getActiveToDoItems(state.getUser().getId()),
                    state,
                    this);
        } catch (Exception e) {
            logger.error(chatId, "Error showing active tasks", e);
            MessageHandler.sendErrorMessage(chatId, "Failed to retrieve active tasks. Please try again.", this);
        }
    }

    /**
     * Handle standard commands from authenticated users
     */
    private void handleCommands(long chatId, String messageText, UserBotState state) {
        logger.info(chatId, "Processing command: '{}'", messageText);

        // Check for task status update commands (DONE/UNDO/DELETE)
        if (messageText.contains("-DONE") || messageText.contains("-UNDO") || messageText.contains("-DELETE")) {
            handleTaskStatusCommand(chatId, messageText, state);
            return;
        }

        // Handle standard commands and button presses
        switch (messageText) {
            case "/start":
            case "🏠 Main Menu":
                MessageHandler.showMainScreen(chatId, state, this);
                break;

            case "/sprint":
            case "🏃‍♂️ Sprint Management":
                sprintHandler.enterSprintMode(chatId, state);
                break;

            case "/todolist":
            case "📝 List All Tasks":
                MessageHandler.showTaskList(chatId, botService.getAllToDoItems(state.getUser().getId()), state, this);
                break;

            case "/additem":
            case "📝 Create New Task":
                taskCreationHandler.startTaskCreation(chatId, state);
                break;

            case "/help":
            case "❓ Help":
                MessageHandler.showHelpInformation(chatId, this);
                break;

            case "🔄 My Active Tasks":
                showActiveTasksForUser(chatId, state);
                break;

            case "✅ Mark Task Complete":
                taskCompletionHandler.startTaskCompletion(chatId, state);
                break;

            case "❌ Hide Keyboard":
                MessageHandler.hideKeyboard(chatId, this);
                break;

            case "👥 Team Management":
                showTeamManagement(chatId, state);
                break;

            case "📊 KPI Dashboard":
                showKpiDashboard(chatId, state);
                break;

            default:
                logger.warn(chatId, "Unknown command: '{}'", messageText);
                MessageHandler.sendMessage(chatId,
                        "I didn't understand that command. Type /help to see available options.", this);
                break;
        }
    }

    /**
     * Show team management with interactive inline keyboard
     */
    private void showTeamManagement(long chatId, UserBotState state) {
        logger.info(chatId, "Showing team management options");
        try {
            // Only managers can access team management
            if (!state.getUser().isManager()) {
                MessageHandler.sendErrorMessage(chatId,
                        "You don't have permission to access team management. Only managers can access this feature.",
                        this);
                MessageHandler.showMainScreen(chatId, state, this);
                return;
            }

            // Show loading animation
            SendMessage loadingMessage = new SendMessage();
            loadingMessage.setChatId(chatId);
            loadingMessage.setText("Loading team management...\n⬜⬜⬜");
            loadingMessage.enableHtml(true);

            Message sentMessage = execute(loadingMessage);

            // Animation frames
            String[] frames = { "⬛⬜⬜", "⬛⬛⬜", "⬛⬛⬛", "✅" };
            for (String frame : frames) {
                Thread.sleep(300);
                EditMessageText updateMessage = new EditMessageText();
                updateMessage.setChatId(chatId);
                updateMessage.setMessageId(sentMessage.getMessageId());
                updateMessage.setText("Loading team management...\n" + frame);
                updateMessage.enableHtml(true);
                execute(updateMessage);
            }

            // Get the team info if user has a team
            if (state.getUser().getTeam() == null) {
                EditMessageText noTeamMessage = new EditMessageText();
                noTeamMessage.setChatId(chatId);
                noTeamMessage.setMessageId(sentMessage.getMessageId());
                noTeamMessage.setText(
                        "You are not currently associated with any team. Please contact the system administrator.");
                noTeamMessage.enableHtml(true);
                execute(noTeamMessage);
                return;
            }

            // Create StringBuilder for message content
            StringBuilder messageText = new StringBuilder();
            messageText.append("<b>Team: ").append(state.getUser().getTeam().getName()).append("</b>\n\n");
            
            // Display team information
            if (state.getUser().getTeam().getDescription() != null) {
                messageText.append("<b>Description:</b> ").append(state.getUser().getTeam().getDescription())
                        .append("\n");
            }

            // Display team members count
            List<User> teamMembers = botService.findUsersByTeamId(state.getUser().getTeam().getId());
            messageText.append("\n<b>Team Members:</b> ").append(teamMembers.size()).append(" total\n");
            messageText.append("\nSelect an option to manage your team:");

            // Create inline keyboard with options
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            // First row - View team members
            List<InlineKeyboardButton> row1 = new ArrayList<>();
            InlineKeyboardButton viewMembersButton = new InlineKeyboardButton();
            viewMembersButton.setText("👥 View Team Members");
            viewMembersButton.setCallbackData("team_members");
            row1.add(viewMembersButton);
            rows.add(row1);

            // Second row - Main menu
            List<InlineKeyboardButton> row2 = new ArrayList<>();
            InlineKeyboardButton mainMenuButton = new InlineKeyboardButton();
            mainMenuButton.setText("🔙 Back to Main Menu");
            mainMenuButton.setCallbackData("team_back_to_menu");
            row2.add(mainMenuButton);
            rows.add(row2);

            markup.setKeyboard(rows);

            // Update the previously sent loading message
            EditMessageText teamMessage = new EditMessageText();
            teamMessage.setChatId(chatId);
            teamMessage.setMessageId(sentMessage.getMessageId());
            teamMessage.setText(messageText.toString());
            teamMessage.enableHtml(true);
            teamMessage.setReplyMarkup(markup);

            execute(teamMessage);
            logger.info(chatId, "Team management information sent successfully");
        } catch (Exception e) {
            logger.error(chatId, "Error showing team management", e);
            MessageHandler.sendErrorMessage(chatId,
                    "There was an error accessing team management. Please try again later.",
                    this);
            MessageHandler.showMainScreen(chatId, state, this);
        }
    }

    /**
     * Show KPI Dashboard for the user
     */
    private void showKpiDashboard(long chatId, UserBotState state) {
        logger.info(chatId, "Showing KPI dashboard");
        try {
            // Show loading animation first
            SendMessage loadingMessage = new SendMessage();
            loadingMessage.setChatId(chatId);
            loadingMessage.setText("Loading KPI dashboard...\n⬜⬜⬜");
            loadingMessage.enableHtml(true);

            Message sentMessage = execute(loadingMessage);

            // Animation frames
            String[] frames = { "⬛⬜⬜", "⬛⬛⬜", "⬛⬛⬛", "✅" };
            for (String frame : frames) {
                Thread.sleep(300);
                EditMessageText updateMessage = new EditMessageText();
                updateMessage.setChatId(chatId);
                updateMessage.setMessageId(sentMessage.getMessageId());
                updateMessage.setText("Loading KPI dashboard...\n" + frame);
                updateMessage.enableHtml(true);
                execute(updateMessage);
            }

            // Check if user can access KPI data
            if (!state.getUser().isManager() && !state.getUser().isDeveloper()) {
                EditMessageText noAccessMessage = new EditMessageText();
                noAccessMessage.setChatId(chatId);
                noAccessMessage.setMessageId(sentMessage.getMessageId());
                noAccessMessage.setText("You don't have permission to access the KPI dashboard. " +
                        "This feature is available to Managers and Developers only.");
                noAccessMessage.enableHtml(true);
                execute(noAccessMessage);
                return;
            }

            // Create the KPI dashboard message
            StringBuilder kpiText = new StringBuilder();
            kpiText.append("<b>📊 KPI Dashboard</b>\n\n");
            kpiText.append("The KPI dashboard provides insights into your team's performance and productivity.\n\n");
            kpiText.append(
                    "Detailed charts and metrics are available in the web interface, but you can view basic information here.");

            // Create inline keyboard with KPI options
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            // First row - View KPI summary
            List<InlineKeyboardButton> row1 = new ArrayList<>();
            InlineKeyboardButton viewKpiButton = new InlineKeyboardButton();
            viewKpiButton.setText("📈 View KPI Summary");
            viewKpiButton.setCallbackData("kpi_view");
            row1.add(viewKpiButton);
            rows.add(row1);

            // Second row - Back to main menu
            List<InlineKeyboardButton> row2 = new ArrayList<>();
            InlineKeyboardButton mainMenuButton = new InlineKeyboardButton();
            mainMenuButton.setText("🔙 Back to Main Menu");
            mainMenuButton.setCallbackData("kpi_back_to_menu");
            row2.add(mainMenuButton);
            rows.add(row2);

            markup.setKeyboard(rows);

            // Update the previously sent loading message
            EditMessageText kpiMessage = new EditMessageText();
            kpiMessage.setChatId(chatId);
            kpiMessage.setMessageId(sentMessage.getMessageId());
            kpiMessage.setText(kpiText.toString());
            kpiMessage.enableHtml(true);
            kpiMessage.setReplyMarkup(markup);

            execute(kpiMessage);
            logger.info(chatId, "KPI dashboard sent successfully");
        } catch (Exception e) {
            logger.error(chatId, "Error showing KPI dashboard", e);
            MessageHandler.sendErrorMessage(chatId,
                    "There was an error accessing the KPI dashboard. Please try again later.",
                    this);
            MessageHandler.showMainScreen(chatId, state, this);
        }
    }

    /**
     * Handle task status update commands in format "TaskID-ACTION"
     * e.g., "123-DONE", "456-UNDO", "789-DELETE"
     */
    private void handleTaskStatusCommand(long chatId, String messageText, UserBotState state) {
        logger.info(chatId, "Processing task status command: '{}'", messageText);
        try {
            // Parse the command format: TaskID-ACTION
            String[] parts = messageText.split("-");
            if (parts.length != 2) {
                MessageHandler.sendErrorMessage(chatId,
                        "Invalid command format. Use 'TaskID-ACTION' (e.g., '123-DONE').", this);
                return;
            }

            int taskId;
            try {
                taskId = Integer.parseInt(parts[0].trim());
            } catch (NumberFormatException e) {
                MessageHandler.sendErrorMessage(chatId, "Invalid task ID. Please provide a valid number.", this);
                return;
            }

            String action = parts[1].trim().toUpperCase();
            if (action.equals("DONE") || action.equals("UNDO") || action.equals("DELETE")) {
                taskCompletionHandler.handleTaskStatusUpdate(chatId, state, action, taskId);
            } else {
                MessageHandler.sendErrorMessage(chatId, "Invalid action. Valid actions are DONE, UNDO, and DELETE.",
                        this);
            }
        } catch (Exception e) {
            logger.error(chatId, "Error processing task status command", e);
            MessageHandler.sendErrorMessage(chatId, "Failed to update task status. Please try again.", this);
        }
    }
}