import { useState } from 'react';
import { 
  History, 
  CheckCircle2, 
  XCircle, 
  Loader2, 
  ChevronDown, 
  UserCheck, 
  Clock 
} from 'lucide-react';
import { WorkflowStep } from '../../types';

interface WorkflowStatusPanelProps {
  steps: WorkflowStep[];
}

export default function WorkflowStatusPanel({ steps }: WorkflowStatusPanelProps) {
  return (
    <div className="flex-1 overflow-y-auto p-4 space-y-3 scrollbar-hide">
      <div className="flex items-center gap-2 mb-2">
         <History size={16} className="text-gray-400" />
         <h3 className="text-xs font-bold text-gray-700 uppercase tracking-widest">Document Lifecycle</h3>
      </div>
      {steps.map((step) => (
        <WorkflowStepCard key={step.stepIndex} step={step} isActive={step.status === 'IN_PROGRESS'} />
      ))}
    </div>
  );
}

function WorkflowStepCard({ step, isActive }: { step: WorkflowStep; isActive: boolean }) {
  const [expanded, setExpanded] = useState(false);

  const statusIcon = () => {
    if (step.status === 'COMPLETED') return <CheckCircle2 size={16} className="text-green-500" />;
    if (step.status === 'REJECTED') return <XCircle size={16} className="text-red-500" />;
    if (step.status === 'IN_PROGRESS') return <Loader2 size={16} className="text-brand-500 animate-spin" />;
    return <div className="w-4 h-4 rounded-full bg-gray-200" />;
  };

  const bgClass = isActive
    ? 'bg-brand-50 border-brand-200 shadow-sm'
    : step.status === 'COMPLETED'
      ? 'border-gray-100 bg-gray-50/50'
      : 'border-transparent opacity-60';

  return (
    <div className={`rounded-lg border ${bgClass} transition-all duration-200 overflow-hidden`}>
      <button
        onClick={() => setExpanded(!expanded)}
        className="w-full flex items-center gap-2.5 px-3 py-2 text-left"
      >
        {statusIcon()}
        <span className={`text-[11px] flex-1 truncate ${
          isActive ? 'font-bold text-brand-900' :
          step.status === 'COMPLETED' ? 'text-gray-700 font-medium' :
          'text-gray-400'
        }`}>
          {step.name}
        </span>
        {(step.assignedToUsername || step.completedAt) && (
          <ChevronDown size={12} className={`text-gray-400 transition-transform ${expanded ? 'rotate-180' : ''}`} />
        )}
      </button>
      {expanded && (step.assignedToUsername || step.startedAt || step.comment || step.rejectionReason) && (
        <div className="px-3 pb-2.5 space-y-1.5 ml-6 border-l border-gray-200 mb-1">
          {step.assignedToUsername && (
            <div className="flex items-center gap-1.5 text-[9px] text-gray-500">
              <UserCheck size={10} />
              <span>Assigned: {step.assignedToUsername}</span>
            </div>
          )}
          {step.startedAt && (
            <div className="flex items-center gap-1.5 text-[9px] text-gray-400 font-medium">
              <Clock size={10} />
              <span>Started: {new Date(step.startedAt).toLocaleDateString()}</span>
            </div>
          )}
          {step.completedAt && (
            <div className="flex items-center gap-1.5 text-[9px] text-green-600 font-bold">
              <CheckCircle2 size={10} />
              <span>Verified: {new Date(step.completedAt).toLocaleDateString()}</span>
            </div>
          )}
          {step.comment && (
            <p className="text-[9px] text-gray-600 italic bg-white rounded-md px-2 py-1 border border-gray-100/50 leading-tight">
              &ldquo;{step.comment}&rdquo;
            </p>
          )}
          {step.rejectionReason && (
            <p className="text-[9px] text-red-600 bg-red-50 rounded px-2 py-1 border border-red-100 font-medium leading-tight">
              Rejected: {step.rejectionReason}
            </p>
          )}
        </div>
      )}
    </div>
  );
}
