import { useState } from 'react';
import teamService from '@/services/teamService';

type TeamMemberCardProps = {
  id: number;
  name: string;
  role: string;
  teamId?: number;
  onMemberRemoved?: () => void;
};

export default function TeamMemberCard({
  id,
  name,
  role,
  teamId,
  onMemberRemoved,
}: TeamMemberCardProps) {
  const [isPopupOpen, setIsPopupOpen] = useState(false);
  const [isRemoving, setIsRemoving] = useState(false);
  const [error, setError] = useState('');

  const handleRemoveMember = async () => {
    if (!teamId) {
      setError('Team ID is required to remove a member');
      return;
    }

    try {
      setIsRemoving(true);
      await teamService.removeMemberFromTeam(teamId, id);
      setIsPopupOpen(false);
      if (onMemberRemoved) {
        onMemberRemoved();
      }
    } catch (err) {
      console.error(`Error removing member ${id} from team ${teamId}:`, err);
      setError('Failed to remove member. Please try again.');
    } finally {
      setIsRemoving(false);
    }
  };

  return (
    <div
      className={
        'p-5 rounded-lg flex flex-row gap-x-4 bg-greyie text-black justify-between items-center'
      }
      key={id}
    >
      {isPopupOpen && (
        <div className="fixed inset-0 flex items-center justify-center w-full bg-black/70 z-20">
          <div className="bg-white p-6 rounded-lg shadow-lg max-w-md">
            <p className="text-gray-700">
              Are you sure you want to remove this person from the team? You can
              add them back if you change your mind.
            </p>
            {error && (
              <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mt-2">
                {error}
              </div>
            )}
            <div className="flex flex-row justify-between mt-4">
              <button
                className="px-4 py-2 bg-redie text-white rounded hover:bg-red-700 disabled:opacity-50"
                onClick={handleRemoveMember}
                disabled={isRemoving}
              >
                {isRemoving ? 'Removing...' : 'Remove'}
              </button>
              <button
                className="px-4 py-2 bg-black/50 text-white rounded hover:bg-black/40"
                onClick={() => setIsPopupOpen(false)}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
      <div>
        <p className="text-3xl">{name}</p>

        <p className="text-xl italic">{role}</p>
      </div>

      <button
        className="bg-redie rounded-lg flex flex-row justify-center items-center h-12 w-35 shadow-lg"
        onClick={() => setIsPopupOpen(true)}
      >
        <span className="text-2xl text-white">Remove</span>
      </button>
    </div>
  );
}
