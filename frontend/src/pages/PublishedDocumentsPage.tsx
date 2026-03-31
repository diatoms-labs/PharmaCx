import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  BookOpen, FileText, Loader2, Users, GraduationCap,
  Search, X, RefreshCw, ChevronDown, ChevronRight,
} from 'lucide-react';
import { TablePageSkeleton } from '../components/ui/PageSkeleton';
import api from '../api/client';
import type { ControlledDocument, TrainingAssignment, AppUser, TrainingStatus, OrganizationalUnit, DocumentTypeConfig } from '../types';
import { useAuth } from '../hooks/useAuth';
import toast from 'react-hot-toast';
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

export default function PublishedDocumentsPage() {
  const { type, department } = useParams();
  const { hasRole } = useAuth();
  const [documents, setDocuments] = useState<ControlledDocument[]>([]);
  const [loading, setLoading] = useState(true);

  const isManager = hasRole('SYSTEM_ADMIN', 'DIRECTOR', 'HEAD_OF_DEPARTMENT', 'MANAGER');

  // Training assignments per document (for managers)
  const [docAssignments, setDocAssignments] = useState<Record<string, TrainingAssignment[]>>({});
  const [expandedDoc, setExpandedDoc] = useState<string | null>(null);
  const [assignmentsLoading, setAssignmentsLoading] = useState<string | null>(null);

  // Assign modal state
  const [assignModalDoc, setAssignModalDoc] = useState<ControlledDocument | null>(null);
  const [allUsers, setAllUsers] = useState<AppUser[]>([]);
  const [selectedUserIds, setSelectedUserIds] = useState<Set<string>>(new Set());
  const [dueDays, setDueDays] = useState(14);
  const [userSearch, setUserSearch] = useState('');
  const [assigning, setAssigning] = useState(false);
  const [usersLoading, setUsersLoading] = useState(false);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      try {
        let res;
        if (type && department) {
          // type and department are codes (e.g. 'SOP', 'QA') — resolve to IDs
          const [dtRes, ouRes] = await Promise.all([
            api.get<DocumentTypeConfig[]>('/document-types'),
            api.get<OrganizationalUnit[]>('/org-units'),
          ]);
          const documentTypeId = dtRes.data.find(dt => dt.code === type)?.id;
          const unitId = ouRes.data.find(u => u.code === department)?.id;
          if (documentTypeId && unitId) {
            res = await api.get('/documents', { params: { documentTypeId, unitId, status: 'PUBLISHED' } });
          } else {
            res = await api.get('/documents', { params: { status: 'PUBLISHED' } });
          }
        } else {
          res = await api.get('/documents', { params: { status: 'PUBLISHED' } });
        }
        const data = res.data.content ?? res.data;
        setDocuments(Array.isArray(data) ? data : []);
      } catch (err) {
        if (!isNotFoundError(err)) showErrorToast(err, 'Failed to load published documents');
      } finally {
        setLoading(false);
      }
    };
    load();
    setExpandedDoc(null);
    setDocAssignments({});
  }, [type, department]);

  // Load assignments for a document
  const loadDocAssignments = async (docId: string) => {
    if (expandedDoc === docId) {
      setExpandedDoc(null);
      return;
    }
    setExpandedDoc(docId);
    if (docAssignments[docId]) return; // already loaded
    setAssignmentsLoading(docId);
    try {
      const res = await api.get<TrainingAssignment[]>(`/training/document/${docId}`);
      setDocAssignments(prev => ({ ...prev, [docId]: res.data }));
    } catch (err) {
      showErrorToast(err, 'Failed to load training assignments');
    } finally {
      setAssignmentsLoading(null);
    }
  };

  // Open assign modal
  const openAssignModal = async (doc: ControlledDocument) => {
    setAssignModalDoc(doc);
    setSelectedUserIds(new Set());
    setDueDays(14);
    setUserSearch('');
    setUsersLoading(true);
    try {
      const res = await api.get<AppUser[]>('/users');
      setAllUsers(res.data.filter(u => u.active));
    } catch (err) {
      showErrorToast(err, 'Failed to load users');
    } finally {
      setUsersLoading(false);
    }
  };

  // Submit assignment
  const handleAssign = async () => {
    if (!assignModalDoc || selectedUserIds.size === 0) return;
    setAssigning(true);
    try {
      const res = await api.post<TrainingAssignment[]>('/training/assign', {
        documentId: assignModalDoc.id,
        traineeUserIds: Array.from(selectedUserIds),
        dueDays,
      });
      toast.success(`Training assigned to ${res.data.length} user(s)`);
      // Refresh assignments for this doc
      const assignRes = await api.get<TrainingAssignment[]>(`/training/document/${assignModalDoc.id}`);
      setDocAssignments(prev => ({ ...prev, [assignModalDoc.id]: assignRes.data }));
      setExpandedDoc(assignModalDoc.id);
      setAssignModalDoc(null);
    } catch (err) {
      showErrorToast(err, 'Failed to assign training');
    } finally {
      setAssigning(false);
    }
  };

  // Reassign failed training
  const handleReassign = async (assignmentId: string, docId: string) => {
    try {
      await api.post(`/training/${assignmentId}/reassign`);
      toast.success('Training re-assigned successfully');
      const res = await api.get<TrainingAssignment[]>(`/training/document/${docId}`);
      setDocAssignments(prev => ({ ...prev, [docId]: res.data }));
    } catch (err) {
      showErrorToast(err, 'Failed to reassign');
    }
  };

  // Toggle user selection
  const toggleUser = (userId: string) => {
    setSelectedUserIds(prev => {
      const next = new Set(prev);
      if (next.has(userId)) next.delete(userId);
      else next.add(userId);
      return next;
    });
  };

  // Filter users by search
  const filteredUsers = allUsers.filter(u => {
    const q = userSearch.toLowerCase();
    return u.fullName.toLowerCase().includes(q)
      || u.username.toLowerCase().includes(q)
      || (u.unitDisplayName ?? u.unitId ?? '').toLowerCase().includes(q);
  });

  if (loading) {
    return <TablePageSkeleton />;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold text-brand-900 flex items-center gap-2">
          <BookOpen size={22} />
          Published Documents
          {type && <span className="text-brand-500 ml-1">/ {fmt(type)}</span>}
          {department && <span className="text-brand-500">/ {fmt(department)}</span>}
        </h2>
      </div>
      <div className="card overflow-hidden">
        {documents.length === 0 ? (
          <div className="flex flex-col items-center py-8 text-brand-500 p-6">
            <FileText size={40} className="mb-3 text-brand-300" />
            <p className="text-sm">No published documents found</p>
          </div>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-brand-50 border-b border-brand-200">
              <tr>
                <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Document Number</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Title</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Type</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Department</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Version</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Effective Date</th>
                {isManager && (
                  <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Training</th>
                )}
              </tr>
            </thead>
            <tbody>
              {documents.map(doc => (
                <React.Fragment key={doc.id}>
                  <tr className="border-b border-brand-100 hover:bg-brand-50">
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
                    <td className="px-4 py-3">{doc.documentTypeId}</td>
                    <td className="px-4 py-3">{doc.unitId}</td>
                    <td className="px-4 py-3">v{doc.version}</td>
                    <td className="px-4 py-3 text-brand-500">
                      {doc.effectiveDate ? new Date(doc.effectiveDate).toLocaleDateString() : '--'}
                    </td>
                    {isManager && (
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => openAssignModal(doc)}
                            className="btn-primary text-xs flex items-center gap-1"
                          >
                            <GraduationCap size={12} /> Assign
                          </button>
                          <button
                            onClick={() => loadDocAssignments(doc.id)}
                            className="btn-secondary text-xs flex items-center gap-1"
                          >
                            <Users size={12} />
                            {expandedDoc === doc.id ? <ChevronDown size={12} /> : <ChevronRight size={12} />}
                          </button>
                        </div>
                      </td>
                    )}
                  </tr>
                  {/* Expanded training assignments row */}
                  {isManager && expandedDoc === doc.id && (
                    <tr key={`${doc.id}-assignments`}>
                      <td colSpan={7} className="bg-brand-50/50 px-4 py-3 border-b border-brand-200">
                        {assignmentsLoading === doc.id ? (
                          <div className="flex items-center gap-2 py-2">
                            <Loader2 size={14} className="animate-spin text-brand-500" />
                            <span className="text-xs text-brand-500">Loading assignments...</span>
                          </div>
                        ) : !docAssignments[doc.id] || docAssignments[doc.id].length === 0 ? (
                          <p className="text-xs text-brand-500 py-1">No training assignments for this document yet.</p>
                        ) : (
                          <div className="space-y-1">
                            <p className="text-xs font-semibold text-brand-600 mb-2">
                              Training Assignments ({docAssignments[doc.id].length})
                            </p>
                            <table className="w-full text-xs">
                              <thead>
                                <tr className="text-brand-500">
                                  <th className="text-left py-1 pr-3 font-medium">Trainee</th>
                                  <th className="text-left py-1 pr-3 font-medium">Department</th>
                                  <th className="text-left py-1 pr-3 font-medium">Status</th>
                                  <th className="text-left py-1 pr-3 font-medium">Quiz</th>
                                  <th className="text-left py-1 pr-3 font-medium">Due</th>
                                  <th className="text-left py-1 font-medium"></th>
                                </tr>
                              </thead>
                              <tbody>
                                {docAssignments[doc.id].map(a => (
                                  <tr key={a.id} className={a.status === 'FAILED' ? 'text-red-700' : 'text-brand-700'}>
                                    <td className="py-1.5 pr-3 font-medium">{a.traineeUsername}</td>
                                    <td className="py-1.5 pr-3">{a.unitId}</td>
                                    <td className="py-1.5 pr-3">
                                      <span className={statusBadge(a.status)}>{fmt(a.status)}</span>
                                    </td>
                                    <td className="py-1.5 pr-3">
                                      {a.quizAttempts.length > 0
                                        ? `${a.quizAttempts.length} attempt(s)`
                                        : 'Not taken'}
                                    </td>
                                    <td className="py-1.5 pr-3">{new Date(a.dueDate).toLocaleDateString()}</td>
                                    <td className="py-1.5">
                                      {a.status === 'FAILED' && (
                                        <button
                                          onClick={() => handleReassign(a.id, doc.id)}
                                          className="btn-secondary text-xs flex items-center gap-1"
                                        >
                                          <RefreshCw size={10} /> Re-assign
                                        </button>
                                      )}
                                    </td>
                                  </tr>
                                ))}
                              </tbody>
                            </table>
                          </div>
                        )}
                      </td>
                    </tr>
                  )}
                </React.Fragment>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Assign Training Modal */}
      {assignModalDoc && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50" onClick={() => setAssignModalDoc(null)}>
          <div className="bg-white rounded-lg shadow-xl w-full max-w-lg p-6 space-y-5" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-semibold text-brand-900">Assign Training</h3>
              <button onClick={() => setAssignModalDoc(null)} className="p-1 rounded hover:bg-gray-100">
                <X size={18} className="text-gray-500" />
              </button>
            </div>

            <div className="text-sm text-brand-600 space-y-1">
              <p className="font-medium">{assignModalDoc.title}</p>
              <p className="text-xs font-mono text-brand-500">{assignModalDoc.documentNumber}</p>
            </div>

            {/* Due days */}
            <div>
              <label className="block text-xs font-medium text-brand-600 mb-1">Due in (days)</label>
              <input
                type="number"
                min={1}
                max={90}
                value={dueDays}
                onChange={e => setDueDays(Number(e.target.value))}
                className="input w-24 text-sm"
              />
            </div>

            {/* User picker */}
            <div>
              <label className="block text-xs font-medium text-brand-600 mb-1">
                Select Users ({selectedUserIds.size} selected)
              </label>
              <div className="relative mb-2">
                <Search size={14} className="absolute left-2.5 top-2.5 text-gray-400" />
                <input
                  type="text"
                  placeholder="Search users..."
                  value={userSearch}
                  onChange={e => setUserSearch(e.target.value)}
                  className="input pl-8 text-sm w-full"
                />
              </div>

              {usersLoading ? (
                <div className="flex justify-center py-4">
                  <Loader2 size={20} className="animate-spin text-brand-500" />
                </div>
              ) : (
                <div className="border border-gray-200 rounded-md max-h-48 overflow-y-auto divide-y divide-gray-100">
                  {filteredUsers.length === 0 ? (
                    <p className="text-xs text-brand-500 p-3">No users found</p>
                  ) : (
                    filteredUsers.map(u => (
                      <label
                        key={u.id}
                        className="flex items-center gap-3 px-3 py-2 hover:bg-gray-50 cursor-pointer"
                      >
                        <input
                          type="checkbox"
                          checked={selectedUserIds.has(u.id)}
                          onChange={() => toggleUser(u.id)}
                          className="rounded border-gray-300 text-brand-600 focus:ring-brand-500"
                        />
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-medium text-brand-800 truncate">{u.fullName}</p>
                          <p className="text-xs text-brand-500">{u.unitDisplayName ?? u.unitId} &middot; {fmt(u.role)}</p>
                        </div>
                      </label>
                    ))
                  )}
                </div>
              )}
            </div>

            {/* Actions */}
            <div className="flex justify-end gap-3 pt-2">
              <button onClick={() => setAssignModalDoc(null)} className="btn-secondary text-sm">
                Cancel
              </button>
              <button
                onClick={handleAssign}
                disabled={assigning || selectedUserIds.size === 0}
                className="btn-primary text-sm flex items-center gap-1"
              >
                {assigning && <Loader2 size={14} className="animate-spin" />}
                Assign Training
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
