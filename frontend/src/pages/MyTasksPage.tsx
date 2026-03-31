import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ClipboardList } from 'lucide-react';
import { TablePageSkeleton } from '../components/ui/PageSkeleton';
import api from '../api/client';
import type { ControlledDocument } from '../types';
import { showErrorToast, isNotFoundError } from '../utils/errorHandler';
import { fmt } from '../utils/format';

const STEPS = ['Request', 'Request Selection', 'Author Draft', 'Peer Review', 'QA Review', 'Approval', 'Published'];

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

export default function MyTasksPage() {
  const [tasks, setTasks] = useState<ControlledDocument[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      try {
        const res = await api.get<ControlledDocument[]>('/documents/my-tasks');
        setTasks(res.data);
      } catch (err) {
        if (!isNotFoundError(err)) showErrorToast(err, 'Failed to load tasks');
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
        <ClipboardList size={22} /> My Tasks
      </h2>
      <div className="card overflow-hidden">
        {tasks.length === 0 ? (
          <div className="p-6">
            <p className="text-sm text-brand-500">No pending tasks</p>
          </div>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-brand-50 border-b border-brand-200">
              <tr>
                <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Document Number</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Title</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Type</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Department</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Current Step</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Status</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Action</th>
              </tr>
            </thead>
            <tbody>
              {tasks.map(doc => (
                <tr key={doc.id} className="border-b border-brand-100 hover:bg-brand-50">
                  <td className="px-4 py-3 font-mono text-xs">{doc.documentNumber || '--'}</td>
                  <td className="px-4 py-3 font-medium text-brand-800">{doc.title}</td>
                  <td className="px-4 py-3">{doc.documentTypeId}</td>
                  <td className="px-4 py-3">{doc.unitId}</td>
                  <td className="px-4 py-3">{STEPS[doc.currentStepIndex] || '--'}</td>
                  <td className="px-4 py-3">
                    <span className={statusBadge(doc.status)}>{fmt(doc.status)}</span>
                  </td>
                  <td className="px-4 py-3">
                    <Link to={`/document/${doc.id}`} className="btn-primary text-xs px-3 py-1">
                      View
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
