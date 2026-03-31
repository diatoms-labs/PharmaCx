import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { Plus, FileText } from 'lucide-react';
import { TablePageSkeleton } from '../components/ui/PageSkeleton';
import api from '../api/client';
import type { ControlledDocument, OrganizationalUnit, DocumentTypeConfig } from '../types';
import { showErrorToast, isNotFoundError } from '../utils/errorHandler';
import { fmt } from '../utils/format';

function statusBadge(status: string) {
  const map: Record<string, string> = {
    REQUESTED: 'badge-dark',
    QA_PREPARATION: 'badge-yellow',
    AUTHOR_DRAFT: 'badge-yellow',
    PEER_REVIEW: 'badge-dark',
    QA_REVIEW: 'badge-dark',
    APPROVAL: 'badge-yellow',
    PUBLISHED: 'badge-green',
    RETIRED: 'badge-red',
  };
  return map[status] || 'badge-gray';
}

export default function DocumentListPage() {
  // :type and :department are codes (e.g. 'SOP', 'QA') from the URL
  const { type, department } = useParams();
  const [documents, setDocuments] = useState<ControlledDocument[]>([]);
  const [loading, setLoading] = useState(true);
  // Resolved display labels for the page heading
  const [typeLabel, setTypeLabel] = useState('');
  const [unitLabel, setUnitLabel] = useState('');

  useEffect(() => {
    const load = async () => {
      try {
        let res;
        if (type && department) {
          // Resolve codes → IDs for the API call
          const [dtRes, ouRes] = await Promise.all([
            api.get<DocumentTypeConfig[]>('/document-types'),
            api.get<OrganizationalUnit[]>('/org-units'),
          ]);
          const dt = dtRes.data.find(d => d.code === type);
          const ou = ouRes.data.find(u => u.code === department);
          setTypeLabel(dt?.displayName ?? fmt(type));
          setUnitLabel(ou?.displayName ?? fmt(department));

          if (dt && ou) {
            res = await api.get('/documents', { params: { documentTypeId: dt.id, unitId: ou.id } });
          } else {
            res = await api.get('/documents', { params: { status: 'PUBLISHED' } });
          }
        } else {
          res = await api.get('/documents', { params: { status: 'PUBLISHED' } });
        }
        const data = res.data.content ?? res.data;
        setDocuments(Array.isArray(data) ? data : []);
      } catch (err) {
        if (!isNotFoundError(err)) showErrorToast(err, 'Failed to load documents');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [type, department]);

  if (loading) return <TablePageSkeleton />;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold text-brand-900">
          {typeLabel || (type ? fmt(type) : 'Documents')}
          {(unitLabel || department) && (
            <span className="text-brand-500 ml-2">/ {unitLabel || fmt(department!)}</span>
          )}
        </h2>
        <Link to="/document/request" className="btn-primary">
          <Plus size={16} /> New Request
        </Link>
      </div>
      <div className="card overflow-hidden">
        {documents.length === 0 ? (
          <div className="flex flex-col items-center py-8 text-brand-500 p-6">
            <FileText size={40} className="mb-3 text-brand-300" />
            <p className="text-sm">No documents found</p>
          </div>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-brand-50 border-b border-brand-200">
              <tr>
                <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Document Number</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Title</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Status</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Version</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Updated</th>
              </tr>
            </thead>
            <tbody>
              {documents.map(doc => (
                <tr key={doc.id} className="border-b border-brand-100 hover:bg-brand-50">
                  <td className="px-4 py-3">
                    <Link to={`/document/${doc.id}`} className="font-mono text-xs text-gray-800 hover:underline">
                      {doc.documentNumber || '--'}
                    </Link>
                  </td>
                  <td className="px-4 py-3">
                    <Link to={`/document/${doc.id}`} className="font-medium text-brand-800 hover:underline">
                      {doc.title}
                    </Link>
                  </td>
                  <td className="px-4 py-3">
                    <span className={statusBadge(doc.status)}>{fmt(doc.status)}</span>
                  </td>
                  <td className="px-4 py-3">v{doc.version}</td>
                  <td className="px-4 py-3 text-brand-500">{new Date(doc.updatedAt).toLocaleDateString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
