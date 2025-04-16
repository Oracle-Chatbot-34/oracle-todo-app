import { UsersRound } from 'lucide-react';

import { User } from '@/services/userService';
import { Sprint } from '@/services/sprintService';

import teamService from '@/services/teamService';
import sprintService from '@/services/sprintService';

type TeamCardProps = {
  teamId: number;
  selectedTeam: number;
  setSelectedTeam: (value: number) => void;
  setIsLoading: (value: boolean) => void;
  setSelectedTeamMembers: React.Dispatch<React.SetStateAction<User[]>>;
  setSelectedTeamSprints: React.Dispatch<React.SetStateAction<Sprint[]>>;
  groupName: string;
};

export default function TeamCard({
  teamId,
  selectedTeam,
  groupName,
  setIsLoading,
  setSelectedTeam,
  setSelectedTeamMembers,
  setSelectedTeamSprints
}: TeamCardProps) {
  const handleClick = async () => {
    setSelectedTeam(teamId);
    // Fetch users from that team
    setIsLoading(true);

    const selectedTeamMembers = await teamService.getTeamMembers(teamId);
    setSelectedTeamMembers(selectedTeamMembers);

    // Fetch sprints from that team
    const selectedTeamSprints = await sprintService.getTeamSprints(teamId);
    setSelectedTeamSprints(selectedTeamSprints);
    
    setIsLoading(false);
  };
  return (
    <div
      key={teamId}
      className={`p-4 rounded-lg flex flex-row items-center gap-x-4 text-2xl cursor-pointer
        transition-all duration-300 ease-in-out justify-between
        ${
          selectedTeam === teamId
            ? 'bg-blue text-white'
            : 'bg-greyie text-black hover:bg-gray-200'
        }`}
      onClick={handleClick}
    >
      <UsersRound className="w-8 h-8" />
      {groupName}
    </div>
  );
}
