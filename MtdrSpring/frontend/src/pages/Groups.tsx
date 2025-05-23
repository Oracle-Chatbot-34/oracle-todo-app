import { useEffect, useState } from 'react';
import { UsersRound, Info } from 'lucide-react';
import { Team } from '@/services/teamService';
import { User } from '@/services/userService';
import { Sprint } from '@/services/sprintService';
import { Link } from 'react-router-dom';

import teamService from '@/services/teamService';
import TeamCard from '@/components/teams/TeamCard';
import TeamMemberCard from '@/components/teams/TeamMemberCard';
import TeamSprints from '@/components/teams/TeamSprints';
import LoadingSpinner from '@/components/LoadingSpinner';
import AddMemberModal from '@/components/teams/AddMemberModal';

export default function Groups() {
  const [teams, setTeams] = useState<Team[]>([]);
  const [selectedTeam, setSelectedTeam] = useState(0);
  const [selectedTeamMembers, setSelectedTeamMembers] = useState<User[]>([]);
  const [selectedTeamSprints, setSelectedTeamSprints] = useState<Sprint[]>([]);

  const [showAddMemberModal, setShowAddMemberModal] = useState(false);

  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchTeams = async () => {
      try {
        setIsLoading(true);
        const teamsData = await teamService.getAllTeams();
        setTeams(teamsData);
      } catch (err) {
        console.error('Error fetching teams:', err);
        setError('Failed to load teams. Please try again.');
      } finally {
        setIsLoading(false);
      }
    };
    fetchTeams();
  }, []);

  const refreshTeamMembers = async () => {
    if (selectedTeam === 0) return;

    try {
      setIsLoading(true);
      const members = await teamService.getTeamMembers(selectedTeam);
      setSelectedTeamMembers(members);
    } catch (err) {
      console.error(
        `Error refreshing team members for team ${selectedTeam}:`,
        err
      );
      setError('Failed to refresh team members. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="bg-background h-full w-full p-6 lg:px-10 py-10 flex items-start justify-center overflow-clip">
      <div className="flex flex-col justify-center p-4 lg:p-10 gap-y-4 gap-x-3 bg-whitie w-full h-full rounded-lg shadow-xl ">
        {/* Title */}
        <div className="flex flex-row items-center gap-[10px]">
          <UsersRound className="w-8 h-8" />
          <p className="text-[24px] font-semibold">Group Management</p>
        </div>

        {error && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded w-full">
            {error}
          </div>
        )}

        <div className="flex lg:flex-row gap-x-3 w-full h-full p-6">
          <div className="bg-whitiish2 w-2/8 h-full rounded-2xl shadow-xl p-5 gap-5 flex flex-col">
            <p className="text-[#424043] text-[1.35rem] lg:text-3xl">Groups</p>
            {isLoading && teams.length === 0 ? (
              <LoadingSpinner />
            ) : (
              <div className="overflow-y-auto max-h-[600px] flex flex-col gap-5">
                {teams.map((team) => (
                  <TeamCard
                    key={team.id}
                    teamId={team.id ?? 0}
                    groupName={team.name}
                    selectedTeam={selectedTeam}
                    setIsLoading={setIsLoading}
                    setSelectedTeam={setSelectedTeam}
                    setSelectedTeamMembers={setSelectedTeamMembers}
                    setSelectedTeamSprints={setSelectedTeamSprints}
                  />
                ))}
              </div>
            )}
          </div>

          <div className="bg-whitiish2 w-4/8 h-full rounded-2xl shadow-xl p-5 gap-5 flex flex-col">
            <p className="text-[#424043] text-[1.35rem] lg:text-3xl">
              Group participants
            </p>

            {isLoading && selectedTeam !== 0 ? (
              <LoadingSpinner />
            ) : (
              <div className="flex flex-col gap-6">
                <div className="overflow-y-auto max-h-120 flex flex-col gap-5">
                  {selectedTeamMembers.map((member) => (
                    <TeamMemberCard
                      key={member.id}
                      id={member.id ?? 0}
                      name={member.fullName}
                      role={member.role}
                      teamId={selectedTeam}
                      onMemberRemoved={refreshTeamMembers}
                    />
                  ))}
                </div>

                {selectedTeam !== 0 ? (
                  <button
                    className="bg-greenie rounded-lg flex flex-row justify-center items-center h-12 w-110 shadow-lg"
                    onClick={() => setShowAddMemberModal(true)}
                  >
                    <span className="text-white text-2xl">Add a member</span>
                  </button>
                ) : (
                  <div className="flex flex-col items-center gap-4">
                    <Info color="#DFDFE4" className="h-40 w-40" />
                    <div className="text-3xl text-greyie ">
                      <p>Select a group to see it's members.</p>
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
          <div className="bg-whitiish2 w-2/8 h-full rounded-2xl shadow-xl p-5 gap-5 flex flex-col">
            <p className="text-[#424043] text-[1.35rem] lg:text-3xl">
              Team sprints
            </p>

            {isLoading && selectedTeam !== 0 ? (
              <LoadingSpinner />
            ) : (
              <div className="flex flex-col gap-6">
                <div className="overflow-y-auto max-h-120 flex flex-col gap-5">
                  {selectedTeamSprints.map((sprint) => (
                    <TeamSprints
                      key={sprint.id}
                      name={sprint.name}
                      status={sprint.status || ''}
                      team={sprint.teamId}
                    />
                  ))}
                </div>

                {selectedTeam !== 0 ? (
                  <p>
                    This view is read only, if you wish to edit sprints, go to:{' '}
                    <Link to="/sprints">
                      <span className="underline">Sprint Management</span>
                    </Link>
                  </p>
                ) : (
                  <div className="flex flex-col items-center gap-4">
                    <Info color="#DFDFE4" className="h-30 w-30" />
                    <div className="text-3xl text-greyie ">
                      <p>Select a group to see it's sprints.</p>
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Add Member Modal */}
      {selectedTeam !== 0 && (
        <AddMemberModal
          isOpen={showAddMemberModal}
          teamId={selectedTeam}
          onClose={() => setShowAddMemberModal(false)}
          onMemberAdded={refreshTeamMembers}
        />
      )}
    </div>
  );
}
