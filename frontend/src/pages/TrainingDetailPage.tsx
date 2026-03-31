import { useEffect, useState, useRef, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  GraduationCap, BookOpen, CheckCircle2, Loader2,
  ArrowLeft, ArrowRight, Clock, AlertCircle, XCircle, RotateCcw,
} from 'lucide-react';
import { TablePageSkeleton } from '../components/ui/PageSkeleton';
import api from '../api/client';
import type { TrainingAssignment, ControlledDocument, EditorConfig, TrainingQuestion, QuizAnswer, DocumentDetailResponse } from '../types';
import SignatureModal from '../components/SignatureModal';
import toast from 'react-hot-toast';
import { showErrorToast } from '../utils/errorHandler';

const STEPS = ['Read Document', 'Take Quiz', 'Acknowledge'];
const MAX_ATTEMPTS = 3;

function stepIndex(status: string): number {
  switch (status) {
    case 'ASSIGNED': case 'IN_PROGRESS': return 0;
    case 'READ': return 1;
    case 'QUIZ_PASSED': return 2;
    case 'COMPLETED': return 3;
    case 'FAILED': return -1;
    default: return 0;
  }
}

export default function TrainingDetailPage() {
  const { id } = useParams();
  const [assignment, setAssignment] = useState<TrainingAssignment | null>(null);
  const [doc, setDoc] = useState<ControlledDocument | null>(null);
  const [loading, setLoading] = useState(true);
  const [activeStep, setActiveStep] = useState(0);

  // Quiz state
  const [answers, setAnswers] = useState<Record<string, number>>({});
  const [quizSubmitted, setQuizSubmitted] = useState(false);
  const [quizResult, setQuizResult] = useState<{ score: number; total: number; passed: boolean } | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // Reading timer
  const readStartRef = useRef<number>(0);
  const [readSeconds, setReadSeconds] = useState(0);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // Re-reading: user chose to re-read document after a failed quiz attempt
  const [reReading, setReReading] = useState(false);

  // Signature
  const [showSignature, setShowSignature] = useState(false);

  // OnlyOffice editor state
  const [editorLoading, setEditorLoading] = useState(false);
  const [editorError, setEditorError] = useState<string | null>(null);
  const editorInstanceRef = useRef<unknown>(null);
  const scriptLoadedRef = useRef(false);
  // Whether the viewer is currently shown (reading step or re-reading)
  const showViewer = assignment != null && assignment.status !== 'ASSIGNED' &&
    (activeStep === 0 || reReading);

  const loadData = useCallback(async () => {
    try {
      const aRes = await api.get<TrainingAssignment>(`/training/${id}`);
      setAssignment(aRes.data);
      setActiveStep(stepIndex(aRes.data.status));

      const dRes = await api.get<DocumentDetailResponse>(`/documents/${aRes.data.documentId}`);
      setDoc(dRes.data.document);
    } catch (err) {
      showErrorToast(err, 'Failed to load training');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { loadData(); }, [loadData]);

  // OnlyOffice editor lifecycle
  const destroyEditor = useCallback(() => {
    if (editorInstanceRef.current) {
      try {
        (editorInstanceRef.current as { destroyEditor?: () => void }).destroyEditor?.();
      } catch { /* ignore */ }
      editorInstanceRef.current = null;
    }
    const el = document.getElementById('training-doc-viewer');
    if (el) el.innerHTML = '';
  }, []);

  const initEditor = useCallback((config: EditorConfig) => {
    requestAnimationFrame(() => {
      try {
        const el = document.getElementById('training-doc-viewer');
        if (!el) { setEditorError('Viewer container not found.'); setEditorLoading(false); return; }

        const DocsAPI = (window as unknown as Record<string, unknown>).DocsAPI as
          { DocEditor: new (id: string, cfg: Record<string, unknown>) => unknown } | undefined;
        if (!DocsAPI) { setEditorError('Document viewer failed to initialize.'); setEditorLoading(false); return; }

        destroyEditor();
        const editorCfg = {
          ...config.config,
          events: {
            onError: (event: { data: unknown }) => console.error('ONLYOFFICE error:', event.data),
            onReady: () => console.log('ONLYOFFICE ready'),
            onDocumentReady: () => console.log('ONLYOFFICE document loaded'),
          },
        };
        editorInstanceRef.current = new DocsAPI.DocEditor('training-doc-viewer', editorCfg);
        setEditorLoading(false);
      } catch (err) {
        console.error('Failed to initialize document viewer:', err);
        setEditorError('Failed to initialize the document viewer.');
        setEditorLoading(false);
      }
    });
  }, [destroyEditor]);

  // Load editor when viewer should be shown
  useEffect(() => {
    if (!doc?.id || !showViewer) return;
    let cancelled = false;

    const loadViewer = async () => {
      setEditorLoading(true);
      setEditorError(null);
      destroyEditor();
      try {
        const res = await api.get<DocumentDetailResponse>(`/documents/${doc.id}`);
        if (cancelled) return;
        if (!res.data.editorConfig) {
          setEditorError('Editor configuration not available.');
          setEditorLoading(false);
          return;
        }
        const config = res.data.editorConfig;

        const scriptUrl = `${config.documentServerUrl}/web-apps/apps/api/documents/api.js`;
        if (scriptLoadedRef.current && (window as unknown as Record<string, unknown>).DocsAPI) {
          initEditor(config);
          return;
        }
        const existing = document.querySelector(`script[src="${scriptUrl}"]`);
        if (existing) existing.remove();

        const script = document.createElement('script');
        script.src = scriptUrl;
        script.async = true;
        script.onload = () => { if (!cancelled) { scriptLoadedRef.current = true; initEditor(config); } };
        script.onerror = () => { if (!cancelled) { setEditorError('Failed to load Document Server.'); setEditorLoading(false); } };
        document.head.appendChild(script);
      } catch {
        if (!cancelled) { setEditorError('Failed to load document viewer.'); setEditorLoading(false); }
      }
    };
    loadViewer();

    return () => { cancelled = true; };
  }, [doc?.id, showViewer, initEditor, destroyEditor]);

  // Destroy editor when viewer is hidden
  useEffect(() => {
    if (!showViewer) destroyEditor();
  }, [showViewer, destroyEditor]);

  // Cleanup on unmount
  useEffect(() => {
    return () => { destroyEditor(); };
  }, [destroyEditor]);

  // Start reading timer when on step 0 and reading
  useEffect(() => {
    if (activeStep === 0 && assignment && assignment.status !== 'ASSIGNED') {
      readStartRef.current = Date.now();
      timerRef.current = setInterval(() => {
        setReadSeconds(Math.floor((Date.now() - readStartRef.current) / 1000));
      }, 1000);
    }
    return () => { if (timerRef.current) clearInterval(timerRef.current); };
  }, [activeStep, assignment]);

  const handleStartReading = async () => {
    try {
      const res = await api.post<TrainingAssignment>(`/training/${id}/start-reading`);
      setAssignment(res.data);
      readStartRef.current = Date.now();
      toast.success('Reading started — review the document below');
    } catch (err) {
      showErrorToast(err, 'Failed to start reading');
    }
  };

  const handleCompleteReading = async () => {
    if (timerRef.current) clearInterval(timerRef.current);
    const duration = assignment?.readDurationSeconds || readSeconds || 30;
    try {
      const res = await api.post<TrainingAssignment>(`/training/${id}/complete-reading`, { durationSeconds: duration });
      setAssignment(res.data);
      setActiveStep(1);
      setReReading(false);
      toast.success('Reading completed — proceed to quiz');
    } catch (err) {
      showErrorToast(err, 'Failed to complete reading');
    }
  };

  const handleBackToQuiz = () => {
    setReReading(false);
    setActiveStep(1);
  };

  const handleReReadDocument = () => {
    setReReading(true);
    setQuizSubmitted(false);
    setQuizResult(null);
    setAnswers({});
  };

  const handleSubmitQuiz = async () => {
    if (!doc || !assignment) return;
    const questions = doc.trainingQuestions || [];
    if (Object.keys(answers).length < questions.length) {
      toast.error('Please answer all questions');
      return;
    }
    setSubmitting(true);
    try {
      const quizAnswers: QuizAnswer[] = questions.map(q => ({
        questionId: q.questionId,
        selectedAnswerIndex: answers[q.questionId] ?? -1,
      }));
      const res = await api.post<TrainingAssignment>(`/training/${id}/submit-quiz`, {
        answers: quizAnswers,
        questions,
      });
      setAssignment(res.data);

      const latest = res.data.quizAttempts[res.data.quizAttempts.length - 1];
      setQuizResult({ score: latest.score, total: latest.totalQuestions, passed: latest.passed });
      setQuizSubmitted(true);

      if (latest.passed) {
        setActiveStep(2);
        toast.success(`Quiz passed! Score: ${latest.score}/${latest.totalQuestions}`);
      } else if (res.data.status === 'FAILED') {
        setActiveStep(-1);
        toast.error(`Training failed. All ${MAX_ATTEMPTS} attempts used.`);
      } else {
        toast.error(`Quiz not passed. Score: ${latest.score}/${latest.totalQuestions}. You can retry.`);
      }
    } catch (err) {
      showErrorToast(err, 'Failed to submit quiz');
    } finally {
      setSubmitting(false);
    }
  };

  const handleAcknowledge = async (signatureData: string) => {
    try {
      const res = await api.post<TrainingAssignment>(`/training/${id}/acknowledge`, { signatureData });
      setAssignment(res.data);
      setActiveStep(3);
      setShowSignature(false);
      toast.success('Training completed!');
    } catch (err) {
      showErrorToast(err, 'Failed to acknowledge');
    }
  };

  if (loading) {
    return <TablePageSkeleton />;
  }

  if (!assignment || !doc) {
    return <div className="card p-6 text-center text-gray-500">Training assignment not found</div>;
  }

  const questions = doc.trainingQuestions || [];
  const completed = assignment.status === 'COMPLETED';
  const failed = assignment.status === 'FAILED';
  const attemptsUsed = assignment.quizAttempts.length;
  const attemptsRemaining = MAX_ATTEMPTS - attemptsUsed;

  // Effective step for stepper display (reReading shows step 0 visually)
  const displayStep = reReading ? 0 : activeStep;

  return (
    <div className="flex flex-col" style={{ height: 'calc(100vh - 64px)' }}>
      {/* Compact header bar */}
      <div className="flex items-center gap-3 px-4 py-2 border-b border-gray-200 bg-white shrink-0">
        <Link to="/" className="text-gray-400 hover:text-gray-600"><ArrowLeft size={18} /></Link>
        <div className="min-w-0">
          <h2 className="text-sm font-semibold text-gray-900 truncate flex items-center gap-1.5">
            <GraduationCap size={16} /> {assignment.documentTitle}
          </h2>
          <p className="text-[11px] text-gray-400 truncate">
            {assignment.documentNumber} &middot; Due {new Date(assignment.dueDate).toLocaleDateString()}
            {assignment.assignedByUsername && <> &middot; Assigned by {assignment.assignedByUsername}</>}
          </p>
        </div>

        {/* Inline stepper */}
        <div className="flex items-center gap-1.5 ml-auto mr-3">
          {STEPS.map((step, i) => {
            const done = (i < displayStep || completed) && !failed;
            const current = i === displayStep && !completed && !failed;
            const isFailed = failed && i === 1;
            return (
              <div key={step} className="flex items-center gap-1.5">
                <div className={`w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-bold ${
                  isFailed ? 'bg-red-600 text-white' :
                  done ? 'bg-gray-700 text-white' :
                  current ? 'bg-gray-800 text-white ring-1 ring-gray-300' :
                  'bg-gray-200 text-gray-400'
                }`}>
                  {isFailed ? <XCircle size={10} /> : done ? <CheckCircle2 size={10} /> : i + 1}
                </div>
                <span className={`text-xs hidden sm:inline ${
                  isFailed ? 'text-red-700 font-semibold' :
                  current ? 'text-gray-800 font-semibold' :
                  done ? 'text-gray-500' : 'text-gray-400'
                }`}>{step}</span>
                {i < STEPS.length - 1 && <div className={`w-6 h-px ${i < displayStep && !failed ? 'bg-gray-400' : 'bg-gray-200'}`} />}
              </div>
            );
          })}
        </div>

        {/* Action button / status badge */}
        {completed ? (
          <span className="px-3 py-1 rounded-full text-xs font-semibold bg-gray-100 text-gray-600 shrink-0">Completed</span>
        ) : failed ? (
          <span className="px-3 py-1 rounded-full text-xs font-semibold bg-red-100 text-red-700 shrink-0">Failed</span>
        ) : reReading ? (
          <div className="flex items-center gap-2 shrink-0">
            <button className="text-xs px-3 py-1.5 border border-gray-300 rounded-md text-gray-600 hover:bg-gray-50" onClick={handleBackToQuiz}>
              Back to Quiz
            </button>
            <button className="btn-primary text-xs px-4 py-1.5 flex items-center gap-1" onClick={handleCompleteReading}>
              Done Reading <ArrowRight size={12} />
            </button>
          </div>
        ) : activeStep === 0 && assignment.status === 'ASSIGNED' ? (
          <button className="btn-primary text-xs px-4 py-1.5 shrink-0" onClick={handleStartReading}>
            Start Reading
          </button>
        ) : activeStep === 0 ? (
          <div className="flex items-center gap-2 shrink-0">
            <span className="flex items-center gap-1 text-[11px] text-gray-400">
              <Clock size={11} /> {Math.floor(readSeconds / 60)}:{(readSeconds % 60).toString().padStart(2, '0')}
            </span>
            <button className="btn-primary text-xs px-4 py-1.5 flex items-center gap-1" onClick={handleCompleteReading}>
              Mark as Read <ArrowRight size={12} />
            </button>
          </div>
        ) : activeStep === 1 ? (
          <span className="text-xs text-gray-500 shrink-0">
            Attempt {attemptsUsed + (quizSubmitted ? 0 : 1)}/{MAX_ATTEMPTS}
          </span>
        ) : activeStep === 2 ? (
          <button className="btn-primary text-xs px-4 py-1.5 shrink-0" onClick={() => setShowSignature(true)}>
            Sign & Acknowledge
          </button>
        ) : null}
      </div>

      {/* Content area — fills remaining height */}
      <div className="flex-1 overflow-hidden relative">
        {/*
          OnlyOffice viewer — ALWAYS rendered to avoid React removeChild errors.
          Hidden via display:none when not in reading mode.
          OnlyOffice manipulates DOM directly, so conditional mount/unmount
          causes "removeChild" conflicts with React's virtual DOM.
        */}
        <div
          className="absolute inset-0"
          style={{ display: showViewer ? 'block' : 'none' }}
        >
          {editorLoading && (
            <div className="absolute inset-0 flex items-center justify-center bg-white z-10">
              <Loader2 size={28} className="animate-spin text-gray-400" />
              <span className="ml-2 text-sm text-gray-500">Loading document...</span>
            </div>
          )}
          {editorError && (
            <div className="absolute inset-0 flex flex-col items-center justify-center bg-white z-10">
              <AlertCircle size={28} className="text-red-400 mb-2" />
              <p className="text-sm text-red-600">{editorError}</p>
              <button
                className="mt-3 text-sm text-gray-600 underline hover:text-gray-800"
                onClick={() => { setEditorError(null); setEditorLoading(true); loadData(); }}
              >
                Retry
              </button>
            </div>
          )}
          <div id="training-doc-viewer" style={{ width: '100%', height: '100%' }} />
        </div>

        {/* Non-viewer content (only shown when viewer is hidden) */}
        {!showViewer && (
          <div className="h-full overflow-auto">
            {/* FAILED state */}
            {failed ? (
              <div className="flex flex-col items-center justify-center h-full">
                <XCircle size={48} className="text-red-500 mb-3" />
                <h3 className="text-lg font-semibold text-gray-900">Training Failed</h3>
                <p className="text-sm text-gray-500 mt-1">
                  All {MAX_ATTEMPTS} quiz attempts used without achieving the passing score (80%).
                </p>
                <p className="text-sm text-gray-500 mt-3">Contact your manager for reassignment.</p>
                {assignment.failedAt && (
                  <p className="text-xs text-gray-400 mt-2">Failed on {new Date(assignment.failedAt).toLocaleDateString()}</p>
                )}
              </div>
            ) : completed ? (
              <div className="flex flex-col items-center justify-center h-full">
                <CheckCircle2 size={48} className="text-gray-500 mb-3" />
                <h3 className="text-lg font-semibold text-gray-900">Training Completed</h3>
                <p className="text-sm text-gray-500 mt-1">
                  Completed on {assignment.acknowledgedAt ? new Date(assignment.acknowledgedAt).toLocaleDateString() : '—'}
                </p>
              </div>
            ) : activeStep === 0 && assignment.status === 'ASSIGNED' ? (
              <div className="flex flex-col items-center justify-center h-full">
                <BookOpen size={48} className="text-gray-300 mb-3" />
                <p className="text-sm text-gray-500">Click "Start Reading" to begin your training</p>
              </div>
            ) : activeStep === 1 ? (
              /* Quiz */
              <div className="max-w-3xl mx-auto p-6 space-y-4">
                {questions.length === 0 ? (
                  <div className="text-center py-8">
                    <p className="text-sm text-gray-500">No quiz questions for this document. Proceed to acknowledgement.</p>
                    <button className="btn-primary text-sm px-6 py-2 mt-4" onClick={() => setActiveStep(2)}>
                      Skip to Acknowledge <ArrowRight size={14} />
                    </button>
                  </div>
                ) : quizSubmitted && quizResult && !quizResult.passed ? (
                  <div className="space-y-4">
                    <div className="bg-red-50 rounded-lg p-4 flex items-start gap-3">
                      <AlertCircle size={18} className="text-red-500 mt-0.5" />
                      <div>
                        <p className="text-sm font-medium text-red-800">Quiz not passed</p>
                        <p className="text-xs text-red-600 mt-0.5">
                          Score: {quizResult.score}/{quizResult.total} (need 80% to pass).
                          Attempts used: {attemptsUsed} of {MAX_ATTEMPTS}.
                        </p>
                      </div>
                    </div>
                    {attemptsRemaining > 0 ? (
                      <div className="flex items-center gap-3">
                        <button
                          className="text-sm px-5 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 flex items-center gap-1.5"
                          onClick={handleReReadDocument}
                        >
                          <RotateCcw size={14} /> Re-read Document
                        </button>
                        <button
                          className="btn-primary text-sm px-5 py-2"
                          onClick={() => { setQuizSubmitted(false); setQuizResult(null); setAnswers({}); }}
                        >
                          Retry Quiz ({attemptsRemaining} {attemptsRemaining === 1 ? 'attempt' : 'attempts'} left)
                        </button>
                      </div>
                    ) : (
                      <p className="text-sm text-red-600 font-medium">
                        All attempts used. Contact your manager for reassignment.
                      </p>
                    )}
                  </div>
                ) : (
                  <>
                    <div className="space-y-4">
                      {questions.map((q: TrainingQuestion, i: number) => (
                        <div key={q.questionId} className="bg-gray-50 rounded-lg p-4">
                          <p className="text-sm font-semibold text-gray-800 mb-3">Q{i + 1}. {q.questionText}</p>
                          <div className="space-y-2">
                            {q.options.map((opt: string, oi: number) => (
                              <label key={oi} className={`flex items-center gap-2.5 px-3 py-2 rounded-md cursor-pointer transition-colors ${
                                answers[q.questionId] === oi ? 'bg-gray-200' : 'hover:bg-gray-100'
                              }`}>
                                <input
                                  type="radio"
                                  name={`q-${q.questionId}`}
                                  checked={answers[q.questionId] === oi}
                                  onChange={() => setAnswers(prev => ({ ...prev, [q.questionId]: oi }))}
                                  className="accent-gray-800"
                                />
                                <span className="text-sm text-gray-700">{String.fromCharCode(65 + oi)}. {opt}</span>
                              </label>
                            ))}
                          </div>
                        </div>
                      ))}
                    </div>
                    <div className="flex justify-end">
                      <button className="btn-primary text-sm px-6 py-2" onClick={handleSubmitQuiz} disabled={submitting}>
                        {submitting ? 'Submitting...' : 'Submit Quiz'}
                      </button>
                    </div>
                  </>
                )}
              </div>
            ) : activeStep === 2 ? (
              /* Acknowledge */
              <div className="max-w-2xl mx-auto p-6 space-y-4">
                <div className="bg-gray-50 rounded-lg p-4 text-sm text-gray-600 space-y-2">
                  <p>By signing below, I acknowledge that I have:</p>
                  <ul className="list-disc ml-5 space-y-1">
                    <li>Read and understood the document: <strong>{doc.title}</strong></li>
                    <li>Passed the training quiz</li>
                    <li>Committed to following the procedures outlined in the document</li>
                  </ul>
                </div>
              </div>
            ) : null}
          </div>
        )}
      </div>

      <SignatureModal
        open={showSignature}
        onClose={() => setShowSignature(false)}
        onSubmit={(sig) => handleAcknowledge(sig)}
        title="Training Acknowledgement"
        submitLabel="Sign & Complete"
      />
    </div>
  );
}
