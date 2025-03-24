import { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import sprintService, { Sprint } from '@/services/sprintService';
import teamService, { Team } from '@/services/teamService';

interface SprintCreateModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSprintCreated: (sprint: Sprint) => void;
}

export default function SprintCreateModal({
  isOpen,
  onClose,
  onSprintCreated,
}: SprintCreateModalProps) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [teamId, setTeamId] = useState('');

  const [teams, setTeams] = useState<Team[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    // Fetch teams for dropdown selection
    const fetchTeams = async () => {
      try {
        const teamsResponse = await teamService.getAllTeams();
        setTeams(teamsResponse);
      } catch (err) {
        console.error('Error fetching teams for sprint creation:', err);
        setError('Failed to load teams data');
      }
    };

    if (isOpen) {
      fetchTeams();
    }
  }, [isOpen]);

  const resetForm = () => {
    setName('');
    setDescription('');
    setStartDate('');
    setEndDate('');
    setTeamId('');
    setError('');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      setLoading(true);
      setError('');

      // Validate required fields
      if (!name) {
        setError('Sprint name is required');
        setLoading(false);
        return;
      }

      if (!teamId) {
        setError('Team is required');
        setLoading(false);
        return;
      }

      // Create sprint object
      const newSprint: Sprint = {
        name,
        description,
        teamId: parseInt(teamId),
        status: 'planned',
      };

      // Add optional fields if they're filled in
      if (startDate) {
        newSprint.startDate = startDate;
      }

      if (endDate) {
        newSprint.endDate = endDate;
      }

      // Create sprint
      const createdSprint = await sprintService.createSprint(newSprint);
      onSprintCreated(createdSprint);

      // Close modal and reset form
      resetForm();
      onClose();
    } catch (err: unknown) {
      console.error('Error creating sprint:', err);
      setError(
        err instanceof Error
          ? err.message
          : typeof err === 'object' &&
            err !== null &&
            'response' in err &&
            err.response &&
            typeof err.response === 'object' &&
            'data' in err.response
          ? String(err.response.data)
          : 'Failed to create sprint. Please try again.'
      );
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 w-full max-w-md">
        <h2 className="text-2xl font-bold mb-4">Create New Sprint</h2>

        {error && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700">
                Sprint Name *
              </label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
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
                Start Date
              </label>
              <input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary focus:border-primary"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700">
                End Date
              </label>
              <input
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary focus:border-primary"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700">
                Team *
              </label>
              <select
                value={teamId}
                onChange={(e) => setTeamId(e.target.value)}
                className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary focus:border-primary"
                required
              >
                <option value="">Select Team</option>
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
              {loading ? 'Creating...' : 'Create Sprint'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
