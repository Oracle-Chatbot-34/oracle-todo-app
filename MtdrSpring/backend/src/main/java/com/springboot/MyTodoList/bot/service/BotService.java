package com.springboot.MyTodoList.bot.service;

import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.service.ToDoItemService;
import com.springboot.MyTodoList.service.SprintService;
import com.springboot.MyTodoList.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

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

    // ToDoItem operations

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

    public ToDoItem addToDoItem(ToDoItem todoItem) {
        logger.info("Adding new todo item: {}", todoItem.getTitle());
        try {
            logger.debug("Todo item details - Description: {}, Done: {}, Team ID: {}, Assignee ID: {}",
                    todoItem.getDescription(), todoItem.isDone(), todoItem.getTeamId(), todoItem.getAssigneeId());

            ToDoItem savedItem = toDoItemService.addToDoItem(todoItem);
            logger.info("Successfully added todo item with ID: {}", savedItem.getID());
            return savedItem;
        } catch (Exception e) {
            logger.error("Error adding todo item: {}", todoItem.getTitle(), e);
            throw new RuntimeException("Failed to add todo item", e);
        }
    }

    public ToDoItem updateToDoItem(ToDoItem toDoItem) {
        logger.info("Updating todo item with ID: {}", toDoItem.getID());
        logger.debug("Update details - Title: {}, Description: {}, Done: {}, Status: {}",
                toDoItem.getTitle(), toDoItem.getDescription(), toDoItem.isDone(), toDoItem.getStatus());

        try {
            ToDoItem updatedItem = toDoItemService.updateToDoItem(toDoItem.getID(), toDoItem);
            logger.info("Successfully updated todo item with ID: {}", toDoItem.getID());
            return updatedItem;
        } catch (Exception e) {
            logger.error("Error updating todo item with ID: {}", toDoItem.getID(), e);
            throw new RuntimeException("Failed to update todo item", e);
        }
    }

    public boolean deleteToDoItem(int id) {
        logger.info("Deleting todo item with ID: {}", id);
        try {
            boolean result = toDoItemService.deleteToDoItem(id);
            if (result) {
                logger.info("Successfully deleted todo item with ID: {}", id);
            } else {
                logger.warn("Failed to delete todo item with ID: {}, item may not exist", id);
            }
            return result;
        } catch (Exception e) {
            logger.error("Error deleting todo item with ID: {}", id, e);
            throw new RuntimeException("Failed to delete todo item", e);
        }
    }

    public ToDoItem startTask(int taskId, Long userId) {
        logger.info("Starting task with ID: {} for user: {}", taskId, userId);
        try {
            return toDoItemService.startTask(taskId, userId);
        } catch (Exception e) {
            logger.error("Error starting task with ID: {}", taskId, e);
            throw new RuntimeException("Failed to start task", e);
        }
    }

    public ToDoItem completeTask(int taskId, double actualHours, String comments) {
        logger.info("Completing task with ID: {}, actual hours: {}", taskId, actualHours);
        try {
            return toDoItemService.completeTask(taskId, actualHours, comments);
        } catch (Exception e) {
            logger.error("Error completing task with ID: {}", taskId, e);
            throw new RuntimeException("Failed to complete task", e);
        }
    }

    public ToDoItem assignTaskToSprint(int taskId, Long sprintId) {
        logger.info("Assigning task with ID: {} to sprint: {}", taskId, sprintId);
        try {
            return toDoItemService.assignTaskToSprint(taskId, sprintId);
        } catch (Exception e) {
            logger.error("Error assigning task with ID: {} to sprint: {}", taskId, sprintId, e);
            throw new RuntimeException("Failed to assign task to sprint", e);
        }
    }

    // User operations

    public Optional<User> findUserById(Long userId) {
        logger.info("Finding user by ID: {}", userId);
        return userService.findById(userId);
    }

    public List<User> findUsersByTeamId(Long teamId) {
        logger.info("Finding users by team ID: {}", teamId);
        return userService.findByTeamId(teamId);
    }

    public Optional<User> findUserByTelegramId(Long telegramId) {
        logger.info("Finding user by Telegram ID: {}", telegramId);
        return userService.findByTelegramId(telegramId);
    }

    public Optional<User> findUserByEmployeeId(String employeeId) {
        logger.info("Finding user by employee ID: {}", employeeId);
        return userService.findByEmployeeId(employeeId);
    }

    public User updateUserTelegramId(User user, Long telegramId) {
        logger.info("Updating Telegram ID for user: {} to {}", user.getUsername(), telegramId);
        try {
            user.setTelegramId(telegramId);
            return userService.updateUser(user);
        } catch (Exception e) {
            logger.error("Error updating Telegram ID for user: {}", user.getUsername(), e);
            throw new RuntimeException("Failed to update user's Telegram ID", e);
        }
    }

    // Sprint operations

    public Optional<Sprint> findActiveSprintByTeamId(Long teamId) {
        logger.info("Finding active sprint for team ID: {}", teamId);
        return sprintService.findActiveSprintByTeamId(teamId);
    }

    public List<ToDoItem> findTasksBySprintId(Long sprintId) {
        logger.info("Finding tasks for sprint ID: {}", sprintId);
        return toDoItemService.findTasksBySprintId(sprintId);
    }

    public List<ToDoItem> findActiveTasksByAssigneeId(Long userId) {
        logger.info("Finding active tasks for user ID: {}", userId);
        return toDoItemService.findActiveTasksByAssigneeId(userId);
    }

    public Sprint createSprint(Sprint sprint) {
        logger.info("Creating new sprint: {}", sprint.getName());
        try {
            sprint.setCreatedAt(OffsetDateTime.now());
            Sprint createdSprint = sprintService.createSprint(sprint);
            logger.info("Successfully created sprint with ID: {}", createdSprint.getId());
            return createdSprint;
        } catch (Exception e) {
            logger.error("Error creating sprint: {}", sprint.getName(), e);
            throw new RuntimeException("Failed to create sprint", e);
        }
    }

    public Sprint completeSprint(Long sprintId) {
        logger.info("Completing sprint with ID: {}", sprintId);
        try {
            return sprintService.completeSprint(sprintId);
        } catch (Exception e) {
            logger.error("Error completing sprint with ID: {}", sprintId, e);
            throw new RuntimeException("Failed to complete sprint", e);
        }
    }
}