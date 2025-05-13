import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Calendar,
  CheckCircle,
  Clock,
  Info,
  LayoutList,
  AlertTriangle,
  Users,
} from 'lucide-react';
import taskService, { Task } from '@/services/tasksService';
import userService, { User } from '../services/userService';
import sprintService, { Sprint } from '@/services/sprintService';
import LoadingSpinner from '@/components/LoadingSpinner';
import { useAuth } from '@/hooks/useAuth';
import { format } from 'date-fns';

export default function Dashboard() {
  const navigate = useNavigate();
  const { isAuthenticated, username } = useAuth();

  // State management
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [tasks, setTasks] = useState<Task[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [activeSprint, setActiveSprint] = useState<Sprint | null>(null);
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [sprintProgress, setSprintProgress] = useState(0);
  const [taskStatusCounts, setTaskStatusCounts] = useState({
    total: 0,
    completed: 0,
    inProgress: 0,
    notStarted: 0,
    delayed: 0,
  });

  // Find current user based on username
  useEffect(() => {
    if (!isAuthenticated || !username) return;

    const fetchCurrentUser = async () => {
      try {
        const allUsers = await userService.getAllUsers();
        setUsers(allUsers);

        const user = allUsers.find((user) => user.username === username);
        if (user) {
          setCurrentUser(user);
        }
      } catch (err) {
        console.error('Error fetching users:', err);
      }
    };

    fetchCurrentUser();
  }, [isAuthenticated, username]);

  // Fetch dashboard data
  useEffect(() => {
    if (!isAuthenticated) return;

    const fetchDashboardData = async () => {
      try {
        setLoading(true);
        setError('');

        // Fetch tasks
        const allTasks = await taskService.getAllTasks();

        // Sort by due date and filter for not completed
        const sortedTasks = [...allTasks].sort((a, b) => {
          const dateA = a.dueDate ? new Date(a.dueDate).getTime() : Infinity;
          const dateB = b.dueDate ? new Date(b.dueDate).getTime() : Infinity;
          return dateA - dateB; // Earliest due date first
        });

        // Get not completed tasks
        const notCompletedTasks = sortedTasks.filter(
          (task) => task.status !== 'COMPLETED' && task.status !== 'DONE'
        );

        setTasks(notCompletedTasks.slice(0, 5)); // Get latest 5 tasks

        // Calculate task status counts
        const counts = {
          total: allTasks.length,
          completed: allTasks.filter(
            (t) => t.status === 'COMPLETED' || t.status === 'DONE'
          ).length,
          inProgress: allTasks.filter((t) => t.status === 'IN_PROGRESS').length,
          notStarted: allTasks.filter(
            (t) =>
              t.status === 'BACKLOG' || t.status === 'SELECTED_FOR_DEVELOPMENT'
          ).length,
          delayed: allTasks.filter((t) => t.status === 'DELAYED').length,
        };

        setTaskStatusCounts(counts);

        // If user belongs to a team, fetch the active sprint
        if (currentUser && currentUser.teamId) {
          try {
            const sprint = await sprintService.getActiveSprintForTeam(
              currentUser.teamId
            );
            if (sprint) {
              setActiveSprint(sprint);

              // Calculate sprint progress
              if (sprint.startDate && sprint.endDate) {
                const startDate = new Date(sprint.startDate);
                const endDate = new Date(sprint.endDate);
                const today = new Date();

                const totalDuration = endDate.getTime() - startDate.getTime();
                const elapsedDuration = today.getTime() - startDate.getTime();

                if (elapsedDuration <= 0) {
                  setSprintProgress(0);
                } else if (elapsedDuration >= totalDuration) {
                  setSprintProgress(100);
                } else {
                  setSprintProgress(
                    Math.round((elapsedDuration / totalDuration) * 100)
                  );
                }
              }
            }
          } catch (err) {
            console.error('Error fetching active sprint:', err);

            // Fallback data for demo
            setActiveSprint({
              id: 1,
              name: 'Backend Sprint 1',
              description: 'Backend Sprint 1 for the new feature X',
              startDate: '2025-04-10',
              endDate: '2025-05-05',
              status: 'ACTIVE',
              teamId: currentUser.teamId,
            });
            setSprintProgress(55);
          }
        }
      } catch (err) {
        console.error('Error fetching dashboard data:', err);
        setError('Failed to load dashboard data');
      } finally {
        setLoading(false);
      }
    };

    fetchDashboardData();
  }, [isAuthenticated, currentUser]);

  // Get assignee name from task
  const getAssigneeName = (assigneeId?: number) => {
    if (!assigneeId) return 'Unassigned';
    const user = users.find((user) => user.id === assigneeId);
    return user ? user.fullName : `Unknown (ID: ${assigneeId})`;
  };

  // Format date for display
  const formatDate = (dateString?: string) => {
    if (!dateString) return 'No date';
    try {
      return format(new Date(dateString), 'MMM d, yyyy');
    } catch {
      return 'Invalid date';
    }
  };

  // Get days left in sprint
  const getDaysLeft = () => {
    if (!activeSprint || !activeSprint.endDate) return 0;

    const endDate = new Date(activeSprint.endDate);
    const today = new Date();

    // Calculate the difference in days
    const diffTime = endDate.getTime() - today.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

    return diffDays > 0 ? diffDays : 0;
  };

  // Get status badge color
  const getStatusColor = (status?: string) => {
    switch (status) {
      case 'COMPLETED':
      case 'DONE':
        return 'bg-green-100 text-green-800';
      case 'IN_PROGRESS':
        return 'bg-blue-100 text-blue-800';
      case 'DELAYED':
        return 'bg-yellow-100 text-yellow-800';
      case 'SELECTED_FOR_DEVELOPMENT':
        return 'bg-purple-100 text-purple-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  // Display priority badge
  const PriorityBadge = ({ priority }: { priority?: string }) => {
    if (!priority) return null;

    const getBadgeColor = () => {
      switch (priority) {
        case 'HIGH':
          return 'bg-red-100 text-red-800';
        case 'MEDIUM':
          return 'bg-yellow-100 text-yellow-800';
        case 'LOW':
          return 'bg-green-100 text-green-800';
        default:
          return 'bg-gray-100 text-gray-800';
      }
    };

    return (
      <span
        className={`px-2 py-1 rounded text-xs font-medium ${getBadgeColor()}`}
      >
        {priority}
      </span>
    );
  };

  return (
    <div className="bg-background h-full w-full p-6 lg:px-10 py-10 flex flex-col items-start justify-start overflow-y-auto">
      {/* Page Header */}
      <div className="w-full mb-6">
        <h1 className="text-4xl font-bold mb-2">Welcome to DashMaster</h1>
        <p className="text-gray-600">
          Your project management dashboard at a glance
        </p>
        {error && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mt-4">
            {error}
          </div>
        )}
      </div>

      {loading ? (
        <div className="w-full h-64 flex items-center justify-center">
          <LoadingSpinner />
        </div>
      ) : (
        <div className="w-full grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Sprint Information */}
          <div className="lg:col-span-1">
            <div className="bg-white rounded-xl shadow-lg p-6 h-full">
              <div className="flex items-center mb-4">
                <Calendar className="w-6 h-6 mr-2 text-primary" />
                <h2 className="text-2xl font-semibold">Current Sprint</h2>
              </div>

              {activeSprint ? (
                <div className="space-y-4">
                  <div>
                    <h3 className="font-medium text-gray-700">Sprint Info</h3>
                    <p className="text-xl font-bold mt-1">
                      {activeSprint.name}
                    </p>
                    <p className="text-gray-600 mt-1">
                      {activeSprint.description}
                    </p>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <h3 className="font-medium text-gray-700">Start Date</h3>
                      <p className="text-lg mt-1">
                        {formatDate(activeSprint.startDate)}
                      </p>
                    </div>
                    <div>
                      <h3 className="font-medium text-gray-700">End Date</h3>
                      <p className="text-lg mt-1">
                        {formatDate(activeSprint.endDate)}
                      </p>
                    </div>
                  </div>

                  <div>
                    <div className="flex justify-between mb-1">
                      <h3 className="font-medium text-gray-700">Progress</h3>
                      <span className="text-sm font-medium">
                        {sprintProgress}%
                      </span>
                    </div>
                    <div className="w-full bg-gray-200 rounded-full h-2.5">
                      <div
                        className="bg-primary h-2.5 rounded-full transition-all duration-500"
                        style={{ width: `${sprintProgress}%` }}
                      ></div>
                    </div>
                    <p className="text-gray-600 mt-2">
                      <Clock className="w-4 h-4 inline mr-1" />
                      {getDaysLeft()} days remaining
                    </p>
                  </div>
                </div>
              ) : (
                <div className="flex flex-col items-center justify-center h-48">
                  <Info className="w-12 h-12 text-gray-400 mb-2" />
                  <p className="text-gray-600 text-center">
                    No active sprint found
                  </p>
                  <button
                    onClick={() => navigate('/sprints')}
                    className="mt-4 px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90"
                  >
                    Go to Sprint Management
                  </button>
                </div>
              )}
            </div>
          </div>

          {/* Task Overview */}
          <div className="lg:col-span-2">
            <div className="bg-white rounded-xl shadow-lg p-6 h-full">
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center">
                  <LayoutList className="w-6 h-6 mr-2 text-primary" />
                  <h2 className="text-2xl font-semibold">Tasks Overview</h2>
                </div>
                <button
                  onClick={() => navigate('/tasks')}
                  className="text-primary hover:underline flex items-center"
                >
                  View All <Info className="w-4 h-4 ml-1" />
                </button>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
                <div className="bg-blue-50 rounded-lg p-4">
                  <h3 className="text-gray-700 mb-1">Total Tasks</h3>
                  <p className="text-3xl font-bold">{taskStatusCounts.total}</p>
                </div>
                <div className="bg-green-50 rounded-lg p-4">
                  <h3 className="text-gray-700 mb-1">Completed</h3>
                  <p className="text-3xl font-bold">
                    {taskStatusCounts.completed}
                  </p>
                </div>
                <div className="bg-yellow-50 rounded-lg p-4">
                  <h3 className="text-gray-700 mb-1">In Progress</h3>
                  <p className="text-3xl font-bold">
                    {taskStatusCounts.inProgress}
                  </p>
                </div>
                <div className="bg-red-50 rounded-lg p-4">
                  <h3 className="text-gray-700 mb-1">Delayed</h3>
                  <p className="text-3xl font-bold">
                    {taskStatusCounts.delayed}
                  </p>
                </div>
              </div>

              <h3 className="font-medium text-gray-700 mb-3">Upcoming Tasks</h3>

              {tasks.length > 0 ? (
                <div className="space-y-3">
                  {tasks.map((task) => (
                    <div
                      key={task.id || task.ID}
                      className="border rounded-lg p-4 hover:bg-gray-50 transition-colors"
                    >
                      <div className="flex justify-between items-start">
                        <div>
                          <h4 className="font-medium text-lg">{task.title}</h4>
                          <p className="text-gray-600 text-sm line-clamp-1 mt-1">
                            {task.description || 'No description'}
                          </p>
                        </div>
                        <div className="flex space-x-2">
                          <span
                            className={`px-2 py-1 rounded text-xs font-medium ${getStatusColor(
                              task.status
                            )}`}
                          >
                            {task.status?.replace(/_/g, ' ') || 'No Status'}
                          </span>
                          <PriorityBadge priority={task.priority} />
                        </div>
                      </div>

                      <div className="flex justify-between mt-3">
                        <div className="flex items-center">
                          <Users className="w-4 h-4 mr-1 text-gray-500" />
                          <span className="text-gray-700">
                            {getAssigneeName(task.assigneeId)}
                          </span>
                        </div>
                        {task.dueDate && (
                          <div
                            className={`flex items-center ${
                              new Date(task.dueDate) < new Date()
                                ? 'text-red-600'
                                : 'text-gray-600'
                            }`}
                          >
                            <Clock className="w-4 h-4 mr-1" />
                            <span>{formatDate(task.dueDate)}</span>
                            {new Date(task.dueDate) < new Date() && (
                              <AlertTriangle className="w-4 h-4 ml-1 text-red-600" />
                            )}
                          </div>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="flex flex-col items-center justify-center h-48 border rounded-lg">
                  <CheckCircle className="w-12 h-12 text-green-400 mb-2" />
                  <p className="text-gray-600 text-center">No pending tasks</p>
                  <button
                    onClick={() => navigate('/tasks')}
                    className="mt-4 px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90"
                  >
                    Create a task
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
