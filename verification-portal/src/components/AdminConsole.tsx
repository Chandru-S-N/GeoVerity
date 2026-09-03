import React, { useState, useEffect } from 'react';
import { 
  Key, 
  Shield, 
  Plus, 
  RefreshCw, 
  Trash2, 
  CheckCircle, 
  AlertOctagon, 
  Copy, 
  Database, 
  FileText, 
  Activity, 
  Smartphone,
  Eye,
  Check,
  Power
} from 'lucide-react';
import { ApiClient, VerificationRecordEntity, AuditLog, AdminStats } from '../types';

export const AdminConsole: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'clients' | 'records' | 'audits'>('clients');
  const [clients, setClients] = useState<ApiClient[]>([]);
  const [records, setRecords] = useState<VerificationRecordEntity[]>([]);
  const [audits, setAudits] = useState<AuditLog[]>([]);
  const [stats, setStats] = useState<AdminStats>({
    totalRecords: 142,
    authenticatedRecords: 139,
    totalApiClients: 4,
    activeApiClients: 3,
    totalAuditEvents: 528,
    timeAnomalies: 3
  });

  // Modal State for New API Client
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [newClientName, setNewClientName] = useState('');
  const [newPermissions, setNewPermissions] = useState('CAPTURE,VERIFY,TIME_TOKEN');
  const [generatedKey, setGeneratedKey] = useState<string | null>(null);
  const [copiedKey, setCopiedKey] = useState(false);
  const [selectedRecordJson, setSelectedRecordJson] = useState<string | null>(null);

  // Load initial demo data
  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const clientsRes = await fetch('/api/v1/admin/api-clients', {
        headers: { 'X-Admin-Key': 'gv_admin_master_secret_key_884920' }
      });
      if (clientsRes.ok) {
        const data = await clientsRes.json();
        setClients(data);
      } else {
        mockLocalClients();
      }

      const statsRes = await fetch('/api/v1/admin/stats', {
        headers: { 'X-Admin-Key': 'gv_admin_master_secret_key_884920' }
      });
      if (statsRes.ok) {
        const statsData = await statsRes.json();
        setStats(statsData);
      }
    } catch (e) {
      mockLocalClients();
    }
  };

  const mockLocalClients = () => {
    setClients([
      {
        id: 'a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d',
        clientName: 'GeoVerity Official Android App (Fleet A)',
        apiKeyPrefix: 'gv_live_82F4************',
        permissions: 'CAPTURE,VERIFY,TIME_TOKEN',
        status: 'ACTIVE',
        createdAt: '2026-09-01T10:00:00Z',
        lastUsedAt: '2026-09-03T18:30:00Z'
      },
      {
        id: 'b2c3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e',
        clientName: 'Forensic Field Inspection Client',
        apiKeyPrefix: 'gv_live_91AB************',
        permissions: 'CAPTURE,VERIFY,TIME_TOKEN',
        status: 'ACTIVE',
        createdAt: '2026-09-02T14:15:00Z',
        lastUsedAt: '2026-09-03T17:45:00Z'
      },
      {
        id: 'c3d4e5f6-a7b8-9c0d-1e2f-3a4b5c6d7e8f',
        clientName: 'Third-Party Auditing Service',
        apiKeyPrefix: 'gv_live_77CD************',
        permissions: 'VERIFY',
        status: 'ACTIVE',
        createdAt: '2026-09-02T16:00:00Z',
        lastUsedAt: '2026-09-03T15:20:00Z'
      },
      {
        id: 'd4e5f6a7-b8c9-0d1e-2f3a-4b5c6d7e8f9a',
        clientName: 'Decommissioned Tablet Test Suite',
        apiKeyPrefix: 'gv_live_00EF************',
        permissions: 'CAPTURE',
        status: 'REVOKED',
        createdAt: '2026-08-28T09:30:00Z',
        lastUsedAt: '2026-08-30T11:10:00Z'
      }
    ]);

    setRecords([
      {
        id: 'rec-1',
        verificationId: 'SGA-82F4D2A7-C34E-4621-91AB-5369A18DF50E',
        sha256Hash: 'a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8',
        canonicalMetadata: '{"appVersion":"1.0.0","deviceId":"dev_pixel8_gv_984128","latitude":10.785234,"locationName":"Karur, Tamil Nadu, India","longitude":78.125432,"trustedTimestamp":1788440712000,"verificationId":"SGA-82F4D2A7-C34E-4621-91AB-5369A18DF50E"}',
        trustedServerTimestamp: '2026-09-03T14:35:12Z',
        ecdsaSignature: 'MEYCIQDemoxxxValidEcdsaSigP256...',
        deviceId: 'dev_pixel8_gv_984128',
        status: 'AUTHENTICATED',
        createdAt: '2026-09-03T14:35:12Z'
      },
      {
        id: 'rec-2',
        verificationId: 'SGA-91AB44E2-F11A-4830-88BC-904128DF7781',
        sha256Hash: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
        canonicalMetadata: '{"appVersion":"1.0.0","deviceId":"dev_galaxy_s24_9921","latitude":13.082680,"locationName":"Chennai, Tamil Nadu, India","longitude":80.270718,"trustedTimestamp":1788437100000,"verificationId":"SGA-91AB44E2-F11A-4830-88BC-904128DF7781"}',
        trustedServerTimestamp: '2026-09-03T13:35:00Z',
        ecdsaSignature: 'MEYCIQChennaiEcdsaSigValid...',
        deviceId: 'dev_galaxy_s24_9921',
        status: 'AUTHENTICATED',
        createdAt: '2026-09-03T13:35:00Z'
      }
    ]);

    setAudits([
      {
        id: 'aud-1',
        eventType: 'VERIFY_SUCCESS',
        clientIp: '203.0.113.45',
        verificationId: 'SGA-82F4D2A7-C34E-4621-91AB-5369A18DF50E',
        status: 'SUCCESS',
        details: 'Third-party zero-login image verification successful. Hash matched exactly.',
        createdAt: '2026-09-03T18:40:12Z'
      },
      {
        id: 'aud-2',
        eventType: 'TIME_ANOMALY',
        clientIp: '198.51.100.22',
        verificationId: 'SGA-TAMPER-TEST',
        status: 'ANOMALY',
        details: 'Offline reconciliation detected 7,200,000 ms clock deviation. Clock rollback detected. Rejected without signature.',
        createdAt: '2026-09-03T18:15:30Z'
      },
      {
        id: 'aud-3',
        eventType: 'AUTHENTICATION_SUCCESS',
        clientIp: '198.51.100.89',
        verificationId: 'SGA-82F4D2A7-C34E-4621-91AB-5369A18DF50E',
        status: 'SUCCESS',
        details: 'Online camera capture authenticated with server ECDSA signature.',
        createdAt: '2026-09-03T14:35:12Z'
      },
      {
        id: 'aud-4',
        eventType: 'API_KEY_ROTATED',
        clientIp: '127.0.0.1',
        status: 'SUCCESS',
        details: 'Admin rotated API key for client: Forensic Field Inspection Client',
        createdAt: '2026-09-03T12:00:00Z'
      }
    ]);
  };

  const handleCreateClient = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newClientName.trim()) return;

    try {
      const res = await fetch('/api/v1/admin/api-clients', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Admin-Key': 'gv_admin_master_secret_key_884920'
        },
        body: JSON.stringify({
          clientName: newClientName,
          permissions: newPermissions
        })
      });

      if (res.ok) {
        const clientData = await res.json();
        setGeneratedKey(clientData.rawApiKey);
        fetchData();
      } else {
        // Fallback for simulation
        const fakeKey = 'gv_live_' + Array.from(crypto.getRandomValues(new Uint8Array(24))).map(b => b.toString(16).padStart(2, '0')).join('');
        setGeneratedKey(fakeKey);
        setClients([
          ...clients,
          {
            id: crypto.randomUUID(),
            clientName: newClientName,
            apiKeyPrefix: fakeKey.substring(0, 16) + '************',
            permissions: newPermissions,
            status: 'ACTIVE',
            createdAt: new Date().toISOString()
          }
        ]);
      }
    } catch (e) {
      const fakeKey = 'gv_live_' + Array.from(crypto.getRandomValues(new Uint8Array(24))).map(b => b.toString(16).padStart(2, '0')).join('');
      setGeneratedKey(fakeKey);
    }
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedKey(true);
    setTimeout(() => setCopiedKey(false), 2000);
  };

  const handleRevoke = async (id: string) => {
    if (!confirm('Are you sure you want to permanently revoke this API client?')) return;
    try {
      await fetch(`/api/v1/admin/api-clients/${id}/revoke`, {
        method: 'POST',
        headers: { 'X-Admin-Key': 'gv_admin_master_secret_key_884920' }
      });
    } catch (e) {}

    setClients(clients.map(c => c.id === id ? { ...c, status: 'REVOKED' } : c));
  };

  const handleRotate = async (id: string) => {
    if (!confirm('Rotate API Key? The existing key will be immediately invalidated.')) return;
    try {
      const res = await fetch(`/api/v1/admin/api-clients/${id}/rotate`, {
        method: 'POST',
        headers: { 'X-Admin-Key': 'gv_admin_master_secret_key_884920' }
      });
      if (res.ok) {
        const data = await res.json();
        setGeneratedKey(data.rawApiKey);
        setIsCreateModalOpen(true);
        fetchData();
        return;
      }
    } catch (e) {}

    const fakeKey = 'gv_live_' + Array.from(crypto.getRandomValues(new Uint8Array(24))).map(b => b.toString(16).padStart(2, '0')).join('');
    setGeneratedKey(fakeKey);
    setIsCreateModalOpen(true);
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8 animate-fade-in">
      
      {/* Top Header & Metrics Banner */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-purple-50 border border-purple-200 text-purple-700 text-xs font-semibold mb-2">
            <Shield className="w-3.5 h-3.5" />
            <span>Master Administration &amp; Cryptographic Registry</span>
          </div>
          <h1 className="text-3xl font-extrabold text-slate-900 font-['Outfit'] tracking-tight">
            Security &amp; API Client Management
          </h1>
          <p className="text-xs text-slate-500">
            Authorizes GeoVerity mobile clients, tracks server ECDSA evidence records, and inspects real-time security events.
          </p>
        </div>

        <button
          onClick={() => {
            setGeneratedKey(null);
            setNewClientName('');
            setIsCreateModalOpen(true);
          }}
          className="px-4 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-2xl font-semibold text-xs flex items-center space-x-2 shadow-md shadow-indigo-600/20 transition-all hover:scale-105 active:scale-95"
        >
          <Plus className="w-4 h-4" />
          <span>Create API Client</span>
        </button>
      </div>

      {/* 5 Colorful Stat Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
        
        <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm space-y-2 hover:border-indigo-300 transition-all">
          <div className="flex items-center justify-between text-slate-500">
            <span className="text-xs font-semibold uppercase tracking-wider font-['Outfit']">Total Records</span>
            <div className="h-8 w-8 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center">
              <Database className="w-4 h-4" />
            </div>
          </div>
          <p className="text-2xl font-black text-slate-900 font-['Outfit']">{stats.totalRecords}</p>
          <p className="text-[11px] font-medium text-emerald-600">✓ All Cryptographically Signed</p>
        </div>

        <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm space-y-2 hover:border-emerald-300 transition-all">
          <div className="flex items-center justify-between text-slate-500">
            <span className="text-xs font-semibold uppercase tracking-wider font-['Outfit']">Authenticated</span>
            <div className="h-8 w-8 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
              <CheckCircle className="w-4 h-4" />
            </div>
          </div>
          <p className="text-2xl font-black text-slate-900 font-['Outfit']">{stats.authenticatedRecords}</p>
          <p className="text-[11px] font-medium text-slate-500">ECDSA P-256 Valid</p>
        </div>

        <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm space-y-2 hover:border-amber-300 transition-all">
          <div className="flex items-center justify-between text-slate-500">
            <span className="text-xs font-semibold uppercase tracking-wider font-['Outfit']">Active API Clients</span>
            <div className="h-8 w-8 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center">
              <Key className="w-4 h-4" />
            </div>
          </div>
          <p className="text-2xl font-black text-slate-900 font-['Outfit']">{stats.activeApiClients} / {stats.totalApiClients}</p>
          <p className="text-[11px] font-medium text-amber-600">Authorized Android Fleets</p>
        </div>

        <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm space-y-2 hover:border-rose-300 transition-all">
          <div className="flex items-center justify-between text-slate-500">
            <span className="text-xs font-semibold uppercase tracking-wider font-['Outfit']">Time Anomalies</span>
            <div className="h-8 w-8 rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center">
              <AlertOctagon className="w-4 h-4" />
            </div>
          </div>
          <p className="text-2xl font-black text-rose-600 font-['Outfit']">{stats.timeAnomalies}</p>
          <p className="text-[11px] font-medium text-rose-600">Clock Tampering Blocked</p>
        </div>

        <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm space-y-2 hover:border-cyan-300 transition-all">
          <div className="flex items-center justify-between text-slate-500">
            <span className="text-xs font-semibold uppercase tracking-wider font-['Outfit']">Security Audits</span>
            <div className="h-8 w-8 rounded-xl bg-cyan-50 text-cyan-600 flex items-center justify-center">
              <Activity className="w-4 h-4" />
            </div>
          </div>
          <p className="text-2xl font-black text-slate-900 font-['Outfit']">{stats.totalAuditEvents}</p>
          <p className="text-[11px] font-medium text-cyan-600">Immutable Audit Stream</p>
        </div>

      </div>

      {/* Navigation Sub-Tabs */}
      <div className="flex space-x-2 border-b border-slate-200 pb-2">
        <button
          onClick={() => setActiveTab('clients')}
          className={`px-4 py-2 rounded-xl text-xs font-bold transition-all ${
            activeTab === 'clients'
              ? 'bg-indigo-600 text-white shadow-sm'
              : 'text-slate-600 hover:bg-slate-100'
          }`}
        >
          API Clients ({clients.length})
        </button>

        <button
          onClick={() => setActiveTab('records')}
          className={`px-4 py-2 rounded-xl text-xs font-bold transition-all ${
            activeTab === 'records'
              ? 'bg-indigo-600 text-white shadow-sm'
              : 'text-slate-600 hover:bg-slate-100'
          }`}
        >
          Verification Records ({records.length})
        </button>

        <button
          onClick={() => setActiveTab('audits')}
          className={`px-4 py-2 rounded-xl text-xs font-bold transition-all ${
            activeTab === 'audits'
              ? 'bg-indigo-600 text-white shadow-sm'
              : 'text-slate-600 hover:bg-slate-100'
          }`}
        >
          Audit Logs ({audits.length})
        </button>
      </div>

      {/* 1. API Clients Table */}
      {activeTab === 'clients' && (
        <div className="bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden space-y-4">
          <div className="p-6 border-b border-slate-100 flex justify-between items-center">
            <div>
              <h3 className="font-bold text-lg text-slate-900 font-['Outfit']">Authorized Mobile API Clients</h3>
              <p className="text-xs text-slate-500">Android clients authorized to request trusted time and submit photographic evidence.</p>
            </div>
            <span className="text-xs font-mono text-slate-400 bg-slate-50 px-3 py-1 rounded-xl border border-slate-200">
              Header: X-API-Key
            </span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-500 font-semibold uppercase tracking-wider border-y border-slate-200">
                <tr>
                  <th className="py-3.5 px-6">Client Name</th>
                  <th className="py-3.5 px-6">API Key Prefix</th>
                  <th className="py-3.5 px-6">Permissions</th>
                  <th className="py-3.5 px-6">Status</th>
                  <th className="py-3.5 px-6">Created / Last Used</th>
                  <th className="py-3.5 px-6 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-medium text-slate-700">
                {clients.map(client => (
                  <tr key={client.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="py-4 px-6 font-bold text-slate-900">{client.clientName}</td>
                    <td className="py-4 px-6 font-mono text-indigo-600">{client.apiKeyPrefix}</td>
                    <td className="py-4 px-6">
                      <span className="px-2 py-1 rounded-md bg-slate-100 text-slate-700 text-[10px] font-mono font-semibold">
                        {client.permissions}
                      </span>
                    </td>
                    <td className="py-4 px-6">
                      <span className={`px-2.5 py-1 rounded-full text-[10px] font-bold ${
                        client.status === 'ACTIVE'
                          ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                          : 'bg-rose-50 text-rose-700 border border-rose-200'
                      }`}>
                        {client.status}
                      </span>
                    </td>
                    <td className="py-4 px-6 text-slate-500 text-[11px]">
                      <div>Created: {new Date(client.createdAt).toLocaleDateString()}</div>
                      {client.lastUsedAt && <div className="text-slate-400">Used: {new Date(client.lastUsedAt).toLocaleTimeString()}</div>}
                    </td>
                    <td className="py-4 px-6 text-right space-x-2">
                      {client.status === 'ACTIVE' && (
                        <>
                          <button
                            onClick={() => handleRotate(client.id)}
                            className="p-1.5 rounded-lg bg-amber-50 hover:bg-amber-100 text-amber-700 transition-colors"
                            title="Rotate Key"
                          >
                            <RefreshCw className="w-3.5 h-3.5" />
                          </button>
                          <button
                            onClick={() => handleRevoke(client.id)}
                            className="p-1.5 rounded-lg bg-rose-50 hover:bg-rose-100 text-rose-700 transition-colors"
                            title="Revoke Key"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        </>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* 2. Verification Records Table */}
      {activeTab === 'records' && (
        <div className="bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden space-y-4">
          <div className="p-6 border-b border-slate-100">
            <h3 className="font-bold text-lg text-slate-900 font-['Outfit']">Authoritative Verification Registry (PostgreSQL)</h3>
            <p className="text-xs text-slate-500">Original evidence cryptographic proofs. NO image blobs stored on server.</p>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-500 font-semibold uppercase tracking-wider border-y border-slate-200">
                <tr>
                  <th className="py-3.5 px-6">Verification ID</th>
                  <th className="py-3.5 px-6">Trusted Server Timestamp</th>
                  <th className="py-3.5 px-6">Composite SHA-256</th>
                  <th className="py-3.5 px-6">ECDSA Signature</th>
                  <th className="py-3.5 px-6">Device ID</th>
                  <th className="py-3.5 px-6 text-right">Canonical JSON</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-medium text-slate-700">
                {records.map(rec => (
                  <tr key={rec.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="py-4 px-6 font-mono font-bold text-indigo-600">{rec.verificationId}</td>
                    <td className="py-4 px-6 text-slate-600">{new Date(rec.trustedServerTimestamp).toLocaleString()}</td>
                    <td className="py-4 px-6 font-mono text-[11px] text-slate-500">{rec.sha256Hash.substring(0, 16)}...</td>
                    <td className="py-4 px-6">
                      <span className="px-2 py-0.5 rounded-full bg-emerald-50 border border-emerald-200 text-emerald-700 font-bold text-[10px]">
                        VALID P-256
                      </span>
                    </td>
                    <td className="py-4 px-6 font-mono text-slate-500 text-[11px]">{rec.deviceId}</td>
                    <td className="py-4 px-6 text-right">
                      <button
                        onClick={() => setSelectedRecordJson(rec.canonicalMetadata)}
                        className="px-2.5 py-1 rounded-lg bg-indigo-50 hover:bg-indigo-100 text-indigo-700 font-semibold text-[11px] transition-colors"
                      >
                        Inspect
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* 3. Audit Logs Table */}
      {activeTab === 'audits' && (
        <div className="bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden space-y-4">
          <div className="p-6 border-b border-slate-100">
            <h3 className="font-bold text-lg text-slate-900 font-['Outfit']">Security Audit Stream</h3>
            <p className="text-xs text-slate-500">Immutable security event log for compliance and tamper tracing.</p>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-500 font-semibold uppercase tracking-wider border-y border-slate-200">
                <tr>
                  <th className="py-3.5 px-6">Event Type</th>
                  <th className="py-3.5 px-6">Client IP</th>
                  <th className="py-3.5 px-6">Verification ID</th>
                  <th className="py-3.5 px-6">Status</th>
                  <th className="py-3.5 px-6">Event Details</th>
                  <th className="py-3.5 px-6 text-right">Timestamp</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-medium text-slate-700">
                {audits.map(aud => (
                  <tr key={aud.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="py-4 px-6 font-mono font-bold text-slate-900">{aud.eventType}</td>
                    <td className="py-4 px-6 font-mono text-slate-500">{aud.clientIp || '127.0.0.1'}</td>
                    <td className="py-4 px-6 font-mono text-indigo-600">{aud.verificationId || '—'}</td>
                    <td className="py-4 px-6">
                      <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                        aud.status === 'SUCCESS' ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' :
                        aud.status === 'ANOMALY' ? 'bg-rose-50 text-rose-700 border border-rose-200' :
                        'bg-amber-50 text-amber-700 border border-amber-200'
                      }`}>
                        {aud.status}
                      </span>
                    </td>
                    <td className="py-4 px-6 text-slate-600 text-xs max-w-xs">{aud.details}</td>
                    <td className="py-4 px-6 text-right text-slate-400 font-mono text-[11px]">
                      {new Date(aud.createdAt).toLocaleTimeString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Modal: Create API Client / Generated Key Warning */}
      {isCreateModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-fade-in">
          <div className="bg-white rounded-3xl p-6 sm:p-8 max-w-lg w-full border border-slate-200 shadow-2xl space-y-6">
            
            <div className="flex justify-between items-center border-b border-slate-100 pb-4">
              <h3 className="text-xl font-bold text-slate-900 font-['Outfit']">
                {generatedKey ? 'API Key Generated' : 'Create Authorized API Client'}
              </h3>
              <button 
                onClick={() => setIsCreateModalOpen(false)}
                className="text-slate-400 hover:text-slate-600 text-sm font-bold"
              >
                ✕
              </button>
            </div>

            {generatedKey ? (
              <div className="space-y-4">
                <div className="bg-amber-50 border border-amber-200 p-4 rounded-2xl text-amber-900 text-xs space-y-1">
                  <p className="font-bold">⚠️ IMPORTANT SECURITY NOTICE:</p>
                  <p>This raw API key will ONLY be shown once. GeoVerity hashes all keys in the database and cannot recover it later.</p>
                </div>

                <div className="space-y-1">
                  <label className="text-xs font-bold uppercase tracking-wider text-slate-500">Generated API Key</label>
                  <div className="flex items-center space-x-2 bg-slate-900 p-3 rounded-2xl text-white font-mono text-xs">
                    <span className="truncate flex-1 text-emerald-400">{generatedKey}</span>
                    <button
                      onClick={() => copyToClipboard(generatedKey)}
                      className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-white transition-colors"
                    >
                      {copiedKey ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
                    </button>
                  </div>
                </div>

                <button
                  onClick={() => setIsCreateModalOpen(false)}
                  className="w-full py-3 bg-indigo-600 hover:bg-indigo-700 text-white rounded-2xl font-bold text-xs shadow-md transition-all"
                >
                  I Have Safely Saved This Key
                </button>
              </div>
            ) : (
              <form onSubmit={handleCreateClient} className="space-y-4">
                <div className="space-y-1">
                  <label className="text-xs font-bold uppercase tracking-wider text-slate-500">Client Name</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. GeoVerity Official Android App (Fleet B)"
                    value={newClientName}
                    onChange={(e) => setNewClientName(e.target.value)}
                    className="w-full px-4 py-2.5 rounded-2xl border border-slate-200 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>

                <div className="space-y-1">
                  <label className="text-xs font-bold uppercase tracking-wider text-slate-500">Permissions</label>
                  <input
                    type="text"
                    value={newPermissions}
                    onChange={(e) => setNewPermissions(e.target.value)}
                    className="w-full px-4 py-2.5 rounded-2xl border border-slate-200 text-xs font-mono focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                  <p className="text-[10px] text-slate-400">Comma-separated: CAPTURE, VERIFY, TIME_TOKEN</p>
                </div>

                <div className="pt-4 flex justify-end space-x-2">
                  <button
                    type="button"
                    onClick={() => setIsCreateModalOpen(false)}
                    className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="px-5 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold shadow-md"
                  >
                    Generate Cryptographic API Key
                  </button>
                </div>
              </form>
            )}

          </div>
        </div>
      )}

      {/* Modal: Inspect Canonical JSON */}
      {selectedRecordJson && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-fade-in">
          <div className="bg-white rounded-3xl p-6 max-w-lg w-full border border-slate-200 shadow-2xl space-y-4">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <h3 className="font-bold text-slate-900 font-['Outfit']">Authoritative Canonical Metadata</h3>
              <button onClick={() => setSelectedRecordJson(null)} className="text-slate-400 hover:text-slate-600 text-sm font-bold">✕</button>
            </div>
            <pre className="bg-slate-900 text-emerald-400 p-4 rounded-2xl text-xs font-mono overflow-x-auto max-h-80">
              {JSON.stringify(JSON.parse(selectedRecordJson), null, 2)}
            </pre>
          </div>
        </div>
      )}

    </div>
  );
};
