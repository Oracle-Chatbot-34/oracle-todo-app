import { ClipboardList } from 'lucide-react';
import { useEffect, useState } from 'react';

type Task = {
  id: number;
  title: string;
  estimatedHours: number;
  actualHours: number;
  assigneeId: number;
  status: string;
  priority: string;
};

type FormattedSprint = {
  id: number;
  name: string;
  tasks: Task[];
};

type Sprint = {
  sprintId: number;
  sprintName: string;
};

type TaskInformationBySprintProps = {
  sprints: Sprint[];
};

/*
Example of props:
{
  {
    sprintId: 1,
    sprintName: "Sprint 1",
  },
  {
    sprintId: 2,
    sprintName: "Sprint 2",
  },
  // Add more sprints as needed
}

*/

export default function RealHours({ sprints }: TaskInformationBySprintProps) {
  const [formattedSprints, setFormattedSprints] = useState<FormattedSprint[]>(
    []
  );

  // Simulate fetching sprints and their tasks
  // For each of the sprints, we will fetch the tasks in that specific sprint
  /*
    Example of fetching tasks for each sprint:
    [
      {
        sprintId: 1,
        tasks: [
          {
            id: 101,
            title: "Login feature",
            estimatedHours: 5,
            actualHours: 6,
            assigneeId: 1,
            status: "Done",
            priority: "High",
          },
          // More tasks...
        ],
      },
      {
        sprintId: 2,
        tasks: [
          {
            id: 201,
            title: "Dashboard chart",
            estimatedHours: 8,
            actualHours : 7,
            assigneeId: 3,
            status: "In Progress",
            priority: "High",
          },
          // More tasks...
        ],
      },
    ]
    */
  useEffect(() => {
    const fakeFetch = async () => {
      const statuses = ['To Do', 'In Progress', 'Done'];
      const priorities = ['Low', 'Medium', 'High'];

      const simulatedData: FormattedSprint[] = sprints.map((sprint) => {
        const numberOfTasks = Math.floor(Math.random() * 2) + 3; // 3 to 4 tasks
        const tasks: Task[] = Array.from(
          { length: numberOfTasks },
          (_, idx) => {
            const estimated = Math.floor(Math.random() * 10) + 1;
            const variation = Math.floor(Math.random() * 3) - 1; // -1, 0, or 1
            const actual = Math.max(1, estimated + variation);
            return {
              id: sprint.sprintId * 100 + idx,
              title: `Task ${idx + 1} of ${sprint.sprintName}`,
              estimatedHours: estimated,
              actualHours: actual,
              assigneeId: Math.floor(Math.random() * 5) + 1,
              status: statuses[Math.floor(Math.random() * statuses.length)],
              priority:
                priorities[Math.floor(Math.random() * priorities.length)],
            };
          }
        );

        return {
          id: sprint.sprintId,
          name: sprint.sprintName,
          tasks,
        };
      });

      setFormattedSprints(simulatedData);
    };

    fakeFetch();
  }, [sprints]);

  return (
    <div className="w-full h-full flex flex-col gap-4 p-5 bg-white rounded-xl shadow-lg">
      <div className="flex flex-row text-2xl gap-4 w-1/2">
        <ClipboardList className="w-6 h-6" />
        <p className="font-semibold">Tasks Information by sprints</p>
      </div>

      <div className="w-full max-h-full flex flex-col gap-2 overflow-y-scroll">
        {formattedSprints.map((sprint) => (
          <div
            key={sprint.id}
            className="w-full h-full flex flex-col gap-4 border-b px-4 pb-4"
          >
            <p className="text-2xl font-semibold">{sprint.name}</p>
            <div className="flex flex-col gap-2 px-4 ">
              {sprint.tasks.map((task) => (
                <div
                  key={task.id}
                  className="flex flex-row gap-4 items-center justify-between"
                >
                  <div className="flex flex-col gap-1">
                    <div className="flex flex-row gap-4">
                      <p className="font-semibold text-lg">{task.title}</p>
                      <p className="font-semibold text-lg">{task.assigneeId}</p>
                    </div>
                    <div className="flex flex-row gap-4">
                      <p>{task.status}</p>
                      <p>{task.priority}</p>
                    </div>
                  </div>
                  <div className="flex flex-col gap-1">
                    <p className="text-md text-gray-700 text-right">
                      Estimated: {task.estimatedHours} hours
                    </p>
                    <p className="text-md text-gray-700 text-right">
                      Actual: {task.actualHours} hours
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
