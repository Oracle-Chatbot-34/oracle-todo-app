import { Loader2 } from 'lucide-react';


export default function LoadingSpinner() {
  return (
    <div className={`flex items-center justify-center w-full h-full`}>
      <Loader2 className={`animate-spin w-full h-full text-primary`} />
    </div>
  );
}