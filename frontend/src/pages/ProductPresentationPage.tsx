import {
   ShieldCheck,
   FileText,
   Users,
   Lock,
   Database,
   ArrowRight,
   CheckCircle2,
   Cpu,
   Bot,
   Globe,
   BarChart3,
   Server,
   Zap,
   Activity,
   UserCheck,
   Building2,
   Binary
} from 'lucide-react';
import { Link } from 'react-router-dom';

export default function ProductPresentationPage() {
   return (
      <div className="min-h-screen bg-white font-sans text-slate-900 overflow-x-hidden selection:bg-pharma-primary/20">
         {/* Navigation */}
         <nav className="fixed top-0 w-full z-50 bg-white/90 backdrop-blur-xl border-b border-slate-100 px-6 py-4">
            <div className="max-w-7xl mx-auto flex items-center justify-between">
               <div className="flex items-center gap-8">
                  <Link to="/" className="flex items-center gap-2 group">
                     <div className="w-9 h-9 rounded-xl bg-pharma-navy flex items-center justify-center text-white font-bold group-hover:rotate-6 transition-transform shadow-lg shadow-pharma-navy/20">P</div>
                     <span className="text-2xl font-black tracking-tight text-pharma-navy italic">Pharma<span className="text-pharma-primary not-italic">CX</span></span>
                  </Link>

                  <div className="hidden lg:flex items-center gap-6">
                     <a href="#solutions" className="text-sm font-bold text-slate-600 hover:text-pharma-primary transition-colors">Solutions</a>
                     <a href="#compliance" className="text-sm font-bold text-slate-600 hover:text-pharma-primary transition-colors">Compliance</a>
                     <a href="#architecture" className="text-sm font-bold text-slate-600 hover:text-pharma-primary transition-colors">Architecture</a>
                     <a href="#industries" className="text-sm font-bold text-slate-600 hover:text-pharma-primary transition-colors">Industries</a>
                  </div>
               </div>

               <div className="flex items-center gap-4">
                  <Link
                     to="/login"
                     className="text-sm font-bold text-pharma-navy hover:text-pharma-primary transition-all px-4 py-2 hover:bg-slate-50 rounded-xl"
                  >
                     Sign In
                  </Link>
               </div>
            </div>
         </nav>

         {/* Hero Section */}
         <section className="relative pt-40 pb-24 px-6 md:px-12 lg:px-24">
            <div className="absolute top-0 left-1/2 -translate-x-1/2 w-full max-w-7xl h-full -z-10">
               <div className="absolute top-20 right-0 w-96 h-96 bg-pharma-primary/10 rounded-full blur-3xl animate-pulse"></div>
               <div className="absolute bottom-40 left-0 w-72 h-72 bg-pharma-accent/10 rounded-full blur-3xl animate-pulse delay-1000"></div>
            </div>

            <div className="max-w-7xl mx-auto grid grid-cols-1 lg:grid-cols-2 gap-16 items-center">
               <div className="z-10 text-center lg:text-left">
                  <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-slate-900 text-white text-[10px] font-black uppercase tracking-[0.2em] mb-8 shadow-xl">
                     <span className="w-2 h-2 rounded-full bg-green-400 animate-ping"></span>
                     Validated GxP Workspace
                  </div>
                  <h1 className="text-6xl md:text-7xl font-black text-pharma-navy leading-[1.05] mb-8 tracking-tighter">
                     Compliance <br />
                     <span className="text-transparent bg-clip-text bg-gradient-to-r from-pharma-primary to-pharma-accent">Execution.</span>
                  </h1>
                  <p className="text-xl text-slate-500 mb-10 max-w-xl mx-auto lg:mx-0 leading-relaxed font-medium">
                     The only unified platform that automates document lifecycles, training matrices, and AI-driven insights within a secure 21 CFR Part 11 air-locked perimeter.
                  </p>
                  <div className="flex flex-wrap justify-center lg:justify-start gap-5">
                     <Link
                        to="/login"
                        className="bg-pharma-navy text-white px-10 py-5 rounded-2xl font-black flex items-center justify-center gap-3 hover:bg-pharma-navy/90 transition-all shadow-2xl shadow-pharma-navy/20 group text-lg whitespace-nowrap"
                     >
                        Get Started <ArrowRight size={22} className="group-hover:translate-x-1 transition-transform shrink-0" />
                     </Link>
                     <div className="hidden sm:flex items-center gap-4 px-6 py-5 rounded-2xl bg-white border border-slate-100 shadow-sm font-bold text-slate-400">
                        <ShieldCheck size={20} className="text-green-500" />
                        Validated for Life Sciences
                     </div>
                  </div>

                  <div className="mt-16 flex items-center justify-center lg:justify-start gap-8 opacity-50 grayscale transition-all hover:grayscale-0">
                     <div className="text-xs uppercase font-black tracking-widest text-slate-400">Trusted in GxP:</div>
                     <div className="flex gap-6 items-center text-sm font-black italic text-slate-500">
                        <span>BIO-LABS</span>
                        <span className="w-1.5 h-1.5 rounded-full bg-pharma-primary"></span>
                        <span>PHARMACORE</span>
                        <span className="w-1.5 h-1.5 rounded-full bg-pharma-primary"></span>
                        <span>MEDVANCE</span>
                     </div>
                  </div>
               </div>

               <div className="relative group perspective-1000">
                  <div className="absolute -inset-10 bg-gradient-to-tr from-pharma-primary/30 to-pharma-accent/30 rounded-3xl blur-[100px] opacity-20 group-hover:opacity-40 transition-opacity"></div>
                  <div className="relative overflow-hidden rounded-[2.5rem] shadow-[0_50px_100px_-20px_rgba(0,0,0,0.3)] border border-white/20 hover:scale-[1.02] transition-transform duration-700">
                     <img
                        src="/presentation-hero.png"
                        alt="PharmaCX Enterprise Suite"
                        className="w-full h-auto object-cover aspect-[4/3] brightness-105"
                     />
                     <div className="absolute inset-0 bg-gradient-to-t from-pharma-navy/50 to-transparent opacity-0 group-hover:opacity-100 transition-opacity flex items-end p-8">
                        <div className="text-white">
                           <div className="font-black text-lg mb-1 italic tracking-widest uppercase">Pharma-AI Interface</div>
                           <div className="text-xs font-bold text-pharma-tint/80">Validated locally hosted generative intelligence</div>
                        </div>
                     </div>
                  </div>
               </div>
            </div>
         </section>

         {/* Solutions Grid */}
         <section id="solutions" className="py-32 bg-slate-50/70 border-y border-slate-100 px-6">
            <div className="max-w-7xl mx-auto">
               <div className="text-center max-w-3xl mx-auto mb-20">
                  <h2 className="text-4xl md:text-5xl font-black text-pharma-navy mb-6 tracking-tight">The Unified Quality Stack</h2>
                  <p className="text-lg text-slate-500 font-medium">Replace disconnected spreadsheets and legacy suites with a single, validated source of truth.</p>
               </div>

               <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
                  {/* DMS */}
                  <div className="bg-white p-10 rounded-3xl border border-slate-100 shadow-sm hover:shadow-[0_20px_40px_-15px_rgba(0,0,0,0.05)] transition-all group relative overflow-hidden">
                     <div className="absolute top-0 right-0 w-32 h-32 bg-pharma-primary/5 rounded-full -mr-16 -mt-16 group-hover:scale-125 transition-transform"></div>
                     <div className="w-16 h-16 bg-pharma-primary/10 rounded-2xl flex items-center justify-center text-pharma-primary mb-8 group-hover:rotate-6 transition-transform">
                        <FileText size={32} />
                     </div>
                     <h3 className="text-2xl font-black text-pharma-navy mb-4 italic tracking-tight">Smart DMS</h3>
                     <p className="text-slate-500 mb-8 font-medium leading-relaxed">
                        Full-lifecycle document control from draft to obsolescence. Intelligent versioning, automated review cycles, and instant e-signatures.
                     </p>
                     <ul className="space-y-4 text-sm font-bold text-slate-700">
                        <li className="flex items-center gap-3">
                           <div className="w-6 h-6 rounded-full bg-green-100 flex items-center justify-center scale-75"><CheckCircle2 size={16} className="text-green-600" /></div>
                           Immutable Change History
                        </li>
                        <li className="flex items-center gap-3">
                           <div className="w-6 h-6 rounded-full bg-green-100 flex items-center justify-center scale-75"><CheckCircle2 size={16} className="text-green-600" /></div>
                           Part 11 E-Signatures
                        </li>
                        <li className="flex items-center gap-3">
                           <div className="w-6 h-6 rounded-full bg-green-100 flex items-center justify-center scale-75"><CheckCircle2 size={16} className="text-green-600" /></div>
                           Cross-site Collaboration
                        </li>
                     </ul>
                  </div>

                  {/* TMS */}
                  <div className="bg-white p-10 rounded-3xl border border-slate-100 shadow-sm hover:shadow-[0_20px_40px_-15px_rgba(0,0,0,0.05)] transition-all group relative overflow-hidden">
                     <div className="absolute top-0 right-0 w-32 h-32 bg-pharma-accent/5 rounded-full -mr-16 -mt-16 group-hover:scale-125 transition-transform"></div>
                     <div className="w-16 h-16 bg-pharma-accent/10 rounded-2xl flex items-center justify-center text-pharma-accent mb-8 group-hover:rotate-6 transition-transform">
                        <Users size={32} />
                     </div>
                     <h3 className="text-2xl font-black text-pharma-navy mb-4 italic tracking-tight">Precision TMS</h3>
                     <p className="text-slate-500 mb-8 font-medium leading-relaxed">
                        Stay audit-ready with a dynamic training matrix. Automatic retraining on document updates and instant qualification dashboards.
                     </p>
                     <ul className="space-y-4 text-sm font-bold text-slate-700">
                        <li className="flex items-center gap-3">
                           <div className="w-6 h-6 rounded-full bg-green-100 flex items-center justify-center scale-75"><CheckCircle2 size={16} className="text-green-600" /></div>
                           Role-based Curricula
                        </li>
                        <li className="flex items-center gap-3">
                           <div className="w-6 h-6 rounded-full bg-green-100 flex items-center justify-center scale-75"><CheckCircle2 size={16} className="text-green-600" /></div>
                           Automated Matrix Updates
                        </li>
                        <li className="flex items-center gap-3">
                           <div className="w-6 h-6 rounded-full bg-green-100 flex items-center justify-center scale-75"><CheckCircle2 size={16} className="text-green-600" /></div>
                           Manager Insight Portals
                        </li>
                     </ul>
                  </div>

                  {/* AI Search */}
                  <div className="bg-white p-10 rounded-3xl border border-slate-100 shadow-sm hover:shadow-[0_20px_40px_-15px_rgba(0,0,0,0.05)] transition-all group relative overflow-hidden">
                     <div className="absolute top-0 right-0 w-32 h-32 bg-brand-500/5 rounded-full -mr-16 -mt-16 group-hover:scale-125 transition-transform"></div>
                     <div className="w-16 h-16 bg-brand-500/10 rounded-2xl flex items-center justify-center text-brand-600 mb-8 group-hover:rotate-6 transition-transform">
                        <Bot size={32} />
                     </div>
                     <h3 className="text-2xl font-black text-pharma-navy mb-4 italic tracking-tight">Pharma-AI</h3>
                     <p className="text-slate-500 mb-8 font-medium leading-relaxed">
                        Secure, local enterprise search. Query your proprietary compliance data using natural language without data leaving your premise.
                     </p>
                     <ul className="space-y-4 text-sm font-bold text-slate-700">
                        <li className="flex items-center gap-3">
                           <div className="w-6 h-6 rounded-full bg-green-100 flex items-center justify-center scale-75"><CheckCircle2 size={16} className="text-green-600" /></div>
                           Privacy-First Local RAG
                        </li>
                        <li className="flex items-center gap-3">
                           <div className="w-6 h-6 rounded-full bg-green-100 flex items-center justify-center scale-75"><CheckCircle2 size={16} className="text-green-600" /></div>
                           Natural Language Analytics
                        </li>
                        <li className="flex items-center gap-3">
                           <div className="w-6 h-6 rounded-full bg-green-100 flex items-center justify-center scale-75"><CheckCircle2 size={16} className="text-green-600" /></div>
                           GAMP 5 Evaluated Engine
                        </li>
                     </ul>
                  </div>
               </div>
            </div>
         </section>

         {/* Compliance Center */}
         <section id="compliance" className="py-24 px-6 bg-pharma-navy relative overflow-hidden">
            <div className="absolute top-0 right-0 w-[50%] h-full bg-pharma-primary/10 -skew-x-12 translate-x-1/2"></div>
            <div className="max-w-7xl mx-auto grid grid-cols-1 lg:grid-cols-2 gap-16 items-center">
               <div className="z-10">
                  <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/10 text-white text-[10px] font-black uppercase tracking-widest border border-white/20 mb-8">
                     <Globe size={14} /> Global Standards
                  </div>
                  <h2 className="text-4xl md:text-5xl font-black text-white mb-8 tracking-tighter">Built for <span className="text-pharma-accent italic">Zero-Gap</span> Regulatory Compliance.</h2>
                  <p className="text-xl text-slate-300 mb-12 font-medium leading-relaxed">
                     PharmaCX is engineered to meet and exceed the most stringent quality standards across international pharmaceutical territories.
                  </p>

                  <div className="grid grid-cols-2 gap-6">
                     <div className="p-6 rounded-2xl bg-white/5 border border-white/10">
                        <div className="text-2xl font-black text-white mb-2 italic tracking-widest">PART 11</div>
                        <div className="text-xs text-slate-400 font-bold uppercase tracking-tight">US FDA 21 CFR Part 11</div>
                     </div>
                     <div className="p-6 rounded-2xl bg-white/5 border border-white/10">
                        <div className="text-2xl font-black text-white mb-2 italic tracking-widest">ANNEX 11</div>
                        <div className="text-xs text-slate-400 font-bold uppercase tracking-tight">EudraLex Annex 11</div>
                     </div>
                     <div className="p-6 rounded-2xl bg-white/5 border border-white/10">
                        <div className="text-2xl font-black text-white mb-2 italic tracking-widest">GAMP 5</div>
                        <div className="text-xs text-slate-400 font-bold uppercase tracking-tight">Risk-Based Validation</div>
                     </div>
                     <div className="p-6 rounded-2xl bg-white/5 border border-white/10">
                        <div className="text-2xl font-black text-white mb-2 italic tracking-widest">ALCOA+</div>
                        <div className="text-xs text-slate-400 font-bold uppercase tracking-tight">Data Integrity Framework</div>
                     </div>
                  </div>
               </div>

               <div className="bg-white rounded-[2rem] p-10 md:p-12 shadow-2xl relative z-20">
                  <h3 className="text-2xl font-black text-pharma-navy mb-8 tracking-tight">The Data Integrity Shield</h3>
                  <div className="space-y-8">
                     <div className="flex gap-5">
                        <div className="w-12 h-12 rounded-xl bg-orange-100 flex items-center justify-center text-orange-600 shrink-0"><Lock size={24} /></div>
                        <div>
                           <h4 className="text-lg font-black text-pharma-navy mb-2 italic tracking-tight">Immutable Audit Trails</h4>
                           <p className="text-sm text-slate-500 font-medium leading-relaxed italic">Timestamped, non-repudiable records of every system interaction, document revision, and training signature.</p>
                        </div>
                     </div>
                     <div className="flex gap-5">
                        <div className="w-12 h-12 rounded-xl bg-blue-100 flex items-center justify-center text-blue-600 shrink-0"><UserCheck size={24} /></div>
                        <div>
                           <h4 className="text-lg font-black text-pharma-navy mb-2 italic tracking-tight">GRANULAR RBAC</h4>
                           <p className="text-sm text-slate-500 font-medium leading-relaxed italic">Multi-layered Role-Based Access Control ensuring users only interact with data matching their qualification profile.</p>
                        </div>
                     </div>
                     <div className="flex gap-5">
                        <div className="w-12 h-12 rounded-xl bg-green-100 flex items-center justify-center text-green-600 shrink-0"><Activity size={24} /></div>
                        <div>
                           <h4 className="text-lg font-black text-pharma-navy mb-2 italic tracking-tight">V8 VALIDATED STATE</h4>
                           <p className="text-sm text-slate-500 font-medium leading-relaxed italic">Continuous monitoring of the application's validated state, ensuring no configuration drift impacts compliance.</p>
                        </div>
                     </div>
                  </div>
               </div>
            </div>
         </section>

         {/* Architecture Section */}
         <section id="architecture" className="py-32 px-6">
            <div className="max-w-7xl mx-auto">
               <div className="text-center mb-20">
                  <div className="text-[10px] font-black uppercase tracking-[0.5em] text-pharma-primary mb-4">Under the hood</div>
                  <h2 className="text-4xl md:text-5xl font-black text-pharma-navy tracking-tight">Secure AI Architecture. <br /> <span className="italic">Air-Locked Intelligence.</span></h2>
               </div>

               <div className="grid grid-cols-1 lg:grid-cols-5 gap-8 items-center">
                  <div className="lg:col-span-2 space-y-8">
                     <div className="p-8 rounded-3xl bg-slate-50 border border-slate-100 hover:-translate-y-1 transition-transform group">
                        <div className="flex items-center gap-4 mb-4">
                           <div className="w-12 h-12 rounded-xl bg-pharma-navy flex items-center justify-center text-white"><Server size={22} /></div>
                           <h4 className="text-xl font-black text-pharma-navy italic tracking-tight">On-Premise Inference</h4>
                        </div>
                        <p className="text-sm text-slate-500 font-medium leading-relaxed">We don't call external APIs. Your private Pharma-AI model runs on your servers, inside your firewall. No data leakage, ever.</p>
                     </div>
                     <div className="p-8 rounded-3xl bg-slate-50 border border-slate-100 hover:-translate-y-1 transition-transform group">
                        <div className="flex items-center gap-4 mb-4">
                           <div className="w-12 h-12 rounded-xl bg-pharma-accent flex items-center justify-center text-white"><Binary size={22} /></div>
                           <h4 className="text-xl font-black text-pharma-navy italic tracking-tight">Direct RAG Integration</h4>
                        </div>
                        <p className="text-sm text-slate-500 font-medium leading-relaxed">Retrieval Augmented Generation pulls from your DMS document store in real-time, providing cited answers from your specific SOPs.</p>
                     </div>
                  </div>

                  <div className="lg:col-span-3">
                     <div className="bg-pharma-navy rounded-[3rem] p-4 p-8 md:p-12 relative overflow-hidden group">
                        <div className="absolute inset-0 bg-gradient-to-br from-pharma-primary/20 via-transparent to-pharma-accent/20"></div>

                        <div className="relative z-10 space-y-12">
                           <div className="flex flex-col items-center justify-center text-center py-12">
                              <div className="relative mb-10">
                                 <div className="absolute inset-0 bg-white/20 blur-3xl animate-pulse scale-150"></div>
                                 <div className="w-24 h-24 rounded-full bg-gradient-to-tr from-white to-slate-200 flex items-center justify-center text-pharma-navy shadow-2xl relative border-4 border-white/20">
                                    <Bot size={48} />
                                 </div>
                              </div>
                              <h3 className="text-3xl font-black text-white italic tracking-tighter mb-4">PHARMA-AI CORE</h3>
                              <div className="px-5 py-1.5 rounded-full bg-pharma-primary text-[10px] font-black uppercase tracking-widest text-white mb-10">Active Security Shield Engaged</div>

                              <div className="grid grid-cols-3 gap-8 w-full max-w-md mx-auto relative">
                                 {/* Connector Lines (SVG) */}
                                 <div className="absolute inset-0 top-1/2 -translate-y-1/2 flex items-center justify-between pointer-events-none px-12">
                                    <div className="h-[2px] w-full bg-white/10"></div>
                                 </div>

                                 <div className="relative z-10 flex flex-col items-center gap-2">
                                    <div className="w-12 h-12 rounded-xl bg-white/5 border border-white/20 flex items-center justify-center text-pharma-accent"><Database size={20} /></div>
                                    <span className="text-[10px] font-black text-white/40 uppercase tracking-tighter">Local Vault</span>
                                 </div>
                                 <div className="relative z-10 flex flex-col items-center gap-2 scale-125">
                                    <div className="w-16 h-16 rounded-2xl bg-pharma-primary flex items-center justify-center text-white shadow-xl shadow-pharma-primary/50"><Cpu size={24} /></div>
                                    <span className="text-[10px] font-black text-white uppercase tracking-tighter">Inference</span>
                                 </div>
                                 <div className="relative z-10 flex flex-col items-center gap-2">
                                    <div className="w-12 h-12 rounded-xl bg-white/5 border border-white/20 flex items-center justify-center text-green-400"><ShieldCheck size={20} /></div>
                                    <span className="text-[10px] font-black text-white/40 uppercase tracking-tighter">Validation</span>
                                 </div>
                              </div>
                           </div>

                           <div className="flex flex-wrap gap-3 justify-center">
                              {['Phi-3 Mini', 'Llama 3', 'Mistral'].map(m => (
                                 <span key={m} className="px-4 py-1.5 rounded-lg bg-white/5 border border-white/10 text-xs font-black tracking-widest text-pharma-tint italic">{m}</span>
                              ))}
                           </div>
                        </div>
                     </div>
                  </div>
               </div>
            </div>
         </section>

         {/* Industries Section */}
         <section id="industries" className="py-24 px-6 bg-slate-50/50">
            <div className="max-w-7xl mx-auto">
               <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
                  <div className="lg:col-span-1 py-12">
                     <h2 className="text-3xl font-black text-pharma-navy italic tracking-tight mb-4">Vertical Excellence.</h2>
                     <p className="text-slate-500 font-medium">Tailored compliance for the future of life sciences.</p>
                  </div>

                  <div className="lg:col-span-3 grid grid-cols-1 md:grid-cols-3 gap-8">
                     <div className="bg-white p-8 rounded-3xl border border-slate-100 hover:border-pharma-primary transition-colors group">
                        <div className="w-12 h-12 rounded-xl bg-pharma-primary/10 text-pharma-primary flex items-center justify-center mb-6 group-hover:scale-110 transition-transform"><Building2 size={24} /></div>
                        <h4 className="text-xl font-black text-pharma-navy italic tracking-tight mb-4">Pharmaceuticals</h4>
                        <p className="text-sm text-slate-500 font-medium leading-relaxed italic">End-to-end management for API and drug product manufacturing workflows.</p>
                     </div>
                     <div className="bg-white p-8 rounded-3xl border border-slate-100 hover:border-pharma-accent transition-colors group">
                        <div className="w-12 h-12 rounded-xl bg-pharma-accent/10 text-pharma-accent flex items-center justify-center mb-6 group-hover:scale-110 transition-transform"><Zap size={24} /></div>
                        <h4 className="text-xl font-black text-pharma-navy italic tracking-tight mb-4">Biotechnology</h4>
                        <p className="text-sm text-slate-500 font-medium leading-relaxed italic">Scalable QMS for fast-growing clinical research and bio-analytical labs.</p>
                     </div>
                     <div className="bg-white p-8 rounded-3xl border border-slate-100 hover:border-brand-500 transition-colors group">
                        <div className="w-12 h-12 rounded-xl bg-brand-500/10 text-brand-600 flex items-center justify-center mb-6 group-hover:scale-110 transition-transform"><BarChart3 size={24} /></div>
                        <h4 className="text-xl font-black text-pharma-navy italic tracking-tight mb-4">Medical Device</h4>
                        <p className="text-sm text-slate-500 font-medium leading-relaxed italic">Risk-based design control and post-market surveillance integration.</p>
                     </div>
                  </div>
               </div>
            </div>
         </section>

         {/* CTA Section */}
         <section className="py-32 px-6 text-center">
            <div className="absolute top-1/2 left-0 w-full h-px bg-slate-100 -z-10"></div>
            <div className="max-w-4xl mx-auto bg-white p-12 md:p-20 rounded-[4rem] shadow-[0_50px_100px_-20px_rgba(0,0,0,0.1)] border border-slate-100 group relative overflow-hidden">
               <div className="absolute -bottom-20 -right-20 w-80 h-80 bg-pharma-primary/5 rounded-full blur-3xl group-hover:scale-150 transition-transform duration-1000"></div>

               <h2 className="text-5xl md:text-6xl font-black text-pharma-navy mb-8 tracking-tighter">Ready to <span className="italic text-pharma-primary">Execute</span>?</h2>
               <p className="text-xl text-slate-500 mb-12 max-w-xl mx-auto font-medium">
                  Join the quality revolution. Digitize your compliance operations today with PharmaCX.
               </p>
               <div className="flex flex-wrap justify-center gap-6">
                  <Link
                     to="/login"
                     className="bg-pharma-navy text-white px-10 py-5 rounded-2xl font-black flex items-center justify-center gap-3 hover:bg-pharma-navy/90 transition-all shadow-2xl shadow-pharma-navy/20 group text-lg whitespace-nowrap"
                  >
                     Get Started <ArrowRight size={22} className="group-hover:translate-x-1 transition-transform shrink-0" />
                  </Link>
               </div>

               <div className="mt-12 flex items-center justify-center gap-3 text-slate-400 font-bold text-sm tracking-widest uppercase italic">
                  <ShieldCheck size={18} className="text-green-500" /> Audit Ready. Validated. Air-Locked.
               </div>
            </div>
         </section>

         {/* Footer */}
         <footer className="py-20 bg-slate-900 border-t border-white/5 px-6">
            <div className="max-w-7xl mx-auto grid grid-cols-1 md:grid-cols-4 gap-12">
               <div className="col-span-1 md:col-span-1">
                  <Link to="/" className="flex items-center gap-2 mb-8 group grayscale opacity-70 hover:grayscale-0 hover:opacity-100">
                     <div className="w-8 h-8 rounded-lg bg-white flex items-center justify-center text-pharma-navy font-bold">P</div>
                     <span className="text-xl font-black tracking-tight text-white italic">Pharma<span className="text-pharma-accent not-italic">CX</span></span>
                  </Link>
                  <p className="text-sm text-slate-500 leading-relaxed font-bold italic tracking-tighter">
                     Leading the digital transformation of pharmaceutical quality management through secure, local-first artificial intelligence.
                  </p>
               </div>

               <div className="col-span-1">
                  <h5 className="text-white font-black uppercase text-xs tracking-widest mb-6 italic text-pharma-accent">Capabilities</h5>
                  <ul className="space-y-4 text-sm font-bold text-slate-400">
                     <li><a href="#" className="hover:text-white transition-colors italic">Smart DMS</a></li>
                     <li><a href="#" className="hover:text-white transition-colors italic">Precision TMS</a></li>
                     <li><a href="#" className="hover:text-white transition-colors italic">Pharma-AI Search</a></li>
                     <li><a href="#" className="hover:text-white transition-colors italic">Validated Workflows</a></li>
                  </ul>
               </div>

               <div className="col-span-1">
                  <h5 className="text-white font-black uppercase text-xs tracking-widest mb-6 italic text-pharma-accent">Compliance</h5>
                  <ul className="space-y-4 text-sm font-bold text-slate-400">
                     <li><a href="#" className="hover:text-white transition-colors italic">21 CFR Part 11</a></li>
                     <li><a href="#" className="hover:text-white transition-colors italic">EudraLex Annex 11</a></li>
                     <li><a href="#" className="hover:text-white transition-colors italic">GAMP 5 Guidelines</a></li>
                     <li><a href="#" className="hover:text-white transition-colors italic">Data Integrity</a></li>
                  </ul>
               </div>

               <div className="col-span-1">
                  <h5 className="text-white font-black uppercase text-xs tracking-widest mb-6 italic text-pharma-accent">Sector Support</h5>
                  <ul className="space-y-4 text-sm font-bold text-slate-400">
                     <li><a href="#" className="hover:text-white transition-colors italic">Bio-Pharma</a></li>
                     <li><a href="#" className="hover:text-white transition-colors italic">Clinical Research</a></li>
                     <li><a href="#" className="hover:text-white transition-colors italic">Manufacturing (API)</a></li>
                     <li><a href="#" className="hover:text-white transition-colors italic">Quality Laboratories</a></li>
                  </ul>
               </div>
            </div>

            <div className="max-w-7xl mx-auto mt-20 pt-8 border-t border-white/5 flex flex-col md:flex-row justify-between items-center gap-6">
               <p className="text-xs text-slate-600 font-bold uppercase tracking-widest italic">© 2026 PharmaCX Compliance Execution Platform. Validated Release v1.4.2</p>
               <div className="flex gap-8 text-[10px] text-slate-600 font-black uppercase tracking-tighter italic">
                  <a href="#" className="hover:text-pharma-accent">Data Privacy Policy</a>
                  <a href="#" className="hover:text-pharma-accent">Cloud Security Standard</a>
                  <a href="#" className="hover:text-pharma-accent">EULA</a>
               </div>
            </div>
         </footer>
      </div>
   );
}
