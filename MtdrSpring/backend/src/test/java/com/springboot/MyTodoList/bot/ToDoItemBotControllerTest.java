package com.springboot.MyTodoList.bot;

import com.springboot.MyTodoList.bot.controller.ToDoItemBotController;
import com.springboot.MyTodoList.bot.handler.TaskCompletionHandler;
import com.springboot.MyTodoList.bot.handler.TaskCreationHandler;
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
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    
    private ToDoItemBotController botController;
    
    private UserBotState userState;
    private User testUser;
    private ToDoItem testTask;
    private Sprint testSprint;
    
    @BeforeEach
    void setUp() {
        // Mock ToDoItemBotController with constructor parameters
        String botToken = "test_token";
        String botName = "test_bot";
        
        // Create a testable version of the bot controller with mocked execute method
        botController = Mockito.spy(new ToDoItemBotController(botToken, botName, toDoItemService, userService, sprintService) {
            @Override
            public Message execute(SendMessage sendMessage) throws TelegramApiException {
                // Mock successful execution
                Message mockMessage = mock(Message.class);
                when(mockMessage.getMessageId()).thenReturn(1);
                return mockMessage;
            }
        });
        
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
        
        // Setup mock behavior
        when(userService.findByTelegramId(CHAT_ID)).thenReturn(Optional.of(testUser));
        when(botService.findUserByTelegramId(CHAT_ID)).thenReturn(Optional.of(testUser));
    }
    
    @Test
    void testCreateTask() throws TelegramApiException {
        // Setup test data
        String taskTitle = "New Task";
        String taskDescription = "Task Description";
        Double estimatedHours = 2.0;
        
        // Create update for task title
        Update titleUpdate = createTextMessageUpdate(CHAT_ID, taskTitle);
        
        // Set up state for task creation
        userState.setNewTaskMode(true);
        userState.setTaskCreationStage("TITLE");
        
        // Mock BotService behavior
        when(botService.findUserByTelegramId(CHAT_ID)).thenReturn(Optional.of(testUser));
        
        // Process task title
        botController.onUpdateReceived(titleUpdate);
        
        // Verify state was updated after title
        verify(botService, never()).addToDoItem(any(ToDoItem.class)); // Not saving yet
        
        // Create update for task description
        Update descUpdate = createTextMessageUpdate(CHAT_ID, taskDescription);
        
        // Update state to description stage
        userState.setTaskCreationStage("DESCRIPTION");
        userState.setTempTaskTitle(taskTitle);
        
        // Process task description
        botController.onUpdateReceived(descUpdate);
        
        // Create update for estimated hours
        Update hoursUpdate = createTextMessageUpdate(CHAT_ID, estimatedHours.toString());
        
        // Update state to hours stage
        userState.setTaskCreationStage("ESTIMATED_HOURS");
        userState.setTempTaskTitle(taskTitle);
        userState.setTempTaskDescription(taskDescription);
        
        // Process hours
        botController.onUpdateReceived(hoursUpdate);
        
        // Create update for confirmation
        Update confirmUpdate = createTextMessageUpdate(CHAT_ID, "Yes, create task");
        
        // Update state to confirmation stage
        userState.setTaskCreationStage("CONFIRM");
        userState.setTempTaskTitle(taskTitle);
        userState.setTempTaskDescription(taskDescription);
        userState.setTempEstimatedHours(estimatedHours);
        userState.setTempAssigneeId(USER_ID); // Assign to self
        userState.setTempPriority("Medium");
        
        // Mock task creation
        when(botService.addToDoItem(any(ToDoItem.class))).thenReturn(testTask);
        
        // Process confirmation
        botController.onUpdateReceived(confirmUpdate);
        
        // Verify task was created
        verify(botService, times(1)).addToDoItem(any(ToDoItem.class));
    }
    
    @Test
    void testViewSprintTasks() throws TelegramApiException {
        // Setup test data
        List<ToDoItem> sprintTasks = new ArrayList<>();
        sprintTasks.add(testTask);
        
        // Mock service behaviors
        when(botService.findActiveSprintByTeamId(any())).thenReturn(Optional.of(testSprint));
        when(botService.findTasksBySprintId(SPRINT_ID)).thenReturn(sprintTasks);
        
        // Create a callback query update for viewing sprint tasks
        Update callbackUpdate = createCallbackQueryUpdate(CHAT_ID, "sprint_view_tasks");
        
        // Process the callback
        botController.onUpdateReceived(callbackUpdate);
        
        // Verify sprint tasks were retrieved
        verify(botService, times(1)).findTasksBySprintId(SPRINT_ID);
    }
    
    @Test
    void testViewUserSprintTasks() throws TelegramApiException {
        // Setup test data
        List<ToDoItem> userSprintTasks = new ArrayList<>();
        userSprintTasks.add(testTask);
        
        // Mock service behaviors
        when(botService.findActiveSprintByTeamId(any())).thenReturn(Optional.of(testSprint));
        when(botService.findBySprintIdAndAssigneeId(SPRINT_ID, USER_ID)).thenReturn(userSprintTasks);
        
        // Create callback for viewing user's sprint tasks
        Update callbackUpdate = createCallbackQueryUpdate(CHAT_ID, "sprint_view_my_tasks");
        
        // Process the callback
        botController.onUpdateReceived(callbackUpdate);
        
        // Verify user sprint tasks were retrieved
        verify(botService, times(1)).findBySprintIdAndAssigneeId(SPRINT_ID, USER_ID);
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
        callbackQuery.setMessage(message);
        
        // Set sender
        User sender = new User();
        sender.setId(CHAT_ID);
        callbackQuery.setFrom(sender);
        
        update.setCallbackQuery(callbackQuery);
        return update;
    }
}