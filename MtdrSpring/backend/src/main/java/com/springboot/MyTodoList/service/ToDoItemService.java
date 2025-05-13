package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.model.TaskStatus;
import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.repository.ToDoItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ToDoItemService {

    @Autowired
    private ToDoItemRepository toDoItemRepository;

    public List<ToDoItem> findAll() {
        return toDoItemRepository.findAll();
    }

    public Optional<ToDoItem> findById(int id) {
        return toDoItemRepository.findById(id);
    }

    @Transactional
    public ToDoItem addToDoItem(ToDoItem toDoItem) {
        // Set creation timestamp if not already set
        if (toDoItem.getCreationTs() == null) {
            toDoItem.setCreationTs(OffsetDateTime.now());
        }
        return toDoItemRepository.save(toDoItem);
    }

    @Transactional
    public boolean deleteToDoItem(int id) {
        if (!toDoItemRepository.existsById(id)) {
            return false;
        }
        toDoItemRepository.deleteById(id);
        return true;
    }

    @Transactional
    public ToDoItem updateToDoItem(int id, ToDoItem updatedItem) {
        return toDoItemRepository.findById(id)
                .map(existingItem -> {
                    // Only update non-null fields to prevent unintended overwrites
                    if (updatedItem.getTitle() != null) {
                        existingItem.setTitle(updatedItem.getTitle());
                    }
                    if (updatedItem.getDescription() != null) {
                        existingItem.setDescription(updatedItem.getDescription());
                    }
                    if (updatedItem.getDueDate() != null) {
                        existingItem.setDueDate(updatedItem.getDueDate());
                    }
                    if (updatedItem.getStatus() != null) {
                        existingItem.setStatus(updatedItem.getStatus());
                    }
                    if (updatedItem.getPriority() != null) {
                        existingItem.setPriority(updatedItem.getPriority());
                    }
                    // Only update numerical fields if they are not null
                    if (updatedItem.getAssigneeId() != null) {
                        existingItem.setAssigneeId(updatedItem.getAssigneeId());
                    }
                    if (updatedItem.getTeamId() != null) {
                        existingItem.setTeamId(updatedItem.getTeamId());
                    }
                    if (updatedItem.getEstimatedHours() != null) {
                        existingItem.setEstimatedHours(updatedItem.getEstimatedHours());
                    }
                    if (updatedItem.getActualHours() != null) {
                        existingItem.setActualHours(updatedItem.getActualHours());
                    }
                    if (updatedItem.getSprintId() != null) {
                        existingItem.setSprintId(updatedItem.getSprintId());
                    }
                    return toDoItemRepository.save(existingItem);
                })
                .orElse(null);
    }

    @Transactional
    public ToDoItem addTaskWithEstimation(ToDoItem toDoItem) throws IllegalArgumentException {
        // Validate estimated hours
        if (toDoItem.getEstimatedHours() == null) {
            throw new IllegalArgumentException("Estimated hours are required for tasks");
        }

        if (toDoItem.getEstimatedHours() > 4.0) {
            throw new IllegalArgumentException(
                    "Tasks cannot exceed 4 hours of estimated work. Please break it down into smaller subtasks.");
        }

        // Set default values
        toDoItem.setCreationTs(OffsetDateTime.now());
        if (toDoItem.getStatus() == null) {
            toDoItem.setStatus(TaskStatus.BACKLOG.name());
        }

        return toDoItemRepository.save(toDoItem);
    }

    @Transactional
    public ToDoItem assignTaskToSprint(int taskId, Long sprintId) {
        return toDoItemRepository.findById(taskId)
                .map(task -> {
                    task.setSprintId(sprintId);
                    task.setStatus(TaskStatus.IN_SPRINT.name());
                    return toDoItemRepository.save(task);
                })
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + taskId));
    }

    @Transactional
    public ToDoItem startTask(int taskId, Long userId) {
        return toDoItemRepository.findById(taskId)
                .map(task -> {
                    task.setAssigneeId(userId);
                    task.setStatus(TaskStatus.IN_PROGRESS.name());
                    return toDoItemRepository.save(task);
                })
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + taskId));
    }

    @Transactional
    public ToDoItem completeTask(int taskId, Double actualHours, String comments) {
        return toDoItemRepository.findById(taskId)
                .map(task -> {
                    task.setActualHours(actualHours);
                    task.setStatus(TaskStatus.COMPLETED.name());

                    // Append comments to description if provided
                    if (comments != null && !comments.trim().isEmpty()) {
                        String currentDescription = task.getDescription() != null ? task.getDescription() : "";
                        task.setDescription(currentDescription + "\n\nCompletion Notes: " + comments);
                    }

                    task.setCompletedAt(OffsetDateTime.now());
                    return toDoItemRepository.save(task);
                })
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + taskId));
    }

    public List<ToDoItem> findTasksBySprintId(Long sprintId) {
        return toDoItemRepository.findBySprintId(sprintId);
    }

    public List<ToDoItem> findActiveTasksByAssigneeId(Long assigneeId) {
        return toDoItemRepository.findByAssigneeIdAndStatusNot(assigneeId, TaskStatus.COMPLETED.name());
    }

    public List<ToDoItem> findByAssigneeId(Long assigneeId) {
        return toDoItemRepository.findByAssigneeId(assigneeId);
    }

    public List<ToDoItem> findTasksByTeamId(Long teamId) {
        return toDoItemRepository.findByTeamId(teamId);
    }

    public List<ToDoItem> findTasksByDateRange(OffsetDateTime startDate, OffsetDateTime endDate) {
        return toDoItemRepository.findByCreationTsBetween(startDate, endDate);
    }

    public List<ToDoItem> findTasksByTeamIdAndDateRange(Long teamId, OffsetDateTime startDate, OffsetDateTime endDate) {
        return toDoItemRepository.findByTeamIdAndCreationTsBetween(teamId, startDate, endDate);
    }

    /**
     * Find tasks by sprint ID and assignee ID
     */
    public List<ToDoItem> findBySprintIdAndAssigneeId(Long sprintId, Long assigneeId) {
        // This method is not implemented yet, so we need to create it
        // For now, let's implement a simple filter on findTasksBySprintId
        return findTasksBySprintId(sprintId).stream()
                .filter(task -> assigneeId.equals(task.getAssigneeId()))
                .toList();
    }
}