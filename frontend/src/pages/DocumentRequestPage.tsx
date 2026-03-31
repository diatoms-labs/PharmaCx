import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Send } from 'lucide-react';
import api from '../api/client';
import toast from 'react-hot-toast';
import { showErrorToast } from '../utils/errorHandler';
import type { DocumentTypeConfig, OrganizationalUnit } from '../types';

export default function DocumentRequestPage() {
  const navigate = useNavigate();
  const [title, setTitle] = useState('');
  const [documentTypeId, setDocumentTypeId] = useState('');
  const [unitId, setUnitId] = useState('');
  const [justification, setJustification] = useState('');
  const [loading, setLoading] = useState(false);
  const [docTypes, setDocTypes] = useState<DocumentTypeConfig[]>([]);
  const [orgUnits, setOrgUnits] = useState<OrganizationalUnit[]>([]);

  useEffect(() => {
    Promise.all([
      api.get<DocumentTypeConfig[]>('/document-types'),
      api.get<OrganizationalUnit[]>('/org-units'),
    ]).then(([dtRes, ouRes]) => {
      setDocTypes(dtRes.data);
      setOrgUnits(ouRes.data);
      if (dtRes.data.length > 0) setDocumentTypeId(dtRes.data[0].id);
      if (ouRes.data.length > 0) setUnitId(ouRes.data[0].id);
    }).catch(() => {});
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const { data } = await api.post('/documents', { title, documentTypeId, unitId, justification });
      toast.success('Document request submitted');
      navigate(`/document/${data.id}`);
    } catch (err) {
      showErrorToast(err, 'Failed to submit request');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <h2 className="text-xl font-semibold text-brand-900">New Document Request</h2>
      <div className="card p-6">
        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label className="label">Document Title</label>
            <input className="input" value={title} onChange={e => setTitle(e.target.value)} placeholder="Enter document title" required />
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
            <textarea className="input min-h-[80px]" value={justification} onChange={e => setJustification(e.target.value)} placeholder="Why is this document needed?" />
          </div>
          <button type="submit" className="btn-primary" disabled={loading}>
            <Send size={16} /> {loading ? 'Submitting...' : 'Submit Request'}
          </button>
        </form>
      </div>
    </div>
  );
}
