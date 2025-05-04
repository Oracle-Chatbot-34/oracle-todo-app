// Custom legend component
export const CustomLegend = (props: any) => {
    const { payload } = props;
    if (!payload || !payload.length) return null;
  
    return (
      <div className="absolute left-8/10 bottom-90 bg-white/90 p-2 rounded-lg shadow-md text-lg">
        {payload.map((entry: any, index: number) => (
          <div
            key={`legend-${index}`}
            className="flex items-center mb-1 gap-2"
          >
            <div
              className="w-3 h-3 rounded-full"
              style={{ backgroundColor: entry.color }}
            />
            <span>{entry.value}</span>
          </div>
        ))}
      </div>
    );
  };
  