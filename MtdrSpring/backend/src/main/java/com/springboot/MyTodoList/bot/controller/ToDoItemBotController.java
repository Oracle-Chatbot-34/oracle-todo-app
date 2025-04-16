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
                MessageHandler.showHelpInformation(chatId, this);
                break;

            case "🔄 My Active Tasks":
                showActiveTasksForUser(chatId, state);
                break;

            case "✅ Mark Task Complete":
                taskCompletionHandler.startTaskCompletion(chatId, state);
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
     * Show active tasks for the user
     */
    private void showActiveTasksForUser(long chatId, UserBotState state) {
        logger.info(chatId, "Showing active tasks for user: {}", state.getUser().getFullName());
        try {
            MessageHandler.showActiveTasksList(chatId,
                    botService.findActiveTasksByAssigneeId(state.getUser().getId()), state, this);
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