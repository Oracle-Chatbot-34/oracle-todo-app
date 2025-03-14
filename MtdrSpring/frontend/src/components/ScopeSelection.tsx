type Props = {
    scopeProp: ScopeType;
    setScopeProp: (scope: ScopeType) => void;
    selectedMemberProp: Member | null;
    setSelectedMemberProp: (member: Member | null) => void;

}
type ScopeType = 'individual' | 'all-group';
type Member = {
    id: string;
    name: string;
};

export default function ScopeSelection({scopeProp, setScopeProp, selectedMemberProp, setSelectedMemberProp}:Props){
    const members: Member[] = [
        { id: '1', name: 'Daniel Barreras' },
        { id: '2', name: 'Benjamin Ortiz' },
        { id: '3', name: 'Emiliano Nieto' },
    ];

    const handleScopeChange = (newScope: ScopeType) => {
        setScopeProp(newScope);
        // Clear selected member when switching to all-group
        if (newScope === 'all-group') {
            setSelectedMemberProp(null);
        }

        console.log("Scope changed to", newScope);
    };

    return (
        <div className="flex flex-col gap-[30px] w-full">
            {/* Scope toggle buttons */}
            <div className="flex flex-col gap-[30px]">
                <div>
                    <p className="text-[#747276] text-[20px] mb-2">Select an scope for the report</p>
                    <div className="flex rounded-xl overflow-hidden mb-6" style={{ border: "2px solid #DFDFE4" }}>
                        <button
                            style={{
                                flex: 1,
                                padding: "10px 20px",
                                backgroundColor: scopeProp === "individual" ? "#00A884" : "white", //Greenie
                                color: scopeProp === "individual" ? "white" : "black",
                                transition: "background-color 0.3s ease, color 0.3s ease",
                                WebkitTransition: "background-color 0.3s ease, color 0.3s ease",
                            }}
                            onClick={() => handleScopeChange('individual')}
                        >
                            <span className="text-[20px]"> Individual</span>
                        </button>
                        <button
                            style={{
                                flex: 1,
                                padding: "10px 20px",
                                backgroundColor: scopeProp === "all-group" ? "#00A884" : "white", //Greenie
                                color: scopeProp === "all-group" ? "white" : "black",
                                transition: "background-color 0.3s ease, color 0.3s ease",
                                WebkitTransition: "background-color 0.3s ease, color 0.3s ease",
                            }}
                            onClick={() => handleScopeChange('all-group')}
                        >
                            <span className="text-[20px]"> All group</span>
                        </button>
                    </div>

                </div>


                {/* Member dropdown that appears only when individual scope is selected */}
                {scopeProp === 'individual' && (
                    <div>
                        <p className="text-[#747276] text-[20px] mb-2">Select a member</p>
                        <select
                            style={{
                                width: "100%",
                                padding: "5px",
                                borderRadius: "10px",
                                border: "2px solid #DFDFE4",
                                transition: "box-shadow 0.2s ease-in-out",
                                backgroundColor: "white",
                                fontSize: "20px",
                            }}
                            value={selectedMemberProp?.id || ""}
                            onChange={(e) => {
                                const memberId = e.target.value;
                                const member = members.find((m) => m.id === memberId) || null;
                                setSelectedMemberProp(member);
                            }}
                        >
                            <option value="" disabled></option>
                            {members.map(member => (
                                <option key={member.id} value={member.id} >
                                    {member.name}
                                </option>
                            ))}
                        </select>
                    </div>
                )}

            </div>
        </div>
    );
};
