package com.springboot.MyTodoList.bot;

import com.springboot.MyTodoList.bot.config.TelegramBotConfig;
// import com.springboot.MyTodoList.bot.controller.ToDoItemBotController;
import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.service.SprintService;
import com.springboot.MyTodoList.service.ToDoItemService;
import com.springboot.MyTodoList.service.UserService;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TelegramBotConfigTest {

  private TelegramBotConfig telegramBotConfig;
  // private ToDoItemBotController botController;

  @Mock
  private ToDoItemService toDoItemService;

  @Mock
  private UserService userService;

  @Mock
  private SprintService sprintService;

  @Mock
  private TelegramBotsApi telegramBotsApi;

  @Mock
  private Update update;

  @Mock
  private Message message;

  private final String validToken = "valid:test:token";
  private final String botName = "TestBot";


  @BeforeEach
  public void setUp() {
    // Setup the configuration class
    telegramBotConfig = new TelegramBotConfig();
    ReflectionTestUtils.setField(telegramBotConfig, "telegramBotToken", validToken);
    ReflectionTestUtils.setField(telegramBotConfig, "botName", botName);

    // // Create a test bot controller for testing specific functionality
    // botController = spy(new ToDoItemBotController(validToken, botName,
    //     toDoItemService, userService, sprintService));
  }

  @Test
  public void testCreateTask() {
    // Setup test data
    Long userId = 1L;
    int taskId = 100;

    User testUser = new User();
    testUser.setId(userId);
    testUser.setFullName("Test User");

    ToDoItem newTask = new ToDoItem();
    newTask.setId(taskId);
    newTask.setTitle("Test Task");
    newTask.setAssigneeId(userId);

    // Mock service method
    when(toDoItemService.addToDoItem(any(ToDoItem.class))).thenReturn(newTask);

    // Test creating a task
    ToDoItem result = toDoItemService.addToDoItem(newTask);

    // Verify results
    assertNotNull(result);
    assertEquals("Test Task", result.getTitle());
    assertEquals(userId, result.getAssigneeId());
    verify(toDoItemService).addToDoItem(any(ToDoItem.class));
  }

  @Test
  public void testGetTasksInSprint() {
    // Setup test data
    Long sprintId = 1L;

    Sprint testSprint = new Sprint();
    testSprint.setId(sprintId);
    testSprint.setName("Test Sprint");

    List<ToDoItem> sprintTasks = new ArrayList<>();
    sprintTasks.add(createTestTask(1, "Task 1", sprintId, 1L));
    sprintTasks.add(createTestTask(2, "Task 2", sprintId, 2L));

    // Mock service method
    when(toDoItemService.findTasksBySprintId(1L)).thenReturn(sprintTasks);

    // Test getting tasks in a sprint
    List<ToDoItem> result = toDoItemService.findTasksBySprintId(sprintId);

    // Verify results
    assertNotNull(result);
    assertEquals(2, result.size());
    verify(toDoItemService).findTasksBySprintId(sprintId);
  }

  @Test
  public void testGetUserTasks() {
    Long userId = 1L;
    Long sprintId = 1L;

    // Setup test data
    User testUser = new User();
    testUser.setId(userId);
    testUser.setFullName("Test User");

    List<ToDoItem> userTasks = new ArrayList<>();
    userTasks.add(createTestTask(1, "User Task 1", sprintId, userId));
    userTasks.add(createTestTask(2, "User Task 2", sprintId, userId));

    // Mock service methods
    when(toDoItemService.findBySprintIdAndAssigneeId(sprintId, userId)).thenReturn(userTasks);

    // Test getting user's tasks
    List<ToDoItem> result = toDoItemService.findBySprintIdAndAssigneeId(sprintId, userId);

    // Verify results
    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals(1, result.get(0).getAssigneeId());
    assertEquals(1, result.get(1).getAssigneeId());
    verify(toDoItemService).findBySprintIdAndAssigneeId(sprintId, userId);
  }

  // Helper method to create test tasks
  private ToDoItem createTestTask(int id, String title, Long sprintId, Long assigneeId) {
    ToDoItem task = new ToDoItem();
    task.setId(id);
    task.setTitle(title);
    task.setSprintId(sprintId);
    task.setAssigneeId(assigneeId);
    return task;
  }
}