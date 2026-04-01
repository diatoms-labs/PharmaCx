import { useState, useRef, useEffect } from 'react';
import { Send, Loader2, Bot, User, ShieldCheck, AlertCircle, PencilLine, X } from 'lucide-react';
import api from '../api/client';
import { DocumentStatus } from '../types';

interface Message {
  role: 'user' | 'ai';
  content: string;
  chunksUsed?: number;
}

interface AIChatPanelProps {
  documentId?: string;
  documentStatus?: DocumentStatus;
  onInsertContent?: (text: string, sectionLabel?: string) => void;
}

function stageLabel(status?: DocumentStatus): { label: string; tag: string } {
  switch (status) {
    case 'AUTHOR_DRAFT':     return { label: 'AI Draft Assistant',   tag: 'DRAFT-MODE' };
    case 'PEER_REVIEW':
    case 'QA_REVIEW':        return { label: 'AI Review Copilot',    tag: 'REVIEW-MODE' };
    case 'PUBLISHED':        return { label: 'AI Document Assistant', tag: 'AUDIT-MODE' };
    default:                 return { label: 'AI Assistant',          tag: 'SEARCH-MODE' };
  }
}

const HISTORY_KEY = (docId: string) => `ai-chat-history-${docId}`;
const MAX_STORED_MESSAGES = 30;

interface InsertDialog {
  text: string;
  sectionLabel: string;
}

export default function AIChatPanel({ documentId, documentStatus, onInsertContent }: AIChatPanelProps) {
  const [insertDialog, setInsertDialog] = useState<InsertDialog | null>(null);
  const [messages, setMessages] = useState<Message[]>(() => {
    if (!documentId) return [];
    try {
      const stored = localStorage.getItem(HISTORY_KEY(documentId));
      return stored ? (JSON.parse(stored) as Message[]) : [];
    } catch {
      return [];
    }
  });
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const bottomRef = useRef<HTMLDivElement>(null);
  const { label, tag } = stageLabel(documentStatus);

  // Persist messages to localStorage whenever they change
  useEffect(() => {
    if (!documentId) return;
    try {
      const toStore = messages.slice(-MAX_STORED_MESSAGES);
      localStorage.setItem(HISTORY_KEY(documentId), JSON.stringify(toStore));
    } catch { /* storage full — silently skip */ }
  }, [messages, documentId]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const send = async () => {
    const trimmed = input.trim();
    if (!trimmed || loading) return;

    setInput('');
    setError(null);
    setMessages(prev => [...prev, { role: 'user', content: trimmed }]);
    setLoading(true);

    try {
      const res = await api.post<{ response: string; model: string; chunksUsed: number }>(
        '/ai/chat',
        {
          message: trimmed,
          documentId: documentId ?? null,
          workflowStage: documentStatus ?? null,
        }
      );
      setMessages(prev => [
        ...prev,
        { role: 'ai', content: res.data.response, chunksUsed: res.data.chunksUsed },
      ]);
    } catch {
      setError('AI service unavailable. Please check that Ollama is running.');
    } finally {
      setLoading(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      send();
    }
  };

  return (
    <div className="flex flex-col h-full bg-white">
      {/* Header */}
      <div className="flex items-center justify-between px-3 py-2 border-b bg-gray-50/50">
        <div className="flex items-center gap-2">
          <Bot size={15} className="text-brand-500" />
          <span className="text-[11px] font-bold text-gray-700 uppercase tracking-tight">{label}</span>
        </div>
        <span className="text-[10px] px-1.5 py-0.5 rounded bg-gray-200 text-gray-500 font-mono">{tag}</span>
      </div>

      {/* CFR disclaimer */}
      <div className="flex items-start gap-1.5 px-3 py-2 bg-amber-50 border-b border-amber-100">
        <ShieldCheck size={12} className="text-amber-600 mt-0.5 flex-shrink-0" />
        <p className="text-[10px] text-amber-700 leading-tight">
          AI suggestions are advisory only. Human review required per 21 CFR Part 11.
        </p>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto px-3 py-4 space-y-4 min-h-0 scrollbar-hide">
        {messages.length === 0 && (
          <div className="text-center py-8">
            <Bot size={32} className="mx-auto text-gray-200 mb-2" />
            <p className="text-[11px] text-gray-400">
              {documentId
                ? 'Ask anything about compliance, section gaps, or drafting.'
                : 'Search across your training base using natural language.'}
            </p>
          </div>
        )}

        {messages.map((msg, i) => (
          <div key={i} className={`flex gap-2 ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
            {msg.role === 'ai' && (
              <div className="flex-shrink-0 w-7 h-7 rounded-full bg-brand-100 flex items-center justify-center">
                <Bot size={14} className="text-brand-600" />
              </div>
            )}
            <div className={`flex flex-col max-w-[85%] ${msg.role === 'user' ? 'items-end' : 'items-start'}`}>
              <div
                className={`rounded-2xl px-4 py-2.5 text-[12px] leading-relaxed shadow-sm ${
                  msg.role === 'user'
                    ? 'bg-brand-600 text-white rounded-br-none'
                    : 'bg-gray-100 text-gray-800 rounded-bl-none border border-gray-200'
                }`}
              >
                {msg.content}
                {msg.role === 'ai' && msg.chunksUsed !== undefined && msg.chunksUsed > 0 && (
                  <p className="text-[9px] mt-2 font-bold opacity-50 uppercase tracking-tighter">
                    Retrieved {msg.chunksUsed} related sections
                  </p>
                )}
              </div>
              
              {msg.role === 'ai' && onInsertContent && insertDialog?.text !== msg.content && (
                <button
                  onClick={() => setInsertDialog({ text: msg.content, sectionLabel: '' })}
                  className="mt-1 flex items-center gap-1.5 text-[10px] text-brand-600 hover:text-brand-800 font-bold px-2 py-1 rounded-full border border-brand-100 hover:bg-brand-50 transition-all"
                >
                  <PencilLine size={12} /> Insert at cursor
                </button>
              )}

              {msg.role === 'ai' && onInsertContent && insertDialog?.text === msg.content && (
                <div className="mt-2 bg-white border border-brand-200 rounded-xl p-3 shadow-lg w-full">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-[11px] font-bold text-gray-700">Confirm Insertion</span>
                    <button onClick={() => setInsertDialog(null)} className="text-gray-400 hover:text-gray-600">
                      <X size={14} />
                    </button>
                  </div>
                  <input
                    type="text"
                    placeholder="Section header (optional)"
                    value={insertDialog.sectionLabel}
                    onChange={e => setInsertDialog({ ...insertDialog, sectionLabel: e.target.value })}
                    className="w-full text-[11px] border border-gray-200 rounded-lg px-2.5 py-1.5 mb-2 focus:outline-none focus:ring-2 focus:ring-brand-500/20"
                  />
                  <button
                    onClick={() => {
                      onInsertContent(insertDialog.text, insertDialog.sectionLabel || undefined);
                      setInsertDialog(null);
                    }}
                    className="w-full flex items-center justify-center gap-1.5 text-[11px] font-bold bg-brand-600 text-white rounded-lg py-1.5 hover:bg-brand-700 transition-colors shadow-md shadow-brand-500/20"
                  >
                    <PencilLine size={12} /> Confirm Insert
                  </button>
                </div>
              )}
            </div>
            {msg.role === 'user' && (
              <div className="flex-shrink-0 w-7 h-7 rounded-full bg-gray-800 flex items-center justify-center">
                <User size={14} className="text-white" />
              </div>
            )}
          </div>
        ))}

        {loading && (
          <div className="flex gap-2 justify-start items-center">
            <div className="flex-shrink-0 w-7 h-7 rounded-full bg-brand-100 flex items-center justify-center">
              <Bot size={14} className="text-brand-600" />
            </div>
            <div className="bg-gray-100 rounded-2xl rounded-bl-none px-4 py-2 border border-gray-200">
              <Loader2 size={16} className="animate-spin text-brand-500" />
            </div>
          </div>
        )}

        {error && (
          <div className="flex items-center gap-2 px-3 py-2 bg-red-50 rounded-xl border border-red-100">
            <AlertCircle size={14} className="text-red-500 flex-shrink-0" />
            <p className="text-[11px] text-red-600 font-medium">{error}</p>
          </div>
        )}

        <div ref={bottomRef} className="h-2" />
      </div>

      {/* Input */}
      <div className="border-t p-3 bg-gray-50/30">
        <div className="flex gap-2 items-end">
          <textarea
            value={input}
            onChange={e => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Ask AI assistant..."
            rows={2}
            className="flex-1 resize-none text-[12px] border border-gray-200 rounded-xl px-3 py-2.5 focus:outline-none focus:ring-2 focus:ring-brand-500/10 placeholder-gray-400 bg-white shadow-sm"
          />
          <button
            onClick={send}
            disabled={!input.trim() || loading}
            className="flex-shrink-0 w-10 h-10 rounded-xl bg-brand-600 text-white flex items-center justify-center hover:bg-brand-700 disabled:opacity-40 disabled:cursor-not-allowed transition-all shadow-lg shadow-brand-500/20"
          >
            <Send size={16} />
          </button>
        </div>
        <p className="text-[9px] text-gray-400 mt-2 text-center font-medium uppercase tracking-widest opacity-80 italic">Verified by helix-ai</p>
      </div>
    </div>
  );
}
