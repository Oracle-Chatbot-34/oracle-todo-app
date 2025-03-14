import { useState } from 'react';
import { Sparkles } from "lucide-react"
import StatusSelections from '../components/StatusSelections';
import ScopeSelection from '../components/ScopeSelection';
import { DateRange } from '@mui/x-date-pickers-pro';
import { Dayjs } from 'dayjs';
import DatePickerRange from '../components/DatePickerRange';
import { Button } from '@mui/material';
import MemberSelection from '@/components/MemberSelection';

type Member = {
  id: string;
  name: string;
};

export default function Reports() {
  const [isIndividual, setIsIndividual] = useState(true);
  const [selectedMember, setSelectedMember] = useState<Member | null>(null);

  const [selectedTaskOptions, setselectedTaskOptions] = useState<string[]>([]);
  const [selectAllTasksType, setselectAllTasksType] = useState(false);

  const [dateRange, setDateRange] = useState<DateRange<Dayjs>>([null, null]);

  const handleGenerateReport = () => {
    // Add backend logic here for generating the report
    console.log('Selected task options:', selectedTaskOptions);
    console.log('Did they selected all tasks type?', selectAllTasksType);

    console.log('Selected date range:', dateRange);
  };

  return (
    <div className="bg-background h-full w-full px-10 py-10 flex items-start justify-center">
      <div className="flex flex-col p-10 gap-y-6 bg-whitie w-full h-full rounded-lg shadow-xl">
        <div className="flex flex-row items-center gap-[20px]">
          <Sparkles className="w-8 h-8" />
          <p className="text-[24px] font-semibold">Intelligent Reports</p>
        </div>

        {/* Form */}
        <div className="flex flex-col items-center justify-around bg-whitiish2 w-1/3 h-full rounded-4xl shadow-xl p-10">
          <ScopeSelection
            isIndividual={isIndividual}
            setIsInidividual={setIsIndividual}
          />
          <MemberSelection
            isIndividual={isIndividual}
            setIsIndividual={setIsIndividual}
            selectedMemberProp={selectedMember}
            setSelectedMemberProp={setSelectedMember}
          />
          <StatusSelections
            selectedTaskOptions={selectedTaskOptions}
            setselectedTaskOptions={setselectedTaskOptions}
            selectAllTasksType={selectAllTasksType}
            setselectAllTasksType={setselectAllTasksType}
          />
          <DatePickerRange
            dateRangeProp={dateRange}
            setDateRangeProp={setDateRange}
          />
          <Button type="button" onClick={handleGenerateReport}>
            Generate report
          </Button>
        </div>
      </div>
    </div>
  );
}
