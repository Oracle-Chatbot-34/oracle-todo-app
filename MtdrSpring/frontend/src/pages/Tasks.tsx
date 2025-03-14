import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Checkbox } from '@/components/ui/checkbox';
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { FileCheck } from 'lucide-react';

type Props = {};

export default function Tasks({}: Props) {
  return (
    <div className="bg-background h-full w-full p-6 lg:px-10 py-10 flex items-start justify-center overflow-clip">
      <div className="flex flex-col justify-start items-start p-6 lg:p-10 gap-y-6 bg-whitie w-full h-full rounded-lg shadow-xl">
        {/* Title */}
        <div className="flex flex-row items-center gap-[20px]">
          <FileCheck className="w-8 h-8" />
          <p className="text-[24px] font-semibold">Intelligent Reports</p>
        </div>
        {/* Bar */}
        <Card className="flex flex-row justify-between items-center w-full px-4 lg:px-6 shadow-lg">
          <div className="flex gap-4">
            <p className="text-2xl">Order by:</p>
            <SelectOrder
              label={'Due date'}
              placeholder={'Due date'}
              values={[
                { label: 'Created at', value: 'created-at' },
                { label: 'Priority', value: 'priority' },
              ]}
            />
            <SelectOrder
              label={'Priority'}
              placeholder={'Priority'}
              values={[
                { label: 'High', value: 'high' },
                { label: 'Medium', value: 'medium' },
                { label: 'Low', value: 'low' },
              ]}
            />
            <SelectOrder
              label={'Assignee'}
              placeholder={'Assignee'}
              values={[
                { label: 'Daniel', value: 'id-daniel' },
                { label: 'Yair', value: 'id-yair' },
              ]}
            />
          </div>
          <div className="flex gap-4 items-center justify-center">
            <label
              htmlFor="terms"
              className="text-lg font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
            >
              Show past tasks
            </label>
            <Checkbox id="past-tasks" />
          </div>
        </Card>
        <div className="flex flex-col md:flex-row w-full gap-6 p-4 z-50">
          <div className="md:w-3/4 max-h-[calc(90vh-300px)] h-full flex flex-col space-y-4 overflow-y-auto pb-6 pr-2 ">
            <TaskCard
              taskId="TASK-001"
              title="Update authentication system"
              description="Implement OAuth 2.0 for better security"
              created={new Date(2023, 10, 15)}
              due={new Date(2023, 11, 30)}
              autor="John Doe"
            />
            <TaskCard
              taskId="TASK-002"
              title="Fix pagination bug in dashboard"
              description="Users report incorrect page counts on search results"
              created={new Date(2023, 11, 2)}
              due={new Date(2023, 11, 10)}
              autor="Alice Smith"
            />
            <TaskCard
              taskId="TASK-003"
              title="Create new landing page"
              description="Design and implement responsive landing page with new branding"
              created={new Date(2023, 11, 5)}
              due={new Date(2023, 12, 20)}
              autor="Maria Garcia"
            />
            <TaskCard
              taskId="TASK-004"
              title="Optimize database queries"
              description="Reports page taking too long to load"
              created={new Date(2023, 10, 28)}
              due={new Date(2023, 11, 8)}
              autor="David Kim"
            />
            <TaskCard
              taskId="TASK-005"
              title="Add export to PDF functionality"
              description="Allow users to download reports as PDF files"
              created={new Date(2023, 11, 1)}
              due={new Date(2023, 12, 15)}
              autor="Emily Chen"
            />
            <TaskCard
              taskId="TASK-006"
              title="Fix mobile responsive issues"
              description="Menu doesn't collapse correctly on iOS devices"
              created={new Date(2023, 11, 7)}
              due={new Date(2023, 11, 14)}
              autor="Robert Johnson"
            />
          </div>

          <div className="md:w-1/4 flex flex-col space-y-4">
            <button className="bg-primary shadow-xl text-white rounded-lg p-6 h-24 flex items-center justify-center text-3xl hover:scale-101 transition-transform ease-in-out duration-150">
              Create new task
            </button>

            <div className="bg-white h-full rounded-lg shadow-md p-6 min-h-40 flex flex-col justify-center items-center">
              <p className="text-4xl">There are</p>
              <p className="text-[130px] flex justify-center items-center">
                {'50'}
              </p>
              <p className="text-4xl">active tasks</p>
            </div>

            <div className="flex flex-col gap-4 bg-white h-full justify-around items-center rounded-lg shadow-md p-6 min-h-48">
              <p className="text-2xl lg:text-3xl">{40} tasks are on time</p>
              <p className="text-2xl lg:text-3xl">
                {7} tasks are behind schedule
              </p>
              <p className="text-2xl lg:text-3xl">
                {3} tasks are beyond deadline
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

type TaskCardProps = {
  taskId: string | undefined;
  title: string;
  description: string | undefined;
  created: Date;
  due: Date;
  autor: string;
};

function TaskCard({
  taskId,
  title,
  description,
  created,
  due,
  autor,
}: TaskCardProps) {
  return (
    <div className="flex bg-card flex-col shrink-0 shadow-md justify-start w-full items-center p-6 rounded-xl gap-2 min-h-32">
      <div className="flex justify-between w-full items-center h-fit">
        <p className="font-bold text-2xl">{title}</p>
        <p className="text-lg text-slate-700">{autor}</p>
      </div>
      {description ?? (
        <p className="text-lg text-left w-full ">{description}</p>
      )}
      <div className="flex justify-between w-full h-fit items-center">
        <div className="flex flex-col gap-2 text-slate-800">
          <p>Created: {created.toDateString()}</p>
          <p className="text-slate-900">Due: {due.toDateString()}</p>
        </div>
        <div className="flex flex-col lg:flex-row w-fit min-w-[6.25rem] h-fit gap-2">
          <Button>Edit</Button>
          <Button variant={'destructive'}>Delete</Button>
        </div>
      </div>
    </div>
  );
}

type SelectProps = {
  label: string;
  placeholder: string;
  values: { value: string; label: string }[];
};

function SelectOrder({ label, placeholder, values }: SelectProps) {
  return (
    <Select>
      <SelectTrigger className="w-[180px]">
        <SelectValue className="text-lg" placeholder={placeholder} />
      </SelectTrigger>
      <SelectContent>
        <SelectGroup>
          <SelectLabel>{label}</SelectLabel>
          {values.map((value) => (
            <SelectItem value={value.value}>{value.label}</SelectItem>
          ))}
        </SelectGroup>
      </SelectContent>
    </Select>
  );
}
