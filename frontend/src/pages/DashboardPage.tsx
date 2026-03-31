import { useEffect, useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import {
  FileText,
  ClipboardList,
  GraduationCap,
  AlertTriangle,
  Calendar,
  ChevronRight,
  FilePlus,
  XCircle,
} from 'lucide-react';
import { DashboardSkeleton } from '../components/ui/PageSkeleton';
import api from '../api/client';
import type { ControlledDocument, TrainingAssignment, AuditEvent } from '../types';
import { useAuth } from '../hooks/useAuth';
import { showErrorToast } from '../utils/errorHandler';
import { fmt } from '../utils/format';

const STEPS = ['Request', 'Request Selection', 'Author Draft', 'Peer Review', 'QA Review', 'Approval', 'Published'];

function taskStatusBadge(status: string) {
  if (status === 'PUBLISHED' || status === 'RETIRED') return 'badge-gray';
  return 'badge-dark';
}

type Tab = 'tasks' | 'qa_prep' | 'training';

function trainingStatusBadge(status: string) {
  if (status === 'COMPLETED' || status === 'QUIZ_PASSED') return 'badge-gray';
  if (status === 'FAILED' || status === 'OVERDUE') return 'badge-red';
  return 'badge-dark';
}

export default function DashboardPage() {
  const { hasRole, isQA } = useAuth();
  const [tasks, setTasks] = useState<ControlledDocument[]>([]);
  const [qaRequests, setQaRequests] = useState<ControlledDocument[]>([]);
  const [training, setTraining] = useState<TrainingAssignment[]>([]);
  const [recentActivity, setRecentActivity] = useState<AuditEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<Tab>('tasks');

  const showQaTab = hasRole('SYSTEM_ADMIN', 'DIRECTOR', 'HEAD_OF_DEPARTMENT', 'MANAGER') || isQA();

  useEffect(() => {
    const load = async () => {
      try {
        const promises: Promise<unknown>[] = [
          api.get<ControlledDocument[]>('/documents/my-tasks'),
          api.get<AuditEvent[]>('/audit/recent'),
          api.get<TrainingAssignment[]>('/training/my-assignments'),
        ];

        if (showQaTab) {
          promises.push(api.get('/documents/pending-requests'));
        }

        const results = await Promise.all(promises);

        setTasks((results[0] as { data: ControlledDocument[] }).data);
        setRecentActivity((results[1] as { data: AuditEvent[] }).data);
        setTraining((results[2] as { data: TrainingAssignment[] }).data);

        if (showQaTab && results[3]) {
          const qaData = (results[3] as { data: ControlledDocument[] | { content: ControlledDocument[] } }).data;
          const list = Array.isArray(qaData) ? qaData : (qaData as { content: ControlledDocument[] }).content ?? [];
          setQaRequests(Array.isArray(list) ? list : []);
        }
      } catch (err) {
        showErrorToast(err, 'Failed to load dashboard data');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [showQaTab]);

  const pendingTraining = useMemo(
    () => training.filter((t) => t.status === 'ASSIGNED' || t.status === 'IN_PROGRESS' || t.status === 'READ' || t.status === 'OVERDUE'),
    [training]
  );
  const failedTraining = useMemo(() => training.filter((t) => t.status === 'FAILED'), [training]);
  const completedTraining = useMemo(() => training.filter((t) => t.status === 'COMPLETED' || t.status === 'QUIZ_PASSED'), [training]);
  const overdueTraining = useMemo(() => training.filter((t) => t.status === 'OVERDUE'), [training]);

  if (loading) {
    return <DashboardSkeleton />;
  }

  const tabs: { key: Tab; label: string; count: number }[] = [
    { key: 'tasks', label: 'My Tasks', count: tasks.length },
    ...(showQaTab ? [{ key: 'qa_prep' as Tab, label: 'Request Selection', count: qaRequests.length }] : []),
    { key: 'training', label: 'My Training', count: pendingTraining.length },
  ];

  return (
    <div className="space-y-6">
      <h2 className="text-xl font-semibold text-brand-900">Dashboard</h2>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="card p-4">
          <div className="flex items-center gap-3">
            <div className="flex items-center justify-center w-10 h-10 rounded-lg bg-gray-100 text-gray-500">
              <ClipboardList size={20} />
            </div>
            <div>
              <p className="text-2xl font-bold text-gray-800">{tasks.length}</p>
              <p className="text-sm text-gray-500">Pending Tasks</p>
            </div>
          </div>
        </div>
        <div className="card p-4">
          <div className="flex items-center gap-3">
            <div className="flex items-center justify-center w-10 h-10 rounded-lg bg-gray-100 text-gray-500">
              <FileText size={20} />
            </div>
            <div>
              <p className="text-2xl font-bold text-gray-800">{showQaTab ? qaRequests.length : '--'}</p>
              <p className="text-sm text-gray-500">QA Pending</p>
            </div>
          </div>
        </div>
        <div className="card p-4">
          <div className="flex items-center gap-3">
            <div className="flex items-center justify-center w-10 h-10 rounded-lg bg-gray-100 text-gray-500">
              <GraduationCap size={20} />
            </div>
            <div>
              <p className="text-2xl font-bold text-gray-800">
                {training.length > 0
                  ? `${Math.round((completedTraining.length / training.length) * 100)}%`
                  : '--'}
              </p>
              <p className="text-sm text-gray-500">Training Compliance</p>
            </div>
          </div>
        </div>
        <div className="card p-4">
          <div className="flex items-center gap-3">
            <div className="flex items-center justify-center w-10 h-10 rounded-lg bg-gray-100 text-gray-500">
              <AlertTriangle size={20} />
            </div>
            <div>
              <p className="text-2xl font-bold text-gray-800">{overdueTraining.length}</p>
              <p className="text-sm text-gray-500">Overdue Items</p>
            </div>
          </div>
        </div>
      </div>

      {/* Tabbed Section: Tasks / QA Prep */}
      <div className="card overflow-hidden">
        <div className="border-b border-gray-200 bg-gray-50 px-1">
          <nav className="flex gap-0" aria-label="Tabs">
            {tabs.map((tab) => (
              <button
                key={tab.key}
                onClick={() => setActiveTab(tab.key)}
                className={`px-4 py-3 text-sm font-medium border-b-2 transition-colors ${
                  activeTab === tab.key
                    ? 'border-gray-800 text-gray-800'
                    : 'border-transparent text-gray-400 hover:text-gray-600 hover:border-gray-300'
                }`}
              >
                {tab.label}
                {tab.count > 0 && (
                  <span
                    className={`ml-2 inline-flex items-center justify-center min-w-[20px] h-5 px-1.5 rounded-full text-xs font-medium ${
                      activeTab === tab.key ? 'bg-gray-800 text-white' : 'bg-gray-200 text-gray-500'
                    }`}
                  >
                    {tab.count}
                  </span>
                )}
              </button>
            ))}
          </nav>
        </div>

        {/* Tab: My Tasks */}
        {activeTab === 'tasks' && (
          <div>
            {tasks.length === 0 ? (
              <div className="p-8 text-center">
                <ClipboardList size={36} className="mx-auto mb-2 text-brand-300" />
                <p className="text-sm text-brand-500">No pending tasks</p>
              </div>
            ) : (
              <table className="w-full text-sm">
                <thead className="bg-brand-50/50 border-b border-brand-200">
                  <tr>
                    <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Doc #</th>
                    <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Title</th>
                    <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Type</th>
                    <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Department</th>
                    <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Step</th>
                    <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Status</th>
                    <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase"></th>
                  </tr>
                </thead>
                <tbody>
                  {tasks.map((doc) => (
                    <tr key={doc.id} className="border-b border-brand-100 hover:bg-brand-50/50 transition-colors">
                      <td className="px-4 py-3 font-mono text-xs text-brand-600">{doc.documentNumber || '--'}</td>
                      <td className="px-4 py-3 font-medium text-brand-800">{doc.title}</td>
                      <td className="px-4 py-3 text-brand-600">{doc.documentTypeId}</td>
                      <td className="px-4 py-3 text-brand-600">{doc.unitId}</td>
                      <td className="px-4 py-3 text-brand-600">{STEPS[doc.currentStepIndex] || '--'}</td>
                      <td className="px-4 py-3">
                        <span className={taskStatusBadge(doc.status)}>{fmt(doc.status)}</span>
                      </td>
                      <td className="px-4 py-3">
                        <Link
                          to={`/document/${doc.id}`}
                          className="inline-flex items-center gap-1 text-xs font-medium text-gray-800 hover:text-gray-600"
                        >
                          Open <ChevronRight size={12} />
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}

        {/* Tab: Request Selection */}
        {activeTab === 'qa_prep' && showQaTab && (
          <div>
            {qaRequests.length === 0 ? (
              <div className="p-8 text-center">
                <FilePlus size={36} className="mx-auto mb-2 text-brand-300" />
                <p className="text-sm text-brand-500">No pending selection requests</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 p-4">
                {qaRequests.map((doc) => (
                  <div key={doc.id} className="border border-brand-200 rounded-lg p-4 space-y-3 hover:shadow-sm transition-shadow">
                    <h4 className="text-sm font-semibold text-brand-800">{doc.title}</h4>
                    <div className="space-y-1 text-xs text-brand-500">
                      <p>Requested by: {doc.requestedBy}</p>
                      <p>{doc.unitId} &middot; {doc.documentTypeId}</p>
                      <p className="flex items-center gap-1">
                        <Calendar size={11} />
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
        )}

        {/* Tab: My Training */}
        {activeTab === 'training' && (
          <div>
            {training.length === 0 ? (
              <div className="p-8 text-center">
                <GraduationCap size={36} className="mx-auto mb-2 text-brand-300" />
                <p className="text-sm text-brand-500">No training assignments</p>
              </div>
            ) : (
              <div className="divide-y divide-brand-100">
                {pendingTraining.length > 0 && (
                  <div className="p-4 space-y-3">
                    <h4 className="text-xs font-semibold text-brand-500 uppercase tracking-wide">
                      Pending / In Progress ({pendingTraining.length})
                    </h4>
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                      {pendingTraining.map((a) => (
                        <Link
                          key={a.id}
                          to={`/training/${a.id}`}
                          className="border border-brand-200 rounded-lg p-3 hover:shadow-sm transition-shadow space-y-2"
                        >
                          <div className="flex items-center justify-between">
                            <span className="text-sm font-medium text-brand-800 truncate">{a.documentTitle}</span>
                            <span className={trainingStatusBadge(a.status)}>{fmt(a.status)}</span>
                          </div>
                          <p className="text-xs font-mono text-brand-500">{a.documentNumber}</p>
                          <div className="flex items-center justify-between text-xs text-brand-400">
                            <span className="flex items-center gap-1">
                              <Calendar size={10} />
                              {new Date(a.assignedAt).toLocaleDateString()}
                            </span>
                            <span>Due: {new Date(a.dueDate).toLocaleDateString()}</span>
                          </div>
                        </Link>
                      ))}
                    </div>
                  </div>
                )}
                {failedTraining.length > 0 && (
                  <div className="p-4 space-y-3">
                    <h4 className="text-xs font-semibold text-red-600 uppercase tracking-wide flex items-center gap-1">
                      <AlertTriangle size={12} /> Failed ({failedTraining.length})
                    </h4>
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                      {failedTraining.map((a) => (
                        <Link
                          key={a.id}
                          to={`/training/${a.id}`}
                          className="border border-red-200 rounded-lg p-3 bg-red-50/50 hover:shadow-sm transition-shadow space-y-2"
                        >
                          <div className="flex items-center justify-between">
                            <span className="text-sm font-medium text-brand-800 truncate">{a.documentTitle}</span>
                            <span className="badge-red flex items-center gap-1">
                              <XCircle size={10} /> Failed
                            </span>
                          </div>
                          <p className="text-xs font-mono text-brand-500">{a.documentNumber}</p>
                          <p className="text-xs text-red-600 font-medium">Contact your manager for reassignment</p>
                        </Link>
                      ))}
                    </div>
                  </div>
                )}
                {completedTraining.length > 0 && (
                  <div className="p-4 space-y-3">
                    <h4 className="text-xs font-semibold text-brand-500 uppercase tracking-wide">
                      Completed ({completedTraining.length})
                    </h4>
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                      {completedTraining.map((a) => (
                        <Link
                          key={a.id}
                          to={`/training/${a.id}`}
                          className="border border-brand-100 rounded-lg p-3 bg-brand-50/50 space-y-1"
                        >
                          <div className="flex items-center justify-between">
                            <span className="text-sm font-medium text-brand-700 truncate">{a.documentTitle}</span>
                            <span className={trainingStatusBadge(a.status)}>{fmt(a.status)}</span>
                          </div>
                          <p className="text-xs font-mono text-brand-400">{a.documentNumber}</p>
                        </Link>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </div>

      {/* Recent Activity + Status Summary */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="card p-5">
          <h3 className="text-sm font-semibold text-brand-700 mb-4">Recent Activity</h3>
          {recentActivity.length === 0 ? (
            <p className="text-sm text-brand-500">No recent activity</p>
          ) : (
            <div className="space-y-3">
              {recentActivity.map((event) => (
                <div
                  key={event.id}
                  className="flex items-start gap-3 text-sm border-b border-brand-100 pb-2 last:border-0"
                >
                  <div className="flex-1">
                    <p className="text-brand-800">
                      <span className="font-medium">{event.username}</span>{' '}
                      {fmt(event.action).toLowerCase()}{' '}
                      <span className="font-medium">{event.resourceName}</span>
                    </p>
                    <p className="text-xs text-brand-400 mt-0.5">
                      {new Date(event.timestamp).toLocaleString()}
                    </p>
                  </div>
                  <span className="badge-gray text-xs">{fmt(event.resourceType)}</span>
                </div>
              ))}
            </div>
          )}
        </div>
        <div className="card p-5">
          <h3 className="text-sm font-semibold text-brand-700 mb-4">Documents by Status</h3>
          {tasks.length === 0 ? (
            <p className="text-sm text-brand-500">No documents yet</p>
          ) : (
            <div className="space-y-2">
              {Object.entries(
                tasks.reduce<Record<string, number>>((acc, doc) => {
                  acc[doc.status] = (acc[doc.status] || 0) + 1;
                  return acc;
                }, {})
              ).map(([status, count]) => (
                <div key={status} className="flex items-center justify-between text-sm">
                  <span className="text-brand-600">{fmt(status)}</span>
                  <span className="font-medium text-brand-800">{count}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
