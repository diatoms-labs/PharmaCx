import { useState, useEffect } from 'react';
import { X, CheckCircle2, XCircle, Loader2 } from 'lucide-react';
import api from '../api/client';
import type { ControlledDocument } from '../types';
import toast from 'react-hot-toast';
import { showErrorToast } from '../utils/errorHandler';

interface Props {
  open: boolean;
  doc: ControlledDocument;
  mode: 'approve' | 'reject';
  onClose: () => void;
  onSuccess: (doc: ControlledDocument) => void;
  onPrepareTransition?: () => Promise<void>;
}

export default function ReviewActionModal({ open, doc, mode, onClose, onSuccess, onPrepareTransition }: Props) {
  const [comment, setComment] = useState('');
  const [rejectionReason, setRejectionReason] = useState('');
  const [loading, setLoading] = useState(false);
  const [stage, setStage] = useState('');

  const isPeerReview = doc.status === 'PEER_REVIEW';

  // Show who is pre-assigned for the next step
  const nextStepLabel = isPeerReview ? 'QA Review' : 'Approval';
  const nextAssignee = isPeerReview
    ? doc.workflowSteps?.[4]?.assignedToUsername
    : doc.workflowSteps?.[5]?.assignedToUsername;

  useEffect(() => {
    if (!open) return;
    setComment('');
    setRejectionReason('');
    setStage('');
  }, [open, mode]);

  if (!open) return null;

  const handleSubmit = async () => {
    if (mode === 'reject' && !rejectionReason.trim()) {
      toast.error('Rejection reason is required');
      return;
    }

    setLoading(true);
    try {
      setStage('Saving document changes...');
      if (onPrepareTransition) await onPrepareTransition();
      setStage(mode === 'approve' ? 'Submitting approval...' : 'Submitting rejection...');
      const res = await api.post<ControlledDocument>(`/documents/${doc.id}/review`, {
        approved: mode === 'approve',
        comment: comment.trim() || null,
        rejectionReason: mode === 'reject' ? rejectionReason.trim() : null,
      });
      toast.success(mode === 'approve' ? 'Review approved' : 'Document sent back to author');
      onSuccess(res.data);
      onClose();
    } catch (err) {
      showErrorToast(err, 'Failed to submit review');
    } finally {
      setLoading(false);
      setStage('');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40" onClick={onClose}>
      <div className="bg-white rounded-lg shadow-xl w-full max-w-md mx-4 max-h-[85vh] flex flex-col" onClick={e => e.stopPropagation()}>
        <div className="px-5 py-4 border-b border-gray-200 flex items-center justify-between flex-shrink-0">
          <div className="flex items-center gap-2">
            {mode === 'approve' ? (
              <CheckCircle2 size={18} className="text-gray-700" />
            ) : (
              <XCircle size={18} className="text-red-500" />
            )}
            <h3 className="text-base font-semibold text-gray-900">
              {mode === 'approve' ? 'Approve Review' : 'Reject & Return to Author'}
            </h3>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600"><X size={18} /></button>
        </div>

        <div className="flex-1 overflow-y-auto p-5 space-y-4 relative">
          {loading && (
            <div className="absolute inset-0 bg-white/80 z-10 flex flex-col items-center justify-center gap-2">
              <Loader2 size={24} className="animate-spin text-gray-500" />
              <p className="text-sm text-gray-500 font-medium">{stage || 'Processing...'}</p>
            </div>
          )}
          {mode === 'reject' && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Rejection Reason <span className="text-red-500">*</span>
              </label>
              <textarea
                className="input"
                rows={3}
                value={rejectionReason}
                onChange={e => setRejectionReason(e.target.value)}
                placeholder="Explain why this document needs revision..."
              />
            </div>
          )}

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Comment</label>
            <textarea
              className="input"
              rows={2}
              value={comment}
              onChange={e => setComment(e.target.value)}
              placeholder="Add a comment..."
            />
          </div>

          {mode === 'approve' && nextAssignee && (
            <div className="bg-gray-50 rounded-lg px-4 py-3">
              <p className="text-xs text-gray-500 mb-1">Next step: {nextStepLabel}</p>
              <p className="text-sm font-medium text-gray-800">{nextAssignee}</p>
              <p className="text-xs text-gray-400 mt-0.5">Pre-assigned at draft submission</p>
            </div>
          )}
        </div>

        <div className="px-5 py-3 border-t border-gray-200 flex justify-end gap-2 flex-shrink-0">
          <button className="btn text-sm px-4 py-1.5 border border-gray-200 text-gray-600 hover:bg-gray-50 rounded-md" onClick={onClose}>
            Cancel
          </button>
          {mode === 'approve' ? (
            <button className="btn-primary text-sm px-4 py-1.5" onClick={handleSubmit} disabled={loading}>
              {loading ? <><Loader2 size={14} className="animate-spin" /> Approving...</> : 'Approve'}
            </button>
          ) : (
            <button
              className="inline-flex items-center gap-1.5 text-sm px-4 py-1.5 rounded-md bg-red-600 text-white hover:bg-red-700 transition-colors"
              onClick={handleSubmit}
              disabled={loading}
            >
              {loading ? <><Loader2 size={14} className="animate-spin" /> Rejecting...</> : 'Reject'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
