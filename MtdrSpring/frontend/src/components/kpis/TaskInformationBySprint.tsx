import { ClipboardList } from 'lucide-react';
import { useEffect, useState } from 'react';
import KPITitle from './KPITtitle';
import api from '@/services/api';
import { config } from '@/lib/config';
import userService from '@/services/userService';
import LoadingSpinner from '@/components/LoadingSpinner';

type Task = {
  id: number;
  title: string;
  estimatedHours: number;
  actualHours: number;
  assigneeId: number;
  assigneeName?: string;
  status: string;
  priority: string;
  sprintId?: number;
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
  sprintId?: number;
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

export default function TaskInformationBySprint({
  sprints,
  definition,
  example,
}: TaskInformationBySprintProps) {
  const [formattedSprints, setFormattedSprints] = useState<FormattedSprint[]>(
    []
  );
  const [loading, setLoading] = useState(false);
  const [users, setUsers] = useState<Record<number, string>>({});
  const [expandedSprints, setExpandedSprints] = useState<Set<number>>(
    new Set()
  );

  // Fetch all users first to have their names available
  useEffect(() => {
    setLoading(true);

    const fetchUsers = async () => {
      try {
        const allUsers = await userService.getAllUsers();
        const userMap: Record<number, string> = {};

        allUsers.forEach((user) => {
          if (user.id) {
            userMap[user.id] = user.fullName;
          }
        });

        setUsers(userMap);
      } catch (err) {
        console.error('Error fetching users:', err);
      }
    };

    fetchUsers();
  }, []);

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
              `${config.apiEndpoint}/tasks/sprint/${sprint.sprintId}`
            );

            let tasksData;

            // Handle different response formats
            if (response.data?.data) {
              tasksData = response.data.data;
            } else if (Array.isArray(response.data)) {
              tasksData = response.data;
            } else {
              tasksData = [];
            }

            // Map the tasks data to our Task type
            const tasks = tasksData.map((task: ApiTask) => ({
              id: task.id,
              title: task.title || `Task ${task.id}`,
              estimatedHours: task.estimatedHours || 0,
              actualHours: task.actualHours || 0,
              assigneeId: task.assigneeId || 0,
              assigneeName:
                task.assigneeName ||
                users[task.assigneeId || 0] ||
                `User ${task.assigneeId}`,
              status: task.status || 'Unknown',
              priority: task.priority || 'Medium',
              sprintId: task.sprintId || sprint.sprintId,
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

        // Set the first sprint as expanded by default if there are sprints and no sprint is expanded
        if (formattedSprintsData.length > 0 && expandedSprints.size === 0) {
          setExpandedSprints(new Set([formattedSprintsData[0].id]));
        }
      } catch (err) {
        console.error('Error fetching tasks:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchTasksForSprints();
  }, [sprints, users]); // Add users to the dependency array, remove expandedSprints to prevent re-fetching

  // Helper function to get user name
  const getUserName = (assigneeId: number) => {
    const fullName = users[assigneeId] || `User ${assigneeId}`;
    const nameParts = fullName.split(' ');

    // Get only first name and first last name
    const firstName = nameParts[0] || '';
    const lastName = nameParts.length > 1 ? nameParts[1] : '';

    return `${firstName} ${lastName}`.trim();
  };

  // Toggle sprint expansion
  const toggleSprint = (sprintId: number) => {
    setExpandedSprints((prevExpanded) => {
      const newExpanded = new Set(prevExpanded);
      if (newExpanded.has(sprintId)) {
        newExpanded.delete(sprintId);
      } else {
        newExpanded.add(sprintId);
      }
      return newExpanded;
    });
  };

  // Function to check if a sprint is expanded
  const isSprintExpanded = (sprintId: number): boolean => {
    return expandedSprints.has(sprintId);
  };

  return (
    <div className="w-full h-full flex flex-col p-5 bg-white rounded-xl shadow-lg">
      <div className="flex flex-row text-2xl gap-4 w-full items-center mb-4">
        <ClipboardList className="w-6 h-6" />
        <KPITitle
          title="Task Information by Sprint"
          KPIObject={{ definition, example }}
        />
      </div>

      <div className="flex-1 overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center h-40">
            <div className="h-28/50 w-28/50">
              <LoadingSpinner />
            </div>
          </div>
        ) : formattedSprints.length === 0 ? (
          <div className="text-center text-gray-500 py-8">
            No sprint data available.
          </div>
        ) : (
          <div className="h-full flex flex-col overflow-y-auto pr-2">
            {formattedSprints.map((sprint) => (
              <div
                key={sprint.id}
                className="w-full flex flex-col border-b border-gray-200 mb-2 pb-2 last:border-b-0"
              >
                <div
                  className="flex items-center justify-between py-2 px-4 bg-gray-50 rounded-lg cursor-pointer hover:bg-gray-100"
                  onClick={() => toggleSprint(sprint.id)}
                >
                  <div className="flex items-center">
                    <p className="text-xl font-semibold">{sprint.name}</p>
                    <span className="ml-3 bg-blue-100 text-blue-800 text-xs font-medium px-2.5 py-0.5 rounded">
                      {sprint.tasks.length} tasks
                    </span>
                  </div>
                  <div className="text-gray-500">
                    {isSprintExpanded(sprint.id) ? '▼' : '▶'}
                  </div>
                </div>

                {isSprintExpanded(sprint.id) && (
                  <div className="max-h-[200px] overflow-y-auto mt-2 px-4">
                    {sprint.tasks.length === 0 ? (
                      <div className="text-center text-gray-500 py-4">
                        No tasks in this sprint.
                      </div>
                    ) : (
                      <div className="flex flex-col gap-3">
                        {sprint.tasks.map((task) => (
                          <div
                            key={task.id}
                            className="flex flex-col sm:flex-row sm:items-center justify-between bg-white p-3 rounded-lg shadow-sm border border-gray-100"
                          >
                            <div className="flex flex-col gap-1 mb-2 sm:mb-0">
                              <div className="flex flex-wrap gap-2 items-center">
                                <p className="font-semibold text-md truncate max-w-[170px] sm:max-w-[200px]">
                                  {task.title}
                                </p>
                                <span className="text-sm text-gray-600">
                                  {getUserName(task.assigneeId)}
                                </span>
                              </div>
                              <div className="flex flex-wrap gap-2 mt-1 text-sm">
                                <span
                                  className={`inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium ${
                                    task.status === 'DONE' ||
                                    task.status === 'COMPLETED'
                                      ? 'bg-green-100 text-green-800'
                                      : task.status === 'IN_PROGRESS'
                                      ? 'bg-blue-100 text-blue-800'
                                      : 'bg-red-100 text-red-800'
                                  }`}
                                >
                                  {task.status}
                                </span>
                                <span
                                  className={`inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium ${
                                    task.priority === 'HIGH'
                                      ? 'bg-red-100 text-red-800'
                                      : task.priority === 'MEDIUM'
                                      ? 'bg-yellow-100 text-yellow-800'
                                      : 'bg-gray-100 text-gray-800'
                                  }`}
                                >
                                  {task.priority}
                                </span>
                              </div>
                            </div>
                            <div className="flex items-center gap-3 text-sm text-gray-700">
                              <div className="text-right">
                                <div>Est: {task.estimatedHours}h</div>
                                <div>Act: {task.actualHours}h</div>
                              </div>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
