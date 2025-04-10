import { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import taskService, { Task } from '@/services/tasksService';
import userService, { User } from '../services/userService';
import teamService, { Team } from '../services/teamService';

interface TaskUpdateModalProps {
  isOpen: boolean;
  onClose: () => void;
  onTaskUpdated: (task: Task) => void;
  task: Task | null;
}

export default function TaskUpdateModal({
  isOpen,
  onClose,
  onTaskUpdated,
  task,
}: TaskUpdateModalProps) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [dueDate, setDueDate] = useState('');
  const [priority, setPriority] = useState('MEDIUM');
  const [status, setStatus] = useState('');
  const [estimatedHours, setEstimatedHours] = useState('');
  const [assigneeId, setAssigneeId] = useState('');
  const [teamId, setTeamId] = useState('');

  const [users, setUsers] = useState<User[]>([]);
  const [teams, setTeams] = useState<Team[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Initialize form with task data when it changes
  useEffect(() => {
    if (task) {
      setTitle(task.title || '');
      setDescription(task.description || '');
      setDueDate(
        task.dueDate
          ? new Date(task.dueDate).toISOString().substring(0, 10)
          : ''
      );
      setPriority(task.priority || 'MEDIUM');
      setStatus(task.status || '');
      setEstimatedHours(
        task.estimatedHours ? task.estimatedHours.toString() : ''
      );
      setAssigneeId(task.assigneeId ? task.assigneeId.toString() : '');
      setTeamId(task.teamId ? task.teamId.toString() : '');
    }
  }, [task]);

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
        console.error('Error fetching data for task update:', err);
        setError('Failed to load users and teams data');
      }
    };

    if (isOpen) {
      fetchData();
    }
  }, [isOpen]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!task || !task.ID) {
      setError('Invalid task data');
      return;
    }

    try {
      setLoading(true);
      setError('');

      // Validate required fields
      if (!title) {
        setError('Title is required');
        setLoading(false);
        return;
      }

      // Create updated task object
      const updatedTask: Task = {
        ...task,
        title,
        description,
        priority,
        status,
      };

      // Add optional fields if they're filled in
      if (dueDate) {
        updatedTask.dueDate = new Date(dueDate).toISOString();
      }

      if (estimatedHours && !isNaN(parseFloat(estimatedHours))) {
        updatedTask.estimatedHours = parseFloat(estimatedHours);
      }

      if (assigneeId) {
        updatedTask.assigneeId = parseInt(assigneeId);
      } else {
        updatedTask.assigneeId = undefined;
      }

      if (teamId) {
        updatedTask.teamId = parseInt(teamId);
      } else {
        updatedTask.teamId = undefined;
      }

      // Update the task
      const result = await taskService.updateTask(task.ID, updatedTask);

      onTaskUpdated(result);

      // Close modal
      onClose();
    } catch (err: unknown) {
      console.error('Error updating task:', err);
      const errorMessage = 
        err instanceof Error ? err.message : 
        typeof err === 'object' && err && 'response' in err && err.response && typeof err.response === 'object' && 'data' in err.response 
          ? String(err.response.data) 
          : 'Failed to update task. Please try again.';
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen || !task) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 w-full max-w-md">
        <h2 className="text-2xl font-bold mb-4">Update Task</h2>

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
                Status
              </label>
              <select
                value={status}
                onChange={(e) => setStatus(e.target.value)}
                className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary focus:border-primary"
              >
                <option value="">Select Status</option>
                <option value="BACKLOG">Backlog</option>
                <option value="SELECTED_FOR_DEVELOPMENT">
                  Selected for Development
                </option>
                <option value="IN_PROGRESS">In Progress</option>
                <option value="COMPLETED">Completed</option>
                <option value="DONE">Done</option>
                <option value="DELAYED">Delayed</option>
              </select>
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
              onClick={onClose}
              disabled={loading}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={loading}>
              {loading ? 'Updating...' : 'Update Task'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
