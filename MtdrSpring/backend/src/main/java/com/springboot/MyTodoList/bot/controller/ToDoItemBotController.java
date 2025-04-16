package com.springboot.MyTodoList.bot.controller;

import com.springboot.MyTodoList.bot.handler.AuthenticationHandler;
import com.springboot.MyTodoList.bot.handler.MessageHandler;
import com.springboot.MyTodoList.bot.handler.SprintHandler;
import com.springboot.MyTodoList.bot.handler.TaskCompletionHandler;
import com.springboot.MyTodoList.bot.handler.TaskCreationHandler;
import com.springboot.MyTodoList.bot.service.BotService;
import com.springboot.MyTodoList.bot.util.BotLogger;
import com.springboot.MyTodoList.model.bot.UserBotState;
import com.springboot.MyTodoList.service.SprintService;
import com.springboot.MyTodoList.service.ToDoItemService;
import com.springboot.MyTodoList.service.UserService;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import com.springboot.MyTodoList.model.User;

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
                default:
                    logger.warn(chatId, "Unknown main menu callback: {}", callbackData);
            }
        } catch (Exception e) {
            logger.error(chatId, "Error processing main menu callback", e);
            MessageHandler.sendErrorMessage(chatId, "Failed to process your request. Please try again.", this);
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
            } else {
                logger.warn(chatId, "Unknown task callback: {}", callbackData);
            }
        } catch (Exception e) {
            logger.error(chatId, "Error processing task callback", e);
            MessageHandler.sendErrorMessage(chatId, "Failed to process task action. Please try again.", this);
        }
    }

    /**
     * Show active tasks for the current user
     */
    private void showActiveTasksForUser(long chatId, UserBotState state) {
        logger.info(chatId, "Showing active tasks for user");
        try {
            // Get active tasks for the user
            MessageHandler.showTaskList(
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

    private void showTeamManagement(long chatId, UserBotState state) {
        logger.info(chatId, "Showing team management options");
        try {
            // Only managers can access team management
            if (!state.getUser().isManager()) {
                MessageHandler.sendErrorMessage(chatId,
                        "You don't have permission to access team management. Only managers can access this feature.",
                        this);
                return;
            }

            // Get the team info if user has a team
            if (state.getUser().getTeam() == null) {
                MessageHandler.sendMessage(chatId,
                        "You are not currently associated with any team. Please contact the system administrator.",
                        this);
                return;
            }

            // Display team information
            StringBuilder messageText = new StringBuilder();
            messageText.append("<b>Team Management</b>\n\n");
            messageText.append("<b>Team:</b> ").append(state.getUser().getTeam().getName()).append("\n");

            if (state.getUser().getTeam().getDescription() != null) {
                messageText.append("<b>Description:</b> ").append(state.getUser().getTeam().getDescription())
                        .append("\n");
            }

            // Display team members
            messageText.append("\n<b>Team Members:</b>\n");
            List<User> teamMembers = botService.findUsersByTeamId(state.getUser().getTeam().getId());

            for (User member : teamMembers) {
                messageText.append("• ")
                        .append(member.getFullName())
                        .append(" (").append(member.getRole()).append(")\n");
            }

            messageText.append("\nTeam management features are available through the web interface. " +
                    "For advanced team management options, please use the DashMaster web application.");

            // Create a keyboard with options
            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            keyboardMarkup.setResizeKeyboard(true);
            List<KeyboardRow> keyboard = new ArrayList<>();

            KeyboardRow row1 = new KeyboardRow();
            row1.add("🏃‍♂️ Sprint Management");
            row1.add("📊 KPI Dashboard");
            keyboard.add(row1);

            KeyboardRow row2 = new KeyboardRow();
            row2.add("🏠 Main Menu");
            keyboard.add(row2);

            keyboardMarkup.setKeyboard(keyboard);

            // Send the message with the keyboard
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.enableHtml(true);
            message.setText(messageText.toString());
            message.setReplyMarkup(keyboardMarkup);

            execute(message);
            logger.info(chatId, "Team management information sent successfully");
        } catch (Exception e) {
            logger.error(chatId, "Error showing team management", e);
            MessageHandler.sendErrorMessage(chatId,
                    "There was an error accessing team management. Please try again later.",
                    this);
        }
    }

    /**
     * Show KPI Dashboard for the user
     */
    private void showKpiDashboard(long chatId, UserBotState state) {
        logger.info(chatId, "Showing KPI dashboard");
        try {
            MessageHandler.sendMessage(chatId,
                    "The KPI Dashboard feature is available in the web interface. " +
                            "Please use the DashMaster web application to view detailed KPI metrics and performance statistics.",
                    this);

            // After showing the message, return to main menu
            MessageHandler.showMainScreen(chatId, state, this);
        } catch (Exception e) {
            logger.error(chatId, "Error showing KPI dashboard", e);
            MessageHandler.sendErrorMessage(chatId,
                    "There was an error accessing the KPI dashboard. Please try again later.",
                    this);
        }
        MessageHandler.sendErrorMessage(chatId,
                "There was an error accessing team management. Please try again later.",
                this);
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
                MessageHandler.sendErrorMessage(chatId, "Invalid command format. Use 'TaskID-ACTION' (e.g., '123-DONE').", this);
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
                MessageHandler.sendErrorMessage(chatId, "Invalid action. Valid actions are DONE, UNDO, and DELETE.", this);
            }
        } catch (Exception e) {
            logger.error(chatId, "Error processing task status command", e);
            MessageHandler.sendErrorMessage(chatId, "Failed to update task status. Please try again.", this);
        }
    }
}
