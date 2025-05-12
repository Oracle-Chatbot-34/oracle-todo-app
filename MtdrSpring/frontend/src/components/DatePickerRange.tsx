import { Form } from '@/components/ui/form';
import { UseFormReturn, FieldPath } from 'react-hook-form';

// Filters
import DateSelector from './DateCalendar';

type SpanSelectorProps<T extends Record<string, any>> = {
  form: UseFormReturn<T>;
};

export default function SpanSelector<T extends Record<string, any> & {
  startDate: any;
  endDate: any;
}>({
  form,
}: SpanSelectorProps<T>) {

  return (
    <div className="w-full h-full bg-white rounded-xl shadow-lg p-4 flex flex-row">
      <Form {...form}>
        <form
          className="flex flex-row gap-x-4 items-center justify-between"
        >
          <DateSelector name={"startDate" as FieldPath<T>} keyLabel="Start Date" form={form} />
          <DateSelector name={"endDate" as FieldPath<T>} keyLabel="End Date" form={form} />
        </form>
      </Form>
    </div>
  );
}
