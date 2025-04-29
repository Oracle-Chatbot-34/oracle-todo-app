import { ClipboardList } from 'lucide-react';
import { useEffect, useState } from 'react';
import KPITitle from './KPITtitle';
import api from '@/services/api';
import { config } from '@/lib/config';

type Task = {
  id: number;
  title: string;
  estimatedHours: number;
  actualHours: number;
  assigneeId: number;
  assigneeName?: string;
  status: string;
  priority: string;
};

type ApiTask = {
  id: number;
  title?: string;
  estimatedHours?: number;
  actualHours?: number;
  assigneeId?: number;
  assigneeName?: string;
  status?: string;
  priority?: string;
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
  definition: string;
  example: string;
};

export default function RealHours({
  sprints,
  definition,
  example,
}: TaskInformationBySprintProps) {
  const [formattedSprints, setFormattedSprints] = useState<FormattedSprint[]>(
    []
  );
  const [loading, setLoading] = useState(false);

  // Fetch real tasks for sprints
  useEffect(() => {
    if (!sprints || sprints.length === 0) return;

    const fetchTasksForSprints = async () => {
      setLoading(true);
      try {
        const formattedSprintsData: FormattedSprint[] = [];

        for (const sprint of sprints) {
          try {
            // Get tasks for this sprint
            const response = await api.get(
              `${config.apiEndpoint}/sprints/${sprint.sprintId}/tasks`
            );

            const tasks = response.data.map((task: ApiTask) => ({
              id: task.id,
              title: task.title || `Task ${task.id}`,
              estimatedHours: task.estimatedHours || 0,
              actualHours: task.actualHours || 0,
              assigneeId: task.assigneeId || 0,
              assigneeName: task.assigneeName || `User ${task.assigneeId}`,
              status: task.status || 'Unknown',
              priority: task.priority || 'Medium',
            }));

            formattedSprintsData.push({
              id: sprint.sprintId,
              name: sprint.sprintName,
              tasks,
            });
          } catch (err) {
            console.error(
              `Error fetching tasks for sprint ${sprint.sprintId}:`,
              err
            );
            // Still add the sprint with empty tasks so it shows up in the UI
            formattedSprintsData.push({
              id: sprint.sprintId,
              name: sprint.sprintName,
              tasks: [],
            });
          }
        }

        setFormattedSprints(formattedSprintsData);
      } catch (err) {
        console.error('Error fetching tasks:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchTasksForSprints();
  }, [sprints]);

  return (
    <div className="w-full h-full flex flex-col p-5 bg-white rounded-xl shadow-lg">
      <div className="flex flex-row text-2xl gap-4 w-17/18 items-center">
        <ClipboardList className="w-6 h-6" />
        <KPITitle
          title="Task Information by Sprint"
          KPIObject={{ definition, example }}
        />
      </div>

      <div className="max-w-full max-h-[65vh] flex flex-col gap-2 p-4 overflow-y-auto">
        {loading ? (
          <div className="flex items-center justify-center h-48">
            <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-gray-900"></div>
          </div>
        ) : formattedSprints.length === 0 ? (
          <div className="text-center text-gray-500 py-8">
            No sprint data available.
          </div>
        ) : (
          formattedSprints.map((sprint) => (
            <div
              key={sprint.id}
              className="w-full max-h-full flex flex-col gap-4 border-b px-4 pb-4"
            >
              <p className="text-2xl font-semibold">{sprint.name}</p>
              <div className="flex flex-col gap-2 px-4 h-fit w-full">
                {sprint.tasks.length === 0 ? (
                  <div className="text-center text-gray-500 py-4">
                    No tasks in this sprint.
                  </div>
                ) : (
                  sprint.tasks.map((task) => (
                    <div
                      key={task.id}
                      className="flex flex-row gap-4 items-center justify-between"
                    >
                      <div className="flex flex-col gap-2 text-lg">
                        <div className="flex flex-row gap-4">
                          <p className="font-semibold">{task.title}</p>
                          <div className="flex flex-row gap-1">
                            <p>Assigned to:</p>
                            <p className="font-semibold">
                              {task.assigneeName || `User ${task.assigneeId}`}
                            </p>
                          </div>
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
                  ))
                )}
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
