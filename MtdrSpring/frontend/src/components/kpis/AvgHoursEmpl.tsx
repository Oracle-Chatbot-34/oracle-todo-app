type Prop = {
  average: number
}

export default function AvgHours({average} :Prop) {
  return (
    <div className="text-center">
      <p className="text-gray-700 text-3xl font-semibold mt-2">
        There is an average of
      </p>
      <p className="text-black text-9xl font-bold mt-1">{average}</p>
      <p className="text-gray-700 text-3xl font-semibold">task(s) per employee</p>
    </div>
  );
}
