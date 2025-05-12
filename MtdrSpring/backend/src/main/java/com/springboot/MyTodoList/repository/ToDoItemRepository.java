package com.springboot.MyTodoList.repository;

import com.springboot.MyTodoList.model.ToDoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import java.util.List;
import java.time.OffsetDateTime;
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

    /**
     * Find tasks by assignee ID
     */
    List<ToDoItem> findByAssigneeId(Long assigneeId);

    /**
     * Find tasks by assignee ID and sprint ID
     */
    List<ToDoItem> findByAssigneeIdAndSprintId(Long assigneeId, Long sprintId);

    /**
     * Find tasks by creation date range
     */
    List<ToDoItem> findByCreationTsBetween(OffsetDateTime startDate, OffsetDateTime endDate);

    /**
     * Find tasks by team ID and creation date range
     */
    List<ToDoItem> findByTeamIdAndCreationTsBetween(Long teamId, OffsetDateTime startDate, OffsetDateTime endDate);

    /**
     * Find tasks by assignee ID and creation date range
     */
    List<ToDoItem> findByAssigneeIdAndCreationTsBetween(Long assigneeId, OffsetDateTime startDate,
            OffsetDateTime endDate);

    /**
     * Count tasks by status
     */
    @Query("SELECT COUNT(t) FROM ToDoItem t WHERE t.status = :status")
    long countByStatus(@Param("status") String status);

    /**
     * Count tasks by assignee and status
     */
    @Query("SELECT COUNT(t) FROM ToDoItem t WHERE t.assigneeId = :assigneeId AND t.status = :status")
    long countByAssigneeIdAndStatus(@Param("assigneeId") Long assigneeId, @Param("status") String status);

    /**
     * Count tasks by team and status
     */
    @Query("SELECT COUNT(t) FROM ToDoItem t WHERE t.teamId = :teamId AND t.status = :status")
    long countByTeamIdAndStatus(@Param("teamId") Long teamId, @Param("status") String status);

    /**
     * Get average completion time in days
     */
    @Query("SELECT AVG(CAST(t.completedAt AS date) - CAST(t.creationTs AS date)) FROM ToDoItem t WHERE t.completedAt IS NOT NULL")
    Double getAverageCompletionTimeInDays();
}