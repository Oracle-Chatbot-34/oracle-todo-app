package com.springboot.MyTodoList.controller;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.model.TaskStatus;
import com.springboot.MyTodoList.model.Team;
import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.model.bot.UserBotState;
import com.springboot.MyTodoList.service.SprintService;
import com.springboot.MyTodoList.service.ToDoItemService;
import com.springboot.MyTodoList.service.UserService;
import com.springboot.MyTodoList.util.BotCommands;
import com.springboot.MyTodoList.util.BotHelper;
import com.springboot.MyTodoList.util.BotLabels;
import com.springboot.MyTodoList.util.BotMessages;

public class ToDoItemBotController extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(ToDoItemBotController.class);
    private ToDoItemService toDoItemService;
    private UserService userService;
    private SprintService sprintService;
    private String botName;
    private ConcurrentHashMap<Long, UserBotState> userStates = new ConcurrentHashMap<>();

    /**
     * Helper method to send messages with error handling
     */
    private void sendMessage(long chatId, String text) {
        logger.info("Sending message to chat ID {}: {}", chatId, text);
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(text);
            execute(message);
            logger.info("Message successfully sent to chat ID {}", chatId);
        } catch (TelegramApiException e) {
            logger.error("Failed to send message to chat ID {}: {}", chatId, text, e);
        }
    }

    /**
     * Helper method to send error messages to users
     */
    private void sendErrorMessage(long chatId, String errorText) {
        logger.error("Sending error message to chat ID {}: {}", chatId, errorText);
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("❌ " + errorText);

        try {
            execute(message);
            logger.info("Error message successfully sent to chat ID {}", chatId);
        } catch (TelegramApiException e) {
            logger.error("Failed to send error message to chat ID {}: {}", chatId, errorText, e);
        }
    }

    /**
     * Shows the main menu screen to the user
     * Called after successful authentication or when returning to the main menu
     */
    private void showMainScreen(long chatId, UserBotState state) {
        logger.info("Showing main screen to chat ID {}, user: {}", chatId, state.getUser().getFullName());
        try {
            SendMessage messageToTelegram = new SendMessage();
            messageToTelegram.setChatId(chatId);

            // Personalized welcome message using the user's name
            String welcomeMessage = "Hello, " + state.getUser().getFullName() + "! " +
                    BotMessages.HELLO_MYTODO_BOT.getMessage();
            messageToTelegram.setText(welcomeMessage);
            logger.debug("Welcome message for chat ID {}: {}", chatId, welcomeMessage);

            // Create a keyboard with options
            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            keyboardMarkup.setResizeKeyboard(true); // Make the keyboard smaller
            List<KeyboardRow> keyboard = new ArrayList<>();

            // First row with main actions
            KeyboardRow row = new KeyboardRow();
            row.add(BotLabels.LIST_ALL_ITEMS.getLabel());
            row.add(BotLabels.ADD_NEW_ITEM.getLabel());
            keyboard.add(row);

            // Second row with utility actions
            row = new KeyboardRow();
            row.add(BotLabels.HIDE_MAIN_SCREEN.getLabel());
            keyboard.add(row);

            // Set the keyboard
            keyboardMarkup.setKeyboard(keyboard);
            messageToTelegram.setReplyMarkup(keyboardMarkup);
            logger.debug("Main screen keyboard created for chat ID {}", chatId);

            execute(messageToTelegram);
            logger.info("Main screen successfully shown to chat ID {}", chatId);
        } catch (TelegramApiException e) {
            logger.error("Error showing main screen to chat ID {}", chatId, e);
            sendErrorMessage(chatId,
                    "There was a problem displaying the main menu. Please try again by typing /start.");
        }
    }

    public ToDoItemBotController(String botToken, String botName, ToDoItemService toDoItemService,
            UserService userService, SprintService sprintService) {
        super(botToken);
        logger.info("Initializing ToDoItemBotController with token: {}, name: {}",
                botToken.substring(0, 5) + "...", botName);
        this.toDoItemService = toDoItemService;
        this.userService = userService;
        this.sprintService = sprintService;
        this.botName = botName;
        logger.info("ToDoItemBotController successfully initialized");
    }

    @Override
    public void onUpdateReceived(Update update) {
        logger.info("Received update: {}", update.getUpdateId());
        long chatId = 0;
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                String messageText = update.getMessage().getText();
                chatId = update.getMessage().getChatId();
                logger.info("Received message from chat ID {}: '{}'", chatId, messageText);

                // Get or initialize user state
                UserBotState state = userStates.getOrDefault(chatId, new UserBotState());
                userStates.put(chatId, state);
                logger.debug(
                        "User state for chat ID {}: authenticated={}, newTaskMode={}, taskCompletionMode={}, assignToSprintMode={}",
                        chatId, state.isAuthenticated(), state.isNewTaskMode(), state.isTaskCompletionMode(),
                        state.isAssignToSprintMode());

                // Handle authentication flow
                if (!state.isAuthenticated()) {
                    logger.info("User not authenticated for chat ID {}", chatId);
                    if (messageText.equals(BotCommands.START_COMMAND.getCommand())) {
                        logger.info("Processing start command for unauthenticated user, chat ID {}", chatId);
                        // Send authentication prompt for /start command
                        SendMessage message = new SendMessage();
                        message.setChatId(chatId);
                        message.setText("Welcome to DashMaster! Please enter your Employee ID to authenticate:");
                        try {
                            execute(message);
                            logger.info("Authentication prompt sent to chat ID {}", chatId);
                        } catch (TelegramApiException e) {
                            logger.error("Error sending authentication prompt to chat ID {}", chatId, e);
                            sendErrorMessage(chatId, "Communication error. Please try again later.");
                        }
                        return;
                    } else {
                        logger.info("Processing authentication attempt for chat ID {}, employee ID: {}", chatId,
                                messageText);
                        // Process message as authentication attempt
                        handleAuthentication(chatId, messageText, state);
                        return;
                    }
                }

                logger.info("Processing message for authenticated user: {}, chat ID: {}",
                        state.getUser().getFullName(), chatId);

                // Task Creation Flow
                if (state.isNewTaskMode()) {
                    logger.info("User in new task mode, processing task creation for chat ID {}", chatId);
                    processTaskCreation(chatId, messageText, state);
                    return;
                }

                // Task Completion Flow
                if (state.isTaskCompletionMode()) {
                    logger.info("User in task completion mode, processing task completion for chat ID {}", chatId);
                    processTaskCompletion(chatId, messageText, state);
                    return;
                }

                // Assign to Sprint Flow
                if (state.isAssignToSprintMode()) {
                    logger.info("User in assign to sprint mode, processing assignment for chat ID {}", chatId);
                    processAssignTaskToSprint(chatId, messageText, state);
                    return;
                }

                // Handle Developer Task Management Commands
                if (state.getUser().isDeveloper() || state.getUser().isManager()) {
                    logger.debug("Processing developer/manager commands for chat ID {}", chatId);
                    if (messageText.equals("📝 Create New Task") || messageText.equals("📝 Create Another Task")) {
                        logger.info("Starting task creation process for chat ID {}", chatId);
                        startTaskCreation(chatId, state);
                        return;
                    } else if (messageText.equals("🔄 My Active Tasks")) {
                        logger.info("Showing active tasks for chat ID {}", chatId);
                        showActiveTasksForUser(chatId, state);
                        return;
                    } else if (messageText.equals("✅ Mark Task Complete")) {
                        logger.info("Starting task completion process for chat ID {}", chatId);
                        startTaskCompletion(chatId, state);
                        return;
                    } else if (messageText.equals("📊 Sprint Board")) {
                        logger.info("Showing sprint board for chat ID {}", chatId);
                        showSprintBoard(chatId, state);
                        return;
                    } else if (messageText.equals("🔄 Start Working on Task")) {
                        logger.info("Starting task work process for chat ID {}", chatId);
                        // Start the process to select a task to start working on
                        startTaskWorkProcess(chatId, state);
                        return;
                    } else if (messageText.equals("➕ Assign Task to Sprint")) {
                        logger.info("Starting assign task to sprint process for chat ID {}", chatId);
                        startAssignTaskToSprint(chatId, state);
                        return;
                    }
                }

                // Continue with other commands for authenticated users
                logger.debug("Processing general commands for chat ID {}", chatId);
                if (messageText.equals(BotCommands.START_COMMAND.getCommand()) ||
                        messageText.equals(BotLabels.SHOW_MAIN_SCREEN.getLabel()) ||
                        messageText.equals("🏠 Main Menu")) {
                    logger.info("Showing main screen for chat ID {}", chatId);
                    try {
                        showMainScreen(chatId, state);
                    } catch (Exception e) {
                        logger.error("Error showing main screen for chat ID {}", chatId, e);
                        sendErrorMessage(chatId, "Failed to display main menu. Please try again.");
                    }
                } else if (messageText.indexOf(BotLabels.DONE.getLabel()) != -1) {
                    logger.info("Processing 'done' action for chat ID {}", chatId);
                    try {
                        String done = messageText.substring(0,
                                messageText.indexOf(BotLabels.DASH.getLabel()));
                        Integer id = Integer.valueOf(done);
                        logger.debug("Marking task ID {} as done for chat ID {}", id, chatId);

                        ToDoItem item = getToDoItemById(id).getBody();
                        if (item != null) {
                            logger.debug("Task found, title: {}, current status: {}", item.getTitle(), item.isDone());
                            item.setDone(true);
                            updateToDoItem(item, id);
                            logger.info("Task ID {} successfully marked as done", id);
                            BotHelper.sendMessageToTelegram(chatId, BotMessages.ITEM_DONE.getMessage(), this);
                        } else {
                            logger.warn("Task not found with ID {}", id);
                            sendErrorMessage(chatId, "Could not find the task you're trying to mark as done.");
                        }
                    } catch (NumberFormatException e) {
                        logger.error("Invalid task ID format in message: {}", messageText, e);
                        sendErrorMessage(chatId, "Invalid task ID format. Please try again.");
                    } catch (Exception e) {
                        logger.error("Error marking task as done", e);
                        sendErrorMessage(chatId, "Failed to mark task as done. Please try again later.");
                    }
                } else if (messageText.indexOf(BotLabels.UNDO.getLabel()) != -1) {
                    logger.info("Processing 'undo' action for chat ID {}", chatId);
                    try {
                        String undo = messageText.substring(0,
                                messageText.indexOf(BotLabels.DASH.getLabel()));
                        Integer id = Integer.valueOf(undo);
                        logger.debug("Undoing task ID {} for chat ID {}", id, chatId);

                        ToDoItem item = getToDoItemById(id).getBody();
                        if (item != null) {
                            logger.debug("Task found, title: {}, current status: {}", item.getTitle(), item.isDone());
                            item.setDone(false);
                            updateToDoItem(item, id);
                            logger.info("Task ID {} successfully undone", id);
                            BotHelper.sendMessageToTelegram(chatId, BotMessages.ITEM_UNDONE.getMessage(), this);
                        } else {
                            logger.warn("Task not found with ID {}", id);
                            sendErrorMessage(chatId, "Could not find the task you're trying to undo.");
                        }
                    } catch (NumberFormatException e) {
                        logger.error("Invalid task ID format in message: {}", messageText, e);
                        sendErrorMessage(chatId, "Invalid task ID format. Please try again.");
                    } catch (Exception e) {
                        logger.error("Error undoing task", e);
                        sendErrorMessage(chatId, "Failed to undo task. Please try again later.");
                    }
                } else if (messageText.indexOf(BotLabels.DELETE.getLabel()) != -1) {
                    logger.info("Processing 'delete' action for chat ID {}", chatId);
                    try {
                        String delete = messageText.substring(0,
                                messageText.indexOf(BotLabels.DASH.getLabel()));
                        Integer id = Integer.valueOf(delete);
                        logger.debug("Deleting task ID {} for chat ID {}", id, chatId);

                        Boolean deletedObj = deleteToDoItem(id).getBody();
                        boolean deleted = deletedObj != null && deletedObj;
                        if (deleted) {
                            logger.info("Task ID {} successfully deleted", id);
                            BotHelper.sendMessageToTelegram(chatId, BotMessages.ITEM_DELETED.getMessage(), this);
                        } else {
                            logger.warn("Failed to delete task ID {}", id);
                            sendErrorMessage(chatId, "Could not delete the task. It may not exist.");
                        }
                    } catch (NumberFormatException e) {
                        logger.error("Invalid task ID format in message: {}", messageText, e);
                        sendErrorMessage(chatId, "Invalid task ID format. Please try again.");
                    } catch (Exception e) {
                        logger.error("Error deleting task", e);
                        sendErrorMessage(chatId, "Failed to delete task. Please try again later.");
                    }
                } else if (messageText.equals(BotCommands.HIDE_COMMAND.getCommand())
                        || messageText.equals(BotLabels.HIDE_MAIN_SCREEN.getLabel())) {
                    logger.info("Processing 'hide' command for chat ID {}", chatId);
                    try {
                        BotHelper.sendMessageToTelegram(chatId, BotMessages.BYE.getMessage(), this);
                        logger.info("Hide command processed successfully for chat ID {}", chatId);
                    } catch (Exception e) {
                        logger.error("Error hiding main screen for chat ID {}", chatId, e);
                        sendErrorMessage(chatId, "Failed to hide main screen. Please try again.");
                    }
                } else if (messageText.equals(BotCommands.TODO_LIST.getCommand())
                        || messageText.equals(BotLabels.LIST_ALL_ITEMS.getLabel())
                        || messageText.equals(BotLabels.MY_TODO_LIST.getLabel())) {
                    logger.info("Processing 'list items' command for chat ID {}", chatId);
                    try {
                        List<ToDoItem> allItems = getAllToDoItems(state.getUser().getId());
                        logger.debug("Retrieved {} items for chat ID {}", (allItems != null ? allItems.size() : 0),
                                chatId);

                        if (allItems == null || allItems.isEmpty()) {
                            logger.info("No items found for chat ID {}", chatId);
                            sendMessage(chatId,
                                    "Your todo list is empty. Add new items using the 'Add New Item' button.");
                            return;
                        }

                        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                        List<KeyboardRow> keyboard = new ArrayList<>();

                        // command back to main screen
                        KeyboardRow mainScreenRowTop = new KeyboardRow();
                        mainScreenRowTop.add(BotLabels.SHOW_MAIN_SCREEN.getLabel());
                        keyboard.add(mainScreenRowTop);

                        KeyboardRow firstRow = new KeyboardRow();
                        firstRow.add(BotLabels.ADD_NEW_ITEM.getLabel());
                        keyboard.add(firstRow);

                        KeyboardRow myTodoListTitleRow = new KeyboardRow();
                        myTodoListTitleRow.add(BotLabels.MY_TODO_LIST.getLabel());
                        keyboard.add(myTodoListTitleRow);

                        List<ToDoItem> activeItems = allItems.stream().filter(item -> item.isDone() == false)
                                .collect(Collectors.toList());
                        logger.debug("Found {} active items for chat ID {}", activeItems.size(), chatId);

                        for (ToDoItem item : activeItems) {
                            KeyboardRow currentRow = new KeyboardRow();
                            currentRow.add(item.getDescription());
                            currentRow.add(item.getID() + BotLabels.DASH.getLabel() + BotLabels.DONE.getLabel());
                            keyboard.add(currentRow);
                            logger.trace("Adding active item to keyboard: ID {}, description: {}",
                                    item.getID(), item.getDescription());
                        }

                        List<ToDoItem> doneItems = allItems.stream().filter(item -> item.isDone() == true)
                                .collect(Collectors.toList());
                        logger.debug("Found {} completed items for chat ID {}", doneItems.size(), chatId);

                        for (ToDoItem item : doneItems) {
                            KeyboardRow currentRow = new KeyboardRow();
                            currentRow.add(item.getDescription());
                            currentRow.add(item.getID() + BotLabels.DASH.getLabel() + BotLabels.UNDO.getLabel());
                            currentRow.add(item.getID() + BotLabels.DASH.getLabel() + BotLabels.DELETE.getLabel());
                            keyboard.add(currentRow);
                            logger.trace("Adding completed item to keyboard: ID {}, description: {}",
                                    item.getID(), item.getDescription());
                        }

                        // command back to main screen
                        KeyboardRow mainScreenRowBottom = new KeyboardRow();
                        mainScreenRowBottom.add(BotLabels.SHOW_MAIN_SCREEN.getLabel());
                        keyboard.add(mainScreenRowBottom);

                        keyboardMarkup.setKeyboard(keyboard);

                        SendMessage messageToTelegram = new SendMessage();
                        messageToTelegram.setChatId(chatId);
                        messageToTelegram.setText(BotLabels.MY_TODO_LIST.getLabel());
                        messageToTelegram.setReplyMarkup(keyboardMarkup);
                        logger.debug("Todo list message and keyboard created for chat ID {}", chatId);

                        execute(messageToTelegram);
                        logger.info("Todo list successfully displayed for chat ID {}", chatId);
                    } catch (Exception e) {
                        logger.error("Error displaying todo list for chat ID {}", chatId, e);
                        sendErrorMessage(chatId, "Failed to load your todo list. Please try again later.");
                    }
                } else if (messageText.equals(BotCommands.ADD_ITEM.getCommand())
                        || messageText.equals(BotLabels.ADD_NEW_ITEM.getLabel())) {
                    logger.info("Processing 'add item' command for chat ID {}", chatId);
                    try {
                        // Esto es lo importante: distinguir entre usuarios normales (flujo simple) y
                        // desarrolladores (flujo completo)
                        if (state.getUser().isDeveloper() || state.getUser().isManager()) {
                            // Flujo completo para desarrolladores/managers
                            startTaskCreation(chatId, state);
                        } else {
                            // Flujo simple para usuarios normales
                            SendMessage messageToTelegram = new SendMessage();
                            messageToTelegram.setChatId(chatId);
                            messageToTelegram.setText(BotMessages.TYPE_NEW_TODO_ITEM.getMessage());

                            // Ocultar teclado
                            ReplyKeyboardRemove keyboardMarkup = new ReplyKeyboardRemove(true);
                            messageToTelegram.setReplyMarkup(keyboardMarkup);

                            state.setNewTaskMode(true);
                            state.setTaskCreationStage("SIMPLE"); // Indicar un flujo simple
                            userStates.put(chatId, state);
                            logger.debug("Set state for chat ID {}: newTaskMode=true, stage=SIMPLE", chatId);

                            execute(messageToTelegram);
                            logger.info("Add item prompt sent successfully to chat ID {}", chatId);
                        }
                    } catch (Exception e) {
                        logger.error("Error initiating new task creation for chat ID {}", chatId, e);
                        sendErrorMessage(chatId, "Failed to start task creation. Please try again later.");
                    }
                } else if (messageText.equals("/help")) {
                    logger.info("Processing 'help' command for chat ID {}", chatId);
                    try {
                        // Send help information
                        StringBuilder helpText = new StringBuilder();
                        helpText.append("📋 *DashMaster Bot Commands*\n\n");
                        helpText.append("• */start* - Show the main menu\n");
                        helpText.append("• */todolist* - View your task list\n");
                        helpText.append("• */additem* - Add a new task\n");
                        helpText.append("• */hide* - Hide the keyboard\n");
                        helpText.append("• */help* - Show this help message\n\n");
                        helpText.append("You can also use the buttons on the keyboard for easier navigation.");

                        SendMessage helpMessage = new SendMessage();
                        helpMessage.setChatId(chatId);
                        helpMessage.setText(helpText.toString());
                        helpMessage.enableMarkdown(true);
                        execute(helpMessage);
                        logger.info("Help information sent successfully to chat ID {}", chatId);
                    } catch (Exception e) {
                        logger.error("Error sending help information to chat ID {}", chatId, e);
                        sendErrorMessage(chatId, "Failed to send help information. Please try again later.");
                    }
                } else if (messageText.equals("/dbstatus") &&
                        (state.getUser().isManager() || state.getUser().isDeveloper())) {
                    logger.info("Processing 'dbstatus' command for admin/developer chat ID {}", chatId);
                    try {
                        // Try a simple database operation for admins/developers
                        List<ToDoItem> items = toDoItemService.findAll();
                        logger.info("Database check successful, found {} items", items.size());
                        sendMessage(chatId,
                                "✅ Database connection is working. Found " + items.size() + " items in the database.");
                    } catch (Exception e) {
                        logger.error("Database health check failed", e);
                        sendErrorMessage(chatId, "Database connection issue: " + e.getMessage());
                    }
                } else {
                    // Handle text as new todo item if in new task mode
                    if (state.isNewTaskMode()) {
                        logger.info("Processing new task creation input for chat ID {}: '{}'", chatId, messageText);
                        try {
                            ToDoItem newItem = new ToDoItem();
                            newItem.setDescription(messageText);
                            newItem.setTitle(messageText.length() > 50 ? messageText.substring(0, 50) : messageText);
                            newItem.setCreation_ts(OffsetDateTime.now());
                            newItem.setDone(false);
                            logger.debug("Created new item with title: {}", newItem.getTitle());

                            // If user is associated with a team, set the team ID
                            if (state.getUser().getTeam() != null) {
                                logger.debug("Setting team ID {} for new item", state.getUser().getTeam().getId());
                                newItem.setTeamId(state.getUser().getTeam().getId());
                            }

                            // Set the user as assignee
                            logger.debug("Setting assignee ID {} for new item", state.getUser().getId());
                            newItem.setAssigneeId(state.getUser().getId());

                            // Add the item
                            logger.debug("Attempting to add new item to database");
                            ToDoItem addedItem = toDoItemService.addToDoItem(newItem);
                            logger.info("Successfully added new item with ID: {}", addedItem.getID());

                            // Reset state
                            state.setNewTaskMode(false);
                            userStates.put(chatId, state);
                            logger.debug("Reset new task mode for chat ID {}", chatId);

                            SendMessage messageToTelegram = new SendMessage();
                            messageToTelegram.setChatId(chatId);
                            messageToTelegram.setText(BotMessages.NEW_ITEM_ADDED.getMessage());

                            execute(messageToTelegram);
                            logger.info("Confirmation message sent for new item creation to chat ID {}", chatId);
                        } catch (Exception e) {
                            logger.error("Error adding new task for chat ID {}", chatId, e);
                            sendErrorMessage(chatId, "Failed to add new task. Please try again later.");

                            // Reset state even on failure
                            state.setNewTaskMode(false);
                            userStates.put(chatId, state);
                            logger.debug("Reset new task mode after error for chat ID {}", chatId);
                        }
                    } else {
                        // Unrecognized command or text
                        logger.warn("Unrecognized command received from chat ID {}: '{}'", chatId, messageText);
                        sendMessage(chatId, "I didn't understand that command. Type /help to see available options.");
                    }
                }
            } else {
                if (update.hasMessage()) {
                    chatId = update.getMessage().getChatId();
                    logger.warn("Received non-text message from chat ID {}", chatId);
                } else {
                    logger.warn("Received update without message: {}", update.getUpdateId());
                }
            }
        } catch (Exception e) {
            // Global error handler
            logger.error("Unexpected error in bot operation", e);
            if (chatId != 0) {
                sendErrorMessage(chatId, "An unexpected error occurred. Please try again later.");
            } else if (update.hasMessage()) {
                sendErrorMessage(update.getMessage().getChatId(),
                        "An unexpected error occurred. Please try again later.");
            }
        }
    }

    private void handleAuthentication(long chatId, String employeeId, UserBotState state) {
        logger.info("Handling authentication for chat ID {}, employee ID: {}", chatId, employeeId);
        try {
            // First, check if this Telegram user is already registered
            Optional<User> userByTelegramId = userService.findByTelegramId(chatId);
            logger.debug("User lookup by Telegram ID {}: present={}", chatId, userByTelegramId.isPresent());

            if (userByTelegramId.isPresent()) {
                // User already registered with this Telegram ID
                User user = userByTelegramId.get();
                logger.info("User already registered with Telegram ID {}: {}, {}",
                        chatId, user.getFullName(), user.getEmployeeId());
                state.setAuthenticated(true);
                state.setUser(user);

                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Welcome back, " + user.getFullName() + "!");

                try {
                    execute(message);
                    logger.info("Welcome back message sent to chat ID {}", chatId);
                    showMainScreen(chatId, state);
                } catch (TelegramApiException e) {
                    logger.error("Error sending welcome back message to chat ID {}", chatId, e);
                    sendErrorMessage(chatId,
                            "Authentication successful, but there was an error displaying the menu. Please type /start to continue.");
                }
            } else {
                // Try to find user by employee ID
                logger.debug("Looking up user by employee ID: {}", employeeId);
                Optional<User> userOpt = userService.findByEmployeeId(employeeId);
                logger.debug("User lookup by employee ID {}: present={}", employeeId, userOpt.isPresent());

                if (userOpt.isPresent() &&
                        (userOpt.get().isEmployee() || userOpt.get().isDeveloper() || userOpt.get().isManager())) {
                    User user = userOpt.get();
                    logger.info("User found by employee ID {}: {}, role: employee={}, developer={}, manager={}",
                            employeeId, user.getFullName(), user.isEmployee(), user.isDeveloper(), user.isManager());

                    // Associate this Telegram ID with the user
                    logger.debug("Associating Telegram ID {} with user {}", chatId, user.getFullName());
                    user.setTelegramId(chatId);
                    userService.updateUser(user);
                    logger.info("Telegram ID {} successfully associated with user {}", chatId, user.getFullName());

                    // Set authenticated state
                    state.setAuthenticated(true);
                    state.setUser(user);
                    logger.debug("User state updated for chat ID {}: authenticated=true", chatId);

                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText("Authentication successful! Welcome, " + user.getFullName()
                            + ". Your Telegram account is now linked to your DashMaster profile.");

                    try {
                        execute(message);
                        logger.info("Authentication success message sent to chat ID {}", chatId);
                        showMainScreen(chatId, state);
                    } catch (TelegramApiException e) {
                        logger.error("Error sending authentication success message to chat ID {}", chatId, e);
                        sendErrorMessage(chatId,
                                "Authentication successful, but there was an error displaying the menu. Please type /start to continue.");
                    }
                } else {
                    logger.warn(
                            "Authentication failed for chat ID {}, employee ID: {} - user not found or not authorized",
                            chatId, employeeId);
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText("Authentication failed. Please enter a valid Employee ID:");

                    try {
                        execute(message);
                        logger.info("Authentication failure message sent to chat ID {}", chatId);
                    } catch (TelegramApiException e) {
                        logger.error("Error sending authentication failure message to chat ID {}", chatId, e);
                        sendErrorMessage(chatId, "Communication error. Please try again.");
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Unexpected error during authentication for chat ID {}", chatId, e);
            sendErrorMessage(chatId, "Authentication system is currently unavailable. Please try again later.");
        }
    }

    @Override
    public String getBotUsername() {
        logger.debug("getBotUsername called, returning: {}", botName);
        return botName;
    }

    // GET /todolist
    public List<ToDoItem> getAllToDoItems(Long userId) {
        logger.info("Fetching todo items for user ID: {}", userId);
        try {
            List<ToDoItem> items = toDoItemService.findByAssigneeId(userId);
            logger.info("Successfully fetched {} todo items for user", items.size());
            return items;
        } catch (Exception e) {
            logger.error("Error fetching todo items for user: {}", userId, e);
            return new ArrayList<>();
        }
    }

    // GET BY ID /todolist/{id}
    public ResponseEntity<ToDoItem> getToDoItemById(@PathVariable int id) {
        logger.info("Fetching todo item by ID: {}", id);
        try {
            ResponseEntity<ToDoItem> responseEntity = toDoItemService.getItemById(id);
            if (responseEntity.getBody() != null) {
                ToDoItem item = responseEntity.getBody();
                logger.info("Successfully fetched todo item with ID {}: {}", id, item != null ? item.getTitle() : "null");
                return new ResponseEntity<ToDoItem>(item, HttpStatus.OK);
            } else {
                logger.warn("Todo item with ID {} not found", id);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error fetching todo item by ID: {}", id, e);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // PUT /todolist
    public ResponseEntity<Void> addToDoItem(@RequestBody ToDoItem todoItem) throws Exception {
        logger.info("Adding new todo item: {}", todoItem.getTitle());

        try {
            logger.debug("Todo item details - Description: {}, Done: {}, Team ID: {}, Assignee ID: {}",
                    todoItem.getDescription(), todoItem.isDone(), todoItem.getTeamId(), todoItem.getAssigneeId());

            ToDoItem td = toDoItemService.addToDoItem(todoItem);
            logger.info("Successfully added todo item with ID: {}", td.getID());

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.set("location", "" + td.getID());
            responseHeaders.set("Access-Control-Expose-Headers", "location");

            return ResponseEntity.ok().headers(responseHeaders).build();
        } catch (Exception e) {
            logger.error("Error adding todo item: {}", todoItem.getTitle(), e);
            throw e; // Re-throw to allow proper error handling in the caller
        }
    }

    // UPDATE /todolist/{id}
    public ResponseEntity<ToDoItem> updateToDoItem(@RequestBody ToDoItem toDoItem, @PathVariable int id) {
        logger.info("Updating todo item with ID: {}", id);
        logger.debug("Update details - Title: {}, Description: {}, Done: {}, Status: {}",
                toDoItem.getTitle(), toDoItem.getDescription(), toDoItem.isDone(), toDoItem.getStatus());

        try {
            ToDoItem toDoItem1 = toDoItemService.updateToDoItem(id, toDoItem);
            logger.info("Successfully updated todo item with ID: {}", id);
            return new ResponseEntity<>(toDoItem1, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error updating todo item with ID: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE todolist/{id}
    public ResponseEntity<Boolean> deleteToDoItem(@PathVariable("id") int id) {
        logger.info("Deleting todo item with ID: {}", id);
        Boolean flag = false;
        try {
            flag = toDoItemService.deleteToDoItem(id);
            if (flag) {
                logger.info("Successfully deleted todo item with ID: {}", id);
            } else {
                logger.warn("Failed to delete todo item with ID: {}, item may not exist", id);
            }
            return new ResponseEntity<>(flag, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error deleting todo item with ID: {}", id, e);
            return new ResponseEntity<>(flag, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Show developer's task menu
     */
    private void showDeveloperTaskMenu(long chatId, UserBotState state) {
        logger.info("Showing developer task menu for chat ID {}, user: {}", chatId, state.getUser().getFullName());
        try {
            SendMessage messageToTelegram = new SendMessage();
            messageToTelegram.setChatId(chatId);

            String welcomeMessage = "Task Management for " + state.getUser().getFullName() + "\n" +
                    "Select an option:";
            messageToTelegram.setText(welcomeMessage);
            logger.debug("Welcome message for developer task menu: {}", welcomeMessage);

            // Create a keyboard with options
            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            keyboardMarkup.setResizeKeyboard(true);
            List<KeyboardRow> keyboard = new ArrayList<>();

            // First row with main actions
            KeyboardRow row = new KeyboardRow();
            row.add("📝 Create New Task");
            row.add("🔄 My Active Tasks");
            keyboard.add(row);

            // Second row
            row = new KeyboardRow();
            row.add("✅ Mark Task Complete");
            row.add("📊 Sprint Board");
            keyboard.add(row);

            // Third row with utility actions
            row = new KeyboardRow();
            row.add("🏠 Main Menu");
            keyboard.add(row);

            // Set the keyboard
            keyboardMarkup.setKeyboard(keyboard);
            messageToTelegram.setReplyMarkup(keyboardMarkup);
            logger.debug("Developer task menu keyboard created with {} rows", keyboard.size());

            execute(messageToTelegram);
            logger.info("Developer task menu successfully shown to chat ID {}", chatId);
        } catch (TelegramApiException e) {
            logger.error("Error showing developer task menu for chat ID {}", chatId, e);
            sendErrorMessage(chatId, "There was a problem displaying the task menu. Please try again.");
        }
    }

    /**
     * Start the task creation flow
     */
    private void startTaskCreation(long chatId, UserBotState state) {
        logger.info("Starting task creation flow for chat ID {}, user: {}", chatId, state.getUser().getFullName());
        try {
            // Set state to task creation mode
            state.setNewTaskMode(true);
            state.setTaskCreationStage("TITLE");
            userStates.put(chatId, state);
            logger.debug("Set user state for chat ID {}: newTaskMode=true, taskCreationStage=TITLE", chatId);

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Let's create a new task. First, please enter the task title:");

            // Hide keyboard for text input
            ReplyKeyboardRemove keyboardRemove = new ReplyKeyboardRemove(true);
            message.setReplyMarkup(keyboardRemove);
            logger.debug("Created task creation prompt with keyboard removed");

            execute(message);
            logger.info("Task creation prompt sent successfully to chat ID {}", chatId);
        } catch (TelegramApiException e) {
            logger.error("Error starting task creation for chat ID {}", chatId, e);
            sendErrorMessage(chatId, "There was a problem starting task creation. Please try again.");
        }
    }

    private void startNewItemCreation(long chatId, UserBotState state) {
        logger.info("Starting new item creation for chat ID {}", chatId);
        try {
            SendMessage messageToTelegram = new SendMessage();
            messageToTelegram.setChatId(chatId);
            messageToTelegram.setText(BotMessages.TYPE_NEW_TODO_ITEM.getMessage());

            // Hide keyboard
            ReplyKeyboardRemove keyboardMarkup = new ReplyKeyboardRemove(true);
            messageToTelegram.setReplyMarkup(keyboardMarkup);

            // Set state to new task mode AND set the initial stage
            state.setNewTaskMode(true);
            state.setTaskCreationStage("DESCRIPTION"); // O el stage que corresponda para un item simple
            userStates.put(chatId, state);
            logger.debug("Set state for chat ID {}: newTaskMode=true, stage=DESCRIPTION", chatId);

            // Send message
            execute(messageToTelegram);
            logger.info("New item creation prompt sent to chat ID {}", chatId);
        } catch (Exception e) {
            logger.error("Error initiating new item creation for chat ID {}", chatId, e);
            sendErrorMessage(chatId, "Failed to start item creation. Please try again later.");
        }
    }

    /**
     * Start the process of creating a new sprint (only for managers)
     */
    private void startSprintCreation(long chatId, UserBotState state) {
        logger.info("Starting sprint creation process for chat ID {}, user: {}", chatId, state.getUser().getFullName());
        try {
            // Check if user is a manager
            if (!state.getUser().isManager()) {
                logger.warn("User with ID {} is not a manager, cannot create sprints", state.getUser().getId());
                sendErrorMessage(chatId, "Only managers can create new sprints.");
                return;
            }

            // Set state for sprint creation mode
            state.setSprintCreationMode(true);
            state.setSprintCreationStage("NAME");
            userStates.put(chatId, state);
            logger.debug("Set user state for chat ID {}: sprintCreationMode=true, sprintCreationStage=NAME", chatId);

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Let's create a new Sprint. First, please enter the Sprint name:");

            // Hide keyboard for text input
            ReplyKeyboardRemove keyboardRemove = new ReplyKeyboardRemove(true);
            message.setReplyMarkup(keyboardRemove);
            logger.debug("Created sprint name prompt with keyboard removed");

            execute(message);
            logger.info("Sprint name prompt sent to chat ID {}", chatId);
        } catch (Exception e) {
            logger.error("Error starting sprint creation for chat ID {}", chatId, e);
            sendErrorMessage(chatId, "There was an error starting sprint creation. Please try again later.");
        }
    }

    /**
     * Process sprint creation stages
     */
    private void processSprintCreation(long chatId, String messageText, UserBotState state) {
        logger.info("Processing sprint creation for chat ID {}, stage: {}",
                chatId, state.getSprintCreationStage());
        logger.debug("Sprint creation input: '{}'", messageText);
        try {
            String stage = state.getSprintCreationStage();

            if (stage == null) {
                logger.warn("Sprint creation stage is null for chat ID {}, assuming NAME stage", chatId);
                stage = "NAME";
                state.setSprintCreationStage(stage);
            }

            if ("NAME".equals(stage)) {
                // Store sprint name and ask for description
                logger.debug("Storing sprint name: '{}'", messageText);
                state.setTempSprintName(messageText);
                state.setSprintCreationStage("DESCRIPTION");
                logger.debug("Updated stage to DESCRIPTION");

                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Great! Now please provide a description for the sprint:");
                execute(message);
                logger.info("Description prompt sent to chat ID {}", chatId);
            } else if ("DESCRIPTION".equals(stage)) {
                // Store description and ask for start date
                logger.debug("Storing sprint description: '{}'", messageText);
                state.setTempSprintDescription(messageText);
                state.setSprintCreationStage("START_DATE");
                logger.debug("Updated stage to START_DATE");

                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Please enter the start date (YYYY-MM-DD format):");
                execute(message);
                logger.info("Start date prompt sent to chat ID {}", chatId);
            } else if ("START_DATE".equals(stage)) {
                // Validate and store start date, then ask for end date
                try {
                    // Simple date validation
                    if (!messageText.matches("\\d{4}-\\d{2}-\\d{2}")) {
                        throw new IllegalArgumentException("Invalid date format");
                    }

                    logger.debug("Storing sprint start date: '{}'", messageText);
                    state.setTempSprintStartDate(messageText);
                    state.setSprintCreationStage("END_DATE");
                    logger.debug("Updated stage to END_DATE");

                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText("Please enter the end date (YYYY-MM-DD format):");
                    execute(message);
                    logger.info("End date prompt sent to chat ID {}", chatId);
                } catch (Exception e) {
                    logger.warn("Invalid date format: '{}'", messageText, e);
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText("Please enter a valid date in YYYY-MM-DD format (e.g., 2025-04-30):");
                    execute(message);
                    logger.info("Date format error message sent to chat ID {}", chatId);
                }
            } else if ("END_DATE".equals(stage)) {
                // Validate end date and move to confirmation
                try {
                    // Simple date validation
                    if (!messageText.matches("\\d{4}-\\d{2}-\\d{2}")) {
                        throw new IllegalArgumentException("Invalid date format");
                    }

                    // Simple validation to ensure end date is after start date
                    String startDate = state.getTempSprintStartDate();
                    if (startDate != null && startDate.compareTo(messageText) >= 0) {
                        logger.warn("End date must be after start date: start='{}', end='{}'", startDate, messageText);
                        SendMessage message = new SendMessage();
                        message.setChatId(chatId);
                        message.setText("End date must be after the start date. Please enter a valid end date:");
                        execute(message);
                        logger.info("End date validation error message sent to chat ID {}", chatId);
                        return;
                    }

                    logger.debug("Storing sprint end date: '{}'", messageText);
                    state.setTempSprintEndDate(messageText);
                    state.setSprintCreationStage("CONFIRMATION");
                    logger.debug("Updated stage to CONFIRMATION");

                    StringBuilder summary = new StringBuilder();
                    summary.append("Please confirm the sprint details:\n\n");
                    summary.append("Name: ").append(state.getTempSprintName()).append("\n");
                    summary.append("Description: ").append(state.getTempSprintDescription()).append("\n");
                    summary.append("Start Date: ").append(state.getTempSprintStartDate()).append("\n");
                    summary.append("End Date: ").append(state.getTempSprintEndDate()).append("\n\n");
                    summary.append("Is this correct?");
                    logger.debug("Sprint confirmation summary: {}", summary.toString());

                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText(summary.toString());

                    ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                    keyboardMarkup.setResizeKeyboard(true);
                    List<KeyboardRow> keyboard = new ArrayList<>();

                    KeyboardRow row = new KeyboardRow();
                    row.add("Yes, create sprint");
                    row.add("No, cancel");
                    keyboard.add(row);

                    keyboardMarkup.setKeyboard(keyboard);
                    message.setReplyMarkup(keyboardMarkup);
                    logger.debug("Created confirmation keyboard for chat ID {}", chatId);

                    execute(message);
                    logger.info("Confirmation prompt sent to chat ID {}", chatId);
                } catch (Exception e) {
                    logger.warn("Invalid date format: '{}'", messageText, e);
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText("Please enter a valid date in YYYY-MM-DD format (e.g., 2025-04-30):");
                    execute(message);
                    logger.info("Date format error message sent to chat ID {}", chatId);
                }
            } else if ("CONFIRMATION".equals(stage)) {
                // Handle confirmation response
                logger.debug("Processing confirmation response: '{}'", messageText);
                if (messageText.equals("Yes, create sprint")) {
                    logger.info("User confirmed sprint creation for chat ID {}", chatId);

                    // Create the sprint
                    Sprint sprint = new Sprint();
                    sprint.setName(state.getTempSprintName());
                    sprint.setDescription(state.getTempSprintDescription());
                    sprint.setStartDate(OffsetDateTime.parse(state.getTempSprintStartDate()));
                    sprint.setEndDate(OffsetDateTime.parse(state.getTempSprintEndDate()));
                    Team team = state.getUser().getTeam();
                    if (team == null) {
                        logger.warn("User with ID {} is not associated with any team", state.getUser().getId());
                        sendErrorMessage(chatId, "You are not associated with any team. Cannot create sprint.");
                        return;
                    }
                    sprint.setTeam(team);
                    sprint.setStatus("ACTIVE");

                    logger.debug("Creating sprint: name='{}', teamId={}, startDate='{}', endDate='{}'",
                            sprint.getName(), sprint.getTeam() != null ? sprint.getTeam().getId() : "N/A", sprint.getStartDate(), sprint.getEndDate());

                    // Save the sprint
                    Sprint savedSprint = sprintService.createSprint(sprint);
                    logger.info("Sprint created successfully with ID {}", savedSprint.getId());

                    // Reset state
                    state.resetSprintCreation();
                    logger.debug("Reset sprint creation state for chat ID {}", chatId);

                    // Show success message
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText("✅ Sprint created successfully with ID: " + savedSprint.getId());

                    ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                    keyboardMarkup.setResizeKeyboard(true);
                    List<KeyboardRow> keyboard = new ArrayList<>();

                    KeyboardRow row = new KeyboardRow();
                    row.add("📊 Sprint Board");
                    row.add("🏠 Main Menu");
                    keyboard.add(row);

                    keyboardMarkup.setKeyboard(keyboard);
                    message.setReplyMarkup(keyboardMarkup);
                    logger.debug("Created success keyboard for chat ID {}", chatId);

                    execute(message);
                    logger.info("Sprint creation success message sent to chat ID {}", chatId);
                } else {
                    // Cancel sprint creation
                    logger.info("User cancelled sprint creation for chat ID {}", chatId);
                    state.resetSprintCreation();
                    logger.debug("Reset sprint creation state for chat ID {}", chatId);

                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText("Sprint creation cancelled. What would you like to do next?");

                    ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                    keyboardMarkup.setResizeKeyboard(true);
                    List<KeyboardRow> keyboard = new ArrayList<>();

                    KeyboardRow row = new KeyboardRow();
                    row.add("🆕 Create New Sprint");
                    row.add("🏠 Main Menu");
                    keyboard.add(row);

                    keyboardMarkup.setKeyboard(keyboard);
                    message.setReplyMarkup(keyboardMarkup);
                    logger.debug("Created cancellation keyboard for chat ID {}", chatId);

                    execute(message);
                    logger.info("Sprint creation cancellation message sent to chat ID {}", chatId);
                }
            }

            // Update user state
            userStates.put(chatId, state);
            logger.debug("Updated user state in map for chat ID {}", chatId);
        } catch (Exception e) {
            logger.error("Error in sprint creation process for chat ID {}", chatId, e);
            sendErrorMessage(chatId, "There was an error in the sprint creation process. Please try again.");

            // Reset sprint creation state
            state.resetSprintCreation();
            userStates.put(chatId, state);
            logger.debug("Reset sprint creation state after error for chat ID {}", chatId);
        }
    }

    /**
     * View all sprints for the user's team
     */
    private void viewAllSprints(long chatId, UserBotState state) {
        logger.info("Viewing all sprints for chat ID {}, user: {}", chatId, state.getUser().getFullName());
        try {
            // Check if user is in a team
            Long teamId = state.getUser().getTeam() != null ? state.getUser().getTeam().getId() : null;
            logger.debug("User team ID: {}", teamId);

            if (teamId == null) {
                logger.warn("User with ID {} is not associated with any team", state.getUser().getId());
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("You are not associated with any team.");

                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                keyboardMarkup.setResizeKeyboard(true);
                List<KeyboardRow> keyboard = new ArrayList<>();

                KeyboardRow row = new KeyboardRow();
                row.add("🏠 Main Menu");
                keyboard.add(row);

                keyboardMarkup.setKeyboard(keyboard);
                message.setReplyMarkup(keyboardMarkup);
                logger.debug("Created no team keyboard for chat ID {}", chatId);

                execute(message);
                logger.info("No team message sent to chat ID {}", chatId);
                return;
            }

            // Get all sprints for the team
            logger.debug("Fetching sprints for team ID: {}", teamId);
            List<Sprint> teamSprints = sprintService.findByTeamId(teamId);
            logger.debug("Found {} sprints for team", teamSprints.size());

            if (teamSprints.isEmpty()) {
                logger.info("No sprints found for team ID {}", teamId);
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("There are no sprints for your team yet.");

                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                keyboardMarkup.setResizeKeyboard(true);
                List<KeyboardRow> keyboard = new ArrayList<>();

                KeyboardRow row = new KeyboardRow();
                if (state.getUser().isManager()) {
                    row.add("🆕 Create New Sprint");
                }
                row.add("🏠 Main Menu");
                keyboard.add(row);

                keyboardMarkup.setKeyboard(keyboard);
                message.setReplyMarkup(keyboardMarkup);
                logger.debug("Created no sprints keyboard for chat ID {}", chatId);

                execute(message);
                logger.info("No sprints message sent to chat ID {}", chatId);
                return;
            }

            // Build sprints list text
            StringBuilder sprintsText = new StringBuilder();
            sprintsText.append("Sprints for your team:\n\n");

            for (Sprint sprint : teamSprints) {
                sprintsText.append("ID: ").append(sprint.getId()).append("\n");
                sprintsText.append("Name: ").append(sprint.getName()).append("\n");
                sprintsText.append("Description: ").append(sprint.getDescription()).append("\n");
                sprintsText.append("Period: ").append(sprint.getStartDate()).append(" to ").append(sprint.getEndDate())
                        .append("\n");
                sprintsText.append("Status: ").append(sprint.isActive() ? "🟢 Active" : "⚪ Inactive").append("\n\n");
                logger.trace("Added sprint to list: ID {}, name: {}, active: {}",
                        sprint.getId(), sprint.getName(), sprint.isActive());
            }
            logger.debug("Sprints list text created with {} sprints", teamSprints.size());

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(sprintsText.toString());

            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            keyboardMarkup.setResizeKeyboard(true);
            List<KeyboardRow> keyboard = new ArrayList<>();

            KeyboardRow row = new KeyboardRow();
            row.add("📊 Sprint Board");

            if (state.getUser().isManager()) {
                row.add("🆕 Create New Sprint");
            }
            keyboard.add(row);

            row = new KeyboardRow();
            if (state.getUser().isManager()) {
                row.add("⏹️ End Active Sprint");
            }
            row.add("🏠 Main Menu");
            keyboard.add(row);

            keyboardMarkup.setKeyboard(keyboard);
            message.setReplyMarkup(keyboardMarkup);
            logger.debug("Created sprints list keyboard for chat ID {}", chatId);

            execute(message);
            logger.info("Sprints list sent to chat ID {}", chatId);
        } catch (Exception e) {
            logger.error("Error viewing all sprints for chat ID {}", chatId, e);
            sendErrorMessage(chatId, "There was an error retrieving the sprints. Please try again later.");
        }
    }

    /**
     * Start the process of ending the active sprint
     */
    private void startEndActiveSprint(long chatId, UserBotState state) {
        logger.info("Starting end active sprint process for chat ID {}, user: {}", chatId,
                state.getUser().getFullName());
        try {
            // Check if user is a manager
            if (!state.getUser().isManager()) {
                logger.warn("User with ID {} is not a manager, cannot end sprints", state.getUser().getId());
                sendErrorMessage(chatId, "Only managers can end sprints.");
                return;
            }

            // Check if user is in a team
            Long teamId = state.getUser().getTeam() != null ? state.getUser().getTeam().getId() : null;
            logger.debug("User team ID: {}", teamId);

            if (teamId == null) {
                logger.warn("User with ID {} is not associated with any team", state.getUser().getId());
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("You are not associated with any team.");

                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                keyboardMarkup.setResizeKeyboard(true);
                List<KeyboardRow> keyboard = new ArrayList<>();

                KeyboardRow row = new KeyboardRow();
                row.add("🏠 Main Menu");
                keyboard.add(row);

                keyboardMarkup.setKeyboard(keyboard);
                message.setReplyMarkup(keyboardMarkup);
                logger.debug("Created no team keyboard for chat ID {}", chatId);

                execute(message);
                logger.info("No team message sent to chat ID {}", chatId);
                return;
            }

            // Get the active sprint
            logger.debug("Fetching active sprint for team ID: {}", teamId);
            Optional<Sprint> activeSprint = sprintService.findActiveSprintByTeamId(teamId);
            logger.debug("Active sprint found: {}", activeSprint.isPresent());

            if (!activeSprint.isPresent()) {
                logger.warn("No active sprint found for team ID {}", teamId);
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("There is no active sprint for your team.");

                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                keyboardMarkup.setResizeKeyboard(true);
                List<KeyboardRow> keyboard = new ArrayList<>();

                KeyboardRow row = new KeyboardRow();
                row.add("🆕 Create New Sprint");
                row.add("🏠 Main Menu");
                keyboard.add(row);

                keyboardMarkup.setKeyboard(keyboard);
                message.setReplyMarkup(keyboardMarkup);
                logger.debug("Created no active sprint keyboard for chat ID {}", chatId);

                execute(message);
                logger.info("No active sprint message sent to chat ID {}", chatId);
                return;
            }

            // Set state for ending active sprint
            state.setEndSprintMode(true);
            userStates.put(chatId, state);
            logger.debug("Set user state for chat ID {}: endSprintMode=true", chatId);

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

            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            keyboardMarkup.setResizeKeyboard(true);
            List<KeyboardRow> keyboard = new ArrayList<>();

            KeyboardRow row = new KeyboardRow();
            row.add("Yes, end sprint");
            row.add("No, cancel");
            keyboard.add(row);

            keyboardMarkup.setKeyboard(keyboard);
            message.setReplyMarkup(keyboardMarkup);
            logger.debug("Created confirmation keyboard for chat ID {}", chatId);

            execute(message);
            logger.info("End sprint confirmation prompt sent to chat ID {}", chatId);
        } catch (Exception e) {
            logger.error("Error starting end active sprint process for chat ID {}", chatId, e);
            sendErrorMessage(chatId, "There was an error in the process. Please try again later.");
        }
    }

    /**
     * Process ending active sprint
     */
    private void processEndActiveSprint(long chatId, String messageText, UserBotState state) {
        logger.info("Processing end active sprint for chat ID {}", chatId);
        logger.debug("Input message: '{}'", messageText);
        try {
            if (messageText.equals("Yes, end sprint")) {
                // Get the active sprint
                Long teamId = state.getUser().getTeam().getId();
                logger.debug("Finding active sprint for team ID: {}", teamId);

                Optional<Sprint> activeSprint = sprintService.findActiveSprintByTeamId(teamId);
                if (activeSprint.isPresent()) {
                    // End the sprint
                    logger.info("Ending active sprint ID {} for team ID {}", activeSprint.get().getId(), teamId);
                    sprintService.completeSprint(activeSprint.get().getId());

                    // Reset state
                    state.setEndSprintMode(false);
                    userStates.put(chatId, state);
                    logger.debug("Reset end sprint mode for chat ID {}", chatId);

                    // Send success message
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText("✅ Sprint \"" + activeSprint.get().getName() + "\" has been ended successfully.");

                    ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                    keyboardMarkup.setResizeKeyboard(true);
                    List<KeyboardRow> keyboard = new ArrayList<>();

                    KeyboardRow row = new KeyboardRow();
                    row.add("🆕 Create New Sprint");
                    row.add("🔍 View All Sprints");
                    keyboard.add(row);

                    row = new KeyboardRow();
                    row.add("🏠 Main Menu");
                    keyboard.add(row);

                    keyboardMarkup.setKeyboard(keyboard);
                    message.setReplyMarkup(keyboardMarkup);
                    logger.debug("Created success keyboard for chat ID {}", chatId);

                    execute(message);
                    logger.info("Sprint end success message sent to chat ID {}", chatId);
                } else {
                    logger.warn("No active sprint found for team ID {} when trying to end it", teamId);
                    sendErrorMessage(chatId, "Could not find the active sprint. It may have already been ended.");

                    // Reset state
                    state.setEndSprintMode(false);
                    userStates.put(chatId, state);
                    logger.debug("Reset end sprint mode for chat ID {}", chatId);
                }
            } else if (messageText.equals("No, cancel")) {
                // Cancel ending sprint
                logger.info("User cancelled ending sprint for chat ID {}", chatId);
                state.setEndSprintMode(false);
                userStates.put(chatId, state);
                logger.debug("Reset end sprint mode for chat ID {}", chatId);

                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Sprint end process cancelled.");

                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                keyboardMarkup.setResizeKeyboard(true);
                List<KeyboardRow> keyboard = new ArrayList<>();

                KeyboardRow row = new KeyboardRow();
                row.add("📊 Sprint Board");
                row.add("🔍 View All Sprints");
                keyboard.add(row);

                row = new KeyboardRow();
                row.add("🏠 Main Menu");
                keyboard.add(row);

                keyboardMarkup.setKeyboard(keyboard);
                message.setReplyMarkup(keyboardMarkup);
                logger.debug("Created cancellation keyboard for chat ID {}", chatId);

                execute(message);
                logger.info("Sprint end cancellation message sent to chat ID {}", chatId);
            } else {
                logger.warn("Unexpected message in end sprint mode: '{}'", messageText);
                sendErrorMessage(chatId, "Please select one of the options.");
            }
        } catch (Exception e) {
            logger.error("Error processing end active sprint for chat ID {}", chatId, e);
            sendErrorMessage(chatId, "There was an error ending the sprint. Please try again later.");

            // Reset state on error
            state.setEndSprintMode(false);
            userStates.put(chatId, state);
            logger.debug("Reset end sprint mode for chat ID {} after error", chatId);
        }
    }

    /**
     * Process task creation stages
     */
    private void processTaskCreation(long chatId, String messageText, UserBotState state) {
        logger.info("Processing task creation for chat ID {}, stage: {}",
                chatId, state.getTaskCreationStage());
        logger.debug("Task creation input: '{}'", messageText);
        try {
            String stage = state.getTaskCreationStage();

            if (stage == null) {
                logger.warn("Task creation stage is null for chat ID {}, assuming TITLE stage", chatId);
                stage = "TITLE";
                state.setTaskCreationStage(stage);
            }

            if ("TITLE".equals(stage)) {
                // Store title and ask for description
                logger.debug("Storing task title: '{}'", messageText);
                state.setTempTaskTitle(messageText);
                state.setTaskCreationStage("DESCRIPTION");
                logger.debug("Updated stage to DESCRIPTION");

                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Great! Now please provide a description for the task:");
                execute(message);
                logger.info("Description prompt sent to chat ID {}", chatId);
            } else if ("DESCRIPTION".equals(stage)) {
                // Store description and ask for estimated hours
                logger.debug("Storing task description: '{}'", messageText);
                state.setTempTaskDescription(messageText);
                state.setTaskCreationStage("ESTIMATED_HOURS");
                logger.debug("Updated stage to ESTIMATED_HOURS");

                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Please enter the estimated hours to complete this task (must be 4 hours or less):");
                execute(message);
                logger.info("Estimated hours prompt sent to chat ID {}", chatId);
            } else if ("ESTIMATED_HOURS".equals(stage)) {
                // Validate estimated hours
                try {
                    double estimatedHours = Double.parseDouble(messageText);
                    if (estimatedHours <= 0 || estimatedHours > 4) {
                        throw new IllegalArgumentException("Estimated hours must be between 0 and 4.");
                    }
                    state.setTempEstimatedHours(estimatedHours);
                    logger.debug("Stored estimated hours: {}", estimatedHours);

                    if (state.getUser().isManager()) {
                        // Managers can assign tasks to others
                        state.setTaskCreationStage("ASSIGNEE");
                        logger.debug("Updated stage to ASSIGNEE");

                        // Fetch team members
                        List<User> teamMembers = userService.findByTeamId(state.getUser().getTeam().getId());
                        logger.debug("Found {} team members for team ID {}",
                                teamMembers.size(), state.getUser().getTeam().getId());

                        SendMessage message = new SendMessage();
                        message.setChatId(chatId);
                        message.setText("Please select who to assign this task to:");

                        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                        keyboardMarkup.setResizeKeyboard(true);
                        List<KeyboardRow> keyboard = new ArrayList<>();

                        // Add manager (self-assignment)
                        KeyboardRow selfRow = new KeyboardRow();
                        selfRow.add("Me (" + state.getUser().getFullName() + ")");
                        keyboard.add(selfRow);

                        // Add all team members
                        for (User member : teamMembers) {
                            if (!member.getId().equals(state.getUser().getId())) {
                                KeyboardRow row = new KeyboardRow();
                                row.add(member.getFullName() + " (ID: " + member.getId() + ")");
                                keyboard.add(row);
                                logger.trace("Added team member to keyboard: {}", member.getFullName());
                            }
                        }

                        keyboardMarkup.setKeyboard(keyboard);
                        message.setReplyMarkup(keyboardMarkup);
                        logger.debug("Created assignee selection keyboard with {} options", keyboard.size());

                        execute(message);
                        logger.info("Assignee selection prompt sent to chat ID {}", chatId);
                    } else {
                        // Developers assign tasks to themselves, skip to priority
                        state.setTaskCreationStage("PRIORITY");
                        logger.debug("Developer user, skipping ASSIGNEE stage and updating to PRIORITY");

                        SendMessage message = new SendMessage();
                        message.setChatId(chatId);
                        message.setText("Please select the priority for this task:");

                        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                        keyboardMarkup.setResizeKeyboard(true);
                        List<KeyboardRow> keyboard = new ArrayList<>();

                        KeyboardRow row = new KeyboardRow();
                        row.add("High");
                        row.add("Medium");
                        row.add("Low");
                        keyboard.add(row);

                        keyboardMarkup.setKeyboard(keyboard);
                        message.setReplyMarkup(keyboardMarkup);

                        execute(message);
                        logger.info("Priority selection prompt sent to chat ID {}", chatId);
                    }
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid estimated hours entered: '{}'", messageText, e);
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText("Please enter a valid number for estimated hours (between 0 and 4):");
                    execute(message);
                    logger.info("Validation error message sent for estimated hours to chat ID {}", chatId);
                }
            } else if ("ASSIGNEE".equals(stage)) {
                // Process assignee selection
                logger.debug("Processing assignee selection: '{}'", messageText);

                Long assigneeId;
                if (messageText.startsWith("Me (")) {
                    // Self-assignment
                    assigneeId = state.getUser().getId();
                    logger.debug("Self-assignment selected, assignee ID: {}", assigneeId);
                } else {
                    // Extract ID from "Name (ID: X)"
                    try {
                        String idPart = messageText.substring(messageText.indexOf("ID: ") + 4,
                                messageText.length() - 1);
                        assigneeId = Long.parseLong(idPart);
                        logger.debug("Extracted assignee ID from selection: {}", assigneeId);
                    } catch (Exception e) {
                        logger.warn("Error parsing assignee ID from: '{}'", messageText, e);
                        sendErrorMessage(chatId, "Invalid selection. Please select a valid assignee.");
                        return;
                    }
                }

                // Store assignee ID and move to priority
                state.setTempAssigneeId(assigneeId);
                state.setTaskCreationStage("PRIORITY");
                logger.debug("Updated stage to PRIORITY, stored assignee ID: {}", assigneeId);

                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Please select the priority for this task:");

                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                keyboardMarkup.setResizeKeyboard(true);
                List<KeyboardRow> keyboard = new ArrayList<>();

                KeyboardRow row = new KeyboardRow();
                row.add("High");
                row.add("Medium");
                row.add("Low");
                keyboard.add(row);

                keyboardMarkup.setKeyboard(keyboard);
                message.setReplyMarkup(keyboardMarkup);

                execute(message);
                logger.info("Priority selection prompt sent to chat ID {}", chatId);
            } else if ("PRIORITY".equals(stage)) {
                // Process priority selection
                logger.debug("Processing priority selection: '{}'", messageText);
                if (!messageText.equals("High") && !messageText.equals("Medium") && !messageText.equals("Low")) {
                    logger.warn("Invalid priority entered: '{}'", messageText);
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText("Please select a valid priority (High, Medium, or Low):");
                    execute(message);
                    logger.info("Validation error message sent for priority to chat ID {}", chatId);
                    return;
                }

                state.setTempPriority(messageText);
                state.setTaskCreationStage("CONFIRMATION");
                logger.debug("Updated stage to CONFIRMATION, stored priority: {}", messageText);

                StringBuilder summary = new StringBuilder();
                summary.append("Please confirm the task details:\n\n");
                summary.append("Title: ").append(state.getTempTaskTitle()).append("\n");
                summary.append("Description: ").append(state.getTempTaskDescription()).append("\n");
                summary.append("Estimated Hours: ").append(state.getTempEstimatedHours()).append("\n");
                summary.append("Priority: ").append(state.getTempPriority()).append("\n");

                // Add assignee information
                if (state.getTempAssigneeId() != null) {
                    Optional<User> assignee = userService.findById(state.getTempAssigneeId());
                    if (assignee.isPresent()) {
                        summary.append("Assignee: ").append(assignee.get().getFullName()).append("\n");
                    }
                } else {
                    summary.append("Assignee: ").append(state.getUser().getFullName()).append(" (you)\n");
                }

                summary.append("\nIs this correct?");
                logger.debug("Task confirmation summary: {}", summary.toString());

                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText(summary.toString());

                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                keyboardMarkup.setResizeKeyboard(true);
                List<KeyboardRow> keyboard = new ArrayList<>();

                KeyboardRow row = new KeyboardRow();
                row.add("Yes, create task");
                row.add("No, cancel");
                keyboard.add(row);

                keyboardMarkup.setKeyboard(keyboard);
                message.setReplyMarkup(keyboardMarkup);

                execute(message);
                logger.info("Confirmation prompt sent to chat ID {}", chatId);
            } else if ("CONFIRMATION".equals(stage)) {
                if (messageText.equals("Yes, create task")) {
                    // Create the task
                    ToDoItem task = new ToDoItem();
                    task.setTitle(state.getTempTaskTitle());
                    task.setDescription(state.getTempTaskDescription());
                    task.setEstimatedHours(state.getTempEstimatedHours());
                    task.setPriority(state.getTempPriority());
                    task.setCreation_ts(OffsetDateTime.now());

                    // Set assignee (from temp state or current user)
                    if (state.getTempAssigneeId() != null) {
                        task.setAssigneeId(state.getTempAssigneeId());
                        logger.debug("Setting assignee from temp state: {}", state.getTempAssigneeId());
                    } else {
                        task.setAssigneeId(state.getUser().getId());
                        logger.debug("Setting current user as assignee: {}", state.getUser().getId());
                    }

                    ToDoItem savedTask = toDoItemService.addToDoItem(task);
                    logger.info("Task created successfully with ID: {}", savedTask.getID());

                    // Reset state
                    state.resetTaskCreation();
                    logger.debug("Reset task creation state for chat ID {}", chatId);

                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText("✅ Task created successfully with ID: " + savedTask.getID());

                    ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                    keyboardMarkup.setResizeKeyboard(true);
                    List<KeyboardRow> keyboard = new ArrayList<>();

                    KeyboardRow row = new KeyboardRow();
                    row.add("📝 Create Another Task");
                    row.add("🏠 Main Menu");
                    keyboard.add(row);

                    keyboardMarkup.setKeyboard(keyboard);
                    message.setReplyMarkup(keyboardMarkup);

                    execute(message);
                    logger.info("Task creation success message sent to chat ID {}", chatId);
                } else {
                    // Cancel task creation
                    logger.info("User cancelled task creation for chat ID {}", chatId);
                    state.resetTaskCreation();
                    logger.debug("Reset task creation state for chat ID {}", chatId);

                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText("Task creation cancelled. What would you like to do next?");

                    ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                    keyboardMarkup.setResizeKeyboard(true);
                    List<KeyboardRow> keyboard = new ArrayList<>();

                    KeyboardRow row = new KeyboardRow();
                    row.add("📝 Create New Task");
                    row.add("🏠 Main Menu");
                    keyboard.add(row);

                    keyboardMarkup.setKeyboard(keyboard);
                    message.setReplyMarkup(keyboardMarkup);

                    execute(message);
                    logger.info("Task creation cancellation message sent to chat ID {}", chatId);
                }
            }

            // Update user state
            userStates.put(chatId, state);
            logger.debug("Updated user state in map for chat ID {}", chatId);
        } catch (Exception e) {
            logger.error("Error in task creation process for chat ID {}", chatId, e);
            sendErrorMessage(chatId, "There was an error in the task creation process. Please try again.");

            // Reset task creation state
            state.resetTaskCreation();
            userStates.put(chatId, state);
            logger.debug("Reset task creation state after error for chat ID {}", chatId);
        }
    }

    /**
     * Show user's active tasks
     */
    private void showActiveTasksForUser(long chatId, UserBotState state) {
        logger.info("Showing active tasks for chat ID {}, user: {}", chatId, state.getUser().getFullName());
        try {
            logger.debug("Fetching active tasks for user ID: {}", state.getUser().getId());
            List<ToDoItem> activeTasks = toDoItemService.findActiveTasksByAssigneeId(state.getUser().getId());
            logger.debug("Found {} active tasks for user", activeTasks.size());

            if (activeTasks.isEmpty()) {
                logger.info("No active tasks found for user with ID {}", state.getUser().getId());
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("You don't have any active tasks at the moment.");

                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                keyboardMarkup.setResizeKeyboard(true);
                List<KeyboardRow> keyboard = new ArrayList<>();

                KeyboardRow row = new KeyboardRow();
                row.add("📝 Create New Task");
                row.add("🏠 Main Menu");
                keyboard.add(row);

                keyboardMarkup.setKeyboard(keyboard);
                message.setReplyMarkup(keyboardMarkup);
                logger.debug("Created empty tasks keyboard for chat ID {}", chatId);

                execute(message);
                logger.info("Empty tasks message sent to chat ID {}", chatId);
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
                logger.trace("Added task to list: ID {}, title: {}, status: {}",
                        task.getID(), task.getTitle(), task.getStatus());
            }
            logger.debug("Created task list text with {} tasks", activeTasks.size());

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(tasksText.toString());

            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            keyboardMarkup.setResizeKeyboard(true);
            List<KeyboardRow> keyboard = new ArrayList<>();

            KeyboardRow row = new KeyboardRow();
            row.add("✅ Mark Task Complete");
            row.add("🔄 Start Working on Task");
            keyboard.add(row);

            row = new KeyboardRow();
            row.add("🏠 Main Menu");
            keyboard.add(row);

            keyboardMarkup.setKeyboard(keyboard);
            message.setReplyMarkup(keyboardMarkup);
            logger.debug("Created active tasks keyboard for chat ID {}", chatId);

            execute(message);
            logger.info("Active tasks list sent to chat ID {}", chatId);
        } catch (Exception e) {
            logger.error("Error showing active tasks for chat ID {}", chatId, e);
            sendErrorMessage(chatId, "There was an error retrieving your active tasks. Please try again later.");
        }
    }

    /**
     * Start the task completion flow
     */
    private void startTaskCompletion(long chatId, UserBotState state) {
        logger.info("Starting task completion flow for chat ID {}, user: {}", chatId, state.getUser().getFullName());
        try {
            logger.debug("Fetching active tasks for user ID: {}", state.getUser().getId());
            List<ToDoItem> activeTasks = toDoItemService.findActiveTasksByAssigneeId(state.getUser().getId());
            logger.debug("Found {} active tasks for user", activeTasks.size());

            if (activeTasks.isEmpty()) {
                logger.info("No active tasks found for user with ID {}", state.getUser().getId());
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("You don't have any active tasks to complete.");

                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                keyboardMarkup.setResizeKeyboard(true);
                List<KeyboardRow> keyboard = new ArrayList<>();

                KeyboardRow row = new KeyboardRow();
                row.add("📝 Create New Task");
                row.add("🏠 Main Menu");
                keyboard.add(row);

                keyboardMarkup.setKeyboard(keyboard);
                message.setReplyMarkup(keyboardMarkup);
                logger.debug("Created empty tasks keyboard for chat ID {}", chatId);

                execute(message);
                logger.info("Empty tasks message sent to chat ID {}", chatId);
                return;
            }

            state.setTaskCompletionMode(true);
            state.setTaskCompletionStage("SELECT_TASK");
            userStates.put(chatId, state);
            logger.debug("Set user state for chat ID {}: taskCompletionMode=true, taskCompletionStage=SELECT_TASK",
                    chatId);

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Please enter the ID of the task you want to mark as complete:");

            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            keyboardMarkup.setResizeKeyboard(true);
            List<KeyboardRow> keyboard = new ArrayList<>();

            for (ToDoItem task : activeTasks) {
                KeyboardRow row = new KeyboardRow();
                row.add("ID: " + task.getID() + " - " + task.getTitle());
                keyboard.add(row);
                logger.trace("Added task to keyboard: ID {}, title: {}", task.getID(), task.getTitle());
            }

            KeyboardRow row = new KeyboardRow();
            row.add("Cancel");
            keyboard.add(row);

            keyboardMarkup.setKeyboard(keyboard);
            message.setReplyMarkup(keyboardMarkup);
            logger.debug("Created task selection keyboard with {} tasks for chat ID {}", activeTasks.size(), chatId);

            execute(message);
            logger.info("Task selection prompt sent to chat ID {}", chatId);
        } catch (Exception e) {
            logger.error("Error starting task completion for chat ID {}", chatId, e);
            sendErrorMessage(chatId,
                    "There was an error starting the task completion process. Please try again later.");
        }
    }

    /**
     * Process task completion stages
     */
    private void processTaskCompletion(long chatId, String messageText, UserBotState state) {
        logger.info("Processing task completion for chat ID {}, stage: {}",
                chatId, state.getTaskCompletionStage());
        logger.debug("Task completion input: '{}'", messageText);
        try {
            String stage = state.getTaskCompletionStage();

            if ("SELECT_TASK".equals(stage)) {
                if (messageText.equals("Cancel")) {
                    logger.info("User cancelled task completion for chat ID {}", chatId);
                    state.resetTaskCompletion();
                    logger.debug("Reset task completion state for chat ID {}", chatId);
                    showDeveloperTaskMenu(chatId, state);
                    return;
                }

                // Extract task ID from message
                int taskId;
                if (messageText.startsWith("ID: ")) {
                    // Parse from "ID: X - Title"
                    String idPart = messageText.substring(4, messageText.indexOf(" - "));
                    taskId = Integer.parseInt(idPart);
                    logger.debug("Extracted task ID {} from selection '{}'", taskId, messageText);
                } else {
                    // Try to parse as a direct ID
                    try {
                        taskId = Integer.parseInt(messageText);
                        logger.debug("Parsed task ID {} directly from input", taskId);
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid task ID format: '{}'", messageText, e);
                        SendMessage message = new SendMessage();
                        message.setChatId(chatId);
                        message.setText("Please enter a valid task ID or select from the list.");
                        execute(message);
                        logger.info("Invalid ID format message sent to chat ID {}", chatId);
                        return;
                    }
                }

                // Check if task exists and belongs to user
                logger.debug("Fetching task with ID {} for validation", taskId);
                ResponseEntity<ToDoItem> response = getToDoItemById(taskId);
                if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                    logger.warn("Task not found with ID {}", taskId);
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText("Task not found. Please enter a valid task ID.");
                    execute(message);
                    logger.info("Task not found message sent to chat ID {}", chatId);
                    return;
                }

                ToDoItem task = response.getBody();
                if (task == null || !task.getAssigneeId().equals(state.getUser().getId())) {
                    logger.warn("Task {} is not assigned to user {}", taskId, state.getUser().getId());
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText("This task is not assigned to you. Please enter a valid task ID.");
                    execute(message);
                    logger.info("Task not assigned message sent to chat ID {}", chatId);
                    return;
                }

                // Store task ID and move to actual hours stage
                state.setTempTaskId(taskId);
                state.setTaskCompletionStage("ACTUAL_HOURS");
                logger.debug("Updated stage to ACTUAL_HOURS, stored task ID: {}", taskId);

                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Please enter the actual hours spent on this task:");

                ReplyKeyboardRemove keyboardRemove = new ReplyKeyboardRemove(true);
                message.setReplyMarkup(keyboardRemove);
                logger.debug("Created actual hours prompt with keyboard removed");

                execute(message);
                logger.info("Actual hours prompt sent to chat ID {}", chatId);
            } else if ("ACTUAL_HOURS".equals(stage)) {
                // Validate and store actual hours
                try {
                    logger.debug("Parsing actual hours from input: '{}'", messageText);
                    double actualHours = Double.parseDouble(messageText);
                    logger.debug("Parsed actual hours: {}", actualHours);

                    if (actualHours <= 0) {
                        logger.warn("Invalid actual hours (<=0) entered: {}", actualHours);
                        SendMessage message = new SendMessage();
                        message.setChatId(chatId);
                        message.setText("Actual hours must be greater than 0. Please enter a valid number:");
                        execute(message);
                        logger.info("Validation error message sent for actual hours to chat ID {}", chatId);
                        return;
                    }

                    // Store actual hours and ask for comments
                    state.setTempActualHours(actualHours);
                    state.setTaskCompletionStage("COMMENTS");
                    logger.debug("Updated stage to COMMENTS, stored actual hours: {}", actualHours);

                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText(
                            "Please enter any comments or notes about the completed task (or type 'skip' to skip):");
                    execute(message);
                    logger.info("Comments prompt sent to chat ID {}", chatId);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid actual hours format entered: '{}'", messageText, e);
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText("Please enter a valid number for actual hours:");
                    execute(message);
                    logger.info("Format error message sent for actual hours to chat ID {}", chatId);
                }
            } else if ("COMMENTS".equals(stage)) {
                // Store comments and complete task
                String comments = messageText.equals("skip") ? "" : messageText;
                logger.debug("Processing comments: '{}'", comments);

                // Complete the task
                logger.debug("Completing task ID {} with actual hours {} and comments",
                        state.getTempTaskId(), state.getTempActualHours());
                ToDoItem task = toDoItemService.completeTask(state.getTempTaskId(), state.getTempActualHours(),
                        comments);
                logger.info("Task ID {} successfully completed", task.getID());

                // Reset state
                state.resetTaskCompletion();
                logger.debug("Reset task completion state for chat ID {}", chatId);

                // Show success message
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("✅ Task " + task.getID() + " marked as completed successfully!");

                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                keyboardMarkup.setResizeKeyboard(true);
                List<KeyboardRow> keyboard = new ArrayList<>();

                KeyboardRow row = new KeyboardRow();
                row.add("🔄 My Active Tasks");
                row.add("📊 Sprint Board");
                keyboard.add(row);

                row = new KeyboardRow();
                row.add("🏠 Main Menu");
                keyboard.add(row);

                keyboardMarkup.setKeyboard(keyboard);
                message.setReplyMarkup(keyboardMarkup);
                logger.debug("Created success keyboard for chat ID {}", chatId);

                execute(message);
                logger.info("Task completion success message sent to chat ID {}", chatId);
            }

            // Update user state
            userStates.put(chatId, state);
            logger.debug("Updated user state in map for chat ID {}", chatId);
        } catch (Exception e) {
            logger.error("Error in task completion process for chat ID {}", chatId, e);
            sendErrorMessage(chatId, "There was an error in the task completion process. Please try again.");

            // Reset task completion state
            state.resetTaskCompletion();
            userStates.put(chatId, state);
            logger.debug("Reset task completion state after error for chat ID {}", chatId);
        }
    }

    /**
     * Show active sprint board
     */
    private void showSprintBoard(long chatId, UserBotState state) {
        logger.info("Showing sprint board for chat ID {}, user: {}", chatId, state.getUser().getFullName());
        try {
            // Try to find the active sprint for the user's team
            Long teamId = state.getUser().getTeam() != null ? state.getUser().getTeam().getId() : null;
            logger.debug("User team ID: {}", teamId);

            if (teamId == null) {
                logger.warn("User with ID {} is not associated with any team", state.getUser().getId());
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("You are not associated with any team.");

                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                keyboardMarkup.setResizeKeyboard(true);
                List<KeyboardRow> keyboard = new ArrayList<>();

                KeyboardRow row = new KeyboardRow();
                row.add("🏠 Main Menu");
                keyboard.add(row);

                keyboardMarkup.setKeyboard(keyboard);
                message.setReplyMarkup(keyboardMarkup);
                logger.debug("Created no team keyboard for chat ID {}", chatId);

                execute(message);
                logger.info("No team message sent to chat ID {}", chatId);
                return;
            }

            // Get the active sprint
            logger.debug("Fetching active sprint for team ID: {}", teamId);
            Optional<Sprint> activeSprint = sprintService.findActiveSprintByTeamId(teamId);
            logger.debug("Active sprint found: {}", activeSprint.isPresent());

            if (!activeSprint.isPresent()) {
                logger.warn("No active sprint found for team ID {}", teamId);
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("There is no active sprint for your team.");

                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                keyboardMarkup.setResizeKeyboard(true);
                List<KeyboardRow> keyboard = new ArrayList<>();

                KeyboardRow row = new KeyboardRow();
                row.add("🏠 Main Menu");
                keyboard.add(row);

                keyboardMarkup.setKeyboard(keyboard);
                message.setReplyMarkup(keyboardMarkup);
                logger.debug("Created no active sprint keyboard for chat ID {}", chatId);

                execute(message);
                logger.info("No active sprint message sent to chat ID {}", chatId);
                return;
            }

            // Get tasks in the sprint
            logger.debug("Fetching tasks for sprint ID: {}", activeSprint.get().getId());
            List<ToDoItem> sprintTasks = toDoItemService.findTasksBySprintId(activeSprint.get().getId());
            logger.debug("Found {} tasks in the sprint", sprintTasks.size());

            if (sprintTasks.isEmpty()) {
                logger.info("No tasks found in sprint ID {} ({})",
                        activeSprint.get().getId(), activeSprint.get().getName());
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("There are no tasks in the current sprint: " + activeSprint.get().getName());

                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                keyboardMarkup.setResizeKeyboard(true);
                List<KeyboardRow> keyboard = new ArrayList<>();

                KeyboardRow row = new KeyboardRow();
                row.add("📝 Create New Task");
                row.add("🏠 Main Menu");
                keyboard.add(row);

                keyboardMarkup.setKeyboard(keyboard);
                message.setReplyMarkup(keyboardMarkup);
                logger.debug("Created empty sprint keyboard for chat ID {}", chatId);

                execute(message);
                logger.info("Empty sprint message sent to chat ID {}", chatId);
                return;
            }

            // Group tasks by status
            Map<String, List<ToDoItem>> tasksByStatus = new HashMap<>();
            logger.debug("Grouping tasks by status");

            for (ToDoItem task : sprintTasks) {
                String status = task.getStatus() != null ? task.getStatus() : "BACKLOG";

                if (!tasksByStatus.containsKey(status)) {
                    tasksByStatus.put(status, new ArrayList<>());
                }

                tasksByStatus.get(status).add(task);
                logger.trace("Added task ID {} to status group {}", task.getID(), status);
            }

            // Build board text
            StringBuilder boardText = new StringBuilder();
            boardText.append("📊 Sprint Board: ").append(activeSprint.get().getName()).append("\n\n");

            // Define status display order
            List<String> statusOrder = Arrays.asList(
                    "BACKLOG",
                    "SELECTED_FOR_DEVELOPMENT",
                    "IN_PROGRESS",
                    "IN_SPRINT",
                    "IN_QA",
                    "COMPLETED");
            logger.debug("Using status order: {}", statusOrder);

            // Add tasks by status
            for (String status : statusOrder) {
                if (tasksByStatus.containsKey(status)) {
                    String displayStatus;

                    try {
                        displayStatus = TaskStatus.valueOf(status).getDisplayName();
                        logger.trace("Mapped status {} to display name {}", status, displayStatus);
                    } catch (IllegalArgumentException e) {
                        displayStatus = status;
                        logger.warn("Could not map status {} to display name, using raw value", status);
                    }

                    boardText.append("✦ ").append(displayStatus).append(" ✦\n");

                    for (ToDoItem task : tasksByStatus.get(status)) {
                        boardText.append("- ID: ").append(task.getID())
                                .append(" | ").append(task.getTitle())
                                .append(" | Est: ").append(task.getEstimatedHours()).append("h");

                        if (task.getActualHours() != null) {
                            boardText.append(" | Act: ").append(task.getActualHours()).append("h");
                        }

                        boardText.append("\n");
                        logger.trace("Added task to board text: ID {}, title: {}", task.getID(), task.getTitle());
                    }

                    boardText.append("\n");
                }
            }
            logger.debug("Sprint board text created with tasks from {} status groups", tasksByStatus.size());

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(boardText.toString());

            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            keyboardMarkup.setResizeKeyboard(true);
            List<KeyboardRow> keyboard = new ArrayList<>();

            KeyboardRow row = new KeyboardRow();
            row.add("🔄 My Active Tasks");
            row.add("📝 Create New Task");
            keyboard.add(row);

            row = new KeyboardRow();
            row.add("✅ Mark Task Complete");
            row.add("🏠 Main Menu");
            keyboard.add(row);

            keyboardMarkup.setKeyboard(keyboard);
            message.setReplyMarkup(keyboardMarkup);
            logger.debug("Created sprint board keyboard for chat ID {}", chatId);

            execute(message);
            logger.info("Sprint board sent to chat ID {}", chatId);
        } catch (Exception e) {
            logger.error("Error showing sprint board for chat ID {}", chatId, e);
            sendErrorMessage(chatId, "There was an error retrieving the sprint board. Please try again later.");
        }
    }

    /**
     * Start the process of assigning a task to a sprint
     */
    private void startAssignTaskToSprint(long chatId, UserBotState state) {
        logger.info("Starting assign task to sprint process for chat ID {}, user: {}",
                chatId, state.getUser().getFullName());
        try {
            // Check if user is a developer or manager
            if (!state.getUser().isDeveloper() && !state.getUser().isManager()) {
                logger.warn("User with ID {} is not a developer or manager, cannot assign tasks to sprint",
                        state.getUser().getId());
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Only developers and managers can assign tasks to sprints.");

                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                keyboardMarkup.setResizeKeyboard(true);
                List<KeyboardRow> keyboard = new ArrayList<>();

                KeyboardRow row = new KeyboardRow();
                row.add("🏠 Main Menu");
                keyboard.add(row);

                keyboardMarkup.setKeyboard(keyboard);
                message.setReplyMarkup(keyboardMarkup);
                logger.debug("Created unauthorized keyboard for chat ID {}", chatId);

                execute(message);
                logger.info("Unauthorized message sent to chat ID {}", chatId);
                return;
            }

            // Get user's active tasks not yet in a sprint
            logger.debug("Fetching active tasks not in sprint for user ID: {}", state.getUser().getId());
            List<ToDoItem> backlogTasks = toDoItemService.findActiveTasksByAssigneeId(state.getUser().getId()).stream()
                    .filter(task -> task.getSprintId() == null)
                    .collect(Collectors.toList());
            logger.debug("Found {} backlog tasks for user", backlogTasks.size());

            if (backlogTasks.isEmpty()) {
                logger.info("No backlog tasks found for user with ID {}", state.getUser().getId());
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("You don't have any backlog tasks to assign to a sprint.");

                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                keyboardMarkup.setResizeKeyboard(true);
                List<KeyboardRow> keyboard = new ArrayList<>();

                KeyboardRow row = new KeyboardRow();
                row.add("📝 Create New Task");
                row.add("🏠 Main Menu");
                keyboard.add(row);

                keyboardMarkup.setKeyboard(keyboard);
                message.setReplyMarkup(keyboardMarkup);
                logger.debug("Created no backlog tasks keyboard for chat ID {}", chatId);

                execute(message);
                logger.info("No backlog tasks message sent to chat ID {}", chatId);
                return;
            }

            // Try to find the active sprint for the user's team
            Long teamId = state.getUser().getTeam() != null ? state.getUser().getTeam().getId() : null;
            logger.debug("User team ID: {}", teamId);

            if (teamId == null) {
                logger.warn("User with ID {} is not associated with any team", state.getUser().getId());
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("You are not associated with any team.");

                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                keyboardMarkup.setResizeKeyboard(true);
                List<KeyboardRow> keyboard = new ArrayList<>();

                KeyboardRow row = new KeyboardRow();
                row.add("🏠 Main Menu");
                keyboard.add(row);

                keyboardMarkup.setKeyboard(keyboard);
                message.setReplyMarkup(keyboardMarkup);
                logger.debug("Created no team keyboard for chat ID {}", chatId);

                execute(message);
                logger.info("No team message sent to chat ID {}", chatId);
                return;
            }

            // Get the active sprint
            logger.debug("Fetching active sprint for team ID: {}", teamId);
            Optional<Sprint> activeSprint = sprintService.findActiveSprintByTeamId(teamId);
            logger.debug("Active sprint found: {}", activeSprint.isPresent());

            if (!activeSprint.isPresent()) {
                logger.warn("No active sprint found for team ID {}", teamId);
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("There is no active sprint for your team.");

                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                keyboardMarkup.setResizeKeyboard(true);
                List<KeyboardRow> keyboard = new ArrayList<>();

                KeyboardRow row = new KeyboardRow();
                row.add("🏠 Main Menu");
                keyboard.add(row);

                keyboardMarkup.setKeyboard(keyboard);
                message.setReplyMarkup(keyboardMarkup);
                logger.debug("Created no active sprint keyboard for chat ID {}", chatId);

                execute(message);
                logger.info("No active sprint message sent to chat ID {}", chatId);
                return;
            }

            // Set state for assigning task to sprint
            state.setAssignToSprintMode(true);
            state.setAssignToSprintStage("SELECT_TASK");
            state.setTempSprintId(activeSprint.get().getId());
            userStates.put(chatId, state);
            logger.debug(
                    "Set user state for chat ID {}: assignToSprintMode=true, assignToSprintStage=SELECT_TASK, sprintId={}",
                    chatId, activeSprint.get().getId());

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Please select a task to add to the sprint \"" + activeSprint.get().getName() + "\":");

            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            keyboardMarkup.setResizeKeyboard(true);
            List<KeyboardRow> keyboard = new ArrayList<>();

            for (ToDoItem task : backlogTasks) {
                KeyboardRow row = new KeyboardRow();
                row.add("ID: " + task.getID() + " - " + task.getTitle());
                keyboard.add(row);
                logger.trace("Added task to keyboard: ID {}, title: {}", task.getID(), task.getTitle());
            }

            KeyboardRow row = new KeyboardRow();
            row.add("Cancel");
            keyboard.add(row);

            keyboardMarkup.setKeyboard(keyboard);
            message.setReplyMarkup(keyboardMarkup);
            logger.debug("Created task selection keyboard with {} tasks for chat ID {}", backlogTasks.size(), chatId);

            execute(message);
            logger.info("Task selection prompt sent to chat ID {}", chatId);
        } catch (Exception e) {
            logger.error("Error starting assign task to sprint process for chat ID {}", chatId, e);
            sendErrorMessage(chatId, "There was an error in the process. Please try again later.");
        }
    }

    /**
     * Process task assignment to sprint
     */
    private void processAssignTaskToSprint(long chatId, String messageText, UserBotState state) {
        logger.info("Processing assign task to sprint for chat ID {}", chatId);
        logger.debug("Input message: '{}'", messageText);
        try {
            if (messageText.equals("Cancel")) {
                logger.info("User cancelled assign task to sprint for chat ID {}", chatId);
                state.resetAssignToSprint();
                logger.debug("Reset assign to sprint state for chat ID {}", chatId);
                showDeveloperTaskMenu(chatId, state);
                return;
            }

            // Extract task ID from message
            int taskId;
            if (messageText.startsWith("ID: ")) {
                // Parse from "ID: X - Title"
                String idPart = messageText.substring(4, messageText.indexOf(" - "));
                taskId = Integer.parseInt(idPart);
                logger.debug("Extracted task ID {} from selection '{}'", taskId, messageText);
            } else {
                // Try to parse as a direct ID
                try {
                    taskId = Integer.parseInt(messageText);
                    logger.debug("Parsed task ID {} directly from input", taskId);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid task ID format: '{}'", messageText, e);
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText("Please enter a valid task ID or select from the list.");
                    execute(message);
                    logger.info("Invalid ID format message sent to chat ID {}", chatId);
                    return;
                }
            }

            // Check if task exists and belongs to user
            logger.debug("Fetching task with ID {} for validation", taskId);
            ResponseEntity<ToDoItem> response = getToDoItemById(taskId);
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                logger.warn("Task not found with ID {}", taskId);
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Task not found. Please enter a valid task ID.");
                execute(message);
                logger.info("Task not found message sent to chat ID {}", chatId);
                return;
            }

            // Assign task to sprint
            logger.debug("Assigning task ID {} to sprint ID {}",
                    taskId, state.getTempSprintId());
            ToDoItem task = toDoItemService.assignTaskToSprint(taskId, state.getTempSprintId());
            logger.info("Task ID {} successfully assigned to sprint ID {}",
                    taskId, state.getTempSprintId());

            // Reset state
            state.resetAssignToSprint();
            logger.debug("Reset assign to sprint state for chat ID {}", chatId);

            // Show success message
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("✅ Task " + task.getID() + " has been added to the sprint successfully!");

            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            keyboardMarkup.setResizeKeyboard(true);
            List<KeyboardRow> keyboard = new ArrayList<>();

            KeyboardRow row = new KeyboardRow();
            row.add("🔄 My Active Tasks");
            row.add("📊 Sprint Board");
            keyboard.add(row);

            row = new KeyboardRow();
            row.add("🏠 Main Menu");
            keyboard.add(row);

            keyboardMarkup.setKeyboard(keyboard);
            message.setReplyMarkup(keyboardMarkup);
            logger.debug("Created success keyboard for chat ID {}", chatId);

            execute(message);
            logger.info("Success message sent to chat ID {}", chatId);
        } catch (Exception e) {
            logger.error("Error assigning task to sprint for chat ID {}", chatId, e);
            sendErrorMessage(chatId, "There was an error assigning the task to the sprint. Please try again later.");

            // Reset state
            state.resetAssignToSprint();
            userStates.put(chatId, state);
            logger.debug("Reset assign to sprint state after error for chat ID {}", chatId);
        }
    }

    /**
     * Start the process for a developer to begin working on a task
     */
    private void startTaskWorkProcess(long chatId, UserBotState state) {
        logger.info("Starting task work process for chat ID {}, user: {}",
                chatId, state.getUser().getFullName());
        try {
            List<ToDoItem> tasksInSprint = new ArrayList<>();

            // If user is in a team, find the active sprint
            if (state.getUser().getTeam() != null) {
                logger.debug("Finding active sprint for team ID: {}", state.getUser().getTeam().getId());
                Optional<Sprint> activeSprint = sprintService
                        .findActiveSprintByTeamId(state.getUser().getTeam().getId());

                if (activeSprint.isPresent()) {
                    logger.debug("Found active sprint ID: {}", activeSprint.get().getId());
                    // Get tasks in the sprint that are not in progress or completed
                    tasksInSprint = toDoItemService.findTasksBySprintId(activeSprint.get().getId()).stream()
                            .filter(task -> !TaskStatus.IN_PROGRESS.name().equals(task.getStatus()) &&
                                    !TaskStatus.COMPLETED.name().equals(task.getStatus()))
                            .collect(Collectors.toList());
                    logger.debug("Found {} available tasks in the sprint", tasksInSprint.size());
                } else {
                    logger.debug("No active sprint found for team ID: {}", state.getUser().getTeam().getId());
                }
            } else {
                logger.debug("User is not associated with a team");
            }

            if (tasksInSprint.isEmpty()) {
                logger.info("No available tasks found in current sprint for chat ID {}", chatId);
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("There are no available tasks in the current sprint.");

                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                keyboardMarkup.setResizeKeyboard(true);
                List<KeyboardRow> keyboard = new ArrayList<>();

                KeyboardRow row = new KeyboardRow();
                row.add("📝 Create New Task");
                row.add("🏠 Main Menu");
                keyboard.add(row);

                keyboardMarkup.setKeyboard(keyboard);
                message.setReplyMarkup(keyboardMarkup);
                logger.debug("Created no available tasks keyboard for chat ID {}", chatId);

                execute(message);
                logger.info("No available tasks message sent to chat ID {}", chatId);
                return;
            }

            state.setStartTaskWorkStage("SELECT_TASK");
            userStates.put(chatId, state);
            logger.debug("Set user state for chat ID {}: startTaskWorkStage=SELECT_TASK", chatId);

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Please select a task to start working on:");

            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            keyboardMarkup.setResizeKeyboard(true);
            List<KeyboardRow> keyboard = new ArrayList<>();

            for (ToDoItem task : tasksInSprint) {
                KeyboardRow row = new KeyboardRow();
                row.add("ID: " + task.getID() + " - " + task.getTitle());
                keyboard.add(row);
                logger.trace("Added task to keyboard: ID {}, title: {}", task.getID(), task.getTitle());
            }

            KeyboardRow row = new KeyboardRow();
            row.add("Cancel");
            keyboard.add(row);

            keyboardMarkup.setKeyboard(keyboard);
            message.setReplyMarkup(keyboardMarkup);
            logger.debug("Created task selection keyboard with {} tasks for chat ID {}", tasksInSprint.size(), chatId);

            execute(message);
            logger.info("Task selection prompt sent to chat ID {}", chatId);
        } catch (Exception e) {
            logger.error("Error starting task work process for chat ID {}", chatId, e);
            sendErrorMessage(chatId, "There was an error starting the task work process. Please try again later.");
        }
    }
}