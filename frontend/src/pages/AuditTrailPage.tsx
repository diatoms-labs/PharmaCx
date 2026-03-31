import { useEffect, useState, useMemo } from 'react';
import { History, Download, Search } from 'lucide-react';
import { TablePageSkeleton } from '../components/ui/PageSkeleton';
import api from '../api/client';
import type { AuditEvent } from '../types';
import { showErrorToast, isNotFoundError } from '../utils/errorHandler';
import { fmt } from '../utils/format';

export default function AuditTrailPage() {
  const [searchQuery, setSearchQuery] = useState('');
  const [events, setEvents] = useState<AuditEvent[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      try {
        const res = await api.get('/audit', { params: { page: 0, size: 50 } });
        const data = res.data.content ?? res.data;
        setEvents(Array.isArray(data) ? data : []);
      } catch (err) {
        if (!isNotFoundError(err)) showErrorToast(err, 'Failed to load audit events');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const filtered = useMemo(() => {
    if (!searchQuery) return events;
    const q = searchQuery.toLowerCase();
    return events.filter(e =>
      e.username.toLowerCase().includes(q) ||
      e.action.toLowerCase().includes(q) ||
      e.resourceName.toLowerCase().includes(q)
    );
  }, [events, searchQuery]);

  if (loading) {
    return <TablePageSkeleton />;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold text-brand-900 flex items-center gap-2">
          <History size={22} /> Audit Trail
        </h2>
        <button className="btn-secondary">
          <Download size={16} /> Export
        </button>
      </div>

      {/* Filters */}
      <div className="card p-4">
        <div className="flex items-center gap-3">
          <div className="relative flex-1">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-brand-400" />
            <input
              className="input pl-9"
              placeholder="Search audit events..."
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
            />
          </div>
        </div>
      </div>

      {/* Table */}
      <div className="card overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-brand-50 border-b border-brand-200">
            <tr>
              <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Timestamp</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">User</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Action</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Resource</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-brand-500 uppercase">Details</th>
            </tr>
          </thead>
          <tbody>
            {filtered.length === 0 ? (
              <tr>
                <td colSpan={5} className="px-4 py-8 text-center text-brand-500">No audit events recorded yet</td>
              </tr>
            ) : (
              filtered.map(event => (
                <tr key={event.id} className="border-b border-brand-100 hover:bg-brand-50">
                  <td className="px-4 py-3 text-brand-500 whitespace-nowrap">
                    {new Date(event.timestamp).toLocaleString()}
                  </td>
                  <td className="px-4 py-3 font-medium text-brand-800">{event.username}</td>
                  <td className="px-4 py-3">
                    <span className="badge-dark">{fmt(event.action)}</span>
                  </td>
                  <td className="px-4 py-3">
                    <div>
                      <span className="text-brand-800">{event.resourceName}</span>
                      <span className="text-xs text-brand-400 ml-1">({fmt(event.resourceType)})</span>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-brand-500 text-xs">
                    {event.reason || '--'}
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
