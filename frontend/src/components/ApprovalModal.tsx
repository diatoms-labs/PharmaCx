import { useState } from 'react';
import { X, ShieldCheck, XCircle, Loader2 } from 'lucide-react';
import api from '../api/client';
import type { ControlledDocument } from '../types';
import SignatureModal from './SignatureModal';
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

export default function ApprovalModal({ open, doc, mode, onClose, onSuccess, onPrepareTransition }: Props) {
  const [rejectionReason, setRejectionReason] = useState('');
  const [comment, setComment] = useState('');
  const [loading, setLoading] = useState(false);
  const [stage, setStage] = useState('');
  const [showSignature, setShowSignature] = useState(false);

  if (!open) return null;

  const handleReject = async () => {
    if (!rejectionReason.trim()) {
      toast.error('Rejection reason is required');
      return;
    }
    setLoading(true);
    try {
      setStage('Saving document changes...');
      if (onPrepareTransition) await onPrepareTransition();
      setStage('Submitting rejection...');
      const res = await api.post<ControlledDocument>(`/documents/${doc.id}/approve`, {
        approved: false,
        rejectionReason: rejectionReason.trim(),
        comment: comment.trim() || null,
        signatureData: null,
      });
      toast.success('Document rejected and returned to author');
      onSuccess(res.data);
      onClose();
    } catch (err) {
      showErrorToast(err, 'Failed to reject document');
    } finally {
      setLoading(false);
      setStage('');
    }
  };

  const handleSignAndApprove = async (signatureData: string, signComment: string) => {
    setLoading(true);
    try {
      setStage('Saving document changes...');
      if (onPrepareTransition) await onPrepareTransition();
      setStage('Publishing document...');
      const res = await api.post<ControlledDocument>(`/documents/${doc.id}/approve`, {
        approved: true,
        signatureData,
        comment: signComment.trim() || comment.trim() || null,
        rejectionReason: null,
      });
      toast.success('Document approved and published!');
      setShowSignature(false);
      onSuccess(res.data);
      onClose();
    } catch (err) {
      showErrorToast(err, 'Failed to approve document');
    } finally {
      setLoading(false);
      setStage('');
    }
  };

  if (mode === 'approve') {
    return (
      <>
        {!showSignature && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40" onClick={onClose}>
            <div className="bg-white rounded-lg shadow-xl w-full max-w-md mx-4" onClick={e => e.stopPropagation()}>
              <div className="px-5 py-4 border-b border-gray-200 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <ShieldCheck size={18} className="text-gray-700" />
                  <h3 className="text-base font-semibold text-gray-900">Final Approval</h3>
                </div>
                <button onClick={onClose} className="text-gray-400 hover:text-gray-600"><X size={18} /></button>
              </div>

              <div className="p-5 space-y-4 relative">
                {loading && (
                  <div className="absolute inset-0 bg-white/80 z-10 flex flex-col items-center justify-center gap-2">
                    <Loader2 size={24} className="animate-spin text-gray-500" />
                    <p className="text-sm text-gray-500 font-medium">{stage || 'Processing...'}</p>
                  </div>
                )}
                <div className="bg-gray-50 rounded-lg p-3">
                  <p className="text-sm font-medium text-gray-900">{doc.title}</p>
                  <p className="text-xs text-gray-500 mt-0.5">{doc.documentNumber} &middot; v{doc.version}</p>
                </div>
                <p className="text-sm text-gray-600">
                  By approving, this document will be <strong>published</strong> and become effective immediately.
                  Training will be automatically assigned to the relevant department.
                </p>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Comment (optional)</label>
                  <textarea
                    className="input"
                    rows={2}
                    value={comment}
                    onChange={e => setComment(e.target.value)}
                    placeholder="Add approval comments..."
                  />
                </div>
              </div>

              <div className="px-5 py-3 border-t border-gray-200 flex justify-end gap-2">
                <button className="btn text-sm px-4 py-1.5 border border-gray-200 text-gray-600 hover:bg-gray-50 rounded-md" onClick={onClose}>
                  Cancel
                </button>
                <button className="btn-primary text-sm px-4 py-1.5" onClick={() => setShowSignature(true)}>
                  Proceed to Sign
                </button>
              </div>
            </div>
          </div>
        )}

        <SignatureModal
          open={showSignature}
          onClose={() => setShowSignature(false)}
          onSubmit={handleSignAndApprove}
          title="Sign to Approve"
          submitLabel="Sign & Approve"
          loading={loading}
          loadingStage={stage}
        />
      </>
    );
  }

  // Reject mode
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40" onClick={onClose}>
      <div className="bg-white rounded-lg shadow-xl w-full max-w-md mx-4" onClick={e => e.stopPropagation()}>
        <div className="px-5 py-4 border-b border-gray-200 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <XCircle size={18} className="text-red-500" />
            <h3 className="text-base font-semibold text-gray-900">Reject Document</h3>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600"><X size={18} /></button>
        </div>

        <div className="p-5 space-y-4 relative">
          {loading && (
            <div className="absolute inset-0 bg-white/80 z-10 flex flex-col items-center justify-center gap-2">
              <Loader2 size={24} className="animate-spin text-gray-500" />
              <p className="text-sm text-gray-500 font-medium">{stage || 'Processing...'}</p>
            </div>
          )}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Rejection Reason <span className="text-red-500">*</span>
            </label>
            <textarea
              className="input"
              rows={3}
              value={rejectionReason}
              onChange={e => setRejectionReason(e.target.value)}
              placeholder="Explain why this document cannot be approved..."
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Comment</label>
            <textarea
              className="input"
              rows={2}
              value={comment}
              onChange={e => setComment(e.target.value)}
              placeholder="Additional comments..."
            />
          </div>
        </div>

        <div className="px-5 py-3 border-t border-gray-200 flex justify-end gap-2">
          <button className="btn text-sm px-4 py-1.5 border border-gray-200 text-gray-600 hover:bg-gray-50 rounded-md" onClick={onClose}>
            Cancel
          </button>
          <button
            className="inline-flex items-center gap-1.5 text-sm px-4 py-1.5 rounded-md bg-red-600 text-white hover:bg-red-700 transition-colors"
            onClick={handleReject}
            disabled={loading}
          >
            {loading ? <><Loader2 size={14} className="animate-spin" /> Rejecting...</> : 'Reject'}
          </button>
        </div>
      </div>
    </div>
  );
}
