import { useState, useEffect, useCallback, useRef } from 'react';
import { useParams } from 'react-router-dom';
import { 
  Loader2, 
  ChevronLeft, 
  ChevronRight,
  Shield,
  MessageSquare,
  History,
  GraduationCap
} from 'lucide-react';
import api from '../api/client';
import AIChatPanel from '../components/AIChatPanel';
import DocumentHeader from '../components/document/DocumentHeader';
import WorkflowStepper from '../components/document/WorkflowStepper';
import OnlyOfficeEditor from '../components/document/OnlyOfficeEditor';
import WorkflowStatusPanel from '../components/document/WorkflowStatusPanel';
import QAPreparationModal from '../components/QAPreparationModal';
import SubmitDraftModal from '../components/SubmitDraftModal';
import ReviewActionModal from '../components/ReviewActionModal';
import ApprovalModal from '../components/ApprovalModal';
import { ControlledDocument, DocumentDetailResponse } from '../types';
import toast from 'react-hot-toast';

/**
 * DocumentDetailPage - Refactored for better code management and scalability.
 * Coordinatets sub-components: Header, Stepper, Editor, and Side Panel.
 */
export default function DocumentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [doc, setDoc] = useState<ControlledDocument | null>(null);
  const [loading, setLoading] = useState(true);
  const [panelOpen, setPanelOpen] = useState(true);
  const [activeTab, setActiveTab] = useState<'ai' | 'workflow' | 'training'>('ai');
  
  // Editor coordination
  const [editorMountKey, setEditorMountKey] = useState(0);
  const editorInstanceRef = useRef<any>(null);
  const [insertStatus, setInsertStatus] = useState<'idle' | 'saving' | 'inserting' | 'reloading' | 'error'>('idle');

  // Modal control
  const [showPrepareModal, setShowPrepareModal] = useState(false);
  const [showSubmitDraftModal, setShowSubmitDraftModal] = useState(false);
  const [showReviewModal, setShowReviewModal] = useState(false);
  const [reviewMode, setReviewMode] = useState<'approve' | 'reject'>('approve');
  const [showApprovalModal, setShowApprovalModal] = useState(false);
  const [approvalMode, setApprovalMode] = useState<'approve' | 'reject'>('approve');

  // ── Data Fetching ───────────────────────────────────────────────────────────

  const fetchDocument = useCallback(async () => {
    if (!id) return;
    try {
      const res = await api.get<DocumentDetailResponse>(`/documents/${id}`);
      setDoc(res.data.document);
      // Auto-switch to training if questions exist but no AI context
      if (res.data.document.trainingQuestions.length > 0 && activeTab === 'ai' && !doc) {
        setActiveTab('training');
      }
    } catch (err) {
      console.error('Failed to load document', err);
      toast.error('Failed to load document details');
    } finally {
      setLoading(false);
    }
  }, [id, activeTab, doc]);

  useEffect(() => {
    fetchDocument();
    // Cleanup on unmount handled by sub-components
  }, [id]); // Reset only when ID changes

  // ── DMS Lifecycle Operations (RESTORED FROM BACKUP) ───────────────────────

  /**
   * prepareForTransition - Ensures OnlyOffice saves all pending changes before status transition.
   * This prevents data loss during workflow movements.
   */
  const prepareForTransition = useCallback(async (): Promise<void> => {
    const fileId = doc?.documentFileId;
    const hadEditor = !!editorInstanceRef.current;
    
    // 1. Snapshot time & kill editor
    const beforeDestroy = Date.now();
    if (hadEditor) {
      try { editorInstanceRef.current.destroyEditor(); } catch (e) { /* ignore */ }
      editorInstanceRef.current = null;
    }
    
    // Fast-path: if no editor, nothing to save
    if (!fileId || !hadEditor) return;

    // 2. Poll backend for save confirmation (OnlyOffice callback loop)
    const maxWait = 8000;
    const interval = 200;
    const startTime = Date.now();
    while (Date.now() - startTime < maxWait) {
      try {
        const res = await api.get<{ saved: boolean }>(`/files/${fileId}/save-status?after=${beforeDestroy}`);
        if (res.data.saved) return;
      } catch { } // network glitch, keep polling
      await new Promise(r => setTimeout(r, interval));
    }
    console.warn('Transition sync timeout—proceeding with best effort');
  }, [doc?.documentFileId]);

  /**
   * insertIntoDocument - Injects AI generated content into the file.
   * Uses prepareForTransition to avoid race conditions.
   */
  const insertIntoDocument = async (text: string, sectionLabel?: string) => {
    if (!doc) return;
    setInsertStatus('saving');
    try {
      await prepareForTransition();
      setInsertStatus('inserting');
      await api.post(`/ai/insert/${doc.id}`, { content: text, sectionLabel });
      
      setInsertStatus('reloading');
      setEditorMountKey(k => k + 1); // Force clean editor remount
      await fetchDocument();
      toast.success('Successfully updated document content');
    } catch (err) {
      setInsertStatus('error');
      console.error('Insert failed', err);
      toast.error('Failed to update document with AI content');
    } finally {
      setInsertStatus('idle');
    }
  };

  const handleDocUpdate = async () => {
    setEditorMountKey(k => k + 1);
    await fetchDocument();
  };

  const handleShowModal = (type: string, mode: any = 'approve') => {
    if (type === 'prepare') setShowPrepareModal(true);
    if (type === 'submit') setShowSubmitDraftModal(true);
    if (type === 'review') { setReviewMode(mode); setShowReviewModal(true); }
    if (type === 'approval') { setApprovalMode(mode); setShowApprovalModal(true); }
  };

  // ── Render Logic ────────────────────────────────────────────────────────────

  if (loading) return (
    <div className="flex flex-col items-center justify-center h-full bg-gray-50">
      <Loader2 className="animate-spin h-10 w-10 text-brand-600 mb-4" />
      <span className="text-xs font-bold text-gray-400 uppercase tracking-widest">Gathering document trace...</span>
    </div>
  );

  if (!doc) return (
    <div className="p-8 text-center text-gray-500 border border-dashed rounded-2xl m-8">
      Document trace not found or session expired.
    </div>
  );

  return (
    <div className="flex flex-col h-full bg-slate-50 overflow-hidden font-inter">
      
      {/* ── Composable Header & Stepper (DMS Control Center) ── */}
      <DocumentHeader 
        doc={doc} 
        panelOpen={panelOpen} 
        setPanelOpen={setPanelOpen} 
        onPrepareForTransition={prepareForTransition}
        onShowModal={handleShowModal}
      />
      
      <WorkflowStepper doc={doc} />

      {/* ── AI Context Sync Banner ── */}
      {insertStatus !== 'idle' && (
        <div className={`px-5 py-2 text-[11px] font-bold border-b transition-all flex items-center justify-between ${
          insertStatus === 'error' ? 'bg-red-50 text-red-700 border-red-100' : 'bg-brand-50 text-brand-700 border-brand-100 shadow-[inset_0_1px_2px_rgba(0,0,0,0.05)]'
        }`}>
          <span className="flex items-center gap-3">
            {insertStatus !== 'error' ? <Loader2 size={13} className="animate-spin text-brand-600" /> : <Shield size={13} />}
            {insertStatus === 'saving' && 'FLUSHING EDITOR CHANGES TO VAULT...'}
            {insertStatus === 'inserting' && 'ORCHESTRATING AI CONTENT INJECTION...'}
            {insertStatus === 'reloading' && 'SYNCHRONIZING DOCUMENT VIEW...'}
            {insertStatus === 'error' && 'INTEGRATION ERROR: DATA SYNC FAILED'}
          </span>
          <span className="text-[9px] opacity-60 uppercase tracking-tighter">Powered by PharmaAI Gateway</span>
        </div>
      )}

      {/* ── Main Viewmarket Area ── */}
      <div className="flex flex-1 min-h-0 relative">
        
        {/* Editor Zone */}
        <div className="flex-1 flex flex-col bg-white overflow-hidden relative shadow-[inset_0_4px_12px_rgba(0,0,0,0.05)]">
          <OnlyOfficeEditor 
            id={id!} 
            doc={doc} 
            onInit={(inst) => { editorInstanceRef.current = inst; }} 
            editorMountKey={editorMountKey} 
          />
          
          <button 
            onClick={() => setPanelOpen(!panelOpen)}
            className="absolute right-0 top-1/2 -translate-y-1/2 bg-white/90 backdrop-blur border-y border-l border-gray-200 rounded-l-xl p-1.5 shadow-2xl hover:bg-white z-30 transition-all active:scale-95"
            title={panelOpen ? "Hide Side Panel" : "Show Side Panel"}
          >
            {panelOpen ? <ChevronRight size={18} className="text-gray-400" /> : <ChevronLeft size={18} className="text-brand-600" />}
          </button>
        </div>

        {/* Action Panel (Workflow, AI, Training) */}
        {panelOpen && (
          <div className="w-80 flex-shrink-0 border-l border-gray-200 bg-white flex flex-col overflow-hidden shadow-2xl z-20 transition-all duration-300">
            {/* Panel Tabs */}
            <div className="flex border-b border-gray-100 bg-gray-50/50 p-1.5 gap-1.5">
               <button 
                 onClick={() => setActiveTab('ai')} 
                 className={`flex-1 py-2 text-[10px] font-bold uppercase tracking-tight flex items-center justify-center gap-2 rounded-lg transition-all ${
                   activeTab === 'ai' ? 'bg-white text-brand-600 shadow-xl shadow-brand-500/10 border border-brand-50' : 'text-gray-400 hover:text-gray-600'
                 }`}
               >
                 <MessageSquare size={14} /> AI Support
               </button>
               <button 
                 onClick={() => setActiveTab('workflow')} 
                 className={`flex-1 py-2 text-[10px] font-bold uppercase tracking-tight flex items-center justify-center gap-2 rounded-lg transition-all ${
                   activeTab === 'workflow' ? 'bg-white text-gray-800 shadow-md border border-gray-100' : 'text-gray-400 hover:text-gray-600'
                 }`}
               >
                 <History size={14} /> Tracking
               </button>
               {doc.trainingQuestions.length > 0 && (
                 <button 
                   onClick={() => setActiveTab('training')} 
                   className={`flex-1 py-2 text-[10px] font-bold uppercase tracking-tight flex items-center justify-center gap-2 rounded-lg transition-all ${
                     activeTab === 'training' ? 'bg-white text-emerald-600 shadow-md border border-emerald-50' : 'text-gray-400 hover:text-gray-600'
                   }`}
                 >
                   <GraduationCap size={14} /> Quiz
                 </button>
               )}
            </div>

            {/* Dynamic Panel Context */}
            <div className="flex-1 flex flex-col min-h-0 relative overflow-hidden">
               {activeTab === 'workflow' && <WorkflowStatusPanel steps={doc.workflowSteps} />}
               
               {activeTab === 'ai' && (
                 <AIChatPanel 
                   documentId={doc.id} 
                   documentStatus={doc.status} 
                   onInsertContent={doc.status === 'AUTHOR_DRAFT' ? insertIntoDocument : undefined} 
                 />
               )}
               
               {activeTab === 'training' && (
                 <div className="flex-1 space-y-4 p-4 overflow-y-auto scrollbar-hide bg-emerald-50/30">
                    <div className="flex items-center gap-2 mb-3">
                       <GraduationCap size={16} className="text-emerald-500" />
                       <h3 className="text-xs font-bold text-emerald-700 uppercase tracking-widest">Training Compliance</h3>
                    </div>
                    {doc.trainingQuestions.map((q, i) => (
                      <div key={q.questionId} className="bg-white rounded-xl p-4 border border-emerald-100 shadow-sm transition-all hover:shadow-md">
                        <p className="text-[12px] font-bold text-gray-800 mb-3 leading-snug">Q{i+1}. {q.questionText}</p>
                        <div className="space-y-2">
                           {q.options.map((opt, oi) => (
                             <div key={oi} className="flex items-start gap-2 text-[11px] text-gray-600 leading-tight">
                               <span className="font-bold text-emerald-500 min-w-[20px] uppercase">{String.fromCharCode(65 + oi)}.</span>
                               <span>{opt}</span>
                             </div>
                           ))}
                        </div>
                      </div>
                    ))}
                    <div className="p-3 bg-emerald-100/50 rounded-lg text-emerald-700 text-[10px] italic font-medium">
                      Note: These questions form the assessment for this document version once published.
                    </div>
                 </div>
               )}
            </div>

            {/* Panel Audit Footer */}
            <div className="px-5 py-4 bg-gray-50 border-t border-gray-100 shadow-[0_-4px_12px_rgba(0,0,0,0.02)]">
               <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2 font-bold opacity-40">
                    <div className="w-1.5 h-1.5 rounded-full bg-brand-500" />
                    <span className="text-[9px] text-gray-800 uppercase tracking-widest">Compliance Active</span>
                  </div>
                  <Shield size={14} className="text-brand-300" />
               </div>
               <p className="text-[9px] text-gray-400 font-medium leading-normal">
                 Every change in this panel is recorded in the permanent audit trail per 21 CFR Part 11.
               </p>
            </div>
          </div>
        )}
      </div>

      {/* ── Transactional Modals ── */}
      <QAPreparationModal open={showPrepareModal} doc={doc} onClose={() => setShowPrepareModal(false)} onSuccess={handleDocUpdate} />
      <SubmitDraftModal open={showSubmitDraftModal} doc={doc} onClose={() => setShowSubmitDraftModal(false)} onSuccess={handleDocUpdate} onPrepareTransition={prepareForTransition} />
      <ReviewActionModal open={showReviewModal} doc={doc} mode={reviewMode} onClose={() => setShowReviewModal(false)} onSuccess={handleDocUpdate} onPrepareTransition={prepareForTransition} />
      <ApprovalModal open={showApprovalModal} doc={doc} mode={approvalMode} onClose={() => setShowApprovalModal(false)} onSuccess={handleDocUpdate} onPrepareTransition={prepareForTransition} />
    </div>
  );
}
