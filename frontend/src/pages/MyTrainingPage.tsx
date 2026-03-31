import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { GraduationCap, Calendar, XCircle, AlertTriangle } from 'lucide-react';
import { TablePageSkeleton } from '../components/ui/PageSkeleton';
import api from '../api/client';
import type { TrainingAssignment, TrainingStatus } from '../types';
import { showErrorToast, isNotFoundError } from '../utils/errorHandler';
import { fmt } from '../utils/format';

function statusBadge(status: TrainingStatus) {
  const map: Record<string, string> = {
    ASSIGNED: 'badge-dark',
    IN_PROGRESS: 'badge-yellow',
    READ: 'badge-yellow',
    QUIZ_PASSED: 'badge-green',
    COMPLETED: 'badge-green',
    FAILED: 'badge-red',
    OVERDUE: 'badge-red',
  };
  return map[status] || 'badge-gray';
}

type GroupKey = 'failed' | 'pending' | 'inProgress' | 'completed';

function groupAssignments(assignments: TrainingAssignment[]) {
  const groups: Record<GroupKey, TrainingAssignment[]> = {
    failed: [],
    pending: [],
    inProgress: [],
    completed: [],
  };
  for (const a of assignments) {
    if (a.status === 'FAILED') {
      groups.failed.push(a);
    } else if (a.status === 'ASSIGNED' || a.status === 'OVERDUE') {
      groups.pending.push(a);
    } else if (a.status === 'IN_PROGRESS' || a.status === 'READ' || a.status === 'QUIZ_PASSED') {
      groups.inProgress.push(a);
    } else {
      groups.completed.push(a);
    }
  }
  return groups;
}

export default function MyTrainingPage() {
  const [assignments, setAssignments] = useState<TrainingAssignment[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      try {
        const res = await api.get<TrainingAssignment[]>('/training/my-assignments');
        setAssignments(res.data);
      } catch (err) {
        if (!isNotFoundError(err)) showErrorToast(err, 'Failed to load training assignments');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  if (loading) {
    return <TablePageSkeleton />;
  }

  const groups = groupAssignments(assignments);

  const sectionLabels: Record<GroupKey, string> = {
    failed: 'Failed',
    pending: 'Pending',
    inProgress: 'In Progress',
    completed: 'Completed',
  };

  return (
    <div className="space-y-6">
      <h2 className="text-xl font-semibold text-brand-900 flex items-center gap-2">
        <GraduationCap size={22} /> My Training
      </h2>
      {assignments.length === 0 ? (
        <div className="card p-6">
          <p className="text-sm text-brand-500">No training assignments</p>
        </div>
      ) : (
        (Object.keys(sectionLabels) as GroupKey[]).map(key => (
          groups[key].length > 0 && (
            <div key={key} className="space-y-3">
              <h3 className={`text-sm font-semibold flex items-center gap-1.5 ${
                key === 'failed' ? 'text-red-700' : 'text-brand-700'
              }`}>
                {key === 'failed' && <AlertTriangle size={14} />}
                {sectionLabels[key]} ({groups[key].length})
              </h3>

              {/* Failed section with distinct styling */}
              {key === 'failed' ? (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                  {groups[key].map(a => (
                    <Link
                      key={a.id}
                      to={`/training/${a.id}`}
                      className="card border-red-200 bg-red-50/50 p-4 hover:shadow-md transition-shadow space-y-2"
                    >
                      <div className="flex items-center justify-between">
                        <span className="text-sm font-medium text-brand-800 truncate">{a.documentTitle}</span>
                        <span className="badge-red flex items-center gap-1">
                          <XCircle size={10} /> Failed
                        </span>
                      </div>
                      <p className="text-xs font-mono text-brand-500">{a.documentNumber}</p>
                      <p className="text-xs text-red-600 font-medium">
                        Contact your manager for reassignment
                      </p>
                      {a.failedAt && (
                        <p className="text-xs text-brand-400">
                          Failed on: {new Date(a.failedAt).toLocaleDateString()}
                        </p>
                      )}
                    </Link>
                  ))}
                </div>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                  {groups[key].map(a => (
                    <Link key={a.id} to={`/training/${a.id}`} className="card p-4 hover:shadow-md transition-shadow space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-sm font-medium text-brand-800 truncate">{a.documentTitle}</span>
                        <span className={statusBadge(a.status)}>{fmt(a.status)}</span>
                      </div>
                      <p className="text-xs font-mono text-brand-500">{a.documentNumber}</p>
                      <div className="flex items-center justify-between text-xs text-brand-500">
                        <span className="flex items-center gap-1">
                          <Calendar size={10} />
                          Assigned: {new Date(a.assignedAt).toLocaleDateString()}
                        </span>
                        <span>Due: {new Date(a.dueDate).toLocaleDateString()}</span>
                      </div>
                      {a.assignedByUsername && (
                        <p className="text-xs text-brand-400">By: {a.assignedByUsername}</p>
                      )}
                    </Link>
                  ))}
                </div>
              )}
            </div>
          )
        ))
      )}
    </div>
  );
}
