import { useState, useEffect } from 'react';
import { FileText, X, Send } from 'lucide-react';
import api from '../api/client';
import toast from 'react-hot-toast';
import { showErrorToast } from '../utils/errorHandler';
import type { DocumentTypeConfig, OrganizationalUnit } from '../types';

interface Props {
  open: boolean;
  onClose: () => void;
  onSuccess: (documentId: string) => void;
}

export default function NewDocumentRequestModal({ open, onClose, onSuccess }: Props) {
  const [title, setTitle] = useState('');
  const [documentTypeId, setDocumentTypeId] = useState('');
  const [unitId, setUnitId] = useState('');
  const [justification, setJustification] = useState('');
  const [loading, setLoading] = useState(false);
  const [docTypes, setDocTypes] = useState<DocumentTypeConfig[]>([]);
  const [orgUnits, setOrgUnits] = useState<OrganizationalUnit[]>([]);

  useEffect(() => {
    if (!open) return;
    Promise.all([
      api.get<DocumentTypeConfig[]>('/document-types'),
      api.get<OrganizationalUnit[]>('/org-units'),
    ]).then(([dtRes, ouRes]) => {
      setDocTypes(dtRes.data);
      setOrgUnits(ouRes.data);
      if (dtRes.data.length > 0) setDocumentTypeId(dtRes.data[0].id);
      if (ouRes.data.length > 0) setUnitId(ouRes.data[0].id);
    }).catch(() => {});
  }, [open]);

  const reset = () => {
    setTitle('');
    setDocumentTypeId(docTypes[0]?.id ?? '');
    setUnitId(orgUnits[0]?.id ?? '');
    setJustification('');
  };

  const handleClose = () => {
    if (!loading) {
      reset();
      onClose();
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const { data } = await api.post('/documents', { title, documentTypeId, unitId, justification });
      toast.success('Document request submitted');
      reset();
      onSuccess(data.id);
    } catch (err) {
      showErrorToast(err, 'Failed to submit request');
    } finally {
      setLoading(false);
    }
  };

  if (!open) return null;

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50" onClick={handleClose}>
      <div className="bg-white rounded-lg shadow-xl w-full max-w-lg p-6 space-y-5" onClick={e => e.stopPropagation()}>
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-semibold text-gray-900 flex items-center gap-2">
            <FileText size={20} /> New Document Request
          </h3>
          <button onClick={handleClose} className="text-gray-400 hover:text-gray-600">
            <X size={20} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="label">Document Title</label>
            <input
              className="input"
              value={title}
              onChange={e => setTitle(e.target.value)}
              placeholder="Enter document title"
              required
              autoFocus
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="label">Document Type</label>
              <select className="input" value={documentTypeId} onChange={e => setDocumentTypeId(e.target.value)}>
                {docTypes.map(t => <option key={t.id} value={t.id}>{t.displayName}</option>)}
              </select>
            </div>
            <div>
              <label className="label">Department</label>
              <select className="input" value={unitId} onChange={e => setUnitId(e.target.value)}>
                {orgUnits.map(u => <option key={u.id} value={u.id}>{u.displayName}</option>)}
              </select>
            </div>
          </div>
          <div>
            <label className="label">Justification (optional)</label>
            <textarea
              className="input min-h-[70px]"
              value={justification}
              onChange={e => setJustification(e.target.value)}
              placeholder="Why is this document needed?"
              rows={3}
            />
          </div>
          <div className="flex justify-end gap-2 pt-1">
            <button type="button" className="btn-secondary text-sm" onClick={handleClose} disabled={loading}>
              Cancel
            </button>
            <button type="submit" className="btn-primary text-sm" disabled={loading || !title.trim()}>
              <Send size={14} /> {loading ? 'Submitting...' : 'Submit Request'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
