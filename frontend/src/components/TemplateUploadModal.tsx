import { useState, useEffect } from 'react';
import { Upload, X } from 'lucide-react';
import api from '../api/client';
import type { DocumentTemplate, DocumentTypeConfig } from '../types';
import toast from 'react-hot-toast';
import { showErrorToast } from '../utils/errorHandler';

interface Props {
  open: boolean;
  onClose: () => void;
  onSuccess: (template: DocumentTemplate) => void;
}

export default function TemplateUploadModal({ open, onClose, onSuccess }: Props) {
  const [name, setName] = useState('');
  const [documentTypeId, setDocumentTypeId] = useState('');
  const [description, setDescription] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [docTypes, setDocTypes] = useState<DocumentTypeConfig[]>([]);

  useEffect(() => {
    if (!open) return;
    api.get<DocumentTypeConfig[]>('/document-types').then(res => {
      setDocTypes(res.data);
      if (res.data.length > 0) setDocumentTypeId(res.data[0].id);
    }).catch(() => {});
  }, [open]);

  if (!open) return null;

  const reset = () => {
    setName('');
    setDocumentTypeId(docTypes[0]?.id ?? '');
    setDescription('');
    setFile(null);
  };

  const handleSubmit = async () => {
    if (!name.trim() || !file) {
      toast.error('Name and file are required');
      return;
    }
    setLoading(true);
    try {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('name', name.trim());
      formData.append('documentTypeId', documentTypeId);
      if (description.trim()) formData.append('description', description.trim());

      const res = await api.post<DocumentTemplate>('/templates', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      toast.success('Template uploaded');
      reset();
      onSuccess(res.data);
    } catch (err) {
      showErrorToast(err, 'Failed to upload template');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40" onClick={() => { reset(); onClose(); }}>
      <div className="bg-white rounded-lg shadow-xl w-full max-w-md mx-4" onClick={e => e.stopPropagation()}>
        <div className="px-5 py-4 border-b border-gray-200 flex items-center justify-between">
          <h3 className="text-base font-semibold text-gray-900">Upload Template</h3>
          <button onClick={() => { reset(); onClose(); }} className="text-gray-400 hover:text-gray-600"><X size={18} /></button>
        </div>

        <div className="p-5 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Template Name</label>
            <input className="input" value={name} onChange={e => setName(e.target.value)} placeholder="e.g. Standard SOP Template" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Document Type</label>
            <select className="input" value={documentTypeId} onChange={e => setDocumentTypeId(e.target.value)}>
              {docTypes.map(t => <option key={t.id} value={t.id}>{t.displayName}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
            <textarea className="input" rows={2} value={description} onChange={e => setDescription(e.target.value)} placeholder="Optional description..." />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">File</label>
            {file ? (
              <div className="flex items-center gap-2 text-sm text-gray-600 bg-gray-50 rounded-md px-3 py-2 border border-gray-200">
                <span className="truncate flex-1">{file.name}</span>
                <button onClick={() => setFile(null)} className="text-gray-400 hover:text-gray-600"><X size={14} /></button>
              </div>
            ) : (
              <label className="flex flex-col items-center gap-2 px-4 py-6 border-2 border-dashed border-gray-300 rounded-lg cursor-pointer hover:border-gray-400 transition-colors">
                <Upload size={24} className="text-gray-400" />
                <span className="text-sm text-gray-500">Click to select a file</span>
                <span className="text-xs text-gray-400">.docx, .xlsx, .pptx</span>
                <input type="file" className="hidden" accept=".docx,.xlsx,.pptx,.pdf" onChange={e => setFile(e.target.files?.[0] ?? null)} />
              </label>
            )}
          </div>
        </div>

        <div className="px-5 py-3 border-t border-gray-200 flex justify-end gap-2">
          <button className="btn text-sm px-4 py-1.5 border border-gray-200 text-gray-600 hover:bg-gray-50 rounded-md" onClick={() => { reset(); onClose(); }}>
            Cancel
          </button>
          <button className="btn-primary text-sm px-4 py-1.5" onClick={handleSubmit} disabled={loading}>
            {loading ? 'Uploading...' : 'Upload'}
          </button>
        </div>
      </div>
    </div>
  );
}
