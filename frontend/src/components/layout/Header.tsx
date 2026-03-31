import { Menu, Search, Bot, FileText, ChevronRight, X } from 'lucide-react';
import { useState, useEffect, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';

interface SearchResult {
  id: string;
  title: string;
  excerpt: string;
  documentNumber?: string;
}

interface HeaderProps {
  onMenuToggle: () => void;
  headerColor?: string | null;
}

export default function Header({ onMenuToggle, headerColor }: HeaderProps) {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<SearchResult[]>([]);
  const [showDropdown, setShowDropdown] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const dropdownRef = useRef<HTMLDivElement>(null);

  const isDocumentView = location.pathname.startsWith('/document/');

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setShowDropdown(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  useEffect(() => {
    setResults([]);
    setShowDropdown(false);
  }, [query]);


  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (query.trim()) {
      navigate(`/ai/search?q=${encodeURIComponent(query.trim())}`);
      setShowDropdown(false);
    }
  };

  const goToResult = (id: string) => {
    navigate(`/documents/detail/${id}`);
    setShowDropdown(false);
    setQuery('');
  };

  return (
    <header
      className="flex items-center h-11 border-b border-brand-200 px-4 gap-4 sticky top-0 z-30 shadow-sm"
      style={{ backgroundColor: headerColor ?? '#ffffff' }}
    >
      <button onClick={onMenuToggle} className="p-1.5 rounded-md hover:bg-black/5 text-brand-500">
        <Menu size={18} />
      </button>

      {/* Global AI Search - Hidden in Document View */}
      {!isDocumentView && (
        <div className="flex-1 max-w-2xl mx-auto relative group">
        <form onSubmit={handleSearch} className="relative">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-brand-500 transition-colors" />
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onFocus={() => query.trim().length > 2 && setShowDropdown(true)}
            placeholder="Search documents, SOPs, and compliance data with AI..."
            className="w-full bg-gray-50 border border-gray-200 rounded-lg pl-9 pr-10 py-1.5 text-[13px] focus:outline-none focus:ring-2 focus:ring-brand-400/20 focus:border-brand-400 transition-all font-medium placeholder:font-normal"
          />
          <div className="absolute right-3 top-1/2 -translate-y-1/2 flex items-center gap-1.5">
            {query && (
              <button
                type="button"
                onClick={() => {
                  setQuery('');
                  setResults([]);
                  setShowDropdown(false);
                }}
                className="p-0.5 rounded-full hover:bg-gray-200 text-gray-400 hover:text-gray-600 transition-all mr-0.5"
                title="Clear search"
              >
                <X size={12} />
              </button>
            )}
              <span className="text-[10px] text-gray-400 bg-white border border-gray-200 rounded px-1.5 py-0.5 flex items-center gap-1">
                <Bot size={10} className="text-brand-500" /> AI
              </span>
          </div>
        </form>

        {/* Search Results Dropdown */}
        {showDropdown && results.length > 0 && (
          <div 
            ref={dropdownRef}
            className="absolute top-full left-0 right-0 mt-1 bg-white border border-gray-200 rounded-lg shadow-xl py-2 overflow-hidden animate-in fade-in slide-in-from-top-1 duration-200"
          >
            <div className="px-3 py-1.5">
              <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Top AI Matches</span>
            </div>
            <div className="max-h-[400px] overflow-y-auto">
              {results.map((res) => (
                <button
                  key={res.id}
                  onClick={() => goToResult(res.id)}
                  className="w-full text-left px-3 py-2.5 hover:bg-brand-50 group/item border-b border-gray-50 last:border-0 transition-colors"
                >
                  <div className="flex items-start gap-3">
                    <div className="p-1.5 bg-brand-100 rounded-md text-brand-600 mt-0.5 group-hover/item:bg-white transition-colors">
                      <FileText size={14} />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="text-[13px] font-semibold text-gray-900 group-hover/item:text-brand-700 truncate">
                          {res.title}
                        </span>
                        {res.documentNumber && (
                          <span className="text-[10px] font-mono text-gray-400 bg-gray-100 px-1 rounded">
                            {res.documentNumber}
                          </span>
                         )}
                      </div>
                      <p className="text-[11px] text-gray-500 line-clamp-1 mt-0.5">
                        {res.excerpt?.replace(/<mark>|<\/mark>/g, '') || 'View document details...'}
                      </p>
                    </div>
                    <ChevronRight size={12} className="text-gray-300 group-hover/item:text-brand-400 mt-2" />
                  </div>
                </button>
              ))}
            </div>
            {results.length > 0 && (
              <button 
                onClick={handleSearch}
                className="w-full text-center py-2 text-[11px] font-bold text-brand-600 hover:bg-brand-100/50 border-t border-gray-50 mt-1"
              >
                See all results for "{query}"
              </button>
            )}
            {results.length === 0 && (
              <div className="px-3 py-4 text-center text-[12px] text-gray-400">
                No exact AI matches found. Press Enter to search everywhere.
              </div>
            )}
          </div>
        )}
      </div>
      )}

      <div className="w-8 shrink-0" />
    </header>
  );
}
