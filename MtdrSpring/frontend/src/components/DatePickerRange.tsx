import React, { useState } from 'react';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { DateRangePicker } from '@mui/x-date-pickers-pro/DateRangePicker';
import { SingleInputDateRangeField } from '@mui/x-date-pickers-pro/SingleInputDateRangeField';
import { DemoContainer } from '@mui/x-date-pickers/internals/demo';
import { DateRange } from '@mui/x-date-pickers-pro';
import { Dayjs } from 'dayjs';

interface MyDateRangePickerProps {
  dateRange?: DateRange<Dayjs>;
  setDateRange?: (dateRange: DateRange<Dayjs>) => void;
}

const MyDateRangePicker: React.FC<MyDateRangePickerProps> = ({ setDateRange }) => {
  const [actualDateRange, setactualDateRange] = useState<DateRange<Dayjs>>([null, null]);

  const handleChange = (newDateRange: DateRange<Dayjs>) => {
    setactualDateRange(newDateRange);
    console.log("Date range changed to", newDateRange);
    if (setDateRange) {
      setDateRange(newDateRange);
    }
  };

  return (
    <div className="w-full">
      <p className="text-[#747276] text-[20px]">Select a time gap</p>
      <LocalizationProvider dateAdapter={AdapterDayjs}>
        <DemoContainer
          components={['DateRangePicker']}
          sx={{
            width: '100%',
            '& .MuiOutlinedInput-root': {
              borderRadius: '14px',
            }
          }}>
          <DateRangePicker
            slots={{ field: SingleInputDateRangeField }}
            slotProps={{
              textField: {
                fullWidth: true,
                variant: "outlined",
              },
            }}
            value={actualDateRange}
            onChange={handleChange}
            disablePast={false}
            disableFuture={false}
          />
        </DemoContainer>
      </LocalizationProvider>

    </div>

  );
};

export default MyDateRangePicker;