package com.springboot.MyTodoList.bot.service;

import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.service.SprintService;
import com.springboot.MyTodoList.service.ToDoItemService;
import com.springboot.MyTodoList.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for the bot to interact with domain services
 */
@Service
public class BotService {
    private static final Logger logger = LoggerFactory.getLogger(BotService.class);

    private final ToDoItemService toDoItemService;
    private final UserService userService;
    private final SprintService sprintService;

    public BotService(
            ToDoItemService toDoItemService,
            UserService userService,
            SprintService sprintService) {
        this.toDoItemService = toDoItemService;
        this.userService = userService;
        this.sprintService = sprintService;
    }

    /**
     * Authenticate user by employee ID
     */
    public Optional<User> authenticateUser(String employeeId) {
        logger.info("Authenticating user with employee ID: {}", employeeId);
        return userService.findByEmployeeId(employeeId);
    }

    /**
     * Find user by ID
     */
    public Optional<User> findUserById(Long userId) {
        logger.info("Finding user by ID: {}", userId);
        return userService.findById(userId);
    }

    /**
     * Find users by team ID
     */
    public List<User> findUsersByTeamId(Long teamId) {
        logger.info("Finding users by team ID: {}", teamId);
        return userService.findByTeamId(teamId);
    }

    /**
     * Get all ToDoItems for a user
     */
    public List<ToDoItem> getAllToDoItems(Long userId) {
        logger.info("Fetching todo items for user ID: {}", userId);
        try {
            List<ToDoItem> items = toDoItemService.findByAssigneeId(userId);
            logger.info("Successfully fetched {} todo items for user", items.size());
            return items;
        } catch (Exception e) {
            logger.error("Error fetching todo items for user: {}", userId, e);
            throw new RuntimeException("Failed to fetch todo items", e);
        }
    }

    /**
     * Get a specific ToDoItem by ID
     */
    public Optional<ToDoItem> getToDoItemById(int id) {
        logger.info("Fetching todo item by ID: {}", id);
        try {
            Optional<ToDoItem> item = toDoItemService.findById(id);
            if (item.isPresent()) {
                logger.info("Successfully fetched todo item with ID {}: {}", id, item.get().getTitle());
            } else {
                logger.warn("Todo item with ID {} not found", id);
            }
            return item;
        } catch (Exception e) {
            logger.error("Error fetching todo item by ID: {}", id, e);
            throw new RuntimeException("Failed to fetch todo item", e);
        }
    }

    /**
     * Add a new ToDoItem
     */
    public ToDoItem addToDoItem(ToDoItem item) {
        logger.info("Adding new todo item: {}", item.getTitle());
        try {
            ToDoItem savedItem = toDoItemService.addToDoItem(item);
            logger.info("Successfully added todo item with ID: {}", savedItem.getID());
            return savedItem;
        } catch (Exception e) {
            logger.error("Error adding todo item: {}", item.getTitle(), e);
            throw new RuntimeException("Failed to add todo item", e);
        }
    }

    /**
     * Update an existing ToDoItem
     */
    public ToDoItem updateToDoItem(ToDoItem item) {
        logger.info("Updating todo item with ID: {}", item.getID());
        try {
            ToDoItem updatedItem = toDoItemService.updateToDoItem(item.getID(), item);
            logger.info("Successfully updated todo item with ID: {}", updatedItem.getID());
            return updatedItem;
        } catch (Exception e) {
            logger.error("Error updating todo item with ID: {}", item.getID(), e);
            throw new RuntimeException("Failed to update todo item", e);
        }
    }

    /**
     * Delete a ToDoItem
     */
    public void deleteToDoItem(int id) {
        logger.info("Deleting todo item with ID: {}", id);
        try {
            toDoItemService.deleteToDoItem(id);
            logger.info("Successfully deleted todo item with ID: {}", id);
        } catch (Exception e) {
            logger.error("Error deleting todo item with ID: {}", id, e);
            throw new RuntimeException("Failed to delete todo item", e);
        }
    }

    /**
     * Find active tasks by assignee ID
     */
    public List<ToDoItem> findActiveTasksByAssigneeId(Long assigneeId) {
        logger.info("Finding active tasks for assignee ID: {}", assigneeId);
        try {
            List<ToDoItem> tasks = toDoItemService.findActiveTasksByAssigneeId(assigneeId);
            logger.info("Found {} active tasks for assignee", tasks.size());
            return tasks;
        } catch (Exception e) {
            logger.error("Error finding active tasks for assignee ID: {}", assigneeId, e);
            throw new RuntimeException("Failed to find active tasks", e);
        }
    }

    /**
     * Complete a task
     */
    public ToDoItem completeTask(int taskId, double actualHours, String comments) {
        logger.info("Completing task with ID: {}", taskId);
        try {
            ToDoItem completedTask = toDoItemService.completeTask(taskId, actualHours, comments);
            logger.info("Successfully completed task with ID: {}", completedTask.getID());
            return completedTask;
        } catch (Exception e) {
            logger.error("Error completing task with ID: {}", taskId, e);
            throw new RuntimeException("Failed to complete task", e);
        }
    }

    /**
     * Find active sprint by team ID
     */
    public Optional<Sprint> findActiveSprintByTeamId(Long teamId) {
        logger.info("Finding active sprint for team ID: {}", teamId);
        try {
            Optional<Sprint> sprint = sprintService.findActiveByTeamId(teamId);
            if (sprint.isPresent()) {
                logger.info("Found active sprint with ID: {} for team", sprint.get().getId());
            } else {
                logger.info("No active sprint found for team ID: {}", teamId);
            }
            return sprint;
        } catch (Exception e) {
            logger.error("Error finding active sprint for team ID: {}", teamId, e);
            throw new RuntimeException("Failed to find active sprint", e);
        }
    }

    /**
     * Find completed sprints by team ID
     */
    public List<Sprint> findCompletedSprintsByTeamId(Long teamId) {
        logger.info("Finding completed sprints for team ID: {}", teamId);
        try {
            List<Sprint> sprints = sprintService.findCompletedByTeamId(teamId);
            logger.info("Found {} completed sprints for team", sprints.size());
            return sprints;
        } catch (Exception e) {
            logger.error("Error finding completed sprints for team ID: {}", teamId, e);
            throw new RuntimeException("Failed to find completed sprints", e);
        }
    }

    /**
     * Find tasks by sprint ID
     */
    public List<ToDoItem> findTasksBySprintId(Long sprintId) {
        logger.info("Finding tasks for sprint ID: {}", sprintId);
        try {
            List<ToDoItem> tasks = toDoItemService.findTasksBySprintId(sprintId);
            logger.info("Found {} tasks for sprint", tasks.size());
            return tasks;
        } catch (Exception e) {
            logger.error("Error finding tasks for sprint ID: {}", sprintId, e);
            throw new RuntimeException("Failed to find tasks for sprint", e);
        }
    }

    /**
     * Find tasks by sprint ID and assignee ID
     */
    public List<ToDoItem> findTasksBySprintIdAndAssigneeId(Long sprintId, Long assigneeId) {
        logger.info("Finding tasks for sprint ID: {} and assignee ID: {}", sprintId, assigneeId);
        try {
            List<ToDoItem> tasks = toDoItemService.findBySprintIdAndAssigneeId(sprintId, assigneeId);
            logger.info("Found {} tasks for sprint and assignee", tasks.size());
            return tasks;
        } catch (Exception e) {
            logger.error("Error finding tasks for sprint ID: {} and assignee ID: {}", sprintId, assigneeId, e);
            throw new RuntimeException("Failed to find tasks for sprint and assignee", e);
        }
    }

    /**
     * Create a new sprint
     */
    public Sprint createSprint(Sprint sprint) {
        logger.info("Creating new sprint: {}", sprint.getName());
        try {
            Sprint savedSprint = sprintService.save(sprint);
            logger.info("Successfully created sprint with ID: {}", savedSprint.getId());
            return savedSprint;
        } catch (Exception e) {
            logger.error("Error creating sprint: {}", sprint.getName(), e);
            throw new RuntimeException("Failed to create sprint", e);
        }
    }

    /**
     * Update an existing sprint
     */
    public Sprint updateSprint(Sprint sprint) {
        logger.info("Updating sprint with ID: {}", sprint.getId());
        try {
            Sprint updatedSprint = sprintService.update(sprint);
            logger.info("Successfully updated sprint with ID: {}", updatedSprint.getId());
            return updatedSprint;
        } catch (Exception e) {
            logger.error("Error updating sprint with ID: {}", sprint.getId(), e);
            throw new RuntimeException("Failed to update sprint", e);
        }
    }

    /**
     * Complete a sprint
     */
    public Sprint completeSprint(Long sprintId) {
        logger.info("Completing sprint with ID: {}", sprintId);
        try {
            Sprint completedSprint = sprintService.completeSprint(sprintId);
            logger.info("Successfully completed sprint with ID: {}", completedSprint.getId());
            return completedSprint;
        } catch (Exception e) {
            logger.error("Error completing sprint with ID: {}", sprintId, e);
            throw new RuntimeException("Failed to complete sprint", e);
        }
    }

    /**
     * Assign a task to a sprint
     */
    public ToDoItem assignTaskToSprint(int taskId, Long sprintId) {
        logger.info("Assigning task {} to sprint {}", taskId, sprintId);
        try {
            ToDoItem updatedTask = toDoItemService.assignTaskToSprint(taskId, sprintId);
            logger.info("Successfully assigned task {} to sprint {}", updatedTask.getID(), sprintId);
            return updatedTask;
        } catch (Exception e) {
            logger.error("Error assigning task {} to sprint {}", taskId, sprintId, e);
            throw new RuntimeException("Failed to assign task to sprint", e);
        }
    }
}