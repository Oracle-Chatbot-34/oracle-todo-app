type KPIDictionary = {
  [key: number]: {
    definition: string;
    example: string;
  };
};

export const dictionaryKPI: KPIDictionary = {
  1: {
    definition:
      'Hours Worked by Developer tracks the total number of hours (actual) logged by each developer across all assigned tasks in a given sprint range.',
    example:
      'If Developer A worked 10 hours on Task 1 and 15 on Task 2, the total hours worked would be 25 hours.',
  },
  2: {
    definition:
      'Completed Tasks by Developer per Sprint measures the number of tasks each developer has marked as completed during a specific sprint.',
    example:
      'If Developer B completed 4 tasks in Sprint 2, the metric would be 4 completed tasks for that sprint.',
  },
  3: {
    definition:
      'This graph illustrates the amount of worked hours across a selected range of sprints (By Sprint). It also gives the total worked hours for the whole sprint range.',
    example: 'Sprint 1: 10 hours, Sprint 2: 15 hours, Total: 25 hours.',
  },
  4: {
    definition:
      'This graph illustrates the amount of completed tasks across a selected range of sprints (By Sprint). It also gives the total amount of completed tasks for the whole sprint range.',
    example: 'Sprint 1: 5 tasks, Sprint 2: 8 tasks, Total: 13 tasks.',
  },
  5: {
    definition:
      'Task Information by Sprints displays all tasks grouped by sprint, including details like assignee, title, status, priority, estimated hours, and actual hours.',
    example:
      'Sprint 1 includes Task A (assigned to Dev 1, High Priority, Estimated: 6h, Actual: 7h, Status: Done).',
  },
  6: {
    definition:
      'Hours Worked by Developer per Sprint provides a detailed breakdown showing exactly how many hours each developer worked in each individual sprint, with stacked bars for easy comparison.',
    example:
      'Sprint 1: Developer A (20h), Developer B (25h). Sprint 2: Developer A (18h), Developer B (30h). This visualization directly addresses Oracle DevOps reporting requirements.',
  },
  7: {
    definition:
      'Total Hours Worked by Sprint shows the cumulative hours worked by all team members combined for each sprint, providing insight into overall team effort and workload distribution across sprints.',
    example:
      'Sprint 1: 65 total hours (Developer A: 25h + Developer B: 20h + Developer C: 20h). Sprint 2: 80 total hours (Developer A: 30h + Developer B: 25h + Developer C: 25h).',
  },
};