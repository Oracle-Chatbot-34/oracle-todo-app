import { useState, useEffect } from 'react';
import PieChart from '../components/PieChart';
import TasksTime from '../components/TasksTime';
import TaskDashCard from '../components/TaskDashCard';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { ChartArea, CheckCheck } from 'lucide-react';
import taskService, { Task } from '@/services/tasksService';
import kpiService from '../services/kpiService';
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
  const [dataPie, setDataPie] = useState<number[]>([0, 0, 0]);
  const [dataLine, setDataLine] = useState<LineChartType[]>([]);

  const [currentUserId, setCurrentUserId] = useState<number | null>(null);

  // Navigate to the tasks page
  const navigateToTasks = () => {
    navigate('/tasks');
  };

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

        setTasks(sortedTasks.slice(0, 3)); // Get latest 3 tasks

        // Get task status for pie chart
        const now = new Date();
        const activeTasks = allTasks.filter((task) => !task.done);

        let onTime = 0;
        let behindSchedule = 0;
        let beyondDeadline = 0;

        activeTasks.forEach((task) => {
          if (!task.dueDate) {
            onTime++; // No due date -> consider on time
          } else {
            const dueDate = new Date(task.dueDate);
            const daysUntilDue = Math.floor(
              (dueDate.getTime() - now.getTime()) / (1000 * 3600 * 24)
            );

            if (daysUntilDue < 0) {
              beyondDeadline++; // Past due
            } else if (daysUntilDue <= 2) {
              behindSchedule++; // Due soon
            } else {
              onTime++; // Plenty of time
            }
          }
        });
        setDataPie([onTime, behindSchedule, beyondDeadline]);

        // Fetch KPI data for trend line
        // const kpiData = await kpiService.getTeamKpis(currentUserId);
        const kpiData = [
          {
            month: 'January',
            avg: 20,
          },

          {
            month: 'February',
            avg: 10,
          },

          {
            month: 'March',
            avg: 15,
          },
        ];
        setDataLine(kpiData);
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
    <div className="bg-background h-full w-full p-6 lg:px-10 py-10 flex items-start justify-center overflow-clip gap-5">
      <div className="flex flex-col justify-center p-4 lg:p-6 gap-y-4 bg-whitie w-1/2 h-full rounded-lg shadow-xl ">
        <div className="flex flex-row items-center gap-4">
          <ChartArea />
          <p className="text-2xl">Analytics</p>
        </div>
        <div className="flex flex-col h-full w-full gap-4 ">
          <div className="w-full h-1/2 bg-whitiish2 rounded-xl shadow-lg flex items-center justify-center">
            {!loading ? (
              <PieChart data={dataPie} />
            ) : (
              <div className="h-40 w-40 ">
                <LoadingSpinner />
              </div>
            )}
          </div>

          <div className="w-full h-1/2 bg-whitiish2 rounded-xl shadow-lg flex items-center justify-center">
            {!loading ? (
              <TasksTime data={dataLine} />
            ) : (
              <div className="h-40 w-40">
                <LoadingSpinner />
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="flex flex-col justify-center p-4 lg:p-6 gap-y-4 bg-whitie w-1/2 h-full rounded-lg shadow-xl ">
        <div className="flex flex-row items-center gap-4">
          <CheckCheck />
          <p className="text-2xl">Latest Tasks</p>
        </div>

        <div className="flex flex-col h-full w-full gap-4 ">
          <div className="w-full bg-whitiish2 rounded-xl shadow-lg ">
            <div className="flex flex-col p-5 gap-3 items-center justify-center">
              {!loading ? (
                <>
                  {tasks.map((task) => (
                    <TaskDashCard
                      key={task.id}
                      id={task.id}
                      title={task.title}
                      dueDate={
                        task.dueDate
                          ? new Date(task.dueDate).toLocaleString()
                          : 'No due date'
                      }
                      assignedTo={getAssigneeName(task)}
                    />
                  ))}
                  <Button
                    className="h-13 w-54 text-2xl"
                    size="lg"
                    onClick={navigateToTasks}
                  >
                    Manage all tasks
                  </Button>
                </>
              ) : (
                <div className="w-40 h-40">
                  <LoadingSpinner />
                </div>
              )}
            </div>
          </div>

          <div className="w-full h-full bg-whitiish2 rounded-xl shadow-lg flex items-center justify-center">
            <div className="flex flex-col items-center justify-center h-full">
              <h2 className="text-2xl font-bold mb-4">Welcome to DashMaster</h2>
              <p className="text-lg text-center">
                Your one-stop solution for task management and team productivity
                tracking.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
