type Props = {
    selectedTaskOptions: string[];
    setselectedTaskOptions: (selectedTaskOptions: string[]) => void;
    selectAllTasksType: boolean;
    setselectAllTasksType: (selectAllTasksType: boolean) => void;

}

export default function StatusSelections({selectedTaskOptions, setselectedTaskOptions, selectAllTasksType, setselectAllTasksType}: Props) {
    const options = ["Completed", "In-progress", "Not completed"];

    const handleselectAllTasksType = () => {
        if (selectAllTasksType) {
            setselectedTaskOptions([]);
        } else {
            setselectedTaskOptions(options);
        }
        setselectAllTasksType(!selectAllTasksType);
    };

    const handleOptionChange = (option: string) => {
        let updatedSelection;
        if (selectedTaskOptions.includes(option)) {
            updatedSelection = selectedTaskOptions.filter((item) => item !== option);
        } else {
            updatedSelection = [...selectedTaskOptions, option];
        }
        setselectedTaskOptions(updatedSelection);
        setselectAllTasksType(updatedSelection.length === options.length);
    };

    return (
        <div className="w-full">
            <div className="flex flex-row gap-[10px] items-center">
                <p className="text-[#747276] text-[20px]" style={{ marginBottom: "5px" }}>Select a task status</p>
                <label
                    style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "8px",
                        cursor: "pointer",
                    }}
                >
                    <input
                        type="checkbox"
                        checked={selectAllTasksType}
                        onChange={handleselectAllTasksType}
                        style={{ cursor: "pointer", color: "#747276", border: "2px solid #DFDFE4" }}
                    />
                    <span style={{ color: "#747276" }}> Select all</span>

                </label>
            </div>

            {/* Task status options */}
            <div className="flex flex-row">
                {options.map((option, index) => (
                    <label
                        key={option}
                        style={{
                            display: "block",
                            padding: "10px 15px",
                            backgroundColor: selectedTaskOptions.includes(option) ? "#00A884" : "white",
                            color: selectedTaskOptions.includes(option) ? "white" : "black",
                            border: "1px solid #ccc",
                            transition: "background-color 0.3s ease, color 0.3s ease",
                            cursor: "pointer",
                            borderRadius: index === 0
                                ? "12px 0 0 12px"
                                : index === options.length - 1
                                    ? "0 12px 12px 0"
                                    : "0",
                            borderLeft: index !== 0 ? "none" : "1px solid #ccc",
                        }}
                        onClick={() => handleOptionChange(option)}
                    >

                        <span className="text-[20px]">{option}</span>
                    </label>
                ))}
            </div>
        </div>
    );
}