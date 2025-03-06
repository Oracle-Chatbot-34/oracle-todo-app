package com.springboot.MyTodoList.controller;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

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

import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.service.ToDoItemService;
import com.springboot.MyTodoList.service.UserService;
import com.springboot.MyTodoList.util.BotCommands;
import com.springboot.MyTodoList.model.bot.UserBotState;
import com.springboot.MyTodoList.util.BotHelper;
import com.springboot.MyTodoList.util.BotLabels;
import com.springboot.MyTodoList.util.BotMessages;

public class ToDoItemBotController extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(ToDoItemBotController.class);
    private ToDoItemService toDoItemService;
    private UserService userService;
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
            sendErrorMessage(chatId, "There was a problem displaying the main menu. Please try again by typing /start.");
        }
    }

    public ToDoItemBotController(String botToken, String botName, ToDoItemService toDoItemService,
            UserService userService) {
        super(botToken);
        logger.info("Bot Token: " + botToken);
        logger.info("Bot name: " + botName);
        this.toDoItemService = toDoItemService;
        this.userService = userService;
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

                // Continue with other commands for authenticated users
                if (messageText.equals(BotCommands.START_COMMAND.getCommand())
                        || messageText.equals(BotLabels.SHOW_MAIN_SCREEN.getLabel())) {
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
                            sendMessage(chatId, "Your todo list is empty. Add new items using the 'Add New Item' button.");
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
                        sendMessage(chatId, "✅ Database connection is working. Found " + items.size() + " items in the database.");
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
            // Find user by employee ID
            Optional<User> userOpt = userService.findByEmployeeId(employeeId);

            if (userOpt.isPresent() &&
                    (userOpt.get().isEmployee() || userOpt.get().isDeveloper() || userOpt.get().isManager())) {
                // Set authenticated state
                state.setAuthenticated(true);
                state.setUser(userOpt.get());

                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Authentication successful! Welcome, " + state.getUser().getFullName() + ".");

                try {
                    execute(message);
                    showMainScreen(chatId, state);
                } catch (TelegramApiException e) {
                    logger.error("Error sending authentication success message", e);
                    sendErrorMessage(chatId, "Authentication successful, but there was an error displaying the menu. Please type /start to continue.");
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
        } catch (org.springframework.dao.InvalidDataAccessResourceUsageException e) {
            logger.error("Database table missing during authentication", e);
            sendErrorMessage(chatId, "System error: Database tables not properly configured. Please contact the system administrator.");
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
}