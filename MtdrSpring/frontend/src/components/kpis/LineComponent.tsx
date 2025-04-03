type LineComponentProps = {
    percentage: number;
};


export default function LineComponent({percentage}: LineComponentProps) {
  return (
    <div className="w-full bg-whitie rounded-lg h-10 relative">
      <div
        className="bg-greenie h-full rounded-lg transition-all duration-500"
        style={{ width: `${percentage}%` }}
      ></div>
      <span className="absolute inset-0 flex items-center justify-center text-white font-bold">
        {percentage}%
      </span>
    </div>
  );
}
