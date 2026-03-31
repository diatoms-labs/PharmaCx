import { useEffect, useState, useRef, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { FileText, Loader2, AlertCircle, ArrowLeft } from 'lucide-react';
import { DocumentDetailSkeleton } from '../components/ui/PageSkeleton';
import api from '../api/client';
import type { DocumentTemplate, EditorConfig, TemplateDetailResponse } from '../types';
import { showErrorToast } from '../utils/errorHandler';

export default function TemplateViewPage() {
  const { id } = useParams();
  const [template, setTemplate] = useState<DocumentTemplate | null>(null);
  const [loading, setLoading] = useState(true);

  const [editorLoading, setEditorLoading] = useState(false);
  const [editorError, setEditorError] = useState<string | null>(null);
  const editorInstanceRef = useRef<unknown>(null);
  const scriptLoadedRef = useRef(false);

  const [initialEditorConfig, setInitialEditorConfig] = useState<EditorConfig | null>(null);

  useEffect(() => {
    api.get<TemplateDetailResponse>(`/templates/${id}`)
      .then(res => {
        setTemplate(res.data.template);
        setInitialEditorConfig(res.data.editorConfig);
      })
      .catch((err) => showErrorToast(err, 'Failed to load template'))
      .finally(() => setLoading(false));
  }, [id]);

  const initEditor = useCallback((config: EditorConfig) => {
    requestAnimationFrame(() => {
      try {
        const editorEl = document.getElementById('template-editor');
        if (!editorEl) { setEditorError('Editor container not found.'); setEditorLoading(false); return; }

        const DocsAPI = (window as unknown as Record<string, unknown>).DocsAPI as
          { DocEditor: new (id: string, cfg: Record<string, unknown>) => unknown } | undefined;
        if (!DocsAPI) { setEditorError('ONLYOFFICE API failed to initialize.'); setEditorLoading(false); return; }

        if (editorInstanceRef.current) {
          try { (editorInstanceRef.current as { destroyEditor?: () => void }).destroyEditor?.(); } catch { /* */ }
        }

        editorInstanceRef.current = new DocsAPI.DocEditor('template-editor', {
          ...config.config,
          events: {
            onReady: () => console.log('Template viewer ready'),
            onDocumentReady: () => console.log('Template document loaded'),
          },
        });
        setEditorLoading(false);
      } catch {
        setEditorError('Failed to initialize editor.');
        setEditorLoading(false);
      }
    });
  }, []);

  useEffect(() => {
    if (!template || !template.fileStorageId) return;
    let cancelled = false;

    const loadEditor = async () => {
      setEditorLoading(true);
      setEditorError(null);
      try {
        // Use the editor config from the combined response if available
        let config: EditorConfig;
        if (initialEditorConfig) {
          config = initialEditorConfig;
          setInitialEditorConfig(null); // consume it
        } else {
          const res = await api.get<TemplateDetailResponse>(`/templates/${id}`);
          if (cancelled) return;
          if (!res.data.editorConfig) {
            setEditorError('Editor configuration not available.');
            setEditorLoading(false);
            return;
          }
          config = res.data.editorConfig;
        }
        if (cancelled) return;
        const scriptUrl = `${config.documentServerUrl}/web-apps/apps/api/documents/api.js`;

        if (scriptLoadedRef.current && (window as unknown as Record<string, unknown>).DocsAPI) {
          initEditor(config);
          return;
        }
        const existingScript = document.querySelector(`script[src="${scriptUrl}"]`);
        if (existingScript) existingScript.remove();

        const script = document.createElement('script');
        script.src = scriptUrl;
        script.async = true;
        script.onload = () => { if (!cancelled) { scriptLoadedRef.current = true; initEditor(config); } };
        script.onerror = () => { if (!cancelled) { setEditorError('Failed to load Document Server.'); setEditorLoading(false); } };
        document.head.appendChild(script);
      } catch {
        if (!cancelled) { setEditorError('Failed to load editor configuration.'); setEditorLoading(false); }
      }
    };
    loadEditor();

    return () => {
      cancelled = true;
      if (editorInstanceRef.current) {
        try { (editorInstanceRef.current as { destroyEditor?: () => void }).destroyEditor?.(); } catch { /* */ }
        editorInstanceRef.current = null;
      }
    };
  }, [template, id, initEditor, initialEditorConfig]);

  if (loading) {
    return <DocumentDetailSkeleton />;
  }

  if (!template) {
    return <div className="card p-6 text-center text-gray-500">Template not found</div>;
  }

  return (
    <div className="flex flex-col h-[calc(100%+48px)] -m-6">
      {/* Header */}
      <div className="flex-shrink-0 bg-white border-b border-gray-200 px-5 py-3">
        <div className="flex items-center gap-3">
          <Link to="/templates" className="text-gray-400 hover:text-gray-600 transition-colors">
            <ArrowLeft size={18} />
          </Link>
          <div className="flex items-center gap-2.5 min-w-0">
            <div className="w-8 h-8 rounded bg-gray-100 flex items-center justify-center flex-shrink-0">
              <FileText size={14} className="text-gray-500" />
            </div>
            <div className="min-w-0">
              <h1 className="text-base font-semibold text-gray-900 truncate">{template.name}</h1>
              <div className="flex items-center gap-2 text-xs text-gray-400 mt-0.5">
                <span className="px-1.5 py-0.5 rounded bg-gray-100 text-gray-600 font-semibold text-[10px] uppercase">{template.documentTypeId}</span>
                <span>&middot;</span>
                <span>v{template.version}</span>
                <span>&middot;</span>
                <span>{template.createdBy}</span>
                <span>&middot;</span>
                <span>{new Date(template.createdAt).toLocaleDateString()}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Editor */}
      <div className="flex-1 flex flex-col min-w-0">
        {!template.fileStorageId ? (
          <div className="flex-1 flex flex-col items-center justify-center bg-gray-50 text-center p-8">
            <FileText size={48} className="text-gray-300 mb-4" />
            <p className="text-gray-500 text-sm">No file associated with this template.</p>
          </div>
        ) : editorError ? (
          <div className="flex-1 flex flex-col items-center justify-center bg-red-50/50 text-center p-8">
            <AlertCircle size={48} className="text-red-300 mb-4" />
            <p className="text-red-600 text-sm">{editorError}</p>
            <button
              className="mt-4 text-xs px-3 py-1.5 rounded-md border border-red-200 text-red-600 hover:bg-red-100"
              onClick={() => { setEditorError(null); scriptLoadedRef.current = false; setTemplate({ ...template }); }}
            >
              Retry
            </button>
          </div>
        ) : (
          <div className="flex-1 relative">
            {editorLoading && (
              <div className="absolute inset-0 z-10 flex flex-col items-center justify-center bg-white/90">
                <Loader2 size={28} className="animate-spin text-gray-400 mb-2" />
                <p className="text-sm text-gray-500">Loading template viewer...</p>
              </div>
            )}
            <div id="template-editor" style={{ width: '100%', height: '100%' }} />
          </div>
        )}
      </div>
    </div>
  );
}
