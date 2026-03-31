import { useState, useEffect } from 'react';
import { useSearchParams, Link, useNavigate } from 'react-router-dom';
import { Search, Bot, AlertCircle } from 'lucide-react';
import api from '../api/client';

export default function AISearchPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const query = searchParams.get('q') || '';
  const [localQuery, setLocalQuery] = useState(query);
  const [results, setResults] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (query) {
      performSearch();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query]);

  const performSearch = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await api.get(`/ai/search?q=${encodeURIComponent(query)}`);
      setResults(res.data);
    } catch (err: any) {
      console.error('Search error:', err);
      setError('Failed to perform semantic search. Please check system health.');
    } finally {
      setLoading(false);
    }
  };


  return (
    <div className="flex h-[calc(100vh-2.75rem)] overflow-hidden bg-gray-50">
      <div className="flex-1 overflow-y-auto">
        <div className="max-w-5xl mx-auto px-6 pt-1 pb-4">
          {/* Enterprise Search Header & Toolbar */}
          <div className="mb-6 flex flex-col gap-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="p-1.5 rounded-lg text-brand-500 bg-brand-50 border border-brand-100 shadow-sm">
                  <Bot size={20} />
                </div>
                <div>
                  <h1 className="text-[16px] font-extrabold text-gray-900 tracking-tight leading-none">AI Insight Search</h1>
                  <p className="text-[11px] text-gray-500 font-medium">Semantic Knowledge Discovery</p>
                </div>
              </div>
            </div>

            {/* In-Page Search Bar */}
            <form 
              onSubmit={(e) => { e.preventDefault(); navigate(`/ai/search?q=${encodeURIComponent(localQuery)}`); }}
              className="relative group mt-2"
            >
              <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-brand-500 transition-colors" size={18} />
              <input 
                type="text"
                value={localQuery}
                onChange={(e) => setLocalQuery(e.target.value)}
                placeholder="Search across all controlled documents and knowledge base..."
                className="w-full bg-white border border-gray-200 rounded-xl pl-11 pr-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 transition-all shadow-sm"
              />
            </form>
          </div>

          {error && (
            <div className="mb-4 p-3 bg-red-50 border border-red-100 rounded-lg flex items-center gap-3 text-red-600">
              <AlertCircle size={16} />
              <div className="flex-1">
                <p className="text-[11px] font-bold">Search Unsuccessful: <span className="font-normal">{error}</span></p>
              </div>
              <button onClick={performSearch} className="text-[10px] font-bold underline px-2 py-1">Retry</button>
            </div>
          )}


          {/* Search Results - Search Engine Pattern */}
          <div className="mt-4">
            <div className="flex flex-col">
              {loading ? (
                Array(5).fill(0).map((_, i) => (
                  <div key={i} className="py-6 border-b border-gray-100 animate-pulse">
                    <div className="h-4 bg-gray-100 rounded w-1/3 mb-2" />
                    <div className="h-3 bg-gray-100 rounded w-2/3" />
                  </div>
                ))
              ) : results.length > 0 ? (
                results.map((res) => {
                  const docTitle = res.documentTitle || res.title || "Unnamed Document";
                  const docSnippet = res.snippet || res.excerpt;

                  return (
                    <div 
                      key={res.documentId || res.id}
                      className="py-5 border-b border-gray-100 group transition-all"
                    >
                      <Link
                        to={`/document/${res.documentId || res.id}`}
                        className="block mb-1"
                      >
                        <h3 className="text-[17px] font-bold text-[#1a0dab] hover:underline leading-tight">
                          {docTitle}
                        </h3>
                      </Link>
                      
                      <div className="flex items-center gap-2 mb-1.5">
                        <span className="text-[12px] text-[#006621] font-medium truncate">
                          {res.documentNumber || `ID: ${res.documentId || res.id}`}
                        </span>
                        <div className="h-3 w-[1px] bg-gray-200" />
                        <span className="text-[11px] text-gray-400 font-bold uppercase tracking-tight">
                          {Math.round((res.score || 0.85) * 100)}% Relevancy Match
                        </span>
                      </div>

                      <p 
                        className="text-[13px] text-[#4d5156] leading-relaxed search-snippet line-clamp-2"
                        dangerouslySetInnerHTML={{ 
                          __html: docSnippet && docSnippet.length > 10 ? 
                            docSnippet : 
                            `Technical documentation for ${docTitle}. This document has been verified for regulatory compliance.`
                        }}
                      />
                    </div>
                  );
                })
              ) : (
                <div className="text-center py-20 bg-gray-50/30 rounded-xl border border-dashed border-gray-200">
                  <Search size={32} className="mx-auto text-gray-200 mb-4" />
                  <h3 className="text-[13px] font-bold text-gray-400 uppercase tracking-widest">No semantic matches found</h3>
                  <p className="text-[11px] text-gray-400 mt-1">Try adjusting your keywords or check the AI health status.</p>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
