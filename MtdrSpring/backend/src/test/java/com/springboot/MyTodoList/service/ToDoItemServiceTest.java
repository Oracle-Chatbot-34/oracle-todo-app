package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.repository.ToDoItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ToDoItemServiceTest {

    @Mock
    private ToDoItemRepository toDoItemRepository;

    @InjectMocks
    private ToDoItemService toDoItemService;

    private ToDoItem testTask;
    private static final int TASK_ID = 1;
    private static final Long USER_ID = 1L;
    private static final Long SPRINT_ID = 1L;

    @BeforeEach
    void setUp() {
        testTask = new ToDoItem();
        testTask.setID(TASK_ID);
        testTask.setTitle("Test Task");
        testTask.setDescription("Test Description");
        testTask.setStatus("BACKLOG");
        testTask.setAssigneeId(USER_ID);
        testTask.setSprintId(SPRINT_ID);
        testTask.setCreationTs(OffsetDateTime.now());
        testTask.setEstimatedHours(2.0);
    }

    @Test
    void testAddTaskWithEstimation() {
        // Setup
        when(toDoItemRepository.save(any(ToDoItem.class))).thenReturn(testTask);

        // Execute
        ToDoItem result = toDoItemService.addTaskWithEstimation(testTask);

        // Verify
        assertNotNull(result);
        assertEquals(TASK_ID, result.getID());
        assertEquals("Test Task", result.getTitle());
        verify(toDoItemRepository, times(1)).save(any(ToDoItem.class));
    }

    @Test
    void testFindTasksBySprintId() {
        // Setup
        List<ToDoItem> sprintTasks = new ArrayList<>();
        sprintTasks.add(testTask);
        when(toDoItemRepository.findBySprintId(SPRINT_ID)).thenReturn(sprintTasks);

        // Execute
        List<ToDoItem> result = toDoItemService.findTasksBySprintId(SPRINT_ID);

        // Verify
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TASK_ID, result.get(0).getID());
        verify(toDoItemRepository, times(1)).findBySprintId(SPRINT_ID);
    }

    @Test
    void testFindBySprintIdAndAssigneeId() {
        // Setup
        List<ToDoItem> userSprintTasks = new ArrayList<>();
        userSprintTasks.add(testTask);
        when(toDoItemRepository.findBySprintId(SPRINT_ID)).thenReturn(userSprintTasks);

        // Execute
        List<ToDoItem> result = toDoItemService.findBySprintIdAndAssigneeId(SPRINT_ID, USER_ID);

        // Verify
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TASK_ID, result.get(0).getID());
        assertEquals(USER_ID, result.get(0).getAssigneeId());
        verify(toDoItemRepository, times(1)).findBySprintId(SPRINT_ID);
    }

    @Test
    void testCompleteTask() {
        // Setup
        ToDoItem completedTask = new ToDoItem();
        completedTask.setID(TASK_ID);
        completedTask.setStatus("COMPLETED");
        completedTask.setCompletedAt(OffsetDateTime.now());
        completedTask.setActualHours(2.5);

        when(toDoItemRepository.findById(TASK_ID)).thenReturn(Optional.of(testTask));
        when(toDoItemRepository.save(any(ToDoItem.class))).thenReturn(completedTask);

        // Execute
        ToDoItem result = toDoItemService.completeTask(TASK_ID, 2.5, "Completed on time");

        // Verify
        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        assertNotNull(result.getCompletedAt());
        assertEquals(2.5, result.getActualHours());
        verify(toDoItemRepository, times(1)).findById(TASK_ID);
        verify(toDoItemRepository, times(1)).save(any(ToDoItem.class));
    }
}