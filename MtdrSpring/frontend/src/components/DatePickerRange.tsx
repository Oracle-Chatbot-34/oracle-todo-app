import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { DateRangePicker } from '@mui/x-date-pickers-pro/DateRangePicker';
import { SingleInputDateRangeField } from '@mui/x-date-pickers-pro/SingleInputDateRangeField';
import { DemoContainer } from '@mui/x-date-pickers/internals/demo';
import { DateRange } from '@mui/x-date-pickers-pro';
import { Dayjs } from 'dayjs';

interface Props {
  dateRangeProp: DateRange<Dayjs>;
  setDateRangeProp: (dateRange: DateRange<Dayjs>) => void;
}

export default function MyDateRangePicker({ dateRangeProp, setDateRangeProp }: Props){

  const handleChange = (dateRangeFunc: DateRange<Dayjs>) => {
    setDateRangeProp(dateRangeFunc);
    if (setDateRangeProp) {
      setDateRangeProp(dateRangeFunc);
    }
  };

  return (
    <div className="w-full">
      <p className="text-[#747276] text-[1.5625rem]">Select a time gap</p>
      <LocalizationProvider dateAdapter={AdapterDayjs}>
        <DemoContainer
          components={['DateRangePicker']}
          sx={{
            width: '100%',
            '& .MuiOutlinedInput-root': {
              borderRadius: '14px',
            },
          }}
        >
          <DateRangePicker
            slots={{ field: SingleInputDateRangeField }}
            slotProps={{
              textField: {
                fullWidth: true,
                variant: 'outlined',
              },
            }}
            value={dateRangeProp}
            onChange={handleChange}
            disablePast={false}
            disableFuture={false}
          />
        </DemoContainer>
      </LocalizationProvider>
    </div>
  );
};

