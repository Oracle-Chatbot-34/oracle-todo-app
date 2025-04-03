type KPIDictionary = {
  [key: number]: {
    definition: string;
    example: string;
  };
};

export const dictionaryKPI: KPIDictionary = {
  1: {
    definition:
      'Task Completion Rate (TCR) measures the percentage of completed tasks out of the total created tasks.',
    example:
      'If a team created 100 tasks and completed 80, the TCR would be (80/100) * 100 = 80%.',
  },
  2: {
    definition:
      'Average Tasks per Employee (ATE) calculates the average number of tasks assigned to each active employee.',
    example:
      'If 5 employees handled 200 tasks, ATE = 200/5 = 40 tasks per employee.',
  },
  3: {
    definition:
      'On-Time Completion Rate (OTCR) determines the percentage of tasks that were completed on or before the deadline.\nOverdue Tasks Ratio (OTR) measures the proportion of completed tasks that were finished late.',
    example:
      'OTCR: If 50 out of 60 completed tasks met the deadline, OTCR = (50/60) * 100 = 83.3%.\nOTR: If 20 out of 80 completed tasks were late, OTR = (20/80) * 100 = 25%.',
  },
  4: {
    definition:
      'Real Hours Worked (RH) calculates the difference between actual hours worked and planned hours.',
    example:
      'If an employee was scheduled for 40 hours but worked 45, RH = 45 - 40 = 5 extra hours.',
  },
  5: {
    definition:
      'OCI Resources Utilization (OCIRU) indicates the percentage of actual resource usage compared to planned usage.',
    example:
      'If the planned usage was 100 CPU hours and actual usage was 85, OCIRU = (85/100) * 100 = 85%.',
  },
  6: {
    definition:
      'Tasks Completed per Week (CR) calculates the percentage of completed tasks out of the planned tasks for a given week.',
    example:
      'If a team planned 50 tasks but completed 45, CR = (45/50) * 100 = 90%.',
  },
};
