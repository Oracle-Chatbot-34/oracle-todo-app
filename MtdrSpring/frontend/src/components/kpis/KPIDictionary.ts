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
      'This graph ilustrates the amount of worked hours across a selected range of sprints (By Sprint). It also gives the total worked hours for the whole sprint range.',
    example: 'Sprint 1: 10 hours, Sprint 2: 15 hours, Total: 25 hours.',
  },
  4: {
    definition:
      'This graph ilustrates the amount of completed tasks across a selected range of sprints (By Sprint). It also gives the total amount of completed tasks for the whole sprint range.',
    example: 'Sprint 1: 5 tasks, Sprint 2: 8 tasks, Total: 13 tasks.',
  },
  5: {
    definition:
      'Task Information by Sprints displays all tasks grouped by sprint, including details like assignee, title, status, priority, estimated hours, and actual hours.',
    example:
      'Sprint 1 includes Task A (assigned to Dev 1, High Priority, Estimated: 6h, Actual: 7h, Status: Done).',
  },
};
