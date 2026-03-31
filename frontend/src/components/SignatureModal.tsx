import { useRef, useEffect, useState } from 'react';
import SignaturePad from 'signature_pad';
import { Eraser, Loader2 } from 'lucide-react';

interface Props {
  open: boolean;
  onClose: () => void;
  onSubmit: (signatureData: string, comment: string) => void;
  title?: string;
  submitLabel?: string;
  loading?: boolean;
  loadingStage?: string;
}

export default function SignatureModal({ open, onClose, onSubmit, title = 'Digital Signature', submitLabel = 'Sign & Submit', loading, loadingStage }: Props) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const padRef = useRef<SignaturePad | null>(null);
  const [comment, setComment] = useState('');

  useEffect(() => {
    if (!open || !canvasRef.current) return;
    const canvas = canvasRef.current;
    const ratio = Math.max(window.devicePixelRatio || 1, 1);
    canvas.width = canvas.offsetWidth * ratio;
    canvas.height = canvas.offsetHeight * ratio;
    canvas.getContext('2d')!.scale(ratio, ratio);

    padRef.current = new SignaturePad(canvas, {
      penColor: '#1f2937',
      backgroundColor: '#ffffff',
    });

    return () => {
      padRef.current?.off();
      padRef.current = null;
    };
  }, [open]);

  if (!open) return null;

  const handleSubmit = () => {
    if (!padRef.current || padRef.current.isEmpty()) return;
    const dataUrl = padRef.current.toDataURL('image/png');
    onSubmit(dataUrl, comment);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40" onClick={onClose}>
      <div className="bg-white rounded-lg shadow-xl w-full max-w-lg mx-4" onClick={e => e.stopPropagation()}>
        <div className="px-5 py-4 border-b border-gray-200">
          <h3 className="text-base font-semibold text-gray-900">{title}</h3>
        </div>

        <div className="p-5 space-y-4 relative">
          {loading && (
            <div className="absolute inset-0 bg-white/80 z-10 flex flex-col items-center justify-center gap-2">
              <Loader2 size={24} className="animate-spin text-gray-500" />
              <p className="text-sm text-gray-500 font-medium">{loadingStage || 'Processing...'}</p>
            </div>
          )}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1.5">Draw your signature</label>
            <div className="border border-gray-300 rounded-lg overflow-hidden bg-white">
              <canvas ref={canvasRef} className="w-full" style={{ height: 160 }} />
            </div>
            <div className="flex gap-2 mt-2">
              <button
                type="button"
                className="inline-flex items-center gap-1 text-xs text-gray-500 hover:text-gray-700 px-2 py-1 rounded border border-gray-200 hover:bg-gray-50"
                onClick={() => padRef.current?.clear()}
              >
                <Eraser size={12} /> Clear
              </button>
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1.5">Comment (optional)</label>
            <textarea
              className="input"
              rows={2}
              value={comment}
              onChange={e => setComment(e.target.value)}
              placeholder="Add a comment..."
            />
          </div>
        </div>

        <div className="px-5 py-3 border-t border-gray-200 flex justify-end gap-2">
          <button className="btn text-sm px-4 py-1.5 border border-gray-200 text-gray-600 hover:bg-gray-50 rounded-md" onClick={onClose}>
            Cancel
          </button>
          <button className="btn-primary text-sm px-4 py-1.5" onClick={handleSubmit} disabled={loading}>
            {loading ? <><Loader2 size={14} className="animate-spin" /> Submitting...</> : submitLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
