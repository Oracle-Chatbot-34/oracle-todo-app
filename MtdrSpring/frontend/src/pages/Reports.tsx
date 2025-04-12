import { useState, useEffect } from 'react';
import { FileText, FileSearch } from 'lucide-react';
import { Button } from '@/components/ui/button';
import PdfDisplayer from '@/components/PdfDisplayer';
import StatusSelections from '@/components/StatusSelections';
import MemberSelection from '@/components/MemberSelection';
import { useForm } from 'react-hook-form';
import api from '@/services/api';
import { config } from '@/lib/config';
import sprintService from '@/services/sprintService';
import teamService from '@/services/teamService';

export type Member = {
  id: number;
  name: string;
};

export default function Reports() {
  const [isIndividual, setIsIndividual] = useState(false);
  const [selectAllTasksType, setSelectAllTasksType] = useState(false);
  const [selectedTaskOptions, setSelectedTaskOptions] = useState<string[]>([]);
  const [selectedMember, setSelectedMember] = useState<Member | null>(null);
  const [pdfHref, setPdfHref] = useState<string | undefined>(undefined);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  interface TeamInfo {
    id: number | undefined;
    name: string;
  }
  
  interface Sprint {
    id: number | undefined;
    name: string;
  }

  const [teams, setTeams] = useState<TeamInfo[]>([]);
  const [selectedTeam, setSelectedTeam] = useState<TeamInfo | null>(null);
  const [sprints, setSprints] = useState<Sprint[]>([]);
  const [startSprint, setStartSprint] = useState<Sprint | null>(null);
  const [endSprint, setEndSprint] = useState<Sprint | null>(null);

  // Define the form for date selection
  const form = useForm({
    defaultValues: {
      startDate: null as Date | null,
      endDate: null as Date | null,
    },
  });

  useEffect(() => {
    const loadTeamsAndSprints = async () => {
      try {
        // Load teams
        const teamsData = await teamService.getAllTeams();
        // Convert to TeamInfo type to ensure id is not optional
        const typedTeams: TeamInfo[] = teamsData.map(team => ({
          id: team.id || 0,  // Provide a default value if id is undefined
          name: team.name
        }));
        setTeams(typedTeams);
        if (typedTeams.length > 0) {
          setSelectedTeam(typedTeams[0]);
        }

        // Load sprints
        const sprintsData = await sprintService.getAllSprints();
        // Convert to Sprint type to ensure id is not optional
        const typedSprints: Sprint[] = sprintsData.map(sprint => ({
          id: sprint.id || 0,  // Provide a default value if id is undefined
          name: sprint.name
        }));
        setSprints(typedSprints);
        if (typedSprints.length > 0) {
          setStartSprint(typedSprints[0]);
        }
      } catch (err) {
        console.error('Error loading teams and sprints:', err);
        setError('Failed to load required data');
      }
    };

    loadTeamsAndSprints();
  }, []);

  const handleReportGeneration = async () => {
    if (!selectedTeam && !selectedMember) {
      setError('Please select a team or an individual member');
      return;
    }

    try {
      setLoading(true);
      setError('');

      const reportRequest = {
        isIndividual,
        teamId: isIndividual ? undefined : selectedTeam?.id,
        userId: isIndividual ? selectedMember?.id : undefined,
        statuses:
          selectedTaskOptions.length > 0 ? selectedTaskOptions : undefined,
        startDate: form.getValues().startDate,
        endDate: form.getValues().endDate,
        startSprintId: startSprint?.id,
        endSprintId: endSprint?.id,
      };

      // Call the Gemini-enabled API for report generation
      const response = await api.post(
        `${config.apiEndpoint}/reports/generate`,
        reportRequest
      );

      if (response.data?.reportUrl) {
        setPdfHref(response.data.reportUrl);
      } else {
        // For demo purposes, show a placeholder PDF
        setPdfHref('https://www.africau.edu/images/default/sample.pdf');
      }
    } catch (err) {
      console.error('Error generating report:', err);
      setError('Failed to generate report. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-background h-full w-full p-6 lg:px-10 py-10 flex items-start justify-center overflow-clip">
      <div className="flex flex-col justify-between p-4 lg:p-10 gap-y-4 bg-whitie w-full h-full rounded-lg shadow-xl ">
        <div className="flex items-center gap-8">
          <div className="flex flex-row items-center gap-3">
            <FileText className="w-8 h-8" />
            <p className="text-[24px] font-semibold">Reports</p>
          </div>
          <div className="flex flex-col sm:flex-row gap-4">
            <button
              className={`px-6 py-3 rounded-xl font-bold ${
                !isIndividual
                  ? 'bg-greenie text-white'
                  : 'bg-gray-300 text-black hover:bg-gray-400'
              }`}
              onClick={() => setIsIndividual(false)}
            >
              Team Report
            </button>
            <button
              className={`px-6 py-3 rounded-xl font-bold ${
                isIndividual
                  ? 'bg-greenie text-white'
                  : 'bg-gray-300 text-black hover:bg-gray-400'
              }`}
              onClick={() => setIsIndividual(true)}
            >
              Individual Report
            </button>
          </div>
        </div>

        {error && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
            {error}
          </div>
        )}

        <div className="flex flex-grow gap-4 pt-4">
          <div className="flex flex-col gap-6 w-2/5">
            {isIndividual ? (
              <MemberSelection
                isIndividual={isIndividual}
                setIsIndividual={setIsIndividual}
                selectedMemberProp={selectedMember}
                setSelectedMemberProp={setSelectedMember}
              />
            ) : (
              <div className="w-full">
                <p className="text-[#747276] text-[1.5625rem]">Select a team</p>
                <select
                  className="w-full pl-4 pr-2 rounded-xl h-12 border-2 border-[#DFDFE4] transition-shadow duration-200 ease-in-out bg-white text-[20px]"
                  value={selectedTeam?.id || ''}
                  onChange={(e) => {
                    const teamId = e.target.value;
                    const team =
                      teams.find((t) => t.id === Number(teamId)) || null;
                    setSelectedTeam(team);
                  }}
                >
                  <option value="" disabled></option>
                  {teams.map((team) => (
                    <option key={team.id} value={team.id}>
                      {team.name}
                    </option>
                  ))}
                </select>
              </div>
            )}

            {/* Sprint selection */}
            <div className="flex flex-col gap-4">
              <p className="text-[#747276] text-[1.5625rem]">Sprint Range</p>
              <div className="flex gap-4">
                <div className="w-1/2">
                  <p className="text-[#747276] text-lg">Start Sprint</p>
                  <select
                    className="w-full pl-4 pr-2 rounded-xl h-12 border-2 border-[#DFDFE4] transition-shadow duration-200 ease-in-out bg-white"
                    value={startSprint?.id || ''}
                    onChange={(e) => {
                      const sprintId = e.target.value;
                      const sprint =
                        sprints.find((s) => s.id === Number(sprintId)) || null;
                      setStartSprint(sprint);

                      // Reset end sprint if it's before start sprint
                      if (endSprint && sprint && endSprint.id! < (sprint.id || 0)) {
                        setEndSprint(null);
                      }
                    }}
                  >
                    <option value="" disabled>
                      Select start sprint
                    </option>
                    {sprints.map((sprint) => (
                      <option key={sprint.id} value={sprint.id}>
                        {sprint.name}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="w-1/2">
                  <p className="text-[#747276] text-lg">
                    End Sprint (optional)
                  </p>
                  <select
                    className="w-full pl-4 pr-2 rounded-xl h-12 border-2 border-[#DFDFE4] transition-shadow duration-200 ease-in-out bg-white"
                    value={endSprint?.id || ''}
                    onChange={(e) => {
                      const sprintId = e.target.value;
                      if (sprintId === '') {
                        setEndSprint(null);
                      } else {
                        const sprint =
                          sprints.find((s) => s.id === Number(sprintId)) ||
                          null;
                        setEndSprint(sprint);
                      }
                    }}
                  >
                    <option value="">None</option>
                    {sprints
                      .filter((sprint) => sprint.id! > (startSprint?.id || 0))
                      .map((sprint) => (
                        <option key={sprint.id} value={sprint.id}>
                          {sprint.name}
                        </option>
                      ))}
                  </select>
                </div>
              </div>
            </div>

            <StatusSelections
              selectedTaskOptions={selectedTaskOptions}
              setselectedTaskOptions={setSelectedTaskOptions}
              selectAllTasksType={selectAllTasksType}
              setselectAllTasksType={setSelectAllTasksType}
            />

            <div className="flex flex-col w-full items-end pt-3">
              <Button
                onClick={handleReportGeneration}
                className="bg-greenie text-white px-6 py-8 rounded-md flex items-center gap-2 text-xl"
                disabled={loading}
              >
                <FileSearch className="w-6 h-6" />
                {loading ? 'Generating...' : 'Generate Report'}
              </Button>
            </div>
          </div>

          <div className="w-3/5 h-full">
            <PdfDisplayer href={pdfHref} />
          </div>
        </div>
      </div>
    </div>
  );
}
