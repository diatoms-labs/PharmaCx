import { useState } from 'react';
import { Plus, Trash2, X, CheckCircle2, HelpCircle } from 'lucide-react';
import type { TrainingQuestion } from '../types';

interface Props {
  open: boolean;
  initialQuestions: TrainingQuestion[];
  onClose: () => void;
  onSave: (questions: TrainingQuestion[]) => void;
}

function uid() {
  return crypto.randomUUID();
}

function emptyQuestion(): TrainingQuestion {
  return {
    questionId: uid(),
    questionText: '',
    questionType: 'MULTIPLE_CHOICE',
    options: ['', '', '', ''],
    correctAnswerIndex: 0,
    explanation: null,
  };
}

export default function TrainingQABuilderModal({ open, initialQuestions, onClose, onSave }: Props) {
  const [questions, setQuestions] = useState<TrainingQuestion[]>(
    initialQuestions.length > 0 ? initialQuestions : [emptyQuestion()]
  );

  if (!open) return null;

  const update = (idx: number, patch: Partial<TrainingQuestion>) => {
    setQuestions(prev => prev.map((q, i) => i === idx ? { ...q, ...patch } : q));
  };

  const updateOption = (qIdx: number, oIdx: number, value: string) => {
    setQuestions(prev => prev.map((q, i) => {
      if (i !== qIdx) return q;
      const opts = [...q.options];
      opts[oIdx] = value;
      return { ...q, options: opts };
    }));
  };

  const addOption = (qIdx: number) => {
    setQuestions(prev => prev.map((q, i) => i === qIdx ? { ...q, options: [...q.options, ''] } : q));
  };

  const removeOption = (qIdx: number, oIdx: number) => {
    setQuestions(prev => prev.map((q, i) => {
      if (i !== qIdx || q.options.length <= 2) return q;
      const opts = q.options.filter((_, j) => j !== oIdx);
      const correct = q.correctAnswerIndex >= opts.length ? 0 : q.correctAnswerIndex;
      return { ...q, options: opts, correctAnswerIndex: correct };
    }));
  };

  const addQuestion = () => setQuestions(prev => [...prev, emptyQuestion()]);

  const removeQuestion = (idx: number) => {
    if (questions.length <= 1) return;
    setQuestions(prev => prev.filter((_, i) => i !== idx));
  };

  const handleSave = () => {
    const valid = questions.filter(q => q.questionText.trim() && q.options.every(o => o.trim()));
    onSave(valid);
  };

  const handleTypeChange = (idx: number, type: 'MULTIPLE_CHOICE' | 'TRUE_FALSE') => {
    if (type === 'TRUE_FALSE') {
      update(idx, { questionType: type, options: ['True', 'False'], correctAnswerIndex: 0 });
    } else {
      update(idx, { questionType: type, options: ['', '', '', ''], correctAnswerIndex: 0 });
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40" onClick={onClose}>
      <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl mx-4 max-h-[90vh] flex flex-col" onClick={e => e.stopPropagation()}>
        <div className="px-5 py-4 border-b border-gray-200 flex items-center justify-between flex-shrink-0">
          <div>
            <h3 className="text-base font-semibold text-gray-900">Training Q&A Builder</h3>
            <p className="text-xs text-gray-400 mt-0.5">Select the correct answer for each question using the radio button</p>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600"><X size={18} /></button>
        </div>

        <div className="flex-1 overflow-y-auto p-5 space-y-4">
          {questions.map((q, qIdx) => (
            <div key={q.questionId} className="border border-gray-200 rounded-lg p-4 space-y-3">
              {/* Question header */}
              <div className="flex items-start gap-2">
                <span className="text-xs font-bold text-white bg-gray-700 rounded px-1.5 py-0.5 mt-1.5 flex-shrink-0">
                  Q{qIdx + 1}
                </span>
                <div className="flex-1 space-y-3">
                  <div className="flex items-center gap-2">
                    <input
                      className="input flex-1"
                      value={q.questionText}
                      onChange={e => update(qIdx, { questionText: e.target.value })}
                      placeholder="Enter question text..."
                    />
                    {questions.length > 1 && (
                      <button onClick={() => removeQuestion(qIdx)} className="text-gray-400 hover:text-red-500 p-1">
                        <Trash2 size={14} />
                      </button>
                    )}
                  </div>

                  <div className="flex items-center gap-3">
                    <select
                      className="input w-40 text-sm"
                      value={q.questionType}
                      onChange={e => handleTypeChange(qIdx, e.target.value as 'MULTIPLE_CHOICE' | 'TRUE_FALSE')}
                    >
                      <option value="MULTIPLE_CHOICE">Multiple Choice</option>
                      <option value="TRUE_FALSE">True / False</option>
                    </select>
                    <span className="text-xs text-gray-400 flex items-center gap-1">
                      <HelpCircle size={11} /> Click radio to mark correct answer
                    </span>
                  </div>

                  {/* Options with correct answer highlighting */}
                  <div className="space-y-1.5">
                    <label className="text-xs font-medium text-gray-500 uppercase tracking-wide">
                      Options (select correct answer)
                    </label>
                    {q.options.map((opt, oIdx) => {
                      const isCorrect = q.correctAnswerIndex === oIdx;
                      return (
                        <div
                          key={oIdx}
                          className={`flex items-center gap-2 rounded-md px-2 py-1.5 transition-colors ${
                            isCorrect
                              ? 'bg-green-50 ring-1 ring-green-200'
                              : 'hover:bg-gray-50'
                          }`}
                        >
                          <input
                            type="radio"
                            name={`correct-${q.questionId}`}
                            checked={isCorrect}
                            onChange={() => update(qIdx, { correctAnswerIndex: oIdx })}
                            className="accent-green-600"
                          />
                          {isCorrect && <CheckCircle2 size={13} className="text-green-500 flex-shrink-0" />}
                          <span className="text-xs font-mono text-gray-400 w-4 flex-shrink-0">{String.fromCharCode(65 + oIdx)}</span>
                          <input
                            className={`input flex-1 text-sm ${isCorrect ? 'border-green-200 bg-white' : ''}`}
                            value={opt}
                            onChange={e => updateOption(qIdx, oIdx, e.target.value)}
                            placeholder={`Option ${String.fromCharCode(65 + oIdx)}`}
                            disabled={q.questionType === 'TRUE_FALSE'}
                          />
                          {q.questionType === 'MULTIPLE_CHOICE' && q.options.length > 2 && (
                            <button onClick={() => removeOption(qIdx, oIdx)} className="text-gray-400 hover:text-red-500">
                              <X size={14} />
                            </button>
                          )}
                        </div>
                      );
                    })}
                    {q.questionType === 'MULTIPLE_CHOICE' && q.options.length < 6 && (
                      <button
                        onClick={() => addOption(qIdx)}
                        className="text-xs text-gray-500 hover:text-gray-700 flex items-center gap-1 ml-8"
                      >
                        <Plus size={12} /> Add option
                      </button>
                    )}
                  </div>

                  {/* Explanation / justification */}
                  <div>
                    <label className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-1 block">
                      Answer Justification
                    </label>
                    <textarea
                      className="input text-sm w-full"
                      rows={2}
                      value={q.explanation || ''}
                      onChange={e => update(qIdx, { explanation: e.target.value || null })}
                      placeholder="Explain why this is the correct answer — shown to trainee after quiz attempt..."
                    />
                  </div>
                </div>
              </div>
            </div>
          ))}

          <button
            onClick={addQuestion}
            className="w-full py-3 border-2 border-dashed border-gray-300 rounded-lg text-sm text-gray-500 hover:border-gray-400 hover:text-gray-700 flex items-center justify-center gap-1.5 transition-colors"
          >
            <Plus size={16} /> Add Question
          </button>
        </div>

        <div className="px-5 py-3 border-t border-gray-200 flex items-center justify-between flex-shrink-0">
          <span className="text-xs text-gray-400">{questions.length} question{questions.length !== 1 ? 's' : ''}</span>
          <div className="flex gap-2">
            <button className="btn text-sm px-4 py-1.5 border border-gray-200 text-gray-600 hover:bg-gray-50 rounded-md" onClick={onClose}>
              Cancel
            </button>
            <button className="btn-primary text-sm px-4 py-1.5" onClick={handleSave}>
              Save Questions
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
