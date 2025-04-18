package com.springboot.MyTodoList.repository;

import com.springboot.MyTodoList.model.ToDoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import java.util.List;

import jakarta.transaction.Transactional;

@Repository
@Transactional
@EnableTransactionManagement
public interface ToDoItemRepository extends JpaRepository<ToDoItem, Integer> {
    /**
     * Find tasks by sprint ID
     */
    List<ToDoItem> findBySprintId(Long sprintId);

    /**
     * Find tasks by assignee ID and status
     */
    List<ToDoItem> findByAssigneeIdAndStatus(Long assigneeId, String status);

    /**
     * Find tasks by assignee ID where status is not the given value
     */
    List<ToDoItem> findByAssigneeIdAndStatusNot(Long assigneeId, String status);

    /**
     * Find tasks by team ID
     */
    List<ToDoItem> findByTeamId(Long teamId);

    List<ToDoItem> findByAssigneeId(Long assigneeId);
}