import React from 'react';
import { ShieldCheck, Key, BookOpen, Sparkles, Network } from 'lucide-react';

interface HeaderProps {
  activeTab: 'verify' | 'thirdparty' | 'simulator' | 'admin' | 'docs';
  setActiveTab: (tab: 'verify' | 'thirdparty' | 'simulator' | 'admin' | 'docs') => void;
}

export const Header: React.FC<HeaderProps> = ({ activeTab, setActiveTab }) => {
  return (
    <header className="sticky top-0 z-50 bg-white/90 backdrop-blur-md border-b border-slate-200 shadow-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          
          {/* Logo & Brand */}
          <div className="flex items-center space-x-3 cursor-pointer" onClick={() => setActiveTab('verify')}>
            <div className="h-10 w-10 rounded-xl bg-gradient-to-tr from-indigo-600 via-indigo-500 to-purple-500 flex items-center justify-center text-white shadow-md shadow-indigo-500/20">
              <ShieldCheck className="h-6 w-6" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <span className="font-['Outfit'] font-bold text-xl text-slate-900 tracking-tight">GeoVerity</span>
                <span className="px-2 py-0.5 text-[10px] font-semibold tracking-wide bg-indigo-50 text-indigo-700 border border-indigo-200/60 rounded-full">
                  ECDSA P-256
                </span>
              </div>
              <p className="text-xs text-slate-500 font-medium">Secure Geolocation &amp; Evidence Authentication</p>
            </div>
          </div>

          {/* Navigation Tabs */}
          <nav className="flex space-x-1 sm:space-x-1.5 bg-slate-100/80 p-1.5 rounded-xl border border-slate-200/80">
            <button
              onClick={() => setActiveTab('verify')}
              className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all duration-200 ${
                activeTab === 'verify'
                  ? 'bg-white text-indigo-600 shadow-sm border border-slate-200'
                  : 'text-slate-600 hover:text-slate-900 hover:bg-white/60'
              }`}
            >
              <ShieldCheck className="h-4 w-4 text-indigo-500" />
              <span>Verify Evidence</span>
            </button>

            <button
              onClick={() => setActiveTab('thirdparty')}
              className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all duration-200 ${
                activeTab === 'thirdparty'
                  ? 'bg-white text-emerald-600 shadow-sm border border-slate-200'
                  : 'text-slate-600 hover:text-slate-900 hover:bg-white/60'
              }`}
            >
              <Network className="h-4 w-4 text-emerald-500" />
              <span>Third-Party API Hub</span>
            </button>

            <button
              onClick={() => setActiveTab('simulator')}
              className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all duration-200 ${
                activeTab === 'simulator'
                  ? 'bg-white text-purple-600 shadow-sm border border-slate-200'
                  : 'text-slate-600 hover:text-slate-900 hover:bg-white/60'
              }`}
            >
              <Sparkles className="h-4 w-4 text-purple-500" />
              <span>Capture &amp; Attack Lab</span>
            </button>

            <button
              onClick={() => setActiveTab('admin')}
              className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all duration-200 ${
                activeTab === 'admin'
                  ? 'bg-white text-amber-600 shadow-sm border border-slate-200'
                  : 'text-slate-600 hover:text-slate-900 hover:bg-white/60'
              }`}
            >
              <Key className="h-4 w-4 text-amber-500" />
              <span>Admin &amp; Keys</span>
            </button>

            <button
              onClick={() => setActiveTab('docs')}
              className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all duration-200 ${
                activeTab === 'docs'
                  ? 'bg-white text-indigo-600 shadow-sm border border-slate-200'
                  : 'text-slate-600 hover:text-slate-900 hover:bg-white/60'
              }`}
            >
              <BookOpen className="h-4 w-4 text-indigo-500" />
              <span>Specs</span>
            </button>
          </nav>

          {/* Right Status */}
          <div className="hidden md:flex items-center space-x-4">
            <div className="flex items-center space-x-2 bg-emerald-50 border border-emerald-200/80 px-3 py-1 rounded-full text-xs font-medium text-emerald-700">
              <span className="h-2 w-2 rounded-full bg-emerald-500 animate-pulse"></span>
              <span>Server Authority Active</span>
            </div>
          </div>

        </div>
      </div>
    </header>
  );
};
