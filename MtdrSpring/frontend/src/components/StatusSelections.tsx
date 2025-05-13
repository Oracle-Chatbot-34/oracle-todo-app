type Props = {
  selectedTaskOptions: string[];
  setselectedTaskOptions: (selectedTaskOptions: string[]) => void;
  selectAllTasksType: boolean;
  setselectAllTasksType: (selectAllTasksType: boolean) => void;
};

export default function StatusSelections({
  selectedTaskOptions,
  setselectedTaskOptions,
  selectAllTasksType,
  setselectAllTasksType,
}: Props) {
  const options = ['Completed', 'In-progress', 'Not completed'];

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
    <div className="w-full flex flex-col gap-2 text-2xl">
      <div className="flex flex-row gap-3 items-center mb-2">
        <p className="font-semibold">Select a task status</p>
        <label className="flex items-center gap-2 cursor-pointer">
          <input
            type="checkbox"
            checked={selectAllTasksType}
            onChange={handleselectAllTasksType}
            className="w-5 h-5 border-2 border-gray-300 text-gray-700 cursor-pointer"
          />
          <span className="text-gray-700 ">Select all</span>
        </label>
      </div>

      {/* Task status options */}
      <div className="flex flex-row w-full ">
        {options.map((option, index) => {
          const isSelected = selectedTaskOptions.includes(option);
          return (
            <label
              key={option}
              onClick={() => handleOptionChange(option)}
              className={`
                px-5 py-3 border border-gray-300 cursor-pointer transition-colors
                ${
                  isSelected ? 'bg-greenie text-white ' : 'bg-white text-black'
                }
                ${index === 0 ? 'rounded-l-xl' : ''}
                ${index === options.length - 1 ? 'rounded-r-xl' : ''}
                ${index !== 0 ? 'border-l-0' : ''}
              `}
            >
              {option}
            </label>
          );
        })}
      </div>
    </div>
  );
}
