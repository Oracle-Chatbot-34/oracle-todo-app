package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.repository.SprintRepository;
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
public class SprintServiceTest {

    @Mock
    private SprintRepository sprintRepository;

    @InjectMocks
    private SprintService sprintService;

    private Sprint testSprint;
    private static final Long SPRINT_ID = 1L;
    private static final Long TEAM_ID = 1L;

    @BeforeEach
    void setUp() {
        testSprint = new Sprint();
        testSprint.setId(SPRINT_ID);
        testSprint.setName("Test Sprint");
        testSprint.setDescription("Test Description");
        testSprint.setStatus("PLANNING");
        testSprint.setStartDate(OffsetDateTime.now());
        testSprint.setEndDate(OffsetDateTime.now().plusDays(14));
        testSprint.setCreatedAt(OffsetDateTime.now());
        testSprint.setUpdatedAt(OffsetDateTime.now());
    }

    @Test
    void testFindById() {
        // Setup
        when(sprintRepository.findById(SPRINT_ID)).thenReturn(Optional.of(testSprint));

        // Execute
        Optional<Sprint> result = sprintService.findById(SPRINT_ID);

        // Verify
        assertTrue(result.isPresent());
        assertEquals(SPRINT_ID, result.get().getId());
        verify(sprintRepository, times(1)).findById(SPRINT_ID);
    }

    @Test
    void testFindActiveByTeamId() {
        // Setup
        when(sprintRepository.findByTeamIdAndStatus(TEAM_ID, "ACTIVE")).thenReturn(Optional.of(testSprint));

        // Execute
        Optional<Sprint> result = sprintService.findActiveByTeamId(TEAM_ID);

        // Verify
        assertTrue(result.isPresent());
        assertEquals(SPRINT_ID, result.get().getId());
        verify(sprintRepository, times(1)).findByTeamIdAndStatus(TEAM_ID, "ACTIVE");
    }

    @Test
    void testStartSprint() {
        // Setup
        Sprint startedSprint = new Sprint();
        startedSprint.setId(SPRINT_ID);
        startedSprint.setStatus("ACTIVE");
        startedSprint.setStartDate(OffsetDateTime.now());

        when(sprintRepository.findById(SPRINT_ID)).thenReturn(Optional.of(testSprint));
        when(sprintRepository.save(any(Sprint.class))).thenReturn(startedSprint);

        // Execute
        Sprint result = sprintService.startSprint(SPRINT_ID);

        // Verify
        assertNotNull(result);
        assertEquals("ACTIVE", result.getStatus());
        assertNotNull(result.getStartDate());
        verify(sprintRepository, times(1)).findById(SPRINT_ID);
        verify(sprintRepository, times(1)).save(any(Sprint.class));
    }

    @Test
    void testCompleteSprint() {
        // Setup
        Sprint completedSprint = new Sprint();
        completedSprint.setId(SPRINT_ID);
        completedSprint.setStatus("COMPLETED");

        when(sprintRepository.findById(SPRINT_ID)).thenReturn(Optional.of(testSprint));
        when(sprintRepository.save(any(Sprint.class))).thenReturn(completedSprint);

        // Execute
        Sprint result = sprintService.completeSprint(SPRINT_ID);

        // Verify
        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        verify(sprintRepository, times(1)).findById(SPRINT_ID);
        verify(sprintRepository, times(1)).save(any(Sprint.class));
    }

    @Test
    void testFindCompletedByTeamId() {
        // Setup
        List<Sprint> allTeamSprints = new ArrayList<>();
        
        Sprint completedSprint = new Sprint();
        completedSprint.setId(2L);
        completedSprint.setStatus("COMPLETED");
        allTeamSprints.add(completedSprint);
        
        Sprint activeSprint = new Sprint();
        activeSprint.setId(3L);
        activeSprint.setStatus("ACTIVE");
        allTeamSprints.add(activeSprint);

        when(sprintRepository.findByTeamId(TEAM_ID)).thenReturn(allTeamSprints);

        // Execute
        List<Sprint> result = sprintService.findCompletedByTeamId(TEAM_ID);

        // Verify
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("COMPLETED", result.get(0).getStatus());
        verify(sprintRepository, times(1)).findByTeamId(TEAM_ID);
    }
}