import { useEffect, useState } from 'react';
import TaskCompletionRate from '@/components/kpis/TaskCompletionRate';
import TimeCompletionRate from '@/components/kpis/TimeCompletionRate';
import LineComponent from '@/components/kpis/LineComponent';
import RealHours from '@/components/kpis/RealHours';

import ScopeSelection from '@/components/ScopeSelection';
import MemberSelection from '@/components/MemberSelection';
import { Member } from '../components/ScopeSelection';

export default function KPI() {
  const dataLineIndividual = {
    data: [
      70,
      75, // Nov 24 (Weeks 1-2)
      80,
      65, // Dec 24 (Weeks 3-4, holiday drop)
      85,
      82, // Jan 25 (Weeks 5-6, post-holiday recovery)
      88,
      92, // Feb 25 (Weeks 7-8, peak performance)
      78,
      70, // Mar 25 (Weeks 9-10, mid-quarter slump)
      83,
      90, // Apr 25 (Weeks 11-12, sprint push)
      89,
      72, // May 25 (Weeks 13-14, pre-summer decline)
      75,
      68, // Jun 25 (Weeks 15-16, summer slowdown)
    ],
    categories: [
      'Nov W1 2024',
      'Nov W2 2024',
      'Dec W1 2024',
      'Dec W2 2024',
      'Jan W1 2025',
      'Jan W2 2025',
      'Feb W1 2025',
      'Feb W2 2025',
      'Mar W1 2025',
      'Mar W2 2025',
      'Apr W1 2025',
      'Apr W2 2025',
      'May W1 2025',
      'May W2 2025',
      'Jun W1 2025',
      'Jun W2 2025',
    ],
  };

  const dataLineTeam = {
    data: [
      65,
      70, // Nov 24 (Weeks 1-2)
      75,
      60, // Dec 24 (Weeks 3-4, holiday impact)
      78,
      72, // Jan 25 (Weeks 5-6, slower ramp-up)
      82,
      85, // Feb 25 (Weeks 7-8, team collaboration peak)
      70,
      65, // Mar 25 (Weeks 9-10, resource constraints)
      80,
      87, // Apr 25 (Weeks 11-12, process improvements)
      84,
      68, // May 25 (Weeks 13-14, attrition effects)
      72,
      62, // Jun 25 (Weeks 15-16, summer vacations)
    ],
    categories: [
      'Nov W1 2024',
      'Nov W2 2024',
      'Dec W1 2024',
      'Dec W2 2024',
      'Jan W1 2025',
      'Jan W2 2025',
      'Feb W1 2025',
      'Feb W2 2025',
      'Mar W1 2025',
      'Mar W2 2025',
      'Apr W1 2025',
      'Apr W2 2025',
      'May W1 2025',
      'May W2 2025',
      'Jun W1 2025',
      'Jun W2 2025',
    ],
  };

  const dataPie = [28, 22, 10];

  const [isIndividual, setIsIndividual] = useState(true);
  const [selectedMember, setSelectedMember] = useState<Member | null>(null);

  return (
    <div className="flex bg-card flex-row shrink-0 shadow-md justify-start w-full items-center p-6 rounded-xl gap-2 min-h-32 bg-whitie">
      {/* Task completion rate */}
      <div className="bg-whitiish2 h-full rounded-2xl shadow-xl p-5 gap-5 flex flex-col">
        <div className="flex flex-row items-center ml-5 mt-3">
          <p className="text-[#424043] text-[1.35rem]">
            Task Completion Rate Over Time %
          </p>
        </div>
        <div className="shadow-xl rounded-lg">
          {isIndividual ? (
            <TaskCompletionRate
              data={dataLineIndividual.data}
              categories={dataLineIndividual.categories}
            />
          ) : (
            <TaskCompletionRate
              data={dataLineTeam.data}
              categories={dataLineTeam.categories}
            />
          )}
        </div>

        <div>
          <ScopeSelection
            isIndividual={isIndividual}
            setIsInidividual={setIsIndividual}
          />
        </div>

        <div>
          <MemberSelection
            selectedMemberProp={selectedMember}
            isIndividual={isIndividual}
            setIsIndividual={setIsIndividual}
            setSelectedMemberProp={setSelectedMember}
          />
        </div>
      </div>
      <div className="flex flex-col gap-5">
        {/* Time completion rate */}
        <div className="bg-whitiish2 rounded-2xl shadow-xl p-2 gap-5 flex flex-col">
          <div className="flex flex-row items-center ml-5 mt-3">
            <p className="text-[#424043] text-[1.35rem]">
              Time Completion Rate Over Time %
            </p>
          </div>
          <div>
            <TimeCompletionRate data={dataPie} />
          </div>
        </div>

        {/* Percentages */}
        <div className="bg-whitiish2 rounded-2xl shadow-xl px-4 py-2 gap-5 flex flex-col">
          <div className="flex flex-row items-center ml-5 mt-3">
            <p className="text-[#424043] text-[1.35rem]">
              OCI Resources Utilization
            </p>
          </div>

          <div>
            <LineComponent percentage={75} />
          </div>
          <div className="flex flex-row items-center ml-5 mt-3">
            <p className="text-[#424043] text-[1.35rem]">
              Tasks completed per Week
            </p>
          </div>

          <div>
            <LineComponent percentage={90} />
          </div>
        </div>
      </div>

      <div>
        {/* Real worked hours */}
        <div>
          <RealHours percentage={85} workedHours={120} plannedHours={140} />
        </div>
      </div>
    </div>
  );
}
