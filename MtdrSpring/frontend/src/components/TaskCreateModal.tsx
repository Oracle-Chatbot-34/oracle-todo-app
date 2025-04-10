import { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import taskService, { Task } from '../services/tasksService';
import userService, { User } from '../services/userService';
import teamService, { Team } from '../services/teamService';

interface TaskCreateModalProps {
  isOpen: boolean;
  onClose: () => void;
  onTaskCreated: (task: Task) => void;
}

export default function TaskCreateModal({
  isOpen,
  onClose,
  onTaskCreated,
}: TaskCreateModalProps) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [dueDate, setDueDate] = useState('');
  const [priority, setPriority] = useState('MEDIUM');
  const [estimatedHours, setEstimatedHours] = useState('');
  const [assigneeId, setAssigneeId] = useState('');
  const [teamId, setTeamId] = useState('');

  const [users, setUsers] = useState<User[]>([]);
  const [teams, setTeams] = useState<Team[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    // Fetch users and teams for dropdown selections
    const fetchData = async () => {
      try {
        const [usersResponse, teamsResponse] = await Promise.all([
          userService.getAllUsers(),
          teamService.getAllTeams(),
        ]);

        setUsers(usersResponse);
        setTeams(teamsResponse);
      } catch (err) {
        console.error('Error fetching data for task creation:', err);
        setError('Failed to load users and teams data');
      }
    };

    if (isOpen) {
      fetchData();
    }
  }, [isOpen]);

  const resetForm = () => {
    setTitle('');
    setDescription('');
    setDueDate('');
    setPriority('MEDIUM');
    setEstimatedHours('');
    setAssigneeId('');
    setTeamId('');
    setError('');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      setLoading(true);
      setError('');

      // Validate required fields
      if (!title) {
        setError('Title is required');
        setLoading(false);
        return;
      }

      // Create task object
      const newTask: Task = {
        title,
        description,
        priority,
        status: 'BACKLOG',
      };

      // Add optional fields if they're filled in
      if (dueDate) {
        newTask.dueDate = new Date(dueDate).toISOString();
      }

      if (estimatedHours && !isNaN(parseFloat(estimatedHours))) {
        newTask.estimatedHours = parseFloat(estimatedHours);
      }

      if (assigneeId) {
        newTask.assigneeId = parseInt(assigneeId);
      }

      if (teamId) {
        newTask.teamId = parseInt(teamId);
      }

      // Create task with estimation (to validate estimated hours)
      const taskId = await taskService.createTaskWithEstimation(newTask);

      // If we have the ID, fetch the complete task
      if (taskId) {
        const createdTask = await taskService.getTaskById(taskId);
        onTaskCreated(createdTask);
      }

      // Close modal and reset form
      resetForm();
      onClose();
    } catch (err: unknown) {
      console.error('Error creating task:', err);
      setError(
        err instanceof Error 
          ? err.message 
          : typeof err === 'object' && err !== null && 'response' in err && err.response && typeof err.response === 'object' && 'data' in err.response
            ? String(err.response.data)
            : 'Failed to create task. Please try again.'
      );
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 w-full max-w-md">
        <h2 className="text-2xl font-bold mb-4">Create New Task</h2>

        {error && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700">
                Title *
              </label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary focus:border-primary"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700">
                Description
              </label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={3}
                className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary focus:border-primary"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700">
                Due Date
              </label>
              <input
                type="date"
                value={dueDate}
                onChange={(e) => setDueDate(e.target.value)}
                className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary focus:border-primary"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700">
                Priority
              </label>
              <select
                value={priority}
                onChange={(e) => setPriority(e.target.value)}
                className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary focus:border-primary"
              >
                <option value="HIGH">High</option>
                <option value="MEDIUM">Medium</option>
                <option value="LOW">Low</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700">
                Estimated Hours
              </label>
              <input
                type="number"
                min="0.5"
                max="4"
                step="0.5"
                value={estimatedHours}
                onChange={(e) => setEstimatedHours(e.target.value)}
                className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary focus:border-primary"
              />
              <p className="text-xs text-gray-500 mt-1">
                Maximum 4 hours per task
              </p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700">
                Assignee
              </label>
              <select
                value={assigneeId}
                onChange={(e) => setAssigneeId(e.target.value)}
                className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary focus:border-primary"
              >
                <option value="">Unassigned</option>
                {users.map((user) => (
                  <option key={user.id} value={user.id}>
                    {user.fullName}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700">
                Team
              </label>
              <select
                value={teamId}
                onChange={(e) => setTeamId(e.target.value)}
                className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary focus:border-primary"
              >
                <option value="">No Team</option>
                {teams.map((team) => (
                  <option key={team.id} value={team.id}>
                    {team.name}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="flex justify-end gap-2 mt-6">
            <Button
              type="button"
              variant="outline"
              onClick={() => {
                resetForm();
                onClose();
              }}
              disabled={loading}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={loading}>
              {loading ? 'Creating...' : 'Create Task'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
