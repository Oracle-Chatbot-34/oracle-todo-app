import { useState } from "react";
import { HiSparkles } from "react-icons/hi2";
import StatusSelections from "../components/StatusSelections";
import ScopeSelection from "../components/ScopeSelection";
import DatePickerRange from "../components/DatePickerRange";

type ScopeType = 'individual' | 'all-group';
type Member = {
    id: string;
    name: string;
};

export default function Reports() {
    {/* State for the scope of the report */}
    const [scope, setScope] = useState<ScopeType>('individual');
    const [selectedMember, setSelectedMember] = useState<Member | null>(null);

    {/* State for the selected task options */}
    const [selectedTaskOptions, setselectedTaskOptions] = useState<string[]>([]);
    const [selectAllTasksType, setselectAllTasksType] = useState(false);

    {/* State for the date picker range */}

    const handleGenerateReport = () => {
        console.log("Generating report with scope", scope, "and selected member", selectedMember);

        console.log("Selected task options:", selectedTaskOptions);
        console.log("Did they selected all tasks type?", selectAllTasksType);
    };

    return (
        <div className="bg-background h-screen w-full flex flex-row items-start justify-center">
            <div className="bg-whitie w-[1350px] h-[750px] rounded-lg shadow-xl" style={{ marginTop: '10px' }}>
                <br />
                <div className="flex flex-row items-center gap-[20px]">
                    <br />
                    <div className="bg-greyie w-[40px] h-[40px] rounded-lg flex items-center justify-center">
                        <HiSparkles className="w-[30px] h-[30px]" />
                    </div>
                    <p className="text-[24px] font-semibold">Intelligent Reports</p>
                </div>
                <br/>
                {/* Filters go here */}
                <div className="bg-whitiish2 w-[600px] h-[600px] rounded-lg shadow-xl" style={{ marginLeft: '30px'}}>
                    <br/>
                    <div className="flex flex-col items-start w-[450px] gap-[35px]" style={{ marginLeft: '50px' }} >
                        <ScopeSelection scopeProp={scope} setScopeProp={setScope} selectedMemberProp={selectedMember} setSelectedMemberProp={setSelectedMember}/>
                        <StatusSelections selectedTaskOptions={selectedTaskOptions} setselectedTaskOptions={setselectedTaskOptions} selectAllTasksType={selectAllTasksType} setselectAllTasksType={setselectAllTasksType}/>
                        <DatePickerRange />
                    </div>

                    <button
                        type="button"
                        onClick={handleGenerateReport}
                        className="bg-greenie rounded-lg text-white text-[20px] font-semibold shadow-xl h-[40px] w-[320px] ml-[50px]"
                    >
                        Generate report
                    </button>
                    

                    
                </div>

            </div>
        </div>
    )
}