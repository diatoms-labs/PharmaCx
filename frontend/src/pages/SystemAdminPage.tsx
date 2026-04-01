import { useEffect, useRef, useState } from 'react';
import {
  Shield, Loader2, ToggleLeft, ToggleRight, X, Check, Settings, Palette,
  ImageIcon, Cpu, Globe, FolderOpen, AlertCircle
} from 'lucide-react';
import api from '../api/client';
import { showErrorToast } from '../utils/errorHandler';
import toast from 'react-hot-toast';
import { notifyBrandingUpdate, BRANDING_DEFAULTS } from '../hooks/useBranding';

type Tab = 'editor-perms' | 'ip-policies' | 'org-units' | 'doc-types' | 'system-settings' | 'branding' | 'ai-config';

// ── System Setting values type ────────────────────────────────────────────────
interface SettingValues {
  downloadEnabled: boolean;
  printEnabled: boolean;
  uploadEnabled: boolean;
  allowExternalAccess: boolean;
  sessionTimeoutMinutes: number;
  maxFileUploadMb: number;
  // Branding
  orgName: string;
  orgSubtitle: string;
  logoUrl: string | null;
  sidebarColor: string;
  accentColor: string;
  headerColor: string | null;
  // AI Configuration
  aiStrategy: 'LOCAL' | 'CLOUD';
  ollamaUrl: string;
  localEmbedModel: string;
  localChatModel: string;
  localLightModel: string;
  cloudAiProvider: string;
  cloudAiApiKey: string;
  cloudAiModel: string;
  externalKnowledgePath?: string; // Stale, but for backward compatibility in types
  backgroundKnowledgePath: string; // New infrastructure-managed
  hostKnowledgePath?: string;
  hostPublishedDocsPath?: string;
  publishedDocsPath: string;
  copyOnPublish: boolean;
}

export default function SystemAdminPage() {
  const [tab, setTab] = useState<Tab>('editor-perms');

  const tabClass = (t: Tab) =>
    `px-4 py-2.5 text-sm font-medium transition-colors border-b-2 ${
      tab === t
        ? 'border-gray-800 text-gray-800'
        : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
    }`;

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-3 mb-2">
        <div className="p-2 bg-gray-100 dark:bg-gray-800 rounded-lg">
          <Settings className="w-5 h-5 text-gray-600 dark:text-gray-400" />
        </div>
        <h1 className="text-xl font-bold text-gray-900 dark:text-white">System Administration</h1>
      </div>

      <div className="border-b border-gray-200 dark:border-gray-800 overflow-x-auto">
        <nav className="flex gap-2">
          <button onClick={() => setTab('editor-perms')} className={tabClass('editor-perms')}>Editor Permissions</button>
          <button onClick={() => setTab('ip-policies')} className={tabClass('ip-policies')}>IP Policies</button>
          <button onClick={() => setTab('org-units')} className={tabClass('org-units')}>Org Units</button>
          <button onClick={() => setTab('doc-types')} className={tabClass('doc-types')}>Doc Types</button>
          <button onClick={() => setTab('system-settings')} className={tabClass('system-settings')}>System Settings</button>
          <button onClick={() => setTab('branding')} className={tabClass('branding')}>Branding</button>
          <button onClick={() => setTab('ai-config')} className={tabClass('ai-config')}>AI Configuration</button>
        </nav>
      </div>

      <div className="py-4">
        {tab === 'editor-perms' && <EditorPermissionsTab />}
        {tab === 'ip-policies' && <IpPoliciesTab />}
        {tab === 'org-units' && <OrgUnitsTab />}
        {tab === 'doc-types' && <DocTypesTab />}
        {tab === 'system-settings' && <SystemSettingsTab />}
        {tab === 'branding' && <BrandingTab />}
        {tab === 'ai-config' && <AIConfigurationTab />}
      </div>
    </div>
  );
}

// ── Placeholder Tabs ──
function EditorPermissionsTab() { return <div className="text-sm text-gray-500">Editor permissions implementation...</div>; }
function IpPoliciesTab() { return <div className="text-sm text-gray-500">IP policies implementation...</div>; }
function OrgUnitsTab() { return <div className="text-sm text-gray-500">Org units implementation...</div>; }
function DocTypesTab() { return <div className="text-sm text-gray-500">Doc types implementation...</div>; }

// ── System Settings Tab ───────────────────────────────────────────────────────

function SystemSettingsTab() {
  const [values, setValues] = useState<SettingValues>({
    downloadEnabled: false,
    printEnabled: false,
    uploadEnabled: true,
    allowExternalAccess: false,
    sessionTimeoutMinutes: 480,
    maxFileUploadMb: 50,
    orgName: '',
    orgSubtitle: '',
    logoUrl: null,
    sidebarColor: '#0F3D6E',
    accentColor: '#1E7FC4',
    headerColor: '#1E7FC4',
    aiStrategy: 'LOCAL',
    ollamaUrl: 'http://localhost:11434',
    localEmbedModel: 'nomic-embed-text',
    localChatModel: 'helix-ai',
    localLightModel: 'phi3:mini',
    cloudAiProvider: 'GOOGLE',
    cloudAiApiKey: '',
    cloudAiModel: 'gemini-1.5-flash',
    backgroundKnowledgePath: '',
    publishedDocsPath: '',
    copyOnPublish: true,
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    api.get<{ settings: SettingValues }>('/system-settings').then(res => {
      if (res.data.settings) setValues(res.data.settings);
    }).catch(() => showErrorToast(null, 'Failed to load system settings'))
      .finally(() => setLoading(false));
  }, []);

  const save = async () => {
    setSaving(true);
    try {
      const res = await api.put<{ settings: SettingValues }>('/system-settings', values);
      if (res.data.settings) setValues(res.data.settings);
      toast.success('System settings saved');
    } catch (err) {
      showErrorToast(err, 'Failed to save settings');
    } finally {
      setSaving(false);
    }
  };

  const BoolRow = ({ label, desc, field }: { label: string; desc: string; field: keyof SettingValues }) => (
    <div className="flex items-center justify-between py-3 border-b border-gray-100 last:border-0">
      <div>
        <p className="text-sm font-medium text-gray-800">{label}</p>
        <p className="text-xs text-gray-400 mt-0.5">{desc}</p>
      </div>
      <button onClick={() => setValues(v => ({ ...v, [field]: !v[field] }))}
        className="flex-shrink-0">
        {values[field]
          ? <ToggleRight size={24} className="text-green-500" />
          : <ToggleLeft size={24} className="text-gray-300" />
        }
      </button>
    </div>
  );

  if (loading) return <div className="flex justify-center py-8"><Loader2 size={20} className="animate-spin text-gray-400" /></div>;

  return (
    <div className="max-w-lg space-y-4">
      <p className="text-sm text-gray-500">
        Global default settings applied system-wide.
      </p>

      <div className="bg-gray-50 rounded-lg border border-gray-200 px-4 divide-y divide-gray-100">
        <BoolRow label="Allow Download (default)" desc="Default download capability for new users" field="downloadEnabled" />
        <BoolRow label="Allow Print (default)" desc="Default print capability for new users" field="printEnabled" />
        <BoolRow label="Allow Upload (default)" desc="Default upload capability for new users" field="uploadEnabled" />
        <BoolRow label="Allow External Access" desc="Permit access from outside the corporate network" field="allowExternalAccess" />
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="block text-xs font-medium text-gray-500 mb-1">Session Timeout (minutes)</label>
          <input type="number" className="input text-sm" min={5} max={1440}
            value={values.sessionTimeoutMinutes}
            onChange={e => setValues(v => ({ ...v, sessionTimeoutMinutes: Number(e.target.value) }))} />
        </div>
        <div>
          <label className="block text-xs font-medium text-gray-500 mb-1">Max File Upload (MB)</label>
          <input type="number" className="input text-sm" min={1} max={500}
            value={values.maxFileUploadMb}
            onChange={e => setValues(v => ({ ...v, maxFileUploadMb: Number(e.target.value) }))} />
        </div>
      </div>

      <button onClick={save} disabled={saving} className="btn-primary text-sm px-4 py-2">
        {saving ? <><Loader2 size={14} className="animate-spin" /> Saving...</> : <><Check size={14} /> Save Settings</>}
      </button>
    </div>
  );
}

// ── AI Configuration Tab ──────────────────────────────────────────────────────

function AIConfigurationTab() {
  const [values, setValues] = useState<SettingValues>({
    downloadEnabled: false,
    printEnabled: false,
    uploadEnabled: true,
    allowExternalAccess: false,
    sessionTimeoutMinutes: 480,
    maxFileUploadMb: 50,
    orgName: '',
    orgSubtitle: '',
    logoUrl: null,
    sidebarColor: '#0F3D6E',
    accentColor: '#1E7FC4',
    headerColor: '#1E7FC4',
    aiStrategy: 'LOCAL',
    ollamaUrl: 'http://localhost:11434',
    localEmbedModel: 'nomic-embed-text',
    localChatModel: 'helix-ai',
    localLightModel: 'phi3:mini',
    cloudAiProvider: 'GOOGLE',
    cloudAiApiKey: '',
    cloudAiModel: 'gemini-1.5-flash',
    externalKnowledgePath: '',
    backgroundKnowledgePath: '',
    hostKnowledgePath: '',
    hostPublishedDocsPath: '',
    publishedDocsPath: '',
    copyOnPublish: true,
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [indexing, setIndexing] = useState(false);

  useEffect(() => {
    const fetchData = async () => {
      try {
        // 1. Fetch persistent system settings
        const settingsRes = await api.get<{ settings: SettingValues }>('/system-settings');
        if (settingsRes.data.settings) {
          setValues((prev: SettingValues) => ({ ...prev, ...settingsRes.data.settings }));
        }
        
        // 2. Fetch real-time AI configuration (infrastructure-managed)
        const aiConfigRes = await api.get<any>('/ai/config');
        const cfg = aiConfigRes.data;
        if (cfg) {
          setValues((prev: SettingValues) => ({
            ...prev,
            aiStrategy: cfg.strategy,
            ollamaUrl: cfg.ollamaUrl,
            localEmbedModel: cfg.embedModel,
            localChatModel: cfg.chatModel,
            localLightModel: cfg.lightChatModel,
            cloudAiProvider: cfg.cloudProvider,
            cloudAiApiKey: cfg.cloudApiKey || '',
            cloudAiModel: cfg.cloudModel,
            backgroundKnowledgePath: cfg.backgroundKnowledgePath,
            hostKnowledgePath: cfg.hostKnowledgePath,
            hostPublishedDocsPath: cfg.hostPublishedDocsPath,
            publishedDocsPath: cfg.publishedDocsPath,
            copyOnPublish: cfg.copyOnPublish
          }));
        }
      } catch (err) {
        showErrorToast(err, 'Failed to load configuration');
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  const save = async () => {
    setSaving(true);
    try {
      const res = await api.put<{ settings: SettingValues }>('/system-settings', values);
      if (res.data.settings) setValues(res.data.settings);
      toast.success('AI Configuration saved');
    } catch (err) {
      showErrorToast(err, 'Failed to save configuration');
    } finally {
      setSaving(false);
    }
  };

  const triggerReindex = async () => {
    setIndexing(true);
    try {
      await api.post('/ai/index/all');
      toast.success('Full AI re-indexing triggered successfully');
    } catch (err) {
      showErrorToast(err, 'Failed to trigger re-indexing');
    } finally {
      setIndexing(false);
    }
  };

  const strategies = [
    { id: 'LOCAL', name: 'Private Local AI', desc: 'Secure host-based Helix AI engine. No data leaves your network.', icon: <Shield size={18} className="text-green-500" /> },
    { id: 'CLOUD', name: 'Elastic Cloud AI', desc: 'High performance processing via Google Gemini or Claude.', icon: <Globe size={18} className="text-blue-500" /> },
  ];

  if (loading) return <div className="flex justify-center py-8"><Loader2 size={24} className="animate-spin text-brand-500" /></div>;

  return (
    <div className="max-w-2xl space-y-6">
      <div className="space-y-4">
        <h3 className="text-sm font-bold text-slate-800 uppercase tracking-wider">AI Host Strategy</h3>
        <p className="text-xs text-gray-500 bg-amber-50 p-3 rounded-lg border border-amber-100 flex gap-2 items-center">
          <AlertCircle size={14} className="text-amber-600 shrink-0" />
          Helix AI prioritizes data sovereignty. Cloud processing is only recommended for public document indexing or sanitized drafting.
        </p>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {strategies.map(s => (
            <button
              key={s.id}
              onClick={() => setValues(v => ({ ...v, aiStrategy: s.id as any }))}
              className={`flex flex-col gap-3 p-5 rounded-2xl border text-left transition-all duration-200 ${
                values.aiStrategy === s.id 
                  ? 'border-brand-500 bg-brand-50/30 ring-2 ring-brand-500/20' 
                  : 'border-slate-200 bg-white hover:border-slate-300'
              }`}
            >
              <div className="flex items-center justify-between">
                {s.icon}
                {values.aiStrategy === s.id && <Check size={14} className="text-brand-600" />}
              </div>
              <div>
                <span className="text-sm font-bold text-slate-900 block mb-1">{s.name}</span>
                <p className="text-xs text-slate-500 leading-relaxed">{s.desc}</p>
              </div>
            </button>
          ))}
        </div>
      </div>

      {values.aiStrategy === 'LOCAL' && (
        <div className="bg-slate-50 rounded-2xl p-6 border border-slate-200 animate-in fade-in slide-in-from-top-2 duration-300">
          <h3 className="text-sm font-bold text-slate-900 mb-4 flex items-center gap-2">
            <Cpu size={16} className="text-green-600" />
            Host Helix AI Configuration (Local)
          </h3>
          
          <div className="grid grid-cols-2 gap-4 mb-4">
            <div className="col-span-2">
              <label className="block text-xs font-bold text-slate-600 mb-1.5 uppercase tracking-tighter">Helix AI Endpoint</label>
              <input 
                type="text" 
                className="input text-sm font-mono"
                placeholder="http://localhost:11434"
                value={values.ollamaUrl}
                onChange={e => setValues(v => ({ ...v, ollamaUrl: e.target.value }))}
              />
              <p className="text-[10px] text-slate-400 mt-1">Use <strong>http://host.docker.internal:11434</strong> if running inside Docker Desktop.</p>
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-600 mb-1.5 uppercase tracking-tighter">Chat Model Name</label>
              <input 
                type="text" 
                className="input text-sm font-mono placeholder-gray-300"
                placeholder="helix-ai"
                value={values.localChatModel}
                onChange={e => setValues(v => ({ ...v, localChatModel: e.target.value }))}
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-600 mb-1.5 uppercase tracking-tighter">Embedding Model</label>
              <input 
                type="text" 
                className="input text-sm font-mono placeholder-gray-300"
                placeholder="nomic-embed-text"
                value={values.localEmbedModel}
                onChange={e => setValues(v => ({ ...v, localEmbedModel: e.target.value }))}
              />
            </div>
          </div>
        </div>
      )}

      {values.aiStrategy === 'CLOUD' && (
        <div className="bg-slate-50 rounded-2xl p-6 border border-slate-200 animate-in fade-in slide-in-from-top-2 duration-300">
          <h3 className="text-sm font-bold text-slate-900 mb-4 flex items-center gap-2">
            <Globe size={16} className="text-brand-600" />
            Cloud Intelligence Provider
          </h3>
          
          <div className="grid grid-cols-2 gap-4 mb-4">
            <div>
              <label className="block text-xs font-bold text-slate-600 mb-1.5 uppercase tracking-tighter">Provider</label>
              <select 
                className="input text-sm"
                value={values.cloudAiProvider}
                onChange={e => setValues(v => ({ ...v, cloudAiProvider: e.target.value }))}
              >
                <option value="GOOGLE">Google Gemini AI</option>
                <option value="CLAUDE">Anthropic Claude</option>
                <option value="OPENAI">OpenAI (GPT-4o)</option>
                <option value="PERPLEXITY">Perplexity AI (Search)</option>
                <option value="MISTRAL">Mistral AI</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-600 mb-1.5 uppercase tracking-tighter">Model Tag</label>
              <input 
                type="text" 
                className="input text-sm font-mono"
                placeholder="e.g. gemini-1.5-flash"
                value={values.cloudAiModel}
                onChange={e => setValues(v => ({ ...v, cloudAiModel: e.target.value }))}
              />
            </div>
            <div className="col-span-2">
              <label className="block text-xs font-bold text-slate-600 mb-1.5 uppercase tracking-tighter">API Access Key</label>
              <input 
                type="password" 
                className="input text-sm font-mono"
                placeholder="sk-..."
                value={values.cloudAiApiKey}
                onChange={e => setValues(v => ({ ...v, cloudAiApiKey: e.target.value }))}
              />
            </div>
          </div>
        </div>
      )}

      <div className="bg-slate-50 rounded-2xl p-6 border border-slate-200">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
            <FolderOpen size={16} className="text-blue-600" />
            Helix AI Grounding Store
          </h3>
          <button 
            onClick={triggerReindex} 
            disabled={indexing}
            className="text-[11px] font-bold uppercase tracking-wider text-brand-600 hover:text-brand-700 disabled:text-gray-400 flex items-center gap-1.5"
          >
            {indexing ? <Loader2 size={12} className="animate-spin" /> : <div className="w-2 h-2 rounded-full bg-brand-500 animate-pulse" />}
            Trigger Full AI Re-index
          </button>
        </div>
        
        <div className="space-y-4">
          <div className="p-4 bg-white rounded-xl border border-slate-100">
            <label className="block text-[10px] font-bold text-slate-400 mb-1.5 uppercase tracking-wider">System Grounding Path (Knowledge-Base)</label>
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-mono text-blue-600 font-bold">{values.hostKnowledgePath || 'Not Configured'}</span>
              <span className="px-2 py-0.5 rounded text-[9px] font-bold bg-blue-50 text-blue-500 border border-blue-100 uppercase">System Physical Path</span>
            </div>
            
            <div className="pt-2 border-t border-slate-50 flex items-center justify-between">
              <span className="text-[10px] font-mono text-slate-400 italic">Container Internal: {values.backgroundKnowledgePath || 'N/A'}</span>
              <span className="text-[9px] font-bold text-slate-300 uppercase">Docker Virtual Mono</span>
            </div>
            
            <p className="text-[10px] text-slate-400 mt-3 italic flex gap-1.5 items-center">
              <AlertCircle size={10} />
              Managed via AI_HOST_KNOWLEDGE_PATH infrastructure setting.
            </p>
          </div>

          <div className="pt-4 border-t border-slate-200">
             <div className="flex items-center justify-between mb-3">
                <div>
                  <p className="text-sm font-bold text-slate-800">Helix AI Hot-Folder Sync</p>
                  <p className="text-[11px] text-slate-500 mt-0.5">SOPs are automatically synced to Helix AI upon publication.</p>
                </div>
                <div className="px-2 py-1 bg-green-50 text-green-600 text-[10px] font-extrabold rounded border border-green-100 uppercase">Always Active</div>
             </div>
             
             <div className="p-3 bg-white rounded-xl border border-slate-100">
                <label className="block text-[10px] font-bold text-slate-400 mb-1 px-1 uppercase tracking-wider">System Export Location (Published-Docs)</label>
                <div className="flex items-center justify-between mb-1">
                   <code className="text-sm text-emerald-600 font-bold block px-1 truncate">{values.hostPublishedDocsPath || 'Not Configured'}</code>
                   <span className="px-2 py-0.5 rounded text-[9px] font-bold bg-emerald-50 text-emerald-600 border border-emerald-100 uppercase whitespace-nowrap">Physical Sync</span>
                </div>
                <div className="px-1 text-[9px] text-slate-400 font-mono">Container Mapping: {values.publishedDocsPath || 'N/A'}</div>
             </div>
          </div>
        </div>
      </div>

      <div className="flex items-center gap-4">
        <button onClick={save} disabled={saving} className="btn-primary text-sm px-8 py-3 shadow-xl shadow-brand-500/20">
          {saving ? <><Loader2 size={16} className="animate-spin" /> Saving Configuration...</> : <><Check size={16} /> Update AI Engine</>}
        </button>
      </div>
    </div>
  );
}

// ── Branding Tab ──────────────────────────────────────────────────────────────

function BrandingTab() {
  const [values, setValues] = useState({
    orgName: BRANDING_DEFAULTS.orgName,
    orgSubtitle: BRANDING_DEFAULTS.orgSubtitle,
    logoUrl: BRANDING_DEFAULTS.logoUrl as string | null,
    sidebarColor: BRANDING_DEFAULTS.sidebarColor,
    accentColor: BRANDING_DEFAULTS.accentColor,
    headerColor: BRANDING_DEFAULTS.headerColor as string | null,
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    api.get<{ settings: SettingValues }>('/system-settings')
      .then(res => {
        if (res.data.settings) {
          const s = res.data.settings;
          setValues({
            orgName: s.orgName,
            orgSubtitle: s.orgSubtitle,
            logoUrl: s.logoUrl,
            sidebarColor: s.sidebarColor,
            accentColor: s.accentColor,
            headerColor: s.headerColor,
          });
        }
      }).catch(() => showErrorToast(null, 'Failed to load branding settings'))
      .finally(() => setLoading(false));
  }, []);

  const save = async () => {
    setSaving(true);
    try {
      const res = await api.put<{ settings: SettingValues }>('/system-settings', values);
      if (res.data.settings) {
        toast.success('Branding updated');
        notifyBrandingUpdate(res.data.settings);
      }
    } catch (err) {
      showErrorToast(err, 'Failed to save branding');
    } finally {
      setSaving(false);
    }
  };

  const handleLogoUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const formData = new FormData();
    formData.append('file', file);
    try {
      const res = await api.post<{ url: string }>('/storage/upload-public', formData);
      setValues(v => ({ ...v, logoUrl: res.data.url }));
      toast.success('Logo uploaded');
    } catch (err) {
      showErrorToast(err, 'Logo upload failed');
    }
  };

  if (loading) return <div className="flex justify-center py-8"><Loader2 size={24} className="animate-spin text-brand-500" /></div>;

  return (
    <div className="max-w-2xl space-y-8">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        <div className="space-y-6">
          <div className="space-y-4">
            <h3 className="text-sm font-bold text-slate-800 uppercase tracking-wider">Organization Identity</h3>
            <div className="space-y-3">
              <div>
                <label className="block text-xs font-bold text-slate-600 mb-1 uppercase">Full Organization Name</label>
                <input type="text" className="input text-sm" value={values.orgName} 
                  onChange={e => setValues(v => ({ ...v, orgName: e.target.value }))} />
              </div>
              <div>
                <label className="block text-xs font-bold text-slate-600 mb-1 uppercase">Department or Subtitle</label>
                <input type="text" className="input text-sm" value={values.orgSubtitle} 
                  onChange={e => setValues(v => ({ ...v, orgSubtitle: e.target.value }))} />
              </div>
            </div>
          </div>

          <div className="space-y-4">
            <h3 className="text-sm font-bold text-slate-800 uppercase tracking-wider">Brand Colors</h3>
            <div className="grid grid-cols-3 gap-3">
              <div>
                <label className="block text-[10px] font-bold text-slate-500 mb-1 uppercase">Sidebar</label>
                <div className="flex gap-2">
                  <input type="color" className="w-8 h-8 rounded border-0 p-0 cursor-pointer" value={values.sidebarColor}
                    onChange={e => setValues(v => ({ ...v, sidebarColor: e.target.value }))} />
                  <input type="text" className="input text-[10px] py-1 px-2 font-mono" value={values.sidebarColor}
                    onChange={e => setValues(v => ({ ...v, sidebarColor: e.target.value }))} />
                </div>
              </div>
              <div>
                <label className="block text-[10px] font-bold text-slate-500 mb-1 uppercase">Accent</label>
                <div className="flex gap-2">
                  <input type="color" className="w-8 h-8 rounded border-0 p-0 cursor-pointer" value={values.accentColor}
                    onChange={e => setValues(v => ({ ...v, accentColor: e.target.value }))} />
                  <input type="text" className="input text-[10px] py-1 px-2 font-mono" value={values.accentColor}
                    onChange={e => setValues(v => ({ ...v, accentColor: e.target.value }))} />
                </div>
              </div>
              <div>
                <label className="block text-[10px] font-bold text-slate-500 mb-1 uppercase">Header</label>
                <div className="flex gap-2">
                  <input type="color" className="w-8 h-8 rounded border-0 p-0 cursor-pointer" value={values.headerColor || '#FFFFFF'}
                    onChange={e => setValues(v => ({ ...v, headerColor: e.target.value }))} />
                  <input type="text" className="input text-[10px] py-1 px-2 font-mono" value={values.headerColor || ''}
                    onChange={e => setValues(v => ({ ...v, headerColor: e.target.value }))} />
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="space-y-6">
          <div className="space-y-4">
            <h3 className="text-sm font-bold text-slate-800 uppercase tracking-wider">Logo Branding</h3>
            <div className="bg-slate-50 border-2 border-dashed border-slate-200 rounded-2xl p-6 flex flex-col items-center justify-center gap-4 transition-colors hover:bg-slate-100/80">
              {values.logoUrl ? (
                <div className="relative group">
                  <img src={values.logoUrl} alt="Logo Preview" className="max-h-24 object-contain shadow-sm rounded-lg" />
                  <button onClick={() => setValues(v => ({ ...v, logoUrl: null }))}
                    className="absolute -top-2 -right-2 p-1 bg-white shadow-md rounded-full text-red-500 opacity-0 group-hover:opacity-100 transition-opacity">
                    <X size={14} />
                  </button>
                </div>
              ) : (
                <div className="flex flex-col items-center gap-2 cursor-pointer" onClick={() => fileInputRef.current?.click()}>
                  <div className="w-12 h-12 rounded-full bg-white flex items-center justify-center text-slate-400 shadow-sm border border-slate-100">
                    <ImageIcon size={24}/>
                  </div>
                  <span className="text-xs font-bold text-brand-600">Upload Organization Logo</span>
                  <span className="text-[10px] text-slate-400">PNG or SVG recommended</span>
                </div>
              )}
              <input type="file" ref={fileInputRef} className="hidden" accept="image/*" onChange={handleLogoUpload} />
            </div>
          </div>
        </div>
      </div>

      <div className="pt-6 border-t border-slate-100 flex items-center justify-between">
        <div className="flex items-center gap-2 text-slate-400">
          <Palette size={16} />
          <span className="text-xs tracking-tight uppercase font-bold">Live branding system active</span>
        </div>
        <button onClick={save} disabled={saving} className="btn-primary px-8 py-3 shadow-xl shadow-brand-500/20">
          {saving ? <><Loader2 size={16} className="animate-spin" /> Saving Changes...</> : <><Check size={16} /> Apply Global Branding</>}
        </button>
      </div>
    </div>
  );
}
