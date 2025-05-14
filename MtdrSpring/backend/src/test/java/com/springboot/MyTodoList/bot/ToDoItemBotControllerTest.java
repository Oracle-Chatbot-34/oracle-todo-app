package com.springboot.MyTodoList.bot;

import com.springboot.MyTodoList.bot.controller.ToDoItemBotController;
import com.springboot.MyTodoList.bot.handler.TaskCompletionHandler;
import com.springboot.MyTodoList.bot.handler.TaskCreationHandler;
import com.springboot.MyTodoList.bot.handler.SprintHandler;
import com.springboot.MyTodoList.bot.service.BotService;
import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.model.bot.UserBotState;
import com.springboot.MyTodoList.service.SprintService;
import com.springboot.MyTodoList.service.ToDoItemService;
import com.springboot.MyTodoList.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ToDoItemBotControllerTest {

    private static final long CHAT_ID = 123456789L;
    private static final int TASK_ID = 42;
    private static final long USER_ID = 1L;
    private static final long SPRINT_ID = 1L;

    @Mock
    private ToDoItemService toDoItemService;

    @Mock
    private UserService userService;

    @Mock
    private SprintService sprintService;

    @Mock
    private BotService botService;

    @Mock
    private TaskCreationHandler taskCreationHandler;

    @Mock
    private TaskCompletionHandler taskCompletionHandler;

    @Mock
    private SprintHandler sprintHandler;

    private ToDoItemBotController botController;

    private UserBotState userState;
    private User testUser;
    private ToDoItem testTask;
    private Sprint testSprint;

    @BeforeEach
    void setUp() throws Exception {
        // Mock ToDoItemBotController with constructor parameters
        String botToken = "test_token";
        String botName = "test_bot";

        // Create the real controller but with mock services
        botController = Mockito
                .spy(new ToDoItemBotController(botToken, botName, toDoItemService, userService, sprintService));

        // Use lenient mocking for all stubs that might not be used in every test
        lenient().doReturn(mock(Message.class)).when(botController).execute(any(SendMessage.class));

        // Create test user
        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setUsername("testuser");
        testUser.setFullName("Test User");
        testUser.setRole("DEVELOPER");
        testUser.setTelegramId(CHAT_ID);

        // Setup user state for testing
        userState = new UserBotState();
        userState.setAuthenticated(true);
        userState.setUser(testUser);

        // Create test task
        testTask = new ToDoItem();
        testTask.setID(TASK_ID);
        testTask.setTitle("Test Task");
        testTask.setDescription("Test Description");
        testTask.setStatus("BACKLOG");
        testTask.setAssigneeId(USER_ID);

        // Create test sprint
        testSprint = new Sprint();
        testSprint.setId(SPRINT_ID);
        testSprint.setName("Test Sprint");
        testSprint.setStatus("ACTIVE");

        // Initialize the userStates map with our test state
        ConcurrentHashMap<Long, UserBotState> userStates = new ConcurrentHashMap<>();
        userStates.put(CHAT_ID, userState);

        // Use reflection to set private userStates field in the controller
        Field userStatesField = ToDoItemBotController.class.getDeclaredField("userStates");
        userStatesField.setAccessible(true);
        userStatesField.set(botController, userStates);

        // Set private handlers using reflection
        Field botServiceField = ToDoItemBotController.class.getDeclaredField("botService");
        botServiceField.setAccessible(true);
        botServiceField.set(botController, botService);

        Field taskCreationHandlerField = ToDoItemBotController.class.getDeclaredField("taskCreationHandler");
        taskCreationHandlerField.setAccessible(true);
        taskCreationHandlerField.set(botController, taskCreationHandler);

        Field taskCompletionHandlerField = ToDoItemBotController.class.getDeclaredField("taskCompletionHandler");
        taskCompletionHandlerField.setAccessible(true);
        taskCompletionHandlerField.set(botController, taskCompletionHandler);

        Field sprintHandlerField = ToDoItemBotController.class.getDeclaredField("sprintHandler");
        sprintHandlerField.setAccessible(true);
        sprintHandlerField.set(botController, sprintHandler);

        // Setup mock behavior with lenient to avoid unnecessary stubbing exceptions
        lenient().when(userService.findByTelegramId(CHAT_ID)).thenReturn(Optional.of(testUser));
        lenient().when(botService.findUserByTelegramId(CHAT_ID)).thenReturn(Optional.of(testUser));
    }

    @Test
    void testCreateTask() throws TelegramApiException {
        // Setup test data
        String taskTitle = "New Task";

        // Create update for task creation command
        Update commandUpdate = createTextMessageUpdate(CHAT_ID, "/additem");

        // Process the command to start task creation
        botController.onUpdateReceived(commandUpdate);

        // Verify taskCreationHandler was called
        verify(taskCreationHandler, times(1)).startTaskCreation(eq(CHAT_ID), any(UserBotState.class));

        // Create update for task title
        Update titleUpdate = createTextMessageUpdate(CHAT_ID, taskTitle);

        // Set up state for task creation
        userState.setNewTaskMode(true);
        userState.setTaskCreationStage("TITLE");

        // Process task title
        botController.onUpdateReceived(titleUpdate);

        // Verify taskCreationHandler was called to process the task title
        verify(taskCreationHandler, times(1)).processTaskCreation(eq(CHAT_ID), eq(taskTitle), any(UserBotState.class));
    }

    @Test
    void testViewSprintTasks() throws TelegramApiException {
        // Setup test data
        List<ToDoItem> sprintTasks = new ArrayList<>();
        sprintTasks.add(testTask);

        // Create a callback query update for viewing sprint tasks
        Update callbackUpdate = createCallbackQueryUpdate(CHAT_ID, "sprint_view_tasks");

        // Set sprint mode
        userState.setSprintMode(true);

        // Process the callback
        botController.onUpdateReceived(callbackUpdate);

        // Verify sprint handler was called with the correct parameters
        verify(sprintHandler, times(1)).processSprintModeCallback(
                eq(CHAT_ID),
                eq("sprint_view_tasks"),
                any(UserBotState.class),
                anyInt());
    }

    @Test
    void testViewUserSprintTasks() throws TelegramApiException {
        // Setup test data
        List<ToDoItem> userSprintTasks = new ArrayList<>();
        userSprintTasks.add(testTask);

        // Create callback for viewing user's sprint tasks
        Update callbackUpdate = createCallbackQueryUpdate(CHAT_ID, "sprint_view_my_tasks");

        // Set sprint mode
        userState.setSprintMode(true);

        // Process the callback
        botController.onUpdateReceived(callbackUpdate);

        // Verify sprint handler was called with the correct parameters
        verify(sprintHandler, times(1)).processSprintModeCallback(
                eq(CHAT_ID),
                eq("sprint_view_my_tasks"),
                any(UserBotState.class),
                anyInt());
    }

    // Helper method to create a text message update
    private Update createTextMessageUpdate(long chatId, String text) {
        Update update = new Update();
        Message message = new Message();
        Chat chat = new Chat();
        chat.setId(chatId);
        message.setChat(chat);
        message.setText(text);
        update.setMessage(message);
        return update;
    }

    // Helper method to create a callback query update
    private Update createCallbackQueryUpdate(long chatId, String callbackData) {
        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setData(callbackData);
        Message message = new Message();
        Chat chat = new Chat();
        chat.setId(chatId);
        message.setChat(chat);
        message.setMessageId(1); // Add a message ID for the callback
        callbackQuery.setMessage(message);

        // Set sender
        org.telegram.telegrambots.meta.api.objects.User sender = new org.telegram.telegrambots.meta.api.objects.User();
        sender.setId(CHAT_ID);
        callbackQuery.setFrom(sender);

        update.setCallbackQuery(callbackQuery);
        return update;
    }
}