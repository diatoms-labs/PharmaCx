import { useEffect, useRef, useState, useCallback } from 'react';
import { Loader2, AlertCircle, FileText } from 'lucide-react';
import { ControlledDocument, EditorConfig, DocumentDetailResponse } from '../../types';
import api from '../../api/client';

interface OnlyOfficeEditorProps {
  id: string; // Document ID
  doc: ControlledDocument;
  onInit: (instance: any) => void;
  editorMountKey: number;
}

/**
 * OnlyOfficeEditor - Provides robust integration with OnlyOffice Document Server.
 * Correctly handles React component lifecycle to avoid "insertBefore on Node" errors
 * by ensuring graceful disposal and re-initialization of the external editor framework.
 */
export default function OnlyOfficeEditor({ id, doc, onInit, editorMountKey }: OnlyOfficeEditorProps) {
  const [editorLoading, setEditorLoading] = useState(false);
  const [editorError, setEditorError] = useState<string | null>(null);
  const editorInstanceRef = useRef<any>(null);
  const editorContainerRef = useRef<HTMLDivElement>(null);
  const configKeyRef = useRef<string | null>(null);

  const destroyEditor = useCallback(() => {
    if (editorInstanceRef.current) {
      try {
        editorInstanceRef.current.destroyEditor();
        console.log('OnlyOffice Editor destroyed successfully');
      } catch (err) {
        console.warn('Failed to destroy editor instance gracefully', err);
      }
      editorInstanceRef.current = null;
    }
    // Deep-clean the container explicitly
    if (editorContainerRef.current) {
      editorContainerRef.current.innerHTML = '';
    }
  }, []);

  const initEditor = useCallback((config: EditorConfig) => {
    // Small delay to ensure DOM is fully ready after React reconciliation
    setTimeout(() => {
      try {
        const DocsAPI = (window as any).DocsAPI;
        if (!DocsAPI) {
          setEditorError('ONLYOFFICE API is not reachable.');
          setEditorLoading(false);
          return;
        }

        destroyEditor();

        // Ensure we have a div inside the container for OnlyOffice to hook into
        if (!editorContainerRef.current) return;
        const innerDiv = document.createElement('div');
        innerDiv.id = 'onlyoffice-inner-editor';
        editorContainerRef.current.appendChild(innerDiv);

        const editorCfg = {
          ...config.config,
          height: '100%',
          width: '100%',
          events: {
            onAppReady: () => {
              console.log('OnlyOffice App Ready');
              setEditorLoading(false);
            },
            onDocumentReady: () => {
              console.log('OnlyOffice Document Ready');
            },
            onError: (event: any) => {
              console.error('OnlyOffice Error:', event);
              setEditorError(`Editor Error: ${event.data || 'Unknown error'}`);
              setEditorLoading(false);
            },
            ...config.config.events as any
          }
        };

        editorInstanceRef.current = new DocsAPI.DocEditor('onlyoffice-inner-editor', editorCfg);
        onInit(editorInstanceRef.current);
      } catch (err) {
        console.error('Failed to init OnlyOffice instance', err);
        setEditorError('The editor failed to initialize. Please try refreshing the page.');
        setEditorLoading(false);
      }
    }, 50); // Minimal delay for DOM stability
  }, [destroyEditor, onInit]);

  useEffect(() => {
    if (!doc.documentFileId) return;

    // Use a composite key to detect changes requiring a full reload (status, version, mount key)
    const currentConfigKey = `${doc.documentFileId}_${doc.status}_${doc.version}_${editorMountKey}`;
    if (configKeyRef.current === currentConfigKey && editorInstanceRef.current) return;
    configKeyRef.current = currentConfigKey;

    let isCancelled = false;

    const loadConfigAndInit = async () => {
      setEditorLoading(true);
      setEditorError(null);
      destroyEditor();

      try {
        // Fetch config from backend — ensures per-user per-stage permissions are applied
        const res = await api.get<DocumentDetailResponse>(`/documents/${id}`);
        if (isCancelled) return;
        
        const config = res.data.editorConfig;
        if (!config || !config.documentServerUrl) {
          setEditorError('Document editing is currently unavailable for this document state.');
          setEditorLoading(false);
          return;
        }

        const apiScriptUrl = `${config.documentServerUrl}/web-apps/apps/api/documents/api.js`;
        
        // Use external script loader if needed
        if ((window as any).DocsAPI) {
          initEditor(config);
          return;
        }

        const scriptTag = document.createElement('script');
        scriptTag.src = apiScriptUrl;
        scriptTag.async = true;
        scriptTag.onload = () => { if (!isCancelled) initEditor(config); };
        scriptTag.onerror = () => { if (!isCancelled) { setEditorError('Failed to load the ONLYOFFICE server API.'); setEditorLoading(false); } };
        document.head.appendChild(scriptTag);

      } catch (err) {
        if (!isCancelled) {
          setEditorError('Failed to connect to the document server.');
          setEditorLoading(false);
        }
      }
    };

    loadConfigAndInit();

    return () => { isCancelled = true; };
  }, [id, doc.id, doc.documentFileId, doc.status, doc.version, editorMountKey, initEditor, destroyEditor]);

  // Mandatory clean-up on component destruction
  useEffect(() => {
    return () => destroyEditor();
  }, [destroyEditor]);

  if (!doc.documentFileId) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center bg-gray-50 text-center p-12">
        <FileText size={56} className="text-gray-300 mb-6 drop-shadow-sm" />
        <h2 className="text-lg font-bold text-gray-700 mb-2">Document Viewmarket Pending</h2>
        <p className="text-sm text-gray-400 max-w-sm mx-auto leading-relaxed">
          The editor will be enabled once the QA team has selected the appropriate template for this request.
        </p>
      </div>
    );
  }

  return (
    <div className="flex-1 relative bg-white overflow-hidden shadow-inner flex flex-col">
       {/* Loader Overlay */}
       {editorLoading && (
         <div className="absolute inset-0 flex flex-col items-center justify-center bg-white/95 backdrop-blur-[2px] z-50">
            <div className="relative">
              <Loader2 className="animate-spin h-12 w-12 text-brand-600 mb-3" />
              <div className="absolute inset-0 bg-brand-500/10 blur-xl rounded-full" />
            </div>
            <p className="text-[11px] font-black text-brand-700 uppercase tracking-widest animate-pulse">Syncing Secure Instance...</p>
         </div>
       )}
       
       {/* Error Overlay */}
       {editorError && (
         <div className="absolute inset-0 flex flex-col items-center justify-center bg-red-50/50 backdrop-blur-[4px] z-50 p-8 text-center animate-in fade-in duration-500">
            <div className="bg-white p-8 rounded-3xl shadow-2xl border border-red-100 max-w-md">
              <AlertCircle className="h-16 w-16 text-red-500 mb-6 mx-auto opacity-80" />
              <h3 className="text-xl font-black text-gray-900 mb-3 uppercase tracking-tighter">Instance Lockdown</h3>
              <p className="text-sm text-gray-500 mb-8 leading-relaxed font-medium">{editorError}</p>
              <button 
                onClick={() => { configKeyRef.current = null; window.location.reload(); }}
                className="w-full py-4 bg-gray-900 text-white rounded-2xl font-black uppercase text-xs tracking-widest hover:bg-black transition-all active:scale-95 shadow-lg shadow-gray-200"
              >
                Reset Connection
              </button>
            </div>
         </div>
       )}

       {/* Editor Container - The inner div will be created dynamically to prevent React conflict */}
       <div ref={editorContainerRef} id="onlyoffice-editor-wrapper" className="w-full h-full flex-1" />
    </div>
  );
}
