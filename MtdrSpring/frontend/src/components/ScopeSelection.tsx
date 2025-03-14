type Props = {
  isIndividual: boolean;
  setIsInidividual: (value: boolean) => void;
};

export type Member = {
  id: string;
  name: string;
};

export default function ScopeSelection({
  isIndividual,
  setIsInidividual,
}: Props) {

  return (
    <div className="flex flex-col items-center justify-around h-full w-full">
      {/* Scope toggle buttons */}
      <div className="w-full">
        <p className="text-[#747276] text-[1.5625rem]">
          Select an scope for the report
        </p>
        <div
          className="flex rounded-xl overflow-hidden mb-6"
          style={{ border: '2px solid #DFDFE4' }}
        >
          <button
            style={{
              flex: 1,
              padding: '10px 20px',
              backgroundColor: isIndividual ? '#00A884' : 'white', //Greenie
              color: isIndividual ? 'white' : 'black',
              transition: 'background-color 0.3s ease, color 0.3s ease',
              WebkitTransition: 'background-color 0.3s ease, color 0.3s ease',
            }}
            onClick={() => setIsInidividual(true)}
          >
            <span className="text-[20px]"> Individual</span>
          </button>
          <button
            style={{
              flex: 1,
              padding: '10px 20px',
              backgroundColor: isIndividual ? 'white' : '#00A884', //Greenie
              color: isIndividual ? 'black' : 'white' ,
              transition: 'background-color 0.3s ease, color 0.3s ease',
              WebkitTransition: 'background-color 0.3s ease, color 0.3s ease',
            }}
            onClick={() => setIsInidividual(false)}
          >
            <span className="text-[20px]"> All group</span>
          </button>
        </div>
      </div>
    </div>
  );
}
