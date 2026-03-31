import { useState, useEffect } from 'react';
import { FileText, Upload, Eye, CheckCircle, XCircle } from 'lucide-react';
import { TablePageSkeleton } from '../components/ui/PageSkeleton';
import { useNavigate } from 'react-router-dom';
import api from '../api/client';
import type { DocumentTemplate } from '../types';
import TemplateUploadModal from '../components/TemplateUploadModal';
import { showErrorToast, isNotFoundError } from '../utils/errorHandler';

export default function TemplateManagementPage() {
  const navigate = useNavigate();
  const [templates, setTemplates] = useState<DocumentTemplate[]>([]);
  const [loading, setLoading] = useState(true);
  const [showUpload, setShowUpload] = useState(false);

  const load = () => {
    setLoading(true);
    api.get<DocumentTemplate[]>('/templates/all')
      .then(res => setTemplates(res.data))
      .catch((err) => { if (!isNotFoundError(err)) showErrorToast(err, 'Failed to load templates'); })
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold text-gray-900 flex items-center gap-2">
          <FileText size={22} /> Templates
          <span className="text-sm font-normal text-gray-400 ml-1">
            {templates.length} template{templates.length !== 1 ? 's' : ''}
          </span>
        </h2>
        <button
          className="p-2 rounded-md bg-gray-800 text-white hover:bg-gray-900 transition-colors"
          onClick={() => setShowUpload(true)}
          title="Upload Template"
        >
          <Upload size={16} />
        </button>
      </div>

      {/* Table */}
      {loading ? (
        <TablePageSkeleton />
      ) : templates.length === 0 ? (
        <div className="text-center py-16">
          <FileText size={40} className="text-gray-300 mx-auto mb-3" />
          <p className="text-gray-500">No templates found</p>
          <p className="text-xs text-gray-400 mt-1">Upload a template to get started</p>
        </div>
      ) : (
        <div className="card overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Name</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Type</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Version</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Status</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Uploaded By</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Created</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Updated</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Description</th>
                <th className="w-12 px-4 py-3"></th>
              </tr>
            </thead>
            <tbody>
              {templates.map(t => (
                <tr
                  key={t.id}
                  className="border-b border-gray-100 hover:bg-gray-50 cursor-pointer group"
                  onClick={() => navigate(`/templates/${t.id}`)}
                >
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2.5">
                      <div className="w-8 h-8 rounded bg-gray-100 flex items-center justify-center flex-shrink-0">
                        <FileText size={14} className="text-gray-500" />
                      </div>
                      <div className="min-w-0">
                        <span className="font-medium text-gray-900 block truncate">{t.name}</span>
                        {t.latest && (
                          <span className="text-[10px] font-semibold text-green-600 uppercase">Latest</span>
                        )}
                      </div>
                    </div>
                  </td>
                  <td className="px-4 py-3">
                    <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-gray-100 text-gray-700 uppercase">
                      {t.documentTypeId}
                    </span>
                  </td>
                  <td className="px-4 py-3 font-mono text-xs text-gray-600">v{t.version}</td>
                  <td className="px-4 py-3">
                    {t.active ? (
                      <span className="inline-flex items-center gap-1 text-xs text-green-600">
                        <CheckCircle size={12} /> Active
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 text-xs text-gray-400">
                        <XCircle size={12} /> Inactive
                      </span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-gray-600 text-xs">{t.createdBy}</td>
                  <td className="px-4 py-3 text-gray-500 text-xs">{new Date(t.createdAt).toLocaleDateString()}</td>
                  <td className="px-4 py-3 text-gray-500 text-xs">
                    {t.updatedAt ? new Date(t.updatedAt).toLocaleDateString() : '—'}
                  </td>
                  <td className="px-4 py-3 text-gray-500 text-xs max-w-[180px] truncate">{t.description || '—'}</td>
                  <td className="px-4 py-3">
                    <button
                      onClick={(e) => { e.stopPropagation(); navigate(`/templates/${t.id}`); }}
                      className="p-1.5 rounded hover:bg-gray-100 text-gray-400 hover:text-gray-700 opacity-0 group-hover:opacity-100 transition-opacity"
                      title="View"
                    >
                      <Eye size={14} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <TemplateUploadModal
        open={showUpload}
        onClose={() => setShowUpload(false)}
        onSuccess={() => { setShowUpload(false); load(); }}
      />
    </div>
  );
}
