import { useEffect, useState } from 'react';
import { Link, useSearchParams, useNavigate } from 'react-router-dom';
import { FolderOpen, Share2, Trash2, Loader2, FileText, Plus, X, Users, ChevronRight } from 'lucide-react';
import { TablePageSkeleton } from '../components/ui/PageSkeleton';
import api from '../api/client';
import type { ControlledDocument, AppUser, UserFolder } from '../types';
import toast from 'react-hot-toast';
import { showErrorToast } from '../utils/errorHandler';
import NewDocumentRequestModal from '../components/NewDocumentRequestModal';
import { fmt } from '../utils/format';

export default function MyDocumentsPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const folderId = searchParams.get('folderId');

  const [folders, setFolders] = useState<UserFolder[]>([]);
  const [currentFolder, setCurrentFolder] = useState<UserFolder | null>(null);
  const [myDocs, setMyDocs] = useState<ControlledDocument[]>([]);
  const [loading, setLoading] = useState(true);

  // Share modal state
  const [shareFolder, setShareFolder] = useState<UserFolder | null>(null);
  const [allUsers, setAllUsers] = useState<AppUser[]>([]);
  const [selectedUserIds, setSelectedUserIds] = useState<string[]>([]);
  const [loadingUsers, setLoadingUsers] = useState(false);

  // New document modal
  const [showNewDocModal, setShowNewDocModal] = useState(false);

  const loadData = async () => {
    setLoading(true);
    try {
      if (folderId) {
        // Inside a folder: fetch folder details + children
        const [folderRes, childrenRes, docsRes] = await Promise.all([
          api.get<UserFolder>(`/folders/${folderId}`),
          api.get<UserFolder[]>(`/folders/${folderId}/children`),
          api.get<ControlledDocument[]>('/documents/my-documents'),
        ]);
        setCurrentFolder(folderRes.data);
        setFolders(childrenRes.data);
        // Filter docs that belong to this folder
        const folderDocIds = new Set(folderRes.data.documentIds);
        setMyDocs(docsRes.data.filter(doc => folderDocIds.has(doc.id)));
      } else {
        // Root: fetch root folders + all docs
        const [foldersRes, docsRes] = await Promise.all([
          api.get<UserFolder[]>('/folders/my'),
          api.get<ControlledDocument[]>('/documents/my-documents'),
        ]);
        setCurrentFolder(null);
        setFolders(foldersRes.data);
        setMyDocs(docsRes.data);
      }
    } catch (err) {
      showErrorToast(err, 'Failed to load documents');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadData(); }, [folderId]);

  const handleDeleteFolder = async (id: string, name: string) => {
    if (!confirm(`Delete folder "${name}"?`)) return;
    try {
      await api.delete(`/folders/${id}`);
      toast.success('Folder deleted');
      window.dispatchEvent(new Event('folder-refresh'));
      loadData();
    } catch (err) {
      showErrorToast(err, 'Failed to delete folder');
    }
  };

  const openShareModal = async (folder: UserFolder) => {
    setShareFolder(folder);
    setSelectedUserIds([...folder.sharedWithUserIds]);
    setLoadingUsers(true);
    try {
      const res = await api.get<AppUser[]>('/users');
      setAllUsers(res.data);
    } catch (err) {
      showErrorToast(err, 'Failed to load users');
    } finally {
      setLoadingUsers(false);
    }
  };

  const handleShare = async () => {
    if (!shareFolder) return;
    try {
      await api.post(`/folders/${shareFolder.id}/share`, { userIds: selectedUserIds });
      toast.success('Folder shared');
      setShareFolder(null);
      loadData();
    } catch (err) {
      showErrorToast(err, 'Failed to share folder');
    }
  };

  const toggleUser = (userId: string) => {
    setSelectedUserIds(prev =>
      prev.includes(userId) ? prev.filter(id => id !== userId) : [...prev, userId]
    );
  };

  if (loading) {
    return <TablePageSkeleton />;
  }

  return (
    <div className="space-y-6">
      {/* Header with breadcrumb */}
      <div className="flex items-center justify-between">
        <div>
          {currentFolder ? (
            <div className="flex items-center gap-1.5 text-sm">
              <button
                onClick={() => navigate('/documents/my')}
                className="text-brand-500 hover:text-brand-800 font-medium flex items-center gap-1"
              >
                <FolderOpen size={18} /> My Documents
              </button>
              <ChevronRight size={14} className="text-brand-400" />
              <span className="text-brand-900 font-semibold">{currentFolder.name}</span>
            </div>
          ) : (
            <h2 className="text-xl font-semibold text-brand-900 flex items-center gap-2">
              <FolderOpen size={22} /> My Documents
            </h2>
          )}
        </div>
        <button
          className="p-2 rounded-md bg-gray-800 text-white hover:bg-gray-900 transition-colors"
          onClick={() => setShowNewDocModal(true)}
          title="New Document Request"
        >
          <Plus size={16} />
        </button>
      </div>

      {/* Folders grid */}
      {folders.length > 0 && (
        <div>
          <h3 className="text-sm font-semibold text-brand-700 mb-3">Folders</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
            {folders.map(folder => (
              <div key={folder.id} className="card p-4 flex items-center gap-3 group">
                <button
                  onClick={() => navigate(`/documents/my?folderId=${folder.id}`)}
                  className="flex items-center gap-3 flex-1 min-w-0 text-left"
                >
                  <FolderOpen size={24} className="text-yellow-500 flex-shrink-0" />
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-brand-800 truncate">{folder.name}</p>
                    <p className="text-xs text-brand-400">
                      {folder.documentIds.length} doc{folder.documentIds.length !== 1 ? 's' : ''}
                      {folder.sharedWithUserIds.length > 0 && (
                        <span className="ml-2">
                          · Shared with {folder.sharedWithUserIds.length}
                        </span>
                      )}
                    </p>
                  </div>
                </button>
                <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                  <button
                    className="p-1.5 rounded hover:bg-brand-100 text-brand-400 hover:text-gray-800"
                    title="Share"
                    onClick={() => openShareModal(folder)}
                  >
                    <Share2 size={14} />
                  </button>
                  <button
                    className="p-1.5 rounded hover:bg-brand-100 text-brand-400 hover:text-red-500"
                    title="Delete"
                    onClick={() => handleDeleteFolder(folder.id, folder.name)}
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Documents table */}
      <div>
        <h3 className="text-sm font-semibold text-brand-700 mb-3">
          {currentFolder ? 'Documents' : 'My Work-in-Progress Documents'}
        </h3>
        {myDocs.length === 0 ? (
          <div className="card p-6 flex flex-col items-center text-brand-500">
            <FileText size={36} className="mb-2 text-brand-300" />
            <p className="text-sm">No documents {currentFolder ? 'in this folder' : 'in progress'}</p>
          </div>
        ) : (
          <div className="card overflow-hidden">
            <table className="w-full text-sm">
              <thead className="bg-brand-50 border-b border-brand-200">
                <tr>
                  <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Document Number</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Title</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Type</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Status</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Updated</th>
                </tr>
              </thead>
              <tbody>
                {myDocs.map(doc => (
                  <tr key={doc.id} className="border-b border-brand-100 hover:bg-brand-50">
                    <td className="px-4 py-3 font-mono text-xs">{doc.documentNumber || '--'}</td>
                    <td className="px-4 py-3">
                      <Link to={`/document/${doc.id}`} className="font-medium text-brand-800 hover:underline">
                        {doc.title}
                      </Link>
                    </td>
                    <td className="px-4 py-3">{doc.documentTypeId}</td>
                    <td className="px-4 py-3">
                      <span className="badge-yellow">{fmt(doc.status)}</span>
                    </td>
                    <td className="px-4 py-3 text-brand-500">{new Date(doc.updatedAt).toLocaleDateString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* New Document Request Modal */}
      <NewDocumentRequestModal
        open={showNewDocModal}
        onClose={() => setShowNewDocModal(false)}
        onSuccess={(id) => {
          setShowNewDocModal(false);
          navigate(`/document/${id}`);
        }}
      />

      {/* Share modal */}
      {shareFolder && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl w-full max-w-md p-6 space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-semibold text-brand-900 flex items-center gap-2">
                <Users size={20} /> Share &ldquo;{shareFolder.name}&rdquo;
              </h3>
              <button onClick={() => setShareFolder(null)} className="text-brand-400 hover:text-brand-600">
                <X size={20} />
              </button>
            </div>
            {loadingUsers ? (
              <div className="flex justify-center py-4">
                <Loader2 size={24} className="animate-spin text-brand-500" />
              </div>
            ) : (
              <div className="max-h-60 overflow-y-auto space-y-1">
                {allUsers.map(user => (
                  <label
                    key={user.id}
                    className="flex items-center gap-3 px-3 py-2 rounded hover:bg-brand-50 cursor-pointer"
                  >
                    <input
                      type="checkbox"
                      checked={selectedUserIds.includes(user.id)}
                      onChange={() => toggleUser(user.id)}
                      className="rounded border-brand-300"
                    />
                    <div>
                      <p className="text-sm font-medium text-brand-800">{user.fullName}</p>
                      <p className="text-xs text-brand-400">{user.username} · {user.unitDisplayName ?? user.unitId}</p>
                    </div>
                  </label>
                ))}
              </div>
            )}
            <div className="flex justify-end gap-2 pt-2">
              <button className="btn-secondary text-sm" onClick={() => setShareFolder(null)}>Cancel</button>
              <button className="btn-primary text-sm" onClick={handleShare}>
                Share with {selectedUserIds.length} user{selectedUserIds.length !== 1 ? 's' : ''}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
