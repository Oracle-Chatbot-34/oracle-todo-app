import { useState, useEffect } from 'react';
import LineComponent from '@/components/kpis/LineComponent';
import TaskDashCard from '../components/TaskDashCard';
import { useNavigate } from 'react-router-dom';
import { AlarmClock, CalendarClock, Info, LayoutList } from 'lucide-react';
import taskService, { Task } from '@/services/tasksService';
import userService from '../services/userService';
import LoadingSpinner from '@/components/LoadingSpinner';
import { useAuth } from '@/hooks/useAuth';

type LineChartType = {
  month: string;
  avg: number;
};

export default function Dashboard() {
  const navigate = useNavigate();
  const { isAuthenticated, username } = useAuth();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [tasks, setTasks] = useState<Task[]>([]);

  const [currentUserId, setCurrentUserId] = useState<number | null>(null);

  // Fetch user ID based on username
  useEffect(() => {
    if (!isAuthenticated || !username) return;

    const fetchCurrentUser = async () => {
      try {
        const users = await userService.getAllUsers();
        const currentUser = users.find((user) => user.username === username);
        if (currentUser && currentUser.id !== undefined) {
          setCurrentUserId(currentUser.id);
        }
      } catch (err) {
        console.error('Error fetching current user:', err);
      }
    };

    fetchCurrentUser();
  }, [isAuthenticated, username]);

  // Fetch dashboard data
  useEffect(() => {
    if (!isAuthenticated || !currentUserId) return;

    const fetchDashboardData = async () => {
      try {
        setLoading(true);

        // Fetch tasks
        const allTasks = await taskService.getAllTasks();

        // Filter to most recent tasks
        const sortedTasks = [...allTasks].sort((a, b) => {
          const dateA = a.creation_ts ? new Date(a.creation_ts).getTime() : 0;
          const dateB = b.creation_ts ? new Date(b.creation_ts).getTime() : 0;
          return dateB - dateA; // Newest first
        });

        // Filter tasks by not completed
        const notCompletedTasks = sortedTasks.filter(
          (task) => task.status !== 'completed'
        );

        setTasks(notCompletedTasks.slice(0, 5)); // Get latest 3 tasks
      } catch (err) {
        console.error('Error fetching dashboard data:', err);
        setError('Failed to load dashboard data');
      } finally {
        setLoading(false);
      }
    };

    fetchDashboardData();
  }, [isAuthenticated, currentUserId]);

  // Find assignee name from task
  const getAssigneeName = (task: Task) => {
    return task.assigneeId ? `ID: ${task.assigneeId}` : 'Unassigned';
  };

  return (
    <div className="bg-background h-full w-full p-6 lg:px-10 py-10 flex items-start justify-center overflow-clip">
      <div className="flex flex-col justify-center w-1/3 h-full">
        <div className="w-full h-1/3 flex flex-col justify-center items-center">
          <p className="text-4xl">Welcome to DashMaster</p>
          {error && (
            <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded w-full">
              {error}
            </div>
          )}
        </div>
        <div className="w-full h-2/3 bg-whitie rounded-tl-xl rounded-bl-xl shadow-accent pl-6 pr-1 py-6">
          <div className="w-full h-full bg-white rounded-xl shadow-xl flex flex-col">
            {/* Title */}
            <div className="flex flex-row gap-3 p-4 items-center">
              <LayoutList className="w-8 h-8" />
              <p className="text-2xl font-semibold">Current Sprint Info</p>
            </div>

            {/* Loading Spinner */}
            {loading ? (
              <div className="flex items-center justify-center h-1/3">
                <LoadingSpinner />
              </div>
            ) : (
              <div className="w-full h-full flex flex-col gap-5 px-8 mt-5">
                <div className="flex flex-row gap-4 items-center">
                  <Info className="w-6 h-6" />
                  <p className="text-xl font-semibold">General Information</p>
                </div>

                <div className="flex flex-col gap-3 text-xl px-4">
                  <p>
                    Sprint Name: {/*sprint.name*/}
                    {'Backend Sprint 1'}
                  </p>
                  <p>
                    Description: {/*sprint.description*/}
                    {'Backend Sprint 1 for the new feature X'}
                  </p>
                </div>

                <div className="flex flex-row gap-4 items-center">
                  <AlarmClock className="w-6 h-6" />
                  <p className="text-xl font-semibold">Important Dates</p>
                </div>

                <div className="flex flex-col gap-3 text-xl px-4">
                  <p>
                    Start date: {/*sprint.startdate*/}
                    {'2025-04-10'}
                  </p>
                  <p>
                    End date: {/*sprint.dueDate*/}
                    {'2025-05-05'}
                  </p>
                </div>

                <div className="flex flex-row gap-4 items-center">
                  <AlarmClock className="w-6 h-6" />
                  <p className="text-xl font-semibold">Sprint Progression</p>
                </div>

                <div className="h-1/5 flex flex-col gap-3 text-xl px-4">
                  <p>There are {3} days left in the current spring.</p>
                  <LineComponent percentage={55} />
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
      <div className="w-2/3 h-full bg-whitie rounded-tl-xl rounded-br-xl rounded-tr-xl shadow-accent p-6">
        <div className="flex flex-col gap-5 p-4 items-start bg-white rounded-xl shadow-xl w-full h-full">
          <div className="flex flex-row gap-4">
            <CalendarClock className="w-8 h-8" />
            <p className="text-2xl font-semibold">Not Completed Tasks!</p>
          </div>
          {tasks.length > 0 ? (
            <div className="w-full flex flex-col gap-5">
              <div className="flex flex-col gap-6 mb-3">
                {tasks.map((task) => (
                  <TaskDashCard
                    key={task.id}
                    id={task.id}
                    title={task.title}
                    assignedTo={task.assigneeId}
                    dueDate={task.dueDate}
                  />
                ))}
              </div>
              <div className="flex flex-row gap-3 text-2xl p-3 w-full font italic items-center justify-between">
                <Info className="w-6 h-6" />
                <div className="flex flex-row">
                  This list is view only, if you wish to manage tasks, go to:
                  <button
                    onClick={() => navigate('/tasks')}
                    className="font-semibold underline"
                  >
                    Task Management
                  </button>
                </div>
              </div>
            </div>
          ) : (
            <div className="h-40 flex items-center justify-center">
              <p>No data available</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
