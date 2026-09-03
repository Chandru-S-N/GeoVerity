import React from 'react';
import { 
  ShieldCheck, 
  Lock, 
  Server, 
  AlertTriangle, 
  Clock, 
  Database, 
  QrCode, 
  CheckCircle2, 
  FileCheck2,
  Code2
} from 'lucide-react';

export const DocumentationView: React.FC = () => {
  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8 animate-fade-in">
      
      <div>
        <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-emerald-50 border border-emerald-200 text-emerald-700 text-xs font-semibold mb-2">
          <ShieldCheck className="w-3.5 h-3.5" />
          <span>Cryptographic Specification &amp; Security Architecture</span>
        </div>
        <h1 className="text-3xl font-extrabold text-slate-900 font-['Outfit'] tracking-tight">
          GeoVerity Security Model &amp; Architecture
        </h1>
        <p className="text-xs text-slate-500">
          Formal breakdown of cryptographic primitives, zero-login authorization, tamper detection, and offline monotonic time reconciliation.
        </p>
      </div>

      {/* 4 Core Pillars */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        
        {/* Pillar 1: Server Authority */}
        <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm space-y-3">
          <div className="h-10 w-10 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center">
            <Server className="w-5 h-5" />
          </div>
          <h3 className="font-bold text-base text-slate-900 font-['Outfit']">
            1. Backend is the Single Authority
          </h3>
          <p className="text-xs text-slate-600 leading-relaxed">
            The Android mobile client is a controlled capture environment, <strong>not an authoritative signer</strong>. 
            All digital signatures are issued exclusively by the backend using server-side <strong>ECDSA NIST P-256 (SHA256withECDSA)</strong>. 
            Private keys are never exposed to clients or embedded in mobile APKs.
          </p>
        </div>

        {/* Pillar 2: Composite Hashing */}
        <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm space-y-3">
          <div className="h-10 w-10 rounded-2xl bg-purple-50 text-purple-600 flex items-center justify-center">
            <Lock className="w-5 h-5" />
          </div>
          <h3 className="font-bold text-base text-slate-900 font-['Outfit']">
            2. Composite SHA-256 Cryptographic Binding
          </h3>
          <p className="text-xs text-slate-600 leading-relaxed">
            The system hashes <code className="bg-slate-100 text-purple-700 px-1 py-0.5 rounded font-mono text-[11px]">finalImageBytes + canonicalMetadata</code>. 
            Modifying a single pixel, altering the footer text, re-compressing the JPEG, or changing timestamp digits results in immediate 
            <strong> 100% hash mismatch</strong> during verification.
          </p>
        </div>

        {/* Pillar 3: Why EXIF is Untrusted */}
        <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm space-y-3">
          <div className="h-10 w-10 rounded-2xl bg-rose-50 text-rose-600 flex items-center justify-center">
            <AlertTriangle className="w-5 h-5" />
          </div>
          <h3 className="font-bold text-base text-slate-900 font-['Outfit']">
            3. Why EXIF &amp; QR are Not Cryptographic Proof
          </h3>
          <p className="text-xs text-slate-600 leading-relaxed">
            EXIF metadata is trivially editable and stripped by many transfer channels. 
            The QR code serves solely as a human-readable and scanner-friendly <strong>identifier</strong>. 
            Authenticity is determined exclusively by the server's ECDSA signature and bit-level hash match.
          </p>
        </div>

        {/* Pillar 4: Zero Server Image Retention */}
        <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm space-y-3">
          <div className="h-10 w-10 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
            <Database className="w-5 h-5" />
          </div>
          <h3 className="font-bold text-base text-slate-900 font-['Outfit']">
            4. Zero Server Image Storage
          </h3>
          <p className="text-xs text-slate-600 leading-relaxed">
            The server retains only cryptographic evidence proofs (<code className="font-mono text-[11px] text-emerald-700">verification_id</code>, <code className="font-mono text-[11px] text-emerald-700">sha256_hash</code>, <code className="font-mono text-[11px] text-emerald-700">canonical_metadata</code>, <code className="font-mono text-[11px] text-emerald-700">ecdsa_signature</code>). 
            No original image files or blobs are permanently stored on the server, drastically reducing storage costs, liability, and privacy risks.
          </p>
        </div>

      </div>

      {/* Offline Mathematical Model */}
      <div className="bg-white rounded-3xl p-8 border border-slate-200 shadow-sm space-y-4">
        <div className="flex items-center space-x-3">
          <div className="h-10 w-10 rounded-2xl bg-amber-50 text-amber-600 flex items-center justify-center">
            <Clock className="w-5 h-5" />
          </div>
          <div>
            <h3 className="font-bold text-lg text-slate-900 font-['Outfit']">
              Offline Monotonic Time Reconciliation Formula
            </h3>
            <p className="text-xs text-slate-500">Detects and rejects clock rewind and time manipulation attacks</p>
          </div>
        </div>

        <div className="bg-slate-900 rounded-2xl p-5 text-slate-100 font-mono text-xs space-y-3">
          <p className="text-indigo-400 font-bold">// Offline Reconciliation Formula</p>
          <p>expectedDeviceTime = lastTrustedServerTimestamp + (captureElapsedRealtime - lastTrustedElapsedRealtime)</p>
          <p>deviation = ABS(deviceCaptureTime - expectedDeviceTime)</p>
          <p className="text-amber-300">// Monotonic Tolerance Check (Threshold = 120,000 ms / 2 mins)</p>
          <p>IF deviation &gt; 120000 ms THEN</p>
          <p className="text-rose-400 pl-4">REJECT: Flag TIME_ANOMALY (No SHA-256 generated, no signature issued)</p>
          <p>ELSE</p>
          <p className="text-emerald-400 pl-4">ACCEPT: Authoritative capture timestamp set to expectedDeviceTime</p>
        </div>
      </div>

      {/* Dedicated Image Layout Specification */}
      <div className="bg-white rounded-3xl p-8 border border-slate-200 shadow-sm space-y-4">
        <h3 className="font-bold text-lg text-slate-900 font-['Outfit'] flex items-center space-x-2">
          <FileCheck2 className="w-5 h-5 text-indigo-600" />
          <span>Controlled Image Structure &amp; Footer Layout</span>
        </h3>
        
        <div className="border border-slate-300 rounded-2xl p-6 bg-slate-50 space-y-4">
          <div className="border-2 border-dashed border-slate-300 rounded-xl h-48 flex items-center justify-center text-slate-400 font-medium text-xs bg-white">
            [ ORIGINAL UNTOUCHED PHOTOGRAPH CONTENT AREA (Clean &amp; Uncovered) ]
          </div>
          
          <div className="bg-slate-900 text-white p-4 rounded-xl flex justify-between items-center text-xs font-mono">
            <div className="space-y-1">
              <p className="font-bold text-white">Location: Karur, Tamil Nadu, India</p>
              <p className="text-slate-400">GPS: 10.785234, 78.125432</p>
              <p className="text-slate-400">Date: 03 Sep 2026</p>
              <p className="text-slate-400">Time: 02:35:12 PM</p>
              <p className="text-indigo-400 font-bold">Verification ID: SGA-82F4D2A7-C34E-4621-91AB-5369A18DF50E</p>
            </div>
            <div className="bg-white text-slate-900 h-24 w-24 rounded-lg flex items-center justify-center text-[10px] font-bold text-center border-2 border-indigo-400">
              DEDICATED<br/>QR CODE
            </div>
          </div>
        </div>
      </div>

    </div>
  );
};
