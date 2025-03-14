import { FileText } from 'lucide-react';

type Props = {
  href: string | undefined;
};

export default function PdfDisplayer({ href }: Props) {
  if (href)
    return (
      <div className="flex flex-col w-full  h-full rounded-xl bg-card justify-center items-center p-2 outline gap-4 relative">
        <div className='w-full h-full top-0 right-0 bg-muted text-6xl font-bold opacity-30 absolute flex justify-center items-center'>
          Example
        </div>
        <iframe src={href} className=' w-full h-full rounded-xl'></iframe>
      </div>
    );

  return (
    <div className="flex flex-col w-full h-full rounded-xl bg-card justify-center items-center p-6 outline gap-4">
      <FileText className="h-20 w-20 stroke-muted-foreground/80" />
      <div className="text-lg text-muted-foreground">
        Your report will be generated here
      </div>
    </div>
  );
}
