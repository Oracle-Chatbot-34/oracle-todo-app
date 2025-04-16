import { useState, useEffect } from 'react';
import { Task } from '@/services/tasksService';
import taskService from '@/services/tasksService';
import { Sprint } from '@/services/sprintService';
import { motion } from 'framer-motion';

import { dummyTasks } from './tasksdummy';

type SprintArrowProps = {
  sprint: Sprint;
  isExpanded: boolean;
  onToggle: () => void;
};

export default function SprintArrow({
  sprint,
  isExpanded,
  onToggle,
}: SprintArrowProps) {
  const [tasksInSprint, setTasksInSprint] = useState<Task[]>([]);

  /*
  useEffect(() => {
    const fetchTasks = async () => {
      try {
        const tasks = await taskService.getAllTasks();
        setTasksInSprint(tasks);
      } catch (error) {
        console.error('Failed to fetch tasks:', error);
      }
    };

    fetchTasks();
  }, []);
  
  */
  useEffect(() => {
    const fetchTasks = async () => {
      setTasksInSprint(dummyTasks);
    };

    fetchTasks();
  }, []);

  return (
    <div>
      Sprint Name: {sprint.name}
      <br/>
      Sprint ID: {sprint.id}
      <br/>
      Sprint Start: {sprint.startDate}
      <br/>
      Sprint End: {sprint.endDate}

    </div>
  );
}
