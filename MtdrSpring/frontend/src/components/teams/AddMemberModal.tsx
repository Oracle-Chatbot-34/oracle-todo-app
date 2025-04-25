import { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import teamService from '@/services/teamService';
import userService, { User } from '@/services/userService';

interface AddMemberModalProps {
  isOpen: boolean;
  teamId: number;
  onClose: () => void;
  onMemberAdded: () => void;
}

export default function AddMemberModal({
  isOpen,
  teamId,
  onClose,
  onMemberAdded,
}: AddMemberModalProps) {
  const [users, setUsers] = useState<User[]>([]);
  const [selectedUserId, setSelectedUserId] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchAvailableUsers = async () => {
      if (!isOpen || !teamId) return;

      try {
        setLoading(true);

        // Get all users
        const allUsers = await userService.getAllUsers();

        // Get team members
        const teamMembers = await teamService.getTeamMembers(teamId);

        // Filter out users already in the team
        const teamMemberIds = teamMembers.map((member) => member.id);
        const availableUsers = allUsers.filter(
          (user) => !teamMemberIds.includes(user.id)
        );

        setUsers(availableUsers);
        // Reset selected user
        setSelectedUserId('');
      } catch (err) {
        console.error('Error fetching available users:', err);
        setError('Failed to load available users');
      } finally {
        setLoading(false);
      }
    };

    fetchAvailableUsers();
  }, [isOpen, teamId]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!selectedUserId) {
      setError('Please select a user to add');
      return;
    }

    try {
      setLoading(true);
      setError('');

      await teamService.addMemberToTeam(teamId, parseInt(selectedUserId));

      onMemberAdded();
      onClose();
    } catch (err) {
      console.error('Error adding member to team:', err);
      setError('Failed to add member to team');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 w-full max-w-md">
        <h2 className="text-2xl font-bold mb-4">Add Team Member</h2>

        {error && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700">
                Select User
              </label>
              <select
                value={selectedUserId}
                onChange={(e) => setSelectedUserId(e.target.value)}
                className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary focus:border-primary"
                disabled={loading || users.length === 0}
              >
                <option value="">Select a user</option>
                {users.map((user) => (
                  <option key={user.id} value={user.id}>
                    {user.fullName} - {user.role}
                  </option>
                ))}
              </select>

              {users.length === 0 && !loading && (
                <p className="text-sm text-gray-500 mt-1">
                  No available users to add to this team.
                </p>
              )}
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
            <Button
              type="submit"
              disabled={loading || !selectedUserId || users.length === 0}
            >
              {loading ? 'Adding...' : 'Add to Team'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
