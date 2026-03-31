import { ControlledDocument } from '../../types';
import { CheckCircle2, XCircle } from 'lucide-react';

interface WorkflowStepperProps {
  doc: ControlledDocument;
}

const STEPS = ['Request', 'Request Selection', 'Author Draft', 'Peer Review', 'QA Review', 'Approval', 'Published'];

export default function WorkflowStepper({ doc }: WorkflowStepperProps) {
  const currentStep = doc.currentStepIndex;

  return (
    <div className="flex items-start px-5 pb-3 bg-white border-b border-gray-100 overflow-x-auto scrollbar-hide">
      {STEPS.map((step, i) => {
        const ws = doc.workflowSteps[i];
        const status = ws ? ws.status : 'PENDING';
        const isCompleted = status === 'COMPLETED' || (doc.status === 'PUBLISHED');
        const isCurrent = i === currentStep && doc.status !== 'PUBLISHED';
        const isRejected = status === 'REJECTED';

        return (
          <div key={step} className={`flex items-start ${i < STEPS.length - 1 ? 'flex-1' : ''}`}>
            {/* Circle + Step Info */}
            <div className="flex flex-col flex-shrink-0" style={{ minWidth: 0 }}>
              <div className="flex items-center gap-1.5">
                <div className={`
                  w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-bold flex-shrink-0 transition-all
                  ${isCompleted ? 'bg-gray-800 text-white' : ''}
                  ${isRejected ? 'bg-red-100 text-red-600' : ''}
                  ${isCurrent ? 'bg-brand-600 text-white shadow-md ring-2 ring-brand-100' : ''}
                  ${!isCompleted && !isCurrent && !isRejected ? 'bg-gray-100 text-gray-400 border border-gray-200' : ''}
                `}>
                  {isCompleted ? <CheckCircle2 size={12} /> : isRejected ? <XCircle size={12} /> : i + 1}
                </div>
                <span className={`text-[11px] whitespace-nowrap font-medium ${
                  isCurrent ? 'text-brand-700 font-bold' :
                  isCompleted ? 'text-gray-500 font-medium' :
                  'text-gray-400'
                }`}>{step}</span>
              </div>
              {ws?.assignedToUsername && (
                <span className={`text-[10px] leading-snug pl-6 truncate max-w-[110px] ${
                  isCurrent ? 'text-gray-600 font-medium' : isCompleted ? 'text-gray-400' : 'text-gray-300'
                }`}>
                  {ws.assignedToUsername}
                </span>
              )}
            </div>
            {/* Connector Link */}
            {i < STEPS.length - 1 && (
              <div className={`flex-1 h-[2px] mt-2.5 mx-2 ${
                i < currentStep ? 'bg-gray-300' : 'bg-gray-100'
              }`} />
            )}
          </div>
        );
      })}
    </div>
  );
}
