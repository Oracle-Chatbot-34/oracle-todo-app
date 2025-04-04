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
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(text);
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Failed to send message: " + text, e);
        }
    }

    /**
     * Helper method to send error messages to users
     */
    private void sendErrorMessage(long chatId, String errorText) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("❌ " + errorText);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Failed to send error message: " + errorText, e);
        }
    }

    /**
     * Shows the main menu screen to the user
     * Called after successful authentication or when returning to the main menu
     */
    private void showMainScreen(long chatId, UserBotState state) {
        try {
            SendMessage messageToTelegram = new SendMessage();
            messageToTelegram.setChatId(chatId);

            // Personalized welcome message using the user's name
            String welcomeMessage = "Hello, " + state.getUser().getFullName() + "! " +
                    BotMessages.HELLO_MYTODO_BOT.getMessage();
            messageToTelegram.setText(welcomeMessage);

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

            execute(messageToTelegram);
        } catch (TelegramApiException e) {
            logger.error("Error showing main screen", e);
            sendErrorMessage(chatId,
                    "There was a problem displaying the main menu. Please try again by typing /start.");
        }
    }

    public ToDoItemBotController(String botToken, String botName, ToDoItemService toDoItemService,
            UserService userService, SprintService sprintService) {
        super(botToken);
        logger.info("Bot Token: " + botToken);
        logger.info("Bot name: " + botName);
        this.toDoItemService = toDoItemService;
        this.userService = userService;
        this.sprintService = sprintService;
        this.botName = botName;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                String messageText = update.getMessage().getText();
                long chatId = update.getMessage().getChatId();

                // Get or initialize user state
                UserBotState state = userStates.getOrDefault(chatId, new UserBotState());
                userStates.put(chatId, state);

                // Handle authentication flow
                if (!state.isAuthenticated()) {
                    if (messageText.equals(BotCommands.START_COMMAND.getCommand())) {
                        // Send authentication prompt for /start command
                        SendMessage message = new SendMessage();
                        message.setChatId(chatId);
                        message.setText("Welcome to DashMaster! Please enter your Employee ID to authenticate:");
                        try {
                            execute(message);
                        } catch (TelegramApiException e) {
                            logger.error("Error sending authentication prompt", e);
                            sendErrorMessage(chatId, "Communication error. Please try again later.");
                        }
                        return;
                    } else {
                        // Process message as authentication attempt
                        handleAuthentication(chatId, messageText, state);
                        return;
                    }
                }

                // Task Creation Flow
                if (state.isNewTaskMode()) {
                    processTaskCreation(chatId, messageText, state);
                    return;
                }

                // Task Completion Flow
                if (state.isTaskCompletionMode()) {
                    processTaskCompletion(chatId, messageText, state);
                    return;
                }

                // Assign to Sprint Flow
                if (state.isAssignToSprintMode()) {
                    processAssignTaskToSprint(chatId, messageText, state);
                    return;
                }

                // Handle Developer Task Management Commands
                if (state.getUser().isDeveloper() || state.getUser().isManager()) {
                    if (messageText.equals("📝 Create New Task") || messageText.equals("📝 Create Another Task")) {
                        startTaskCreation(chatId, state);
                        return;
                    } else if (messageText.equals("🔄 My Active Tasks")) {
                        showActiveTasksForUser(chatId, state);
                        return;
                    } else if (messageText.equals("✅ Mark Task Complete")) {
                        startTaskCompletion(chatId, state);
                        return;
                    } else if (messageText.equals("📊 Sprint Board")) {
                        showSprintBoard(chatId, state);
                        return;
                    } else if (messageText.equals("🔄 Start Working on Task")) {
                        // Start the process to select a task to start working on
                        startTaskWorkProcess(chatId, state);
                        return;
                    } else if (messageText.equals("➕ Assign Task to Sprint")) {
                        startAssignTaskToSprint(chatId, state);
                        return;
                    }
                }

                // Continue with other commands for authenticated users
                if (messageText.equals(BotCommands.START_COMMAND.getCommand()) ||
                        messageText.equals(BotLabels.SHOW_MAIN_SCREEN.getLabel()) ||
                        messageText.equals("🏠 Main Menu")) {
                    try {
                        showMainScreen(chatId, state);
                    } catch (Exception e) {
                        logger.error("Error showing main screen", e);
                        sendErrorMessage(chatId, "Failed to display main menu. Please try again.");
                    }
                } else if (messageText.indexOf(BotLabels.DONE.getLabel()) != -1) {
                    try {
                        String done = messageText.substring(0,
                                messageText.indexOf(BotLabels.DASH.getLabel()));
                        Integer id = Integer.valueOf(done);

                        ToDoItem item = getToDoItemById(id).getBody();
                        if (item != null) {
                            item.setDone(true);
                            updateToDoItem(item, id);
                            BotHelper.sendMessageToTelegram(chatId, BotMessages.ITEM_DONE.getMessage(), this);
                        } else {
                            sendErrorMessage(chatId, "Could not find the task you're trying to mark as done.");
                        }
                    } catch (NumberFormatException e) {
                        logger.error("Invalid task ID format", e);
                        sendErrorMessage(chatId, "Invalid task ID format. Please try again.");
                    } catch (Exception e) {
                        logger.error("Error marking task as done", e);
                        sendErrorMessage(chatId, "Failed to mark task as done. Please try again later.");
                    }
                } else if (messageText.indexOf(BotLabels.UNDO.getLabel()) != -1) {
                    try {
                        String undo = messageText.substring(0,
                                messageText.indexOf(BotLabels.DASH.getLabel()));
                        Integer id = Integer.valueOf(undo);

                        ToDoItem item = getToDoItemById(id).getBody();
                        if (item != null) {
                            item.setDone(false);
                            updateToDoItem(item, id);
                            BotHelper.sendMessageToTelegram(chatId, BotMessages.ITEM_UNDONE.getMessage(), this);
                        } else {
                            sendErrorMessage(chatId, "Could not find the task you're trying to undo.");
                        }
                    } catch (NumberFormatException e) {
                        logger.error("Invalid task ID format", e);
                        sendErrorMessage(chatId, "Invalid task ID format. Please try again.");
                    } catch (Exception e) {
                        logger.error("Error undoing task", e);
                        sendErrorMessage(chatId, "Failed to undo task. Please try again later.");
                    }
                } else if (messageText.indexOf(BotLabels.DELETE.getLabel()) != -1) {
                    try {
                        String delete = messageText.substring(0,
                                messageText.indexOf(BotLabels.DASH.getLabel()));
                        Integer id = Integer.valueOf(delete);

                        boolean deleted = deleteToDoItem(id).getBody();
                        if (deleted) {
                            BotHelper.sendMessageToTelegram(chatId, BotMessages.ITEM_DELETED.getMessage(), this);
                        } else {
                            sendErrorMessage(chatId, "Could not delete the task. It may not exist.");
                        }
                    } catch (NumberFormatException e) {
                        logger.error("Invalid task ID format", e);
                        sendErrorMessage(chatId, "Invalid task ID format. Please try again.");
                    } catch (Exception e) {
                        logger.error("Error deleting task", e);
                        sendErrorMessage(chatId, "Failed to delete task. Please try again later.");
                    }
                } else if (messageText.equals(BotCommands.HIDE_COMMAND.getCommand())
                        || messageText.equals(BotLabels.HIDE_MAIN_SCREEN.getLabel())) {
                    try {
                        BotHelper.sendMessageToTelegram(chatId, BotMessages.BYE.getMessage(), this);
                    } catch (Exception e) {
                        logger.error("Error hiding main screen", e);
                        sendErrorMessage(chatId, "Failed to hide main screen. Please try again.");
                    }
                } else if (messageText.equals(BotCommands.TODO_LIST.getCommand())
                        || messageText.equals(BotLabels.LIST_ALL_ITEMS.getLabel())
                        || messageText.equals(BotLabels.MY_TODO_LIST.getLabel())) {
                    try {
                        List<ToDoItem> allItems = getAllToDoItems();
                        if (allItems == null || allItems.isEmpty()) {
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

                        for (ToDoItem item : activeItems) {
                            KeyboardRow currentRow = new KeyboardRow();
                            currentRow.add(item.getDescription());
                            currentRow.add(item.getID() + BotLabels.DASH.getLabel() + BotLabels.DONE.getLabel());
                            keyboard.add(currentRow);
                        }

                        List<ToDoItem> doneItems = allItems.stream().filter(item -> item.isDone() == true)
                                .collect(Collectors.toList());

                        for (ToDoItem item : doneItems) {
                            KeyboardRow currentRow = new KeyboardRow();
                            currentRow.add(item.getDescription());
                            currentRow.add(item.getID() + BotLabels.DASH.getLabel() + BotLabels.UNDO.getLabel());
                            currentRow.add(item.getID() + BotLabels.DASH.getLabel() + BotLabels.DELETE.getLabel());
                            keyboard.add(currentRow);
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

                        execute(messageToTelegram);
                    } catch (Exception e) {
                        logger.error("Error displaying todo list", e);
                        sendErrorMessage(chatId, "Failed to load your todo list. Please try again later.");
                    }
                } else if (messageText.equals(BotCommands.ADD_ITEM.getCommand())
                        || messageText.equals(BotLabels.ADD_NEW_ITEM.getLabel())) {
                    try {
                        SendMessage messageToTelegram = new SendMessage();
                        messageToTelegram.setChatId(chatId);
                        messageToTelegram.setText(BotMessages.TYPE_NEW_TODO_ITEM.getMessage());
                        // hide keyboard
                        ReplyKeyboardRemove keyboardMarkup = new ReplyKeyboardRemove(true);
                        messageToTelegram.setReplyMarkup(keyboardMarkup);

                        // Set state to new task mode
                        state.setNewTaskMode(true);
                        userStates.put(chatId, state);

                        // send message
                        execute(messageToTelegram);
                    } catch (Exception e) {
                        logger.error("Error initiating new task creation", e);
                        sendErrorMessage(chatId, "Failed to start task creation. Please try again later.");
                    }
                } else if (messageText.equals("/help")) {
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
                    } catch (Exception e) {
                        logger.error("Error sending help information", e);
                        sendErrorMessage(chatId, "Failed to send help information. Please try again later.");
                    }
                } else if (messageText.equals("/dbstatus") &&
                        (state.getUser().isManager() || state.getUser().isDeveloper())) {
                    try {
                        // Try a simple database operation for admins/developers
                        List<ToDoItem> items = toDoItemService.findAll();
                        sendMessage(chatId,
                                "✅ Database connection is working. Found " + items.size() + " items in the database.");
                    } catch (Exception e) {
                        logger.error("Database health check failed", e);
                        sendErrorMessage(chatId, "Database connection issue: " + e.getMessage());
                    }
                } else {
                    // Handle text as new todo item if in new task mode
                    if (state.isNewTaskMode()) {
                        try {
                            ToDoItem newItem = new ToDoItem();
                            newItem.setDescription(messageText);
                            newItem.setTitle(messageText.length() > 50 ? messageText.substring(0, 50) : messageText);
                            newItem.setCreation_ts(OffsetDateTime.now());
                            newItem.setDone(false);

                            // If user is associated with a team, set the team ID
                            if (state.getUser().getTeam() != null) {
                                newItem.setTeamId(state.getUser().getTeam().getId());
                            }

                            // Set the user as assignee
                            newItem.setAssigneeId(state.getUser().getId());

                            // Reset state
                            state.setNewTaskMode(false);
                            userStates.put(chatId, state);

                            SendMessage messageToTelegram = new SendMessage();
                            messageToTelegram.setChatId(chatId);
                            messageToTelegram.setText(BotMessages.NEW_ITEM_ADDED.getMessage());

                            execute(messageToTelegram);
                        } catch (Exception e) {
                            logger.error("Error adding new task", e);
                            sendErrorMessage(chatId, "Failed to add new task. Please try again later.");

                            // Reset state even on failure
                            state.setNewTaskMode(false);
                            userStates.put(chatId, state);
                        }
                    } else {
                        // Unrecognized command or text
                        sendMessage(chatId, "I didn't understand that command. Type /help to see available options.");
                    }
                }
            }
        } catch (Exception e) {
            // Global error handler
            logger.error("Unexpected error in bot operation", e);
            long chatId = update.getMessage().getChatId();
            sendErrorMessage(chatId, "An unexpected error occurred. Please try again later.");
        }
    }

    private void handleAuthentication(long chatId, String employeeId, UserBotState state) {
        try {
            // First, check if this Telegram user is already registered
            Optional<User> userByTelegramId = userService.findByTelegramId(chatId);

            if (userByTelegramId.isPresent()) {
                // User already registered with this Telegram ID
                User user = userByTelegramId.get();
                state.setAuthenticated(true);
                state.setUser(user);

                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Welcome back, " + user.getFullName() + "!");

                try {
                    execute(message);
                    showMainScreen(chatId, state);
                } catch (TelegramApiException e) {
                    logger.error("Error sending welcome back message", e);
                    sendErrorMessage(chatId,
                            "Authentication successful, but there was an error displaying the menu. Please type /start to continue.");
                }
            } else {
                // Try to find user by employee ID
                Optional<User> userOpt = userService.findByEmployeeId(employeeId);

                if (userOpt.isPresent() &&
                        (userOpt.get().isEmployee() || userOpt.get().isDeveloper() || userOpt.get().isManager())) {
                    // Associate this Telegram ID with the user
                    User user = userOpt.get();
                    user.setTelegramId(chatId);
                    userService.updateUser(user);

                    // Set authenticated state
                    state.setAuthenticated(true);
                    state.setUser(user);

                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText("Authentication successful! Welcome, " + user.getFullName()
                            + ". Your Telegram account is now linked to your DashMaster profile.");

                    try {
                        execute(message);
                        showMainScreen(chatId, state);
                    } catch (TelegramApiException e) {
                        logger.error("Error sending authentication success message", e);
                        sendErrorMessage(chatId,
                                "Authentication successful, but there was an error displaying the menu. Please type /start to continue.");
                    }
                } else {
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText("Authentication failed. Please enter a valid Employee ID:");

                    try {
                        execute(message);
                    } catch (TelegramApiException e) {
                        logger.error("Error sending authentication failure message", e);
                        sendErrorMessage(chatId, "Communication error. Please try again.");
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Unexpected error during authentication", e);
            sendErrorMessage(chatId, "Authentication system is currently unavailable. Please try again later.");
        }
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    // GET /todolist
    public List<ToDoItem> getAllToDoItems() {
        try {
            return toDoItemService.findAll();
        } catch (Exception e) {
            logger.error("Error fetching all todo items", e);
            return new ArrayList<>(); // Return empty list instead of null
        }
    }

    // GET BY ID /todolist/{id}
    public ResponseEntity<ToDoItem> getToDoItemById(@PathVariable int id) {
        try {
            ResponseEntity<ToDoItem> responseEntity = toDoItemService.getItemById(id);
            return new ResponseEntity<ToDoItem>(responseEntity.getBody(), HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error fetching todo item by ID: " + id, e);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // PUT /todolist
    public ResponseEntity<Void> addToDoItem(@RequestBody ToDoItem todoItem) throws Exception {
        try {
            ToDoItem td = toDoItemService.addToDoItem(todoItem);
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.set("location", "" + td.getID());
            responseHeaders.set("Access-Control-Expose-Headers", "location");

            return ResponseEntity.ok().headers(responseHeaders).build();
        } catch (Exception e) {
            logger.error("Error adding todo item", e);
            throw e; // Re-throw to allow proper error handling in the caller
        }
    }

    // UPDATE /todolist/{id}
    public ResponseEntity<ToDoItem> updateToDoItem(@RequestBody ToDoItem toDoItem, @PathVariable int id) {
        try {
            ToDoItem toDoItem1 = toDoItemService.updateToDoItem(id, toDoItem);
            return new ResponseEntity<>(toDoItem1, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error updating todo item with ID: " + id, e);
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }

    // DELETE todolist/{id}
    public ResponseEntity<Boolean> deleteToDoItem(@PathVariable("id") int id) {
        Boolean flag = false;
        try {
            flag = toDoItemService.deleteToDoItem(id);
            return new ResponseEntity<>(flag, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error deleting todo item with ID: " + id, e);
            return new ResponseEntity<>(flag, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Show developer's task menu
     */
    private void showDeveloperTaskMenu(long chatId, UserBotState state) {
        try {
            SendMessage messageToTelegram = new SendMessage();
            messageToTelegram.setChatId(chatId);

            String welcomeMessage = "Task Management for " + state.getUser().getFullName() + "\n" +
                    "Select an option:";
            messageToTelegram.setText(welcomeMessage);

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

            execute(messageToTelegram);
        } catch (TelegramApiException e) {
            logger.error("Error showing developer task menu", e);
            sendErrorMessage(chatId, "There was a problem displaying the task menu. Please try again.");
        }
    }

    /**
     * Start the task creation flow
     */
    private void startTaskCreation(long chatId, UserBotState state) {
        try {
            // Set state to task creation mode
            state.setNewTaskMode(true);
            state.setTaskCreationStage("TITLE");
            userStates.put(chatId, state);

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Let's create a new task. First, please enter the task title:");

            // Hide keyboard for text input
            ReplyKeyboardRemove keyboardRemove = new ReplyKeyboardRemove(true);
            message.setReplyMarkup(keyboardRemove);

            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error starting task creation", e);
            sendErrorMessage(chatId, "There was a problem starting task creation. Please try again.");
        }
    }

    /**
     * Process task creation stages
     */
    private void processTaskCreation(long chatId, String messageText, UserBotState state) {
        try {
            String stage = state.getTaskCreationStage();

            if ("TITLE".equals(stage)) {
                // Store title and ask for description
                state.setTempTaskTitle(messageText);
                state.setTaskCreationStage("DESCRIPTION");

                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Great! Now please provide a description for the task:");
                execute(message);
            } else if ("DESCRIPTION".equals(stage)) {
                // Store description and ask for estimated hours
                state.setTempTaskDescription(messageText);
                state.setTaskCreationStage("ESTIMATED_HOURS");

                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Please enter the estimated hours to complete this task (must be 4 hours or less):");
                execute(message);
            } else if ("ESTIMATED_HOURS".equals(stage)) {
                // Validate and store estimated hours
                try {
                    double estimatedHours = Double.parseDouble(messageText);

                    if (estimatedHours <= 0) {
                        SendMessage message = new SendMessage();
                        message.setChatId(chatId);
                        message.setText("Estimated hours must be greater than 0. Please enter a valid number:");
                        execute(message);
                        return;
                    }

                    if (estimatedHours > 4.0) {
                        SendMessage message = new SendMessage();
                        message.setChatId(chatId);
                        message.setText(
                                "Tasks cannot exceed 4 hours of estimated work. Please break it down into smaller subtasks. Enter a value of 4.0 or less:");
                        execute(message);
                        return;
                    }

                    // Store estimated hours and ask for priority
                    state.setTempEstimatedHours(estimatedHours);
                    state.setTaskCreationStage("PRIORITY");

                    // Create keyboard for priority selection
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
                } catch (NumberFormatException e) {
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText("Please enter a valid number for estimated hours:");
                    execute(message);
                }
            } else if ("PRIORITY".equals(stage)) {
                // Validate priority
                if (!messageText.equals("High") && !messageText.equals("Medium") && !messageText.equals("Low")) {
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText("Please select a valid priority (High, Medium, or Low):");
                    execute(message);
                    return;
                }

                // Store priority and ask for confirmation
                state.setTempPriority(messageText);
                state.setTaskCreationStage("CONFIRMATION");

                StringBuilder summary = new StringBuilder();
                summary.append("Please confirm the task details:\n\n");
                summary.append("Title: ").append(state.getTempTaskTitle()).append("\n");
                summary.append("Description: ").append(state.getTempTaskDescription()).append("\n");
                summary.append("Estimated Hours: ").append(state.getTempEstimatedHours()).append("\n");
                summary.append("Priority: ").append(state.getTempPriority()).append("\n\n");
                summary.append("Is this correct?");

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
            } else if ("CONFIRMATION".equals(stage)) {
                if (messageText.equals("Yes, create task")) {
                    // Create the task
                    ToDoItem task = new ToDoItem();
                    task.setTitle(state.getTempTaskTitle());
                    task.setDescription(state.getTempTaskDescription());
                    task.setEstimatedHours(state.getTempEstimatedHours());
                    task.setPriority(state.getTempPriority());
                    task.setCreation_ts(OffsetDateTime.now());
                    task.setAssigneeId(state.getUser().getId());

                    // If user is in a team, set the team ID
                    if (state.getUser().getTeam() != null) {
                        task.setTeamId(state.getUser().getTeam().getId());
                    }

                    task.setStatus(TaskStatus.BACKLOG.name());
                    task.setDone(false);

                    // Save the task
                    ToDoItem savedTask = toDoItemService.addTaskWithEstimation(task);

                    // Reset state
                    state.resetTaskCreation();

                    // Show success message
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
                } else {
                    // Cancel task creation
                    state.resetTaskCreation();

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
                }
            }

            // Update user state
            userStates.put(chatId, state);
        } catch (Exception e) {
            logger.error("Error in task creation process", e);
            sendErrorMessage(chatId, "There was an error in the task creation process. Please try again.");

            // Reset task creation state
            state.resetTaskCreation();
            userStates.put(chatId, state);
        }
    }

    /**
     * Show user's active tasks
     */
    private void showActiveTasksForUser(long chatId, UserBotState state) {
        try {
            List<ToDoItem> activeTasks = toDoItemService.findActiveTasksByAssigneeId(state.getUser().getId());

            if (activeTasks.isEmpty()) {
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

                execute(message);
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

            execute(message);
        } catch (Exception e) {
            logger.error("Error showing active tasks", e);
            sendErrorMessage(chatId, "There was an error retrieving your active tasks. Please try again later.");
        }
    }

    /**
     * Start the task completion flow
     */
    private void startTaskCompletion(long chatId, UserBotState state) {
        try {
            List<ToDoItem> activeTasks = toDoItemService.findActiveTasksByAssigneeId(state.getUser().getId());

            if (activeTasks.isEmpty()) {
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

                execute(message);
                return;
            }

            state.setTaskCompletionStage("SELECT_TASK");
            userStates.put(chatId, state);

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
            }

            KeyboardRow row = new KeyboardRow();
            row.add("Cancel");
            keyboard.add(row);

            keyboardMarkup.setKeyboard(keyboard);
            message.setReplyMarkup(keyboardMarkup);

            execute(message);
        } catch (Exception e) {
            logger.error("Error starting task completion", e);
            sendErrorMessage(chatId,
                    "There was an error starting the task completion process. Please try again later.");
        }
    }

    /**
     * Process task completion stages
     */
    private void processTaskCompletion(long chatId, String messageText, UserBotState state) {
        try {
            String stage = state.getTaskCompletionStage();

            if ("SELECT_TASK".equals(stage)) {
                if (messageText.equals("Cancel")) {
                    state.resetTaskCompletion();
                    showDeveloperTaskMenu(chatId, state);
                    return;
                }

                // Extract task ID from message
                int taskId;
                if (messageText.startsWith("ID: ")) {
                    // Parse from "ID: X - Title"
                    String idPart = messageText.substring(4, messageText.indexOf(" - "));
                    taskId = Integer.parseInt(idPart);
                } else {
                    // Try to parse as a direct ID
                    try {
                        taskId = Integer.parseInt(messageText);
                    } catch (NumberFormatException e) {
                        SendMessage message = new SendMessage();
                        message.setChatId(chatId);
                        message.setText("Please enter a valid task ID or select from the list.");
                        execute(message);
                        return;
                    }
                }

                // Check if task exists and belongs to user
                ResponseEntity<ToDoItem> response = getToDoItemById(taskId);
                if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText("Task not found. Please enter a valid task ID.");
                    execute(message);
                    return;
                }

                ToDoItem task = response.getBody();
                if (!task.getAssigneeId().equals(state.getUser().getId())) {
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText("This task is not assigned to you. Please enter a valid task ID.");
                    execute(message);
                    return;
                }

                // Store task ID and move to actual hours stage
                state.setTempTaskId(taskId);
                state.setTaskCompletionStage("ACTUAL_HOURS");

                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Please enter the actual hours spent on this task:");

                ReplyKeyboardRemove keyboardRemove = new ReplyKeyboardRemove(true);
                message.setReplyMarkup(keyboardRemove);

                execute(message);
            } else if ("ACTUAL_HOURS".equals(stage)) {
                // Validate and store actual hours
                try {
                    double actualHours = Double.parseDouble(messageText);

                    if (actualHours <= 0) {
                        SendMessage message = new SendMessage();
                        message.setChatId(chatId);
                        message.setText("Actual hours must be greater than 0. Please enter a valid number:");
                        execute(message);
                        return;
                    }

                    // Store actual hours and ask for comments
                    state.setTempActualHours(actualHours);
                    state.setTaskCompletionStage("COMMENTS");

                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText(
                            "Please enter any comments or notes about the completed task (or type 'skip' to skip):");
                    execute(message);
                } catch (NumberFormatException e) {
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText("Please enter a valid number for actual hours:");
                    execute(message);
                }
            } else if ("COMMENTS".equals(stage)) {
                // Store comments and complete task
                String comments = messageText.equals("skip") ? "" : messageText;

                // Complete the task
                ToDoItem task = toDoItemService.completeTask(state.getTempTaskId(), state.getTempActualHours(),
                        comments);

                // Reset state
                state.resetTaskCompletion();

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

                execute(message);
            }

            // Update user state
            userStates.put(chatId, state);
        } catch (Exception e) {
            logger.error("Error in task completion process", e);
            sendErrorMessage(chatId, "There was an error in the task completion process. Please try again.");

            // Reset task completion state
            state.resetTaskCompletion();
            userStates.put(chatId, state);
        }
    }

    /**
     * Show active sprint board
     */
    private void showSprintBoard(long chatId, UserBotState state) {
        try {
            // Try to find the active sprint for the user's team
            Long teamId = state.getUser().getTeam() != null ? state.getUser().getTeam().getId() : null;

            if (teamId == null) {
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

                execute(message);
                return;
            }

            // Get the active sprint
            Optional<Sprint> activeSprint = sprintService.findActiveSprintByTeamId(teamId);

            if (!activeSprint.isPresent()) {
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

                execute(message);
                return;
            }

            // Get tasks in the sprint
            List<ToDoItem> sprintTasks = toDoItemService.findTasksBySprintId(activeSprint.get().getId());

            if (sprintTasks.isEmpty()) {
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

                execute(message);
                return;
            }

            // Group tasks by status
            Map<String, List<ToDoItem>> tasksByStatus = new HashMap<>();

            for (ToDoItem task : sprintTasks) {
                String status = task.getStatus() != null ? task.getStatus() : "BACKLOG";

                if (!tasksByStatus.containsKey(status)) {
                    tasksByStatus.put(status, new ArrayList<>());
                }

                tasksByStatus.get(status).add(task);
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

            // Add tasks by status
            for (String status : statusOrder) {
                if (tasksByStatus.containsKey(status)) {
                    String displayStatus;

                    try {
                        displayStatus = TaskStatus.valueOf(status).getDisplayName();
                    } catch (IllegalArgumentException e) {
                        displayStatus = status;
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
                    }

                    boardText.append("\n");
                }
            }

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

            execute(message);
        } catch (Exception e) {
            logger.error("Error showing sprint board", e);
            sendErrorMessage(chatId, "There was an error retrieving the sprint board. Please try again later.");
        }
    }

    /**
     * Start the process of assigning a task to a sprint
     */
    private void startAssignTaskToSprint(long chatId, UserBotState state) {
        try {
            // Check if user is a developer or manager
            if (!state.getUser().isDeveloper() && !state.getUser().isManager()) {
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

                execute(message);
                return;
            }

            // Get user's active tasks not yet in a sprint
            List<ToDoItem> backlogTasks = toDoItemService.findActiveTasksByAssigneeId(state.getUser().getId()).stream()
                    .filter(task -> task.getSprintId() == null)
                    .collect(Collectors.toList());

            if (backlogTasks.isEmpty()) {
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

                execute(message);
                return;
            }

            // Try to find the active sprint for the user's team
            Long teamId = state.getUser().getTeam() != null ? state.getUser().getTeam().getId() : null;

            if (teamId == null) {
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

                execute(message);
                return;
            }

            // Get the active sprint
            Optional<Sprint> activeSprint = sprintService.findActiveSprintByTeamId(teamId);

            if (!activeSprint.isPresent()) {
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

                execute(message);
                return;
            }

            // Set state for assigning task to sprint
            state.setAssignToSprintStage("SELECT_TASK");
            state.setTempSprintId(activeSprint.get().getId());
            userStates.put(chatId, state);

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
            }

            KeyboardRow row = new KeyboardRow();
            row.add("Cancel");
            keyboard.add(row);

            keyboardMarkup.setKeyboard(keyboard);
            message.setReplyMarkup(keyboardMarkup);

            execute(message);
        } catch (Exception e) {
            logger.error("Error starting assign task to sprint", e);
            sendErrorMessage(chatId, "There was an error in the process. Please try again later.");
        }
    }

    /**
     * Process task assignment to sprint
     */
    private void processAssignTaskToSprint(long chatId, String messageText, UserBotState state) {
        try {
            if (messageText.equals("Cancel")) {
                state.resetAssignToSprint();
                showDeveloperTaskMenu(chatId, state);
                return;
            }

            // Extract task ID from message
            int taskId;
            if (messageText.startsWith("ID: ")) {
                // Parse from "ID: X - Title"
                String idPart = messageText.substring(4, messageText.indexOf(" - "));
                taskId = Integer.parseInt(idPart);
            } else {
                // Try to parse as a direct ID
                try {
                    taskId = Integer.parseInt(messageText);
                } catch (NumberFormatException e) {
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText("Please enter a valid task ID or select from the list.");
                    execute(message);
                    return;
                }
            }

            // Check if task exists and belongs to user
            ResponseEntity<ToDoItem> response = getToDoItemById(taskId);
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Task not found. Please enter a valid task ID.");
                execute(message);
                return;
            }

            // Assign task to sprint
            ToDoItem task = toDoItemService.assignTaskToSprint(taskId, state.getTempSprintId());

            // Reset state
            state.resetAssignToSprint();

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

            execute(message);
        } catch (Exception e) {
            logger.error("Error assigning task to sprint", e);
            sendErrorMessage(chatId, "There was an error assigning the task to the sprint. Please try again later.");

            // Reset state
            state.resetAssignToSprint();
            userStates.put(chatId, state);
        }
    }

    /**
     * Start the process for a developer to begin working on a task
     */
    private void startTaskWorkProcess(long chatId, UserBotState state) {
        try {
            List<ToDoItem> tasksInSprint = new ArrayList<>();

            // If user is in a team, find the active sprint
            if (state.getUser().getTeam() != null) {
                Optional<Sprint> activeSprint = sprintService
                        .findActiveSprintByTeamId(state.getUser().getTeam().getId());

                if (activeSprint.isPresent()) {
                    // Get tasks in the sprint that are not in progress or completed
                    tasksInSprint = toDoItemService.findTasksBySprintId(activeSprint.get().getId()).stream()
                            .filter(task -> !TaskStatus.IN_PROGRESS.name().equals(task.getStatus()) &&
                                    !TaskStatus.COMPLETED.name().equals(task.getStatus()))
                            .collect(Collectors.toList());
                }
            }

            if (tasksInSprint.isEmpty()) {
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

                execute(message);
                return;
            }

            state.setStartTaskWorkStage("SELECT_TASK");
            userStates.put(chatId, state);

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
            }

            KeyboardRow row = new KeyboardRow();
            row.add("Cancel");
            keyboard.add(row);

            keyboardMarkup.setKeyboard(keyboard);
            message.setReplyMarkup(keyboardMarkup);

            execute(message);
        } catch (Exception e) {
            logger.error("Error starting task work process", e);
            sendErrorMessage(chatId, "There was an error starting the task work process. Please try again later.");
        }
    }

}