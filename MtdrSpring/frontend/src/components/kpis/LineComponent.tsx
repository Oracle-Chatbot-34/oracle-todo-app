type LineComponentProps = {
  percentage: number;
};

export default function LineComponent({ percentage }: LineComponentProps) {
  // Dynamic HSL green
  const hue = 166; // base green hue
  const saturation = Math.min(30 + percentage * 0.7, 100); // from 30% to 100%
  const lightness = Math.max(40 - percentage * 0.1, 20); // from 40% to 20%
  const dynamicGreen = `hsl(${hue}, ${saturation}%, ${lightness}%)`;

  // Determine text color
  const textColor = percentage >= 55 ? 'white' : 'black';

  return (
    <div className="w-full bg-white rounded-lg h-full relative text-2xl">
      <div
        className="h-full rounded-lg transition-all duration-500"
        style={{
          width: `${percentage}%`,
          backgroundColor: dynamicGreen,
        }}
      ></div>
      <span
        className="absolute inset-0 flex items-center justify-center font-bold transition-all duration-300"
        style={{ color: textColor }}
      >
        {percentage}%
      </span>
    </div>
  );
}
