import { useEffect, useState } from 'react';
import { Share2, FolderOpen } from 'lucide-react';
import { TablePageSkeleton } from '../components/ui/PageSkeleton';
import api from '../api/client';
import { showErrorToast, isNotFoundError } from '../utils/errorHandler';

interface UserFolder {
  id: string;
  name: string;
  ownerId: string;
  ownerUsername: string;
  sharedWithUserIds: string[];
  documentIds: string[];
  createdAt: string;
}

export default function SharedWithMePage() {
  const [folders, setFolders] = useState<UserFolder[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      try {
        const res = await api.get<UserFolder[]>('/folders/shared');
        setFolders(res.data);
      } catch (err) {
        if (!isNotFoundError(err)) showErrorToast(err, 'Failed to load shared folders');
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
        <Share2 size={22} /> Shared with Me
      </h2>
      {folders.length === 0 ? (
        <div className="card p-6 flex flex-col items-center text-brand-500">
          <Share2 size={36} className="mb-2 text-brand-300" />
          <p className="text-sm">No folders have been shared with you</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
          {folders.map(folder => (
            <div key={folder.id} className="card p-4 flex items-center gap-3">
              <FolderOpen size={24} className="text-accent-500 flex-shrink-0" />
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-brand-800 truncate">{folder.name}</p>
                <p className="text-xs text-brand-400">
                  Shared by {folder.ownerUsername} · {folder.documentIds.length} doc{folder.documentIds.length !== 1 ? 's' : ''}
                </p>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
