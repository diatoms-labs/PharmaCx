import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  ShieldCheck, 
  Cpu, 
  CheckCircle2, 
  ArrowRight 
} from 'lucide-react';
import { useAuth } from '../hooks/useAuth';
import { showErrorToast } from '../utils/errorHandler';

export default function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await login(username, password);
      navigate('/dashboard');
    } catch (err) {
      showErrorToast(err, 'Invalid username or password');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col md:flex-row bg-white font-sans">
      {/* Left side: Branding (Hidden on mobile) */}
      <div className="hidden md:flex md:w-5/12 bg-pharma-navy text-white p-16 flex-col justify-between relative overflow-hidden text-slate-50">
        {/* Decorative subtle gradient/pattern */}
        <div className="absolute top-0 right-0 w-64 h-64 bg-pharma-primary opacity-10 rounded-full -mr-32 -mt-32 blur-3xl"></div>
        <div className="absolute bottom-0 left-0 w-48 h-48 bg-pharma-accent opacity-5 rounded-full -ml-24 -mb-24 blur-2xl"></div>

        <div className="relative z-10">
          <div className="flex items-center gap-3 mb-16">
            <div className="w-10 h-10 rounded-lg bg-white flex items-center justify-center text-pharma-navy shadow-lg shadow-black/20">
              <span className="text-xl font-bold">P</span>
            </div>
            <div>
              <div className="text-lg font-bold tracking-tight text-white leading-none">Pharma<span className="text-pharma-accent">CX</span></div>
              <div className="text-[10px] text-pharma-accent uppercase tracking-widest mt-0.5 opacity-80 font-bold">Compliance Execution</div>
            </div>
          </div>

          <h1 className="text-4xl md:text-5xl font-extrabold leading-tight mb-6">
            The Compliance <br /><span className="text-pharma-accent">Platform.</span>
          </h1>
          <p className="text-lg text-pharma-tint/70 leading-relaxed max-w-sm mb-10">
            Authoritative, precise, and audit-ready. Manage every GxP document and training record in one validated system.
          </p>
        </div>

        <div className="relative z-10 grid grid-cols-1 gap-4 text-sm font-bold mt-16 opacity-90">
          <div className="flex items-center gap-2 text-pharma-tint">
            <div className="w-5 h-5 rounded-full bg-pharma-accent/20 flex items-center justify-center text-pharma-accent">
               <ShieldCheck size={12} strokeWidth={3} />
            </div>
            <span>21 CFR Part 11 Compliant</span>
          </div>
          <div className="flex items-center gap-2 text-pharma-tint">
            <div className="w-5 h-5 rounded-full bg-pharma-accent/20 flex items-center justify-center text-pharma-accent">
               <Cpu size={12} strokeWidth={3} />
            </div>
            <span>Local Secure AI (Helix AI)</span>
          </div>
          <div className="flex items-center gap-2 text-pharma-tint">
            <div className="w-5 h-5 rounded-full bg-pharma-accent/20 flex items-center justify-center text-pharma-accent">
               <CheckCircle2 size={12} strokeWidth={3} />
            </div>
            <span>GAMP 5 Validation Ready</span>
          </div>
        </div>
      </div>

      {/* Right side: Login Form */}
      <div className="flex-1 flex items-center justify-center p-8 bg-gray-50/30">
        <div className="w-full max-w-[400px]">
          {/* Mobile Logo */}
          <div className="md:hidden flex items-center justify-center gap-2 mb-10">
             <div className="w-10 h-10 rounded-lg bg-pharma-navy flex items-center justify-center text-white">
                <span className="text-xl font-bold">P</span>
             </div>
             <span className="text-xl font-bold text-pharma-navy">Pharma<span className="text-pharma-primary">CX</span></span>
          </div>

          <div className="mb-10">
            <h2 className="text-2xl font-bold text-gray-900 mb-2">Sign in to your portal</h2>
            <p className="text-sm text-gray-500 font-medium">Enter your credentials to manage compliance</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label className="block text-[11px] font-bold text-gray-400 uppercase tracking-[0.1em] mb-2">Username or Email</label>
              <input
                type="text"
                className="w-full px-4 py-3.5 bg-white border border-gray-200 rounded-xl focus:outline-none focus:ring-4 focus:ring-pharma-primary/5 focus:border-pharma-primary transition-all text-sm font-medium"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="email@example.com"
                required
              />
            </div>
            <div>
              <div className="flex items-center justify-between mb-2">
                <label className="block text-[11px] font-bold text-gray-400 uppercase tracking-[0.1em]">Password</label>
                <button type="button" className="text-xs font-bold text-pharma-primary hover:underline">Forgot password?</button>
              </div>
              <input
                type="password"
                className="w-full px-4 py-3.5 bg-white border border-gray-200 rounded-xl focus:outline-none focus:ring-4 focus:ring-pharma-primary/5 focus:border-pharma-primary transition-all text-sm font-medium"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;"
                required
              />
            </div>
            
            <button 
              type="submit" 
              className="w-full bg-pharma-navy text-white font-bold py-4 rounded-xl hover:bg-pharma-navy/95 active:scale-[0.98] transition-all flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed shadow-xl shadow-pharma-navy/20 text-sm"
              disabled={loading}
            >
              {loading ? (
                <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
              ) : (
                <>
                  <span>Sign In</span>
                  <ArrowRight size={18} />
                </>
              )}
            </button>
          </form>

        </div>
      </div>
    </div>
  );
}
