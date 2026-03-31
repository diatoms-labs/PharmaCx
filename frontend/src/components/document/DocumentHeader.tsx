import { useNavigate } from 'react-router-dom';
import { 
  ArrowLeft, 
  PanelRightOpen, 
  PanelRightClose,
  FileText,
  Send,
  CheckCircle2,
  XCircle,
  ShieldCheck
} from 'lucide-react';
import { ControlledDocument } from '../../types';
import { usePermissions } from '../../hooks/usePermissions';
import { useAuth } from '../../hooks/useAuth';

interface DocumentHeaderProps {
  doc: ControlledDocument;
  panelOpen: boolean;
  setPanelOpen: (open: boolean) => void;
  onPrepareForTransition: () => Promise<void>;
  onShowModal: (modal: 'prepare' | 'submit' | 'review' | 'approval', mode?: 'approve' | 'reject') => void;
}

const STATUS_COLORS: Record<string, string> = {
  REQUESTED: 'bg-gray-800 text-white',
  QA_PREPARATION: 'bg-gray-800 text-white',
  AUTHOR_DRAFT: 'bg-gray-800 text-white',
  PEER_REVIEW: 'bg-gray-800 text-white',
  QA_REVIEW: 'bg-gray-800 text-white',
  APPROVAL: 'bg-gray-800 text-white',
  PUBLISHED: 'bg-green-100 text-green-700',
  RETIRED: 'bg-gray-100 text-gray-500',
};

export default function DocumentHeader({ 
  doc, 
  panelOpen, 
  setPanelOpen, 
  onShowModal 
}: DocumentHeaderProps) {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { canDownload, canPrint } = usePermissions();

  const statusClass = STATUS_COLORS[doc.status] || 'bg-gray-50 text-gray-500 ring-1 ring-gray-200';

  // RBAC permissions
  const currentStep = doc.currentStepIndex;
  const currentWs = doc.workflowSteps[currentStep];
  const isAssignedUser = user?.id != null && (
    currentWs?.assignedToUserId === user.id ||
    (doc.status === 'AUTHOR_DRAFT' && doc.authorId === user.id)
  );
  const isQAUser = user?.unitCode === 'QA';
  const canActOnRequestSelection = doc.status === 'QA_PREPARATION' && isQAUser && user?.id !== doc.requestedBy;

  return (
    <div className="flex-shrink-0 bg-white border-b border-gray-200 z-10">
      <div className="flex items-center justify-between px-5 py-3">
        <div className="flex items-center gap-3 min-w-0">
          <button onClick={() => navigate(-1)} className="text-gray-400 hover:text-gray-600 transition-colors">
            <ArrowLeft size={18} />
          </button>
          <div className="min-w-0">
            <div className="flex items-center gap-2.5">
              <h1 className="text-base font-semibold text-gray-900 truncate">{doc.title}</h1>
              <span className={`px-2 py-0.5 rounded-full text-[10px] font-semibold uppercase tracking-wide ${statusClass}`}>
                {doc.status.replace('_', ' ')}
              </span>
            </div>
            <div className="flex items-center gap-2 text-xs text-gray-400 mt-0.5">
              <span className="font-mono">{doc.documentNumber || 'Pending'}</span>
              <span>&middot;</span>
              <span>{doc.documentTypeId}</span>
              <span>&middot;</span>
              <span>{doc.unitId}</span>
              <span>&middot;</span>
              <span>v{doc.version}</span>
              {(!canDownload || !canPrint) && (
                <>
                  <span>&middot;</span>
                  <span className="text-amber-500 font-medium">
                    {[!canDownload && 'Download', !canPrint && 'Print'].filter(Boolean).join(' & ')} restricted
                  </span>
                </>
              )}
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2 flex-shrink-0">
          {/* Action Buttons based on status and assignment */}
          {doc.status === 'QA_PREPARATION' && canActOnRequestSelection && (
            <button className="btn-primary text-sm px-4 py-1.5 flex items-center gap-2" onClick={() => onShowModal('prepare')}>
              <FileText size={14} /> Select Document
            </button>
          )}
          {doc.status === 'AUTHOR_DRAFT' && isAssignedUser && (
            <button className="btn-primary text-sm px-4 py-1.5 flex items-center gap-2" onClick={() => onShowModal('submit')}>
              <Send size={14} /> Submit for Review
            </button>
          )}
          {(doc.status === 'PEER_REVIEW' || doc.status === 'QA_REVIEW') && isAssignedUser && (
            <>
              <button className="btn-primary text-sm px-4 py-1.5 flex items-center gap-2" onClick={() => onShowModal('review', 'approve')}>
                <CheckCircle2 size={14} /> Approve
              </button>
              <button
                className="inline-flex items-center gap-1.5 text-sm px-4 py-1.5 rounded-md border border-gray-200 text-gray-600 hover:bg-gray-50 transition-colors"
                onClick={() => onShowModal('review', 'reject')}
              >
                <XCircle size={14} /> Reject
              </button>
            </>
          )}
          {doc.status === 'APPROVAL' && isAssignedUser && (
            <>
              <button
                className="btn-primary text-sm px-4 py-1.5 flex items-center gap-2"
                onClick={() => onShowModal('approval', 'approve')}
              >
                <ShieldCheck size={14} /> Approve &amp; Sign
              </button>
              <button
                className="inline-flex items-center gap-1.5 text-sm px-4 py-1.5 rounded-md border border-gray-200 text-gray-600 hover:bg-gray-50 transition-colors"
                onClick={() => onShowModal('approval', 'reject')}
              >
                Reject
              </button>
            </>
          )}

          <div className="w-px h-6 bg-gray-200 mx-1" />
          <button
            onClick={() => setPanelOpen(!panelOpen)}
            className="p-1.5 rounded-md text-gray-400 hover:bg-gray-100 hover:text-gray-600 transition-colors"
            title={panelOpen ? 'Close panel' : 'Open panel'}
          >
            {panelOpen ? <PanelRightClose size={18} /> : <PanelRightOpen size={18} />}
          </button>
        </div>
      </div>
    </div>
  );
}
