import { useState, useEffect } from 'react';
import { X, GraduationCap, AlertCircle, CheckCircle2, ChevronDown, Loader2 } from 'lucide-react';
import api from '../api/client';
import type { ControlledDocument, AppUser, TrainingQuestion } from '../types';
import TrainingQABuilderModal from './TrainingQABuilderModal';
import toast from 'react-hot-toast';
import { showErrorToast } from '../utils/errorHandler';

interface Props {
  open: boolean;
  doc: ControlledDocument;
  onClose: () => void;
  onSuccess: (doc: ControlledDocument) => void;
  onPrepareTransition?: () => Promise<void>;
}

export default function SubmitDraftModal({ open, doc, onClose, onSuccess, onPrepareTransition }: Props) {
  const [tab, setTab] = useState<'questions' | 'reviewers'>('reviewers');
  const [reviewers, setReviewers] = useState<AppUser[]>([]);
  const [approvers, setApprovers] = useState<AppUser[]>([]);
  const [selectedPeerReviewerId, setSelectedPeerReviewerId] = useState('');
  const [selectedQAReviewerId, setSelectedQAReviewerId] = useState('');
  const [selectedApproverId, setSelectedApproverId] = useState('');
  const [questions, setQuestions] = useState<TrainingQuestion[]>(doc.trainingQuestions || []);
  const [showQABuilder, setShowQABuilder] = useState(false);
  const [loading, setLoading] = useState(false);
  const [stage, setStage] = useState('');
  const [loadingReviewers, setLoadingReviewers] = useState(false);
  const [loadingApprovers, setLoadingApprovers] = useState(false);

  // Detect re-submission: reviewers already assigned from a previous submission
  const existingPeerReviewerId = doc.workflowSteps[3]?.assignedToUserId || '';
  const existingQAReviewerId = doc.workflowSteps[4]?.assignedToUserId || '';
  const existingApproverId = doc.workflowSteps[5]?.assignedToUserId || '';
  const isResubmission = !!(existingPeerReviewerId && existingQAReviewerId && existingApproverId);

  useEffect(() => {
    if (!open) return;
    setQuestions(doc.trainingQuestions || []);

    if (isResubmission) {
      // Re-submission after rejection: pre-fill with existing assignments
      setSelectedPeerReviewerId(existingPeerReviewerId);
      setSelectedQAReviewerId(existingQAReviewerId);
      setSelectedApproverId(existingApproverId);
      setTab('questions'); // Default to Q&A tab since reviewers are locked
    } else {
      // First submission: start fresh
      setSelectedPeerReviewerId('');
      setSelectedQAReviewerId('');
      setSelectedApproverId('');
      setTab('reviewers');
    }

    setLoadingReviewers(true);
    api.get<AppUser[]>(`/documents/${doc.id}/eligible-reviewers`)
      .then(res => setReviewers(res.data))
      .catch(() => setReviewers([]))
      .finally(() => setLoadingReviewers(false));

    setLoadingApprovers(true);
    api.get<AppUser[]>('/documents/eligible-approvers')
      .then(res => setApprovers(res.data))
      .catch(() => setApprovers([]))
      .finally(() => setLoadingApprovers(false));
  }, [open, doc.id, doc.trainingQuestions]);

  if (!open) return null;

  const handleSubmit = async () => {
    if (!selectedPeerReviewerId) {
      toast.error('Please select a peer reviewer');
      setTab('reviewers');
      return;
    }
    if (!selectedQAReviewerId) {
      toast.error('Please select a QA reviewer');
      setTab('reviewers');
      return;
    }
    if (!selectedApproverId) {
      toast.error('Please select an approver');
      setTab('reviewers');
      return;
    }
    setLoading(true);
    try {
      setStage('Saving document changes...');
      if (onPrepareTransition) await onPrepareTransition();
      setStage('Assigning reviewers and submitting...');
      const res = await api.post<ControlledDocument>(`/documents/${doc.id}/submit-draft`, {
        peerReviewerUserId: selectedPeerReviewerId,
        qaReviewerUserId: selectedQAReviewerId,
        approverUserId: selectedApproverId,
        trainingQuestions: questions,
      });
      toast.success('Draft submitted for review');
      onSuccess(res.data);
      onClose();
    } catch (err) {
      showErrorToast(err, 'Failed to submit draft');
    } finally {
      setLoading(false);
      setStage('');
    }
  };

  const allReviewersSelected = selectedPeerReviewerId && selectedQAReviewerId && selectedApproverId;
  const isLoadingUsers = loadingReviewers || loadingApprovers;

  const getUserName = (users: AppUser[], id: string) => users.find(u => u.id === id)?.fullName || '';

  // Reviewer assignment rows
  const assignmentSteps = [
    {
      label: 'Peer Review',
      stepNum: 4,
      description: 'Reviews document for technical accuracy',
      users: reviewers,
      selected: selectedPeerReviewerId,
      onSelect: setSelectedPeerReviewerId,
    },
    {
      label: 'QA Review',
      stepNum: 5,
      description: 'Verifies compliance with quality standards',
      users: reviewers,
      selected: selectedQAReviewerId,
      onSelect: setSelectedQAReviewerId,
    },
    {
      label: 'Final Approval',
      stepNum: 6,
      description: 'Director or HOD final sign-off',
      users: approvers,
      selected: selectedApproverId,
      onSelect: setSelectedApproverId,
    },
  ];

  return (
    <>
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40" onClick={onClose}>
        <div className="bg-white rounded-lg shadow-xl w-full max-w-lg mx-4 max-h-[85vh] flex flex-col" onClick={e => e.stopPropagation()}>
          <div className="px-5 py-4 border-b border-gray-200 flex items-center justify-between flex-shrink-0">
            <h3 className="text-base font-semibold text-gray-900">Submit for Review</h3>
            <button onClick={onClose} className="text-gray-400 hover:text-gray-600"><X size={18} /></button>
          </div>

          {/* Two tabs: Reviewers + Q&A */}
          <div className="flex border-b border-gray-200 flex-shrink-0">
            <button
              className={`flex-1 flex items-center justify-center gap-1.5 px-3 py-2.5 text-sm font-medium transition-colors ${
                tab === 'reviewers' ? 'text-gray-800 border-b-2 border-gray-800' : 'text-gray-400 hover:text-gray-600'
              }`}
              onClick={() => setTab('reviewers')}
            >
              Assign Reviewers
              {allReviewersSelected && <CheckCircle2 size={13} className="text-green-500" />}
            </button>
            <button
              className={`flex-1 flex items-center justify-center gap-1.5 px-3 py-2.5 text-sm font-medium transition-colors ${
                tab === 'questions' ? 'text-gray-800 border-b-2 border-gray-800' : 'text-gray-400 hover:text-gray-600'
              }`}
              onClick={() => setTab('questions')}
            >
              <GraduationCap size={15} /> Q&A ({questions.length})
            </button>
          </div>

          <div className="flex-1 overflow-y-auto p-5 relative">
            {loading && (
              <div className="absolute inset-0 bg-white/80 z-10 flex flex-col items-center justify-center gap-2">
                <Loader2 size={24} className="animate-spin text-gray-500" />
                <p className="text-sm text-gray-500 font-medium">{stage || 'Processing...'}</p>
              </div>
            )}
            {/* ─── Reviewers Tab: single view with all 3 assignments ─── */}
            {tab === 'reviewers' && (
              <div className="space-y-4">
                {isResubmission ? (
                  <div className="flex items-center gap-2 text-sm text-gray-600 bg-gray-50 rounded-lg px-3 py-2">
                    <CheckCircle2 size={14} className="text-green-500 flex-shrink-0" />
                    Reviewers are already assigned from the previous submission.
                  </div>
                ) : (
                  <p className="text-sm text-gray-500">
                    Assign a reviewer for each workflow step. All must be selected before submitting.
                  </p>
                )}

                {isLoadingUsers ? (
                  <p className="text-sm text-gray-400 text-center py-6">Loading eligible users...</p>
                ) : (
                  <div className="space-y-3">
                    {assignmentSteps.map((step) => (
                      <div key={step.label} className={`border rounded-lg p-3 ${isResubmission ? 'border-gray-100 bg-gray-50' : 'border-gray-200'}`}>
                        <div className="flex items-center justify-between mb-1.5">
                          <div>
                            <p className="text-sm font-medium text-gray-800">{step.label}</p>
                            <p className="text-xs text-gray-400">{step.description}</p>
                          </div>
                          {step.selected && (
                            <CheckCircle2 size={16} className="text-green-500 flex-shrink-0" />
                          )}
                        </div>
                        <div className="relative">
                          <select
                            className="input w-full appearance-none pr-8 text-sm"
                            value={step.selected}
                            onChange={e => step.onSelect(e.target.value)}
                            disabled={isResubmission}
                          >
                            <option value="">Select assignee...</option>
                            {step.users.map(u => (
                              <option key={u.id} value={u.id}>
                                {u.fullName} — {u.role?.replace(/_/g, ' ')} · {u.unitDisplayName ?? u.unitId}
                              </option>
                            ))}
                          </select>
                          <ChevronDown size={14} className="absolute right-2.5 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
                        </div>
                      </div>
                    ))}
                  </div>
                )}

                {!allReviewersSelected && !isLoadingUsers && !isResubmission && (
                  <div className="flex items-center gap-2 text-sm text-amber-600 bg-amber-50 rounded-lg px-3 py-2">
                    <AlertCircle size={14} /> All three reviewers must be assigned
                  </div>
                )}
              </div>
            )}

            {/* ─── Q&A Tab: shows questions with correct answers highlighted ─── */}
            {tab === 'questions' && (
              <div className="space-y-3">
                <p className="text-sm text-gray-500">
                  Training questions for the TMS. These will be used for quizzes after this document is published.
                </p>
                {questions.length > 0 ? (
                  <div className="space-y-3">
                    {questions.map((q, i) => (
                      <div key={q.questionId} className="border border-gray-200 rounded-lg p-3 space-y-2">
                        <div className="flex items-start gap-2">
                          <span className="text-xs font-bold text-gray-500 bg-gray-100 rounded px-1.5 py-0.5 flex-shrink-0">
                            Q{i + 1}
                          </span>
                          <p className="text-sm font-medium text-gray-800 flex-1">{q.questionText || '(empty)'}</p>
                          <span className="text-[10px] text-gray-400 uppercase tracking-wide flex-shrink-0">
                            {q.questionType === 'TRUE_FALSE' ? 'T/F' : 'MCQ'}
                          </span>
                        </div>
                        <div className="ml-7 space-y-1">
                          {q.options.map((opt, oi) => {
                            const isCorrect = oi === q.correctAnswerIndex;
                            return (
                              <div
                                key={oi}
                                className={`flex items-center gap-2 text-sm rounded px-2 py-1 ${
                                  isCorrect
                                    ? 'bg-green-50 text-green-700 font-medium'
                                    : 'text-gray-500'
                                }`}
                              >
                                <span className="font-mono text-xs w-4 flex-shrink-0">
                                  {String.fromCharCode(65 + oi)}.
                                </span>
                                <span className="flex-1">{opt || '—'}</span>
                                {isCorrect && (
                                  <CheckCircle2 size={13} className="text-green-500 flex-shrink-0" />
                                )}
                              </div>
                            );
                          })}
                        </div>
                        {q.explanation && (
                          <p className="ml-7 text-xs text-gray-500 italic bg-gray-50 rounded px-2 py-1.5">
                            Explanation: {q.explanation}
                          </p>
                        )}
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="text-center py-6">
                    <GraduationCap size={32} className="text-gray-300 mx-auto mb-2" />
                    <p className="text-sm text-gray-400">No questions added yet</p>
                  </div>
                )}
                <button
                  className="btn-primary text-sm px-4 py-1.5 w-full"
                  onClick={() => setShowQABuilder(true)}
                >
                  {questions.length > 0 ? 'Edit Questions' : 'Add Questions'}
                </button>
              </div>
            )}
          </div>

          {/* Summary footer */}
          <div className="px-5 py-2 border-t border-gray-100 bg-gray-50 flex-shrink-0">
            <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-gray-500">
              <span>
                Peer: <strong className={selectedPeerReviewerId ? 'text-green-600' : 'text-red-400'}>
                  {selectedPeerReviewerId ? getUserName(reviewers, selectedPeerReviewerId) : 'Not set'}
                </strong>
              </span>
              <span>
                QA: <strong className={selectedQAReviewerId ? 'text-green-600' : 'text-red-400'}>
                  {selectedQAReviewerId ? getUserName(reviewers, selectedQAReviewerId) : 'Not set'}
                </strong>
              </span>
              <span>
                Approver: <strong className={selectedApproverId ? 'text-green-600' : 'text-red-400'}>
                  {selectedApproverId ? getUserName(approvers, selectedApproverId) : 'Not set'}
                </strong>
              </span>
              <span>Q&A: <strong>{questions.length}</strong></span>
            </div>
          </div>

          <div className="px-5 py-3 border-t border-gray-200 flex justify-end gap-2 flex-shrink-0">
            <button className="btn text-sm px-4 py-1.5 border border-gray-200 text-gray-600 hover:bg-gray-50 rounded-md" onClick={onClose}>
              Cancel
            </button>
            <button className="btn-primary text-sm px-4 py-1.5" onClick={handleSubmit} disabled={loading}>
              {loading ? <><Loader2 size={14} className="animate-spin" /> Submitting...</> : 'Submit for Review'}
            </button>
          </div>
        </div>
      </div>

      <TrainingQABuilderModal
        open={showQABuilder}
        initialQuestions={questions}
        onClose={() => setShowQABuilder(false)}
        onSave={(q) => { setQuestions(q); setShowQABuilder(false); }}
      />
    </>
  );
}
