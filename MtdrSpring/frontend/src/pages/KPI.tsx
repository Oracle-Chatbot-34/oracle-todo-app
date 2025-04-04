import { useEffect, useState } from 'react';
import TaskCompletionRate from '@/components/kpis/TaskCompletionRate';
import TimeCompletionRate from '@/components/kpis/TimeCompletionRate';
import LineComponent from '@/components/kpis/LineComponent';
import RealHours from '@/components/kpis/RealHours';
import KPITitle from '@/components/kpis/KPITtitle';
import AvgHours from '@/components/kpis/AvgHoursEmpl';
import { dictionaryKPI } from '@/components/kpis/KPIDictionary';

import ScopeSelection from '@/components/ScopeSelection';
import MemberSelection from '@/components/MemberSelection';
import { Member } from '../components/ScopeSelection';
import { ChartPie } from 'lucide-react';
import {
  dataDaniel,
  dataBenja,
  dataEmiliano,
  dataTeam,
} from '@/components/kpis/KpiExamples';

// type KPIObject = {
//   definition: string;
//   example: string;
// };

export default function KPI() {
  const [isIndividual, setIsIndividual] = useState(true);
  const [selectedMember, setSelectedMember] = useState<Member | null>(null);
  const [currentData, setCurrentData] = useState(dataDaniel);

  useEffect(() => {
    console.log('Selected member:', selectedMember);
    if (!isIndividual) {
      setCurrentData(dataTeam);
      return;
    }
    
    if (selectedMember?.id == 1) {
      setCurrentData(dataDaniel);
    } else if (selectedMember?.id == 2) {
      setCurrentData(dataBenja);
    } else if (selectedMember?.id == 3) {
      setCurrentData(dataEmiliano);
    }

  }, [selectedMember, isIndividual]);

  return (
    <div className="bg-background h-full w-full p-6 lg:px-10 py-10 flex items-start justify-center overflow-clip">
      <div className="flex flex-col justify-center items-center p-4 lg:p-10 gap-y-4 bg-whitie w-full h-full rounded-lg shadow-xl ">
        {/* Title */}
        <div className="flex flex-row items-center gap-[10px]">
          <ChartPie className="w-8 h-8" />
          <p className="text-[24px] font-semibold">
            Key Performance Indicators
          </p>
        </div>

        <div className="flex lg:flex-row gap-x-3 w-full h-full p-6">
          {/* Task completion rate */}
          <div className="bg-whitiish2 w-1/3 h-full rounded-2xl shadow-xl p-5 gap-5 flex flex-col">
            <KPITitle
              title="Task completion rate"
              KPIObject={dictionaryKPI[1]}
            />
            <div className="flex flex-col gap-6 w-full p-2">
              {isIndividual ? (
                <TaskCompletionRate
                  data={currentData.line}
                  categories={currentData.categories}
                />
              ) : (
                <TaskCompletionRate
                  data={dataTeam.line}
                  categories={dataTeam.categories}
                />
              )}

              <ScopeSelection
                isIndividual={isIndividual}
                setIsInidividual={setIsIndividual}
              />

              <MemberSelection
                selectedMemberProp={selectedMember}
                isIndividual={isIndividual}
                setIsIndividual={setIsIndividual}
                setSelectedMemberProp={setSelectedMember}
              />
            </div>
          </div>

          <div className="flex flex-col gap-5 w-1/3 h-full">
            {/* Time completion rate */}
            <div className="bg-whitiish2 rounded-2xl shadow-xl p-2 gap-5 flex flex-col h-1/2 justify-center items-center">
              <KPITitle
                title="Time Completion Rate Over Time %"
                KPIObject={dictionaryKPI[3]}
              />
              <TimeCompletionRate data={currentData.pie} />
            </div>

            {/* Percentages */}
            <div className="bg-whitiish2 rounded-2xl shadow-xl px-6 py-8 gap-5 flex flex-col justify-around items-center h-1/2 ">
              <div className="w-full flex flex-col gap-4">
                <KPITitle
                  title="OCI Resources Utilization"
                  KPIObject={dictionaryKPI[5]}
                />
                <LineComponent percentage={currentData.percetages.oci} />
              </div>

              <div className="w-full flex flex-col gap-4">
                <KPITitle
                  title="Tasks Completed per Week"
                  KPIObject={dictionaryKPI[6]}
                />
                <LineComponent percentage={currentData.percetages.tasks} />
              </div>
            </div>
          </div>

          {/* Real worked hours */}
          <div className="flex flex-col w-1/3 h-full gap-5">
            <div className="bg-whitiish2 rounded-2xl shadow-xl p-2 gap-5 flex flex-col h-full justify-center">
              <KPITitle
                title="Real Hours Worked"
                KPIObject={dictionaryKPI[4]}
              />
              <RealHours percentage={(currentData.hours[0] * 100) / currentData.hours[1] } workedHours={currentData.hours[0]} plannedHours={currentData.hours[1]} />
            </div>

            <div className="bg-whitiish2 rounded-2xl shadow-xl h-full p-2 gap-5 flex flex-col justify-center items-center">
              <KPITitle
                title="Average Tasks by Employee"
                KPIObject={dictionaryKPI[2]}
              />
              <AvgHours average={2.5} />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
