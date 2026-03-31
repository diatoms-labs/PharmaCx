import React, { useState, useEffect, useCallback } from 'react';
import { NavLink, useLocation, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard,
  FileText,
  FolderOpen,
  FolderPlus,
  Building2,
  FileStack,
  ChevronDown,
  ChevronRight,
  Users,
  Bell,
  LogOut,
  User,
  PanelLeftClose,
  Settings,
} from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import api from '../../api/client';
import type { UserFolder, OrganizationalUnit, DocumentTypeConfig } from '../../types';
import type { BrandingValues } from '../../hooks/useBranding';

interface SidebarProps {
  open: boolean;
  onToggle: () => void;
  branding: BrandingValues;
}

const formatRole = (role: string) =>
  role.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());

function Sidebar({ open, onToggle, branding }: SidebarProps) {
  const { user, hasRole, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  const [myDocsExpanded, setMyDocsExpanded] = useState(true);
  const [sharedExpanded, setSharedExpanded] = useState(false);
  const [expandedUnits, setExpandedUnits] = useState<Set<string>>(new Set());

  // Dynamic data from API
  const [orgUnits, setOrgUnits] = useState<OrganizationalUnit[]>([]);
  const [docTypes, setDocTypes] = useState<DocumentTypeConfig[]>([]);

  // My Documents folder state
  const [folders, setFolders] = useState<UserFolder[]>([]);
  const [creatingFolder, setCreatingFolder] = useState(false);
  const [newFolderName, setNewFolderName] = useState('');

  // Department shared folder state (keyed by unit.id)
  const [deptFolders, setDeptFolders] = useState<Record<string, UserFolder[]>>({});
  const [creatingDeptFolder, setCreatingDeptFolder] = useState<string | null>(null);
  const [newDeptFolderName, setNewDeptFolderName] = useState('');

  // Fetch org units and doc types on mount
  useEffect(() => {
    api.get<OrganizationalUnit[]>('/org-units').then(res => setOrgUnits(res.data)).catch(() => {});
    api.get<DocumentTypeConfig[]>('/document-types').then(res => setDocTypes(res.data)).catch(() => {});
  }, []);

  const loadFolders = useCallback(async () => {
    try {
      const res = await api.get<UserFolder[]>('/folders/my');
      setFolders(res.data);
    } catch { /* silent */ }
  }, []);

  const loadDeptFolders = useCallback(async () => {
    try {
      const res = await api.get<UserFolder[]>('/folders/shared-all');
      const grouped: Record<string, UserFolder[]> = {};
      for (const f of res.data) {
        if (f.ownerUnitId) {
          if (!grouped[f.ownerUnitId]) grouped[f.ownerUnitId] = [];
          grouped[f.ownerUnitId].push(f);
        }
      }
      setDeptFolders(grouped);
    } catch { /* silent */ }
  }, []);

  useEffect(() => { loadFolders(); loadDeptFolders(); }, [loadFolders, loadDeptFolders]);

  // Listen for folder-refresh events from other components
  useEffect(() => {
    const handler = () => { loadFolders(); loadDeptFolders(); };
    window.addEventListener('folder-refresh', handler);
    return () => window.removeEventListener('folder-refresh', handler);
  }, [loadFolders, loadDeptFolders]);

  const handleCreateFolder = async () => {
    const name = newFolderName.trim();
    if (!name) return;
    try {
      await api.post('/folders', { name });
      setNewFolderName('');
      setCreatingFolder(false);
      loadFolders();
    } catch { /* silent */ }
  };

  const handleCreateDeptFolder = async (unitId: string) => {
    const name = newDeptFolderName.trim();
    if (!name) return;
    try {
      // ownerUnitId is the MongoDB unit ID (not the code)
      await api.post('/folders', { name, ownerUnitId: unitId, sharedWithAll: 'true' });
      setNewDeptFolderName('');
      setCreatingDeptFolder(null);
      loadDeptFolders();
    } catch { /* silent */ }
  };

  useEffect(() => {
    if (location.pathname.startsWith('/published')) setSharedExpanded(true);
  }, [location.pathname]);

  const toggleUnit = (unitId: string) => {
    setExpandedUnits((prev: Set<string>) => {
      const next = new Set(prev);
      if (next.has(unitId)) next.delete(unitId);
      else next.add(unitId);
      return next;
    });
  };

  const handleLogout = () => { logout(); navigate('/login'); };

  const isActive = (path: string) => location.pathname === path;
  const isActivePrefix = (prefix: string) => location.pathname.startsWith(prefix);

  const navLinkClass = (active: boolean) =>
    `flex items-center gap-2.5 px-3 py-2 rounded-md text-[13px] transition-colors cursor-pointer ${
      active
        ? 'bg-white/12 text-white font-medium'
        : 'text-gray-400 hover:bg-white/6 hover:text-gray-200'
    }`;

  const sectionHeader =
    'px-3 pt-4 pb-1.5 text-[10px] font-semibold uppercase tracking-wider text-gray-500';

  if (!open) return null;

  return (
    <aside
      className="w-64 flex-shrink-0 flex flex-col h-full"
      style={{ backgroundColor: branding.sidebarColor }}
    >
      {/* Brand Header */}
      <div className="flex-shrink-0 px-4 py-3.5 border-b" style={{ borderColor: 'rgba(255,255,255,0.08)' }}>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            {branding.logoUrl ? (
              <img
                src={branding.logoUrl}
                alt="Logo"
                className="w-8 h-8 rounded-lg object-contain"
                style={{ backgroundColor: 'rgba(255,255,255,0.1)' }}
              />
            ) : (
              <div className="w-8 h-8 rounded-lg flex items-center justify-center"
                style={{ backgroundColor: 'rgba(255,255,255,0.1)' }}>
                <FileText size={15} className="text-white" />
              </div>
            )}
            <div>
              <h1 className="text-sm font-bold text-white tracking-tight">{branding.orgName}</h1>
              <p className="text-[10px] leading-tight" style={{ color: 'rgba(255,255,255,0.4)' }}>
                {branding.orgSubtitle}
              </p>
            </div>
          </div>
          <button
            onClick={onToggle}
            className="p-1 rounded-md transition-colors"
            style={{ color: 'rgba(255,255,255,0.4)' }}
            onMouseEnter={e => (e.currentTarget.style.backgroundColor = 'rgba(255,255,255,0.08)')}
            onMouseLeave={e => (e.currentTarget.style.backgroundColor = 'transparent')}
            title="Collapse sidebar"
          >
            <PanelLeftClose size={16} />
          </button>
        </div>
      </div>

      {/* Navigation */}
      <div className="flex-1 overflow-y-auto py-2">
        {/* Primary */}
        <div className="px-2 space-y-0.5">
          <NavLink to="/" className={() => navLinkClass(isActive('/'))}>
            <LayoutDashboard size={16} /> Dashboard
          </NavLink>
        </div>

        <div className="border-t my-3 mx-3" style={{ borderColor: 'rgba(255,255,255,0.08)' }} />

        {/* Workspace */}
        <div className={sectionHeader}>Workspace</div>
        <div className="px-2 space-y-0.5">
          {/* My Documents row: chevron toggles tree, label navigates, + creates folder */}
          <div className={`w-full flex items-center gap-2.5 px-3 py-2 rounded-md text-[13px] transition-colors cursor-pointer ${
            isActive('/documents/my') && !new URLSearchParams(location.search).get('folderId')
              ? 'bg-white/12 text-white font-medium'
              : isActivePrefix('/documents/my')
                ? 'text-gray-200'
                : 'text-gray-400 hover:bg-white/6 hover:text-gray-200'
          }`}>
            <button onClick={() => setMyDocsExpanded(!myDocsExpanded)} className="flex-shrink-0">
              {myDocsExpanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
            </button>
            <button onClick={() => navigate('/documents/my')} className="flex items-center gap-2 flex-1 text-left">
              <FolderOpen size={16} />
              <span>My Documents</span>
            </button>
            <button
              onClick={(e: React.MouseEvent) => { e.stopPropagation(); setCreatingFolder(true); setMyDocsExpanded(true); }}
              className="p-0.5 rounded hover:bg-white/10 text-gray-500 hover:text-white transition-colors"
              title="New Folder"
            >
              <FolderPlus size={12} />
            </button>
          </div>

          {/* Templates */}
          <NavLink to="/templates" className={() => navLinkClass(isActive('/templates'))}>
            <FileStack size={16} /> Templates
          </NavLink>

          {/* Folder tree + inline folder creation */}
          {myDocsExpanded && (
            <div className="ml-5 space-y-0.5 border-l pl-2" style={{ borderColor: 'rgba(255,255,255,0.08)' }}>
              {folders.map((folder: UserFolder) => {
                const folderId = new URLSearchParams(location.search).get('folderId');
                const isFolderActive = isActive('/documents/my') && folderId === folder.id;
                return (
                  <button key={folder.id} onClick={() => navigate(`/documents/my?folderId=${folder.id}`)}
                    className={`w-full ${navLinkClass(isFolderActive)}`}>
                    <FolderOpen size={14} />
                    <span className="text-xs truncate">{folder.name}</span>
                  </button>
                );
              })}
              {creatingFolder && (
                <div className="flex items-center gap-1.5 py-1">
                  <FolderOpen size={13} className="text-gray-500 flex-shrink-0" />
                  <input type="text" value={newFolderName}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) => setNewFolderName(e.target.value)}
                    onKeyDown={(e: React.KeyboardEvent<HTMLInputElement>) => {
                      if (e.key === 'Enter') handleCreateFolder();
                      if (e.key === 'Escape') { setCreatingFolder(false); setNewFolderName(''); }
                    }}
                    onBlur={() => { if (!newFolderName.trim()) { setCreatingFolder(false); setNewFolderName(''); } }}
                    placeholder="Folder name" autoFocus
                    className="text-white text-xs rounded px-2 py-1 w-full focus:outline-none placeholder-gray-500"
                    style={{ backgroundColor: 'rgba(255,255,255,0.1)', border: '1px solid rgba(255,255,255,0.15)' }}
                  />
                </div>
              )}
            </div>
          )}
        </div>

        <div className="border-t my-3 mx-3" style={{ borderColor: 'rgba(255,255,255,0.08)' }} />

        {/* Shared / Departments */}
        <div className={sectionHeader}>Shared</div>
        <div className="px-2 space-y-0.5">
          <button
            onClick={() => setSharedExpanded(!sharedExpanded)}
            className={`w-full ${navLinkClass(isActivePrefix('/published'))}`}
          >
            {sharedExpanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
            <Building2 size={16} />
            <span className="flex-1 text-left">Departments</span>
          </button>
          {sharedExpanded && (
            <div className="ml-5 space-y-0.5 border-l pl-2" style={{ borderColor: 'rgba(255,255,255,0.08)' }}>
              {orgUnits.map(unit => (
                <div key={unit.id}>
                  <div className={`w-full flex items-center gap-2.5 px-3 py-2 rounded-md text-[13px] transition-colors cursor-pointer ${
                    isActivePrefix('/published/') && location.pathname.includes(`/${unit.code}`)
                      ? 'bg-white/12 text-white font-medium'
                      : 'text-gray-400 hover:bg-white/6 hover:text-gray-200'
                  }`}>
                    <button onClick={() => toggleUnit(unit.id)} className="flex-shrink-0">
                      {expandedUnits.has(unit.id) ? <ChevronDown size={12} /> : <ChevronRight size={12} />}
                    </button>
                    <span className="flex-1 text-left text-xs">{unit.displayName}</span>
                    <button
                      onClick={(e: React.MouseEvent) => {
                        e.stopPropagation();
                        setCreatingDeptFolder(unit.id);
                        setExpandedUnits((prev: Set<string>) => new Set(prev).add(unit.id));
                      }}
                      className="p-0.5 rounded hover:bg-white/10 text-gray-500 hover:text-white transition-colors"
                      title="New shared folder"
                    >
                      <FolderPlus size={11} />
                    </button>
                  </div>
                  {expandedUnits.has(unit.id) && (
                    <div className="ml-4 space-y-0.5 border-l pl-2" style={{ borderColor: 'rgba(255,255,255,0.06)' }}>
                      {/* Doc types as sub-links: /published/{typeCode}/{unitCode} */}
                      {docTypes.map(dt => (
                        <NavLink key={dt.id}
                          to={`/published/${dt.code}/${unit.code}`}
                          className={() => navLinkClass(isActive(`/published/${dt.code}/${unit.code}`))}
                        >
                          <FileText size={13} />
                          <span className="text-xs">{dt.displayName}s</span>
                        </NavLink>
                      ))}
                      {/* Shared department folders */}
                      {(deptFolders[unit.id] || []).map((folder: UserFolder) => (
                        <button key={folder.id}
                          onClick={() => navigate(`/documents/my?folderId=${folder.id}`)}
                          className={`w-full ${navLinkClass(false)}`}>
                          <FolderOpen size={13} />
                          <span className="text-xs truncate">{folder.name}</span>
                        </button>
                      ))}
                      {/* Inline folder creation */}
                      {creatingDeptFolder === unit.id && (
                        <div className="flex items-center gap-1.5 py-1">
                          <FolderOpen size={12} className="text-gray-500 flex-shrink-0" />
                          <input type="text" value={newDeptFolderName}
                            onChange={(e: React.ChangeEvent<HTMLInputElement>) => setNewDeptFolderName(e.target.value)}
                            onKeyDown={(e: React.KeyboardEvent<HTMLInputElement>) => {
                              if (e.key === 'Enter') handleCreateDeptFolder(unit.id);
                              if (e.key === 'Escape') { setCreatingDeptFolder(null); setNewDeptFolderName(''); }
                            }}
                            onBlur={() => { if (!newDeptFolderName.trim()) { setCreatingDeptFolder(null); setNewDeptFolderName(''); } }}
                            placeholder="Folder name" autoFocus
                            className="text-white text-xs rounded px-2 py-1 w-full focus:outline-none placeholder-gray-500"
                            style={{ backgroundColor: 'rgba(255,255,255,0.1)', border: '1px solid rgba(255,255,255,0.15)' }}
                          />
                        </div>
                      )}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="border-t my-3 mx-3" style={{ borderColor: 'rgba(255,255,255,0.08)' }} />

        {/* Administration */}
        {hasRole('SYSTEM_ADMIN') && (
          <>
            <div className={sectionHeader}>Administration</div>
            <div className="px-2 space-y-0.5">
              <NavLink to="/users" className={() => navLinkClass(isActive('/users'))}>
                <Users size={16} /> User Management
              </NavLink>
              <NavLink to="/admin" className={() => navLinkClass(isActive('/admin'))}>
                <Settings size={16} /> System Settings
              </NavLink>
            </div>
          </>
        )}
      </div>

      {/* Bottom */}
      <div className="flex-shrink-0 border-t" style={{ borderColor: 'rgba(255,255,255,0.08)' }}>
        <button className="w-full flex items-center gap-2.5 px-4 py-2.5 text-[13px] text-gray-400 hover:text-gray-200 transition-colors"
          style={{ }} onMouseEnter={e => (e.currentTarget.style.backgroundColor = 'rgba(255,255,255,0.05)')}
          onMouseLeave={e => (e.currentTarget.style.backgroundColor = 'transparent')}>
          <Bell size={15} />
          <span className="flex-1 text-left">Notifications</span>
          <span className="w-5 h-5 rounded-full text-white text-[10px] font-bold flex items-center justify-center"
            style={{ backgroundColor: 'rgba(255,255,255,0.2)' }}>3</span>
        </button>

        <div className="flex items-center gap-2.5 px-4 py-3" style={{ backgroundColor: 'rgba(0,0,0,0.2)' }}>
          <div className="flex items-center justify-center w-8 h-8 rounded-full flex-shrink-0"
            style={{ backgroundColor: 'rgba(255,255,255,0.1)', color: 'rgba(255,255,255,0.7)' }}>
            <User size={14} />
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-xs font-medium text-white truncate">{user?.fullName}</p>
            <p className="text-[10px] truncate" style={{ color: 'rgba(255,255,255,0.4)' }}>
              {user ? formatRole(user.role) : ''}
            </p>
          </div>
          <button onClick={handleLogout}
            className="p-1.5 rounded-md transition-colors flex-shrink-0"
            style={{ color: 'rgba(255,255,255,0.3)' }}
            onMouseEnter={e => { e.currentTarget.style.backgroundColor = 'rgba(255,255,255,0.1)'; e.currentTarget.style.color = '#f87171'; }}
            onMouseLeave={e => { e.currentTarget.style.backgroundColor = 'transparent'; e.currentTarget.style.color = 'rgba(255,255,255,0.3)'; }}
            title="Logout">
            <LogOut size={14} />
          </button>
        </div>
      </div>
    </aside>
  );
}

export default React.memo(Sidebar);
