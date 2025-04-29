package com.springboot.MyTodoList.repository;

import com.springboot.MyTodoList.model.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, Long> {
    
    List<Sprint> findByTeamIdOrderByStartDateDesc(Long teamId);
    
    @Query("SELECT s FROM Sprint s WHERE s.team.id = :teamId AND s.status = 'ACTIVE'")
    Optional<Sprint> findActiveByTeamId(@Param("teamId") Long teamId);
    
    // New method to find sprints by ID range
    List<Sprint> findByIdBetweenOrderById(Long startId, Long endId);
}