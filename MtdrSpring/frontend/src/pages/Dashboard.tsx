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

export default function Dashboard() {
  const navigate = useNavigate();
  const { isAuthenticated, username } = useAuth();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [tasks, setTasks] = useState<Task[]>([]);
  const [dataPie, setDataPie] = useState<number[]>([0, 0, 0]);
  const [dataLine, setDataLine] = useState({
    data: [] as number[],
    categories: [] as string[],
  });
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
            onTime++; // If no due date, consider it on time
          } else {
            const dueDate = new Date(task.dueDate);
            const daysUntilDue = Math.floor(
              (dueDate.getTime() - now.getTime()) / (1000 * 3600 * 24)
            );

            if (daysUntilDue < 0) {
              beyondDeadline++; // Already past due
            } else if (daysUntilDue <= 2) {
              behindSchedule++; // Due in 2 days or less
            } else {
              onTime++; // Due in more than 2 days
            }
          }
        });

        setDataPie([onTime, behindSchedule, beyondDeadline]);

        // Fetch KPI data for trend line
        const kpiData = await kpiService.getUserKpis(currentUserId);

        if (kpiData && kpiData.taskCompletionTrend && kpiData.trendLabels) {
          setDataLine({
            data: kpiData.taskCompletionTrend,
            categories: kpiData.trendLabels,
          });
        }
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
    <div className="bg-background h-screen w-full flex flex-row items-center justify-center gap-[40px]">
      <div className="bg-whitie w-[650px] h-[800px] rounded-lg shadow-xl">
        <br />
        <div className="flex flex-row items-center gap-[20px]">
          <br />
          <div className="w-[40px] h-[40px] rounded-lg flex items-center justify-center">
            <ChartArea className="w-[30px] h-[30px]" />
          </div>
          <p className="text-[24px] font-semibold">Analytics</p>
        </div>

        <div className="flex flex-col items-center gap-[20px]">
          {/* Chart goes here */}
          <div className="shadow-xl rounded-lg">
            {loading ? (
              <div className="h-80 flex items-center justify-center">
                <LoadingSpinner size={8} />
              </div>
            ) : (
              <PieChart data={dataPie} />
            )}
          </div>
          {/* Chart goes here */}
          <div className="shadow-xl rounded-lg">
            {loading ? (
              <div className="h-80 flex items-center justify-center">
                <LoadingSpinner size={8} />
              </div>
            ) : (
              <TasksTime
                data={dataLine.data}
                categories={dataLine.categories}
              />
            )}
          </div>
        </div>
      </div>
      <div className="flex flex-col items-center gap-[20px]">
        <div className="bg-whitie w-[650px] h-[570px] rounded-lg shadow-xl">
          <br />
          <div className="flex flex-row items-center gap-[20px]">
            <br />
            <div className="w-[40px] h-[40px] rounded-lg flex items-center justify-center">
              <CheckCheck className="w-[30px] h-[30px]" />
            </div>
            <p className="text-[24px] font-semibold">Latest tasks</p>
          </div>
          <div className="flex flex-col items-center gap-[30px]">
            {/* Task list */}
            <div>
              {loading ? (
                <div className="h-80 flex items-center justify-center">
                  <LoadingSpinner size={8} />
                </div>
              ) : error ? (
                <div className="text-red-500 text-center p-4">{error}</div>
              ) : tasks.length === 0 ? (
                <div className="text-center p-4">No tasks found</div>
              ) : (
                tasks.map((task) => (
                  <TaskDashCard
                    key={task.ID}
                    id={task.ID}
                    title={task.title}
                    dueDate={
                      task.dueDate
                        ? new Date(task.dueDate).toLocaleDateString()
                        : 'No due date'
                    }
                    assignedTo={getAssigneeName(task)}
                  />
                ))
              )}
            </div>
            <div className="bg-greyie rounded-lg text-black text-[20px] font-semibold shadow-xl">
              <Button
                className="h-[40px] w-54 text-xl"
                size={'lg'}
                onClick={navigateToTasks}
              >
                Manage all tasks
              </Button>
            </div>
          </div>
        </div>
        <div className="bg-whitie w-[650px] h-[205px] rounded-lg shadow-xl p-4">
          {/* Active task */}
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
  );
}
