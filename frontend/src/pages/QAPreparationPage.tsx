import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { FileText, User, Calendar } from 'lucide-react';
import { TablePageSkeleton } from '../components/ui/PageSkeleton';
import api from '../api/client';
import type { ControlledDocument } from '../types';
import { showErrorToast, isNotFoundError } from '../utils/errorHandler';

export default function QAPreparationPage() {
  const [requests, setRequests] = useState<ControlledDocument[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      try {
        const res = await api.get('/documents/pending-requests');
        const data = res.data.content ?? res.data;
        setRequests(Array.isArray(data) ? data : []);
      } catch (err) {
        if (!isNotFoundError(err)) showErrorToast(err, 'Failed to load pending requests');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  if (loading) {
    return <TablePageSkeleton />;
  }

  return (
    <div className="space-y-6">
      <h2 className="text-xl font-semibold text-brand-900 flex items-center gap-2">
        <FileText size={22} /> Request Selection — Pending Requests
      </h2>
      {requests.length === 0 ? (
        <div className="card p-6">
          <p className="text-sm text-brand-500">No pending requests</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {requests.map(doc => (
            <div key={doc.id} className="card p-5 space-y-3">
              <h3 className="text-sm font-semibold text-brand-800">{doc.title}</h3>
              <div className="space-y-1 text-xs text-brand-500">
                <p className="flex items-center gap-1">
                  <User size={12} />
                  Requested by: {doc.requestedBy}
                </p>
                <p>{doc.unitId}</p>
                <p>{doc.documentTypeId}</p>
                <p className="flex items-center gap-1">
                  <Calendar size={12} />
                  {new Date(doc.createdAt).toLocaleDateString()}
                </p>
              </div>
              <Link to={`/document/${doc.id}`} className="btn-primary text-xs inline-block">
                Select
              </Link>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
