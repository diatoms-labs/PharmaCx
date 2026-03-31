import { useEffect, useState } from 'react';
import { Users, Plus } from 'lucide-react';
import { TablePageSkeleton } from '../components/ui/PageSkeleton';
import api from '../api/client';
import type { AppUser } from '../types';
import { showErrorToast, isNotFoundError } from '../utils/errorHandler';
import { fmt } from '../utils/format';

export default function UsersPage() {
  const [users, setUsers] = useState<AppUser[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      try {
        const res = await api.get<AppUser[]>('/users');
        setUsers(res.data);
      } catch (err) {
        if (!isNotFoundError(err)) showErrorToast(err, 'Failed to load users');
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
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold text-brand-900 flex items-center gap-2">
          <Users size={22} /> User Management
        </h2>
        <button className="btn-primary">
          <Plus size={16} /> Add User
        </button>
      </div>
      <div className="card overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-brand-50 border-b border-brand-200">
            <tr>
              <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Name</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Username</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Role</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Department</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Status</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-brand-500">No users found</td>
              </tr>
            ) : (
              users.map(user => (
                <tr key={user.id} className="border-b border-brand-100 hover:bg-brand-50">
                  <td className="px-4 py-3 font-medium text-brand-800">{user.fullName}</td>
                  <td className="px-4 py-3 text-brand-600">{user.username}</td>
                  <td className="px-4 py-3">{fmt(user.role)}</td>
                  <td className="px-4 py-3">{user.unitDisplayName ?? user.unitId}</td>
                  <td className="px-4 py-3">
                    <span className={user.active ? 'badge-green' : 'badge-red'}>
                      {user.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <button className="btn-secondary text-xs px-3 py-1">Edit</button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
