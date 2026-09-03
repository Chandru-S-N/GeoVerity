import React, { useState } from 'react';
import { Header } from './components/Header';
import { VerificationPortal } from './components/VerificationPortal';
import { ThirdPartyDemo } from './components/ThirdPartyDemo';
import { AdminConsole } from './components/AdminConsole';
import { SimulatorLab } from './components/SimulatorLab';
import { DocumentationView } from './components/DocumentationView';

export function App() {
  const [activeTab, setActiveTab] = useState<'verify' | 'thirdparty' | 'simulator' | 'admin' | 'docs'>('verify');

  return (
    <div className="min-h-screen bg-[#F8FAFC] flex flex-col selection:bg-indigo-500 selection:text-white">
      {/* Navigation Header */}
      <Header activeTab={activeTab} setActiveTab={setActiveTab} />

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 w-full">
        {activeTab === 'verify' && <VerificationPortal />}
        {activeTab === 'thirdparty' && <ThirdPartyDemo />}
        {activeTab === 'simulator' && <SimulatorLab />}
        {activeTab === 'admin' && <AdminConsole />}
        {activeTab === 'docs' && <DocumentationView />}
      </main>

      {/* Global Footer */}
      <footer className="bg-white border-t border-slate-200 py-8 text-center text-xs text-slate-500 space-y-2">
        <div className="flex justify-center items-center space-x-6 text-slate-600 font-medium">
          <button onClick={() => setActiveTab('verify')} className="hover:text-indigo-600">Verification Portal</button>
          <button onClick={() => setActiveTab('thirdparty')} className="hover:text-indigo-600">Third-Party API Hub</button>
          <button onClick={() => setActiveTab('simulator')} className="hover:text-indigo-600">Attack Lab</button>
          <button onClick={() => setActiveTab('admin')} className="hover:text-indigo-600">Admin Console</button>
          <button onClick={() => setActiveTab('docs')} className="hover:text-indigo-600">Security Architecture</button>
        </div>
        <p>© 2026 GeoVerity Platform. Built with Spring Boot 3, Java 21, Kotlin Jetpack Compose &amp; React.</p>
        <p className="text-[11px] text-slate-400">Never claims "100% secure". Cryptographically authenticated via server-authority ECDSA P-256 and composite SHA-256.</p>
      </footer>
    </div>
  );
}

export default App;
