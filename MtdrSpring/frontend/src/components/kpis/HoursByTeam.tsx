import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  LabelList,
  ResponsiveContainer,
  XAxis,
  YAxis,
} from 'recharts';
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
} from '@/components/ui/chart';
import { useEffect, useState } from 'react';
import sprintService from '@/services/sprintService';

type ChartConfig = Record<
  string,
  {
    label: string;
    color: string;
  }
>;
type Sprint = {
  id: number;
  name: string;
};

type SprintDataDict = {
  id: number;
  name: string;
  entries: { member: string; hours: number }[];
};

type HoursByTeamProps = {
  teamId: number;
};

type ChartDataEntree = {
  member: string;
  workedHours: number;
};

const generateChartConfig = (
  chartData: { member: string; workedHours?: number }[]
): ChartConfig => {
  return chartData.reduce((config, entry, index) => {
    config[entry.member] = {
      label: entry.member,
      color: `hsl(${(index * 50) % 360}, 70%, 60%)`, // consistent unique color
    };
    return config;
  }, {} as ChartConfig);
};

export default function HoursByTeam({ teamId }: HoursByTeamProps) {
  const [chartConfig, setChartConfig] = useState<ChartConfig>({});
  const [chartData, setChartData] = useState<ChartDataEntree[]>([]);
  const [sprintDataDict, setSprintDataDict] = useState<SprintDataDict[]>([]);
  const [beginningSprint, setBeginningSprint] = useState<Sprint | null>(null);
  const [endSprint, setEndSprint] = useState<Sprint | null>(null);

  useEffect(() => {
    const fetchData = async () => {
      // 1. Simulate fetching sprints
      const tempSprints = Array.from({ length: 6 }, (_, i) => ({
        id: i + 1,
        name: `Sprint ${i + 1}`,
      }));

      // 2. Simulate team members
      const members = ['Rober', 'Benjamin', 'Hannia'];

      // 3. Dictionary to group data by sprint
      const sprintDataDict: Record<
        number,
        {
          id: number;
          name: string;
          entries: { member: string; hours: number }[];
        }
      > = {};

      // 4. Loop through each sprint and generate data
      tempSprints.forEach((sprint) => {
        const entries = members.map((member) => ({
          member,
          hours: Math.floor(Math.random() * 40) + 5, // Simulate hours (5 to 45)
        }));

        sprintDataDict[sprint.id] = {
          id: sprint.id,
          name: sprint.name,
          entries,
        };
      });

      console.log('Structured Sprint Data:', sprintDataDict);

      // Optional: save in state - convert dictionary to array
      setSprintDataDict(Object.values(sprintDataDict));
    };

    fetchData();
  }, []);

  useEffect(() => {
    if (!beginningSprint) return;
    if (endSprint && endSprint.id < beginningSprint.id) return;

    const startId = beginningSprint.id;
    const endId = endSprint?.id || beginningSprint.id;

    const memberHoursMap: Record<string, number> = {};

    for (let id = startId; id <= endId; id++) {
      const sprint = sprintDataDict[id];
      if (!sprint) continue;

      sprint.entries.forEach(({ member, hours }) => {
        if (!memberHoursMap[member]) {
          memberHoursMap[member] = 0;
        }
        memberHoursMap[member] += hours;
      });
    }

    const formattedChartData = Object.entries(memberHoursMap).map(
      ([member, workedHours]) => ({
        member,
        workedHours,
      })
    );

    console.log('Filtered chart data:', formattedChartData);
    setChartData(formattedChartData);
    setChartConfig(generateChartConfig(formattedChartData));
  }, [beginningSprint, endSprint, sprintDataDict]);

  return (
    <div className="w-full h-full flex flex-col items-center">
      <div className="flex flex-row w-full items-center justify-center gap-2">
        <p>Select a sprint:</p>
        <select
          value={beginningSprint?.id || ''}
          onChange={(e) =>
            setBeginningSprint(
              sprintDataDict.find((s) => s.id === Number(e.target.value)) ||
                null
            )
          }
        >
          <option value="">Select start sprint</option>
          {sprintDataDict.map((sprint) => (
            <option key={sprint.id} value={sprint.id}>
              {sprint.name}
            </option>
          ))}
        </select>

        <select
          value={endSprint?.id || ''}
          onChange={(e) =>
            setEndSprint(
              sprintDataDict.find((s) => s.id === Number(e.target.value)) ||
                null
            )
          }
          className="ml-2"
        >
          <option value="">Select end sprint (optional)</option>
          {sprintDataDict
            .filter(
              (sprint) => !beginningSprint || sprint.id > beginningSprint.id
            )
            .map((sprint) => (
              <option key={sprint.id} value={sprint.id}>
                {sprint.name}
              </option>
            ))}
        </select>
      </div>
      <div className="flex w-full h-full">
        <ResponsiveContainer width="100%" height="100%">
          <ChartContainer config={chartConfig as ChartConfig}>
            <BarChart
              accessibilityLayer
              data={chartData}
              margin={{
                top: 20,
              }}
            >
              <CartesianGrid vertical={false} />
              <YAxis />

              <XAxis
                dataKey="member"
                tickLine={false}
                tickMargin={10}
                axisLine={false}
                tickFormatter={(value) => value}
              />
              <ChartTooltip
                cursor={false}
                content={<ChartTooltipContent hideLabel />}
              />
              <Bar dataKey="workedHours" radius={8}>
                {chartData.map((entry, index) => (
                  <Cell
                    key={`cell-${index}`}
                    fill={chartConfig[entry.member]?.color || '#ccc'}
                  />
                ))}
                <LabelList
                  position="top"
                  offset={12}
                  className="fill-foreground"
                  fontSize={12}
                />
              </Bar>
            </BarChart>
          </ChartContainer>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
