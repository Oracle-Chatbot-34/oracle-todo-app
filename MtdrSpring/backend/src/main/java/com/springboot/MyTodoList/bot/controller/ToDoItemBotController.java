package com.springboot.MyTodoList.bot.controller;

import com.springboot.MyTodoList.bot.handler.AuthenticationHandler;
import com.springboot.MyTodoList.bot.handler.MessageHandler;
import com.springboot.MyTodoList.bot.handler.SprintHandler;
import com.springboot.MyTodoList.bot.handler.TaskCompletionHandler;
import com.springboot.MyTodoList.bot.handler.TaskCreationHandler;
import com.springboot.MyTodoList.bot.service.BotService;
import com.springboot.MyTodoList.bot.util.BotLogger;
import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.model.bot.UserBotState;
import com.springboot.MyTodoList.service.SprintService;
import com.springboot.MyTodoList.service.ToDoItemService;
import com.springboot.MyTodoList.service.UserService;
import com.springboot.MyTodoList.util.BotCommands;


import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

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
    public void onUpdateReceived(Update update) {
        long chatId = 0;

        try {
            // Process message if it exists and has text
            if (update.hasMessage() && update.getMessage().hasText()) {
                String messageText = update.getMessage().getText();
                chatId = update.getMessage().getChatId();
                logger.info(chatId, "Received message: '{}'", messageText);

                // Get or initialize user state
                UserBotState state = userStates.getOrDefault(chatId, new UserBotState());
                userStates.put(chatId, state);
                logger.debug(chatId,
                        "User state: authenticated={}, newTaskMode={}, taskCompletionMode={}, assignToSprintMode={}",
                        state.isAuthenticated(), state.isNewTaskMode(), state.isTaskCompletionMode(),
                        state.isAssignToSprintMode());

                // Handle authentication flow
                if (!state.isAuthenticated()) {
                    handleUnauthenticatedUser(chatId, messageText, state);
                    return;
                }

                // Handle various task modes
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
            logger.error("Unexpected error in bot operation", e);
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
        if (messageText.equals(BotCommands.START_COMMAND.getCommand())) {
            logger.info(chatId, "Processing start command for unauthenticated user");
            // Send authentication prompt for /start command
            try {
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Welcome to DashMaster! Please enter your Employee ID to authenticate:");
                execute(message);
                logger.info(chatId, "Authentication prompt sent");
            } catch (TelegramApiException e) {
                logger.error(chatId, "Error sending authentication prompt", e);
                MessageHandler.sendErrorMessage(chatId, "Communication error. Please try again.", this);
            }
        } else {
            logger.info(chatId, "Processing authentication attempt with employee ID: {}", messageText);
            // Process message as authentication attempt
            authHandler.handleAuthentication(chatId, messageText, state);
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

            case "/todolist":
            case "📝 List All Tasks":
                showUserTaskList(chatId, state);
                break;

            case "/additem":
            case "📝 Create New Task":
                startTaskCreation(chatId, state);
                break;

            case "/hide":
            case "❌ Hide Keyboard":
                MessageHandler.hideKeyboard(chatId, this);
                break;

            case "/help":
                MessageHandler.showHelpInformation(chatId, this);
                break;

            case "🔄 My Active Tasks":
                showActiveTasksForUser(chatId, state);
                break;

            case "✅ Mark Task Complete":
                taskCompletionHandler.startTaskCompletion(chatId, state);
                break;

            case "📊 Sprint Board":
            case "📊 View Sprint Board":
                sprintHandler.showSprintBoard(chatId, state);
                break;

            case "🆕 Create New Sprint":
                sprintHandler.startSprintCreation(chatId, state);
                break;

            case "➕ Assign Task to Sprint":
                sprintHandler.startAssignTaskToSprint(chatId, state);
                break;

            case "⏹️ End Active Sprint":
                sprintHandler.startEndActiveSprint(chatId, state);
                break;

            default:
                logger.warn(chatId, "Unknown command: '{}'", messageText);
                MessageHandler.sendMessage(chatId,
                        "I didn't understand that command. Type /help to see available options.", this);
                break;
        }
    }

    /**
     * Handle task status update commands (DONE/UNDO/DELETE)
     */
    private void handleTaskStatusCommand(long chatId, String messageText, UserBotState state) {
        try {
            String command = null;
            int taskId;

            if (messageText.contains("-DONE")) {
                command = "DONE";
                taskId = Integer.parseInt(messageText.substring(0, messageText.indexOf("-DONE")));
            } else if (messageText.contains("-UNDO")) {
                command = "UNDO";
                taskId = Integer.parseInt(messageText.substring(0, messageText.indexOf("-UNDO")));
            } else if (messageText.contains("-DELETE")) {
                command = "DELETE";
                taskId = Integer.parseInt(messageText.substring(0, messageText.indexOf("-DELETE")));
            } else {
                logger.warn(chatId, "Invalid task command format: '{}'", messageText);
                return;
            }

            logger.info(chatId, "Processing task command: {} for task ID: {}", command, taskId);
            taskCompletionHandler.handleTaskStatusUpdate(chatId, state, command, taskId);
        } catch (NumberFormatException e) {
            logger.error(chatId, "Invalid task ID format in message: '{}'", messageText, e);
            MessageHandler.sendErrorMessage(chatId, "Invalid task ID format. Please try again.", this);
        } catch (Exception e) {
            logger.error(chatId, "Error processing task status update", e);
            MessageHandler.sendErrorMessage(chatId, "Failed to update task. Please try again later.", this);
        }
    }

    /**
     * Show the user's task list
     */
    private void showUserTaskList(long chatId, UserBotState state) {
        logger.info(chatId, "Showing task list for user: {}", state.getUser().getFullName());
        try {
            List<ToDoItem> tasks = botService.getAllToDoItems(state.getUser().getId());
            MessageHandler.showTaskList(chatId, tasks, state, this);
        } catch (Exception e) {
            logger.error(chatId, "Error showing task list", e);
            MessageHandler.sendErrorMessage(chatId, "Failed to load your task list. Please try again later.", this);
        }
    }

    /**
     * Start the task creation process based on user role
     */
    private void startTaskCreation(long chatId, UserBotState state) {
        logger.info(chatId, "Starting task creation for user: {}", state.getUser().getFullName());
        try {
            // Different flows for different user roles
            if (state.getUser().isDeveloper() || state.getUser().isManager()) {
                // Full task creation flow for developers/managers
                taskCreationHandler.startTaskCreation(chatId, state);
            } else {
                // Simple item creation for regular employees
                taskCreationHandler.startSimpleItemCreation(chatId, state);
            }
        } catch (Exception e) {
            logger.error(chatId, "Error starting task creation", e);
            MessageHandler.sendErrorMessage(chatId, "Failed to start task creation. Please try again later.", this);
        }
    }

    /**
     * Show active tasks for the user
     */
    private void showActiveTasksForUser(long chatId, UserBotState state) {
        logger.info(chatId, "Showing active tasks for user: {}", state.getUser().getFullName());
        try {
            List<ToDoItem> activeTasks = botService.findActiveTasksByAssigneeId(state.getUser().getId());

            if (activeTasks.isEmpty()) {
                MessageHandler.sendMessage(chatId, "You don't have any active tasks at the moment.", this);
                return;
            }

            StringBuilder tasksText = new StringBuilder();
            tasksText.append("Your Active Tasks:\n\n");

            for (ToDoItem task : activeTasks) {
                tasksText.append("ID: ").append(task.getID()).append("\n");
                tasksText.append("Title: ").append(task.getTitle()).append("\n");
                tasksText.append("Status: ").append(task.getStatus()).append("\n");
                tasksText.append("Estimated Hours: ").append(task.getEstimatedHours()).append("\n");
                tasksText.append("Priority: ").append(task.getPriority()).append("\n\n");
            }

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(tasksText.toString());

            execute(message);
            logger.info(chatId, "Active tasks list sent");
        } catch (Exception e) {
            logger.error(chatId, "Error showing active tasks", e);
            MessageHandler.sendErrorMessage(chatId,
                    "There was an error retrieving your active tasks. Please try again later.", this);
        }
    }

    @Override
    public String getBotUsername() {
        return botName;
    }
}