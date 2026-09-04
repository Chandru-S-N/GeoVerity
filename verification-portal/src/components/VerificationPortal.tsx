import React, { useState, useRef } from 'react';
import { 
  UploadCloud, 
  CheckCircle2, 
  XCircle, 
  ShieldCheck, 
  MapPin, 
  Clock, 
  Smartphone, 
  Hash, 
  FileText, 
  AlertTriangle, 
  ArrowRight, 
  RefreshCw,
  Download,
  Eye,
  Lock
} from 'lucide-react';
import { VerificationResponse } from '../types';

export const VerificationPortal: React.FC = () => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [isVerifying, setIsVerifying] = useState<boolean>(false);
  const [result, setResult] = useState<VerificationResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileChange = (file: File) => {
    setSelectedFile(file);
    setResult(null);
    setError(null);
    const url = URL.createObjectURL(file);
    setPreviewUrl(url);
    performVerification(file);
  };

  const performVerification = async (file: File) => {
    setIsVerifying(true);
    setError(null);

    try {
      const formData = new FormData();
      formData.append('file', file);

      const response = await fetch('/api/v1/verify', {
        method: 'POST',
        body: formData,
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || 'Verification service failed to process request');
      }

      const data: VerificationResponse = await response.json();
      setResult(data);
    } catch (err: any) {
      console.error('Verification error:', err);
      // If server is not reachable, provide realistic demonstration fallback
      setError(err.message || 'Unable to connect to backend verification authority');
    } finally {
      setIsVerifying(false);
    }
  };

  // Demo samples for quick testing
  const loadDemoSample = async (type: 'authentic' | 'tampered' | 'unregistered') => {
    setIsVerifying(true);
    setError(null);
    setResult(null);

    // Create a mock canvas image representing GeoVerity composite photo
    const canvas = document.createElement('canvas');
    canvas.width = 600;
    canvas.height = 700;
    const ctx = canvas.getContext('2d')!;

    // Draw main photo area (clean scenery)
    const gradient = ctx.createLinearGradient(0, 0, 600, 520);
    gradient.addColorStop(0, '#0284C7');
    gradient.addColorStop(0.5, '#38BDF8');
    gradient.addColorStop(1, '#BAE6FD');
    ctx.fillStyle = gradient;
    ctx.fillRect(0, 0, 600, 520);

    // Draw photo label
    ctx.fillStyle = '#FFFFFF';
    ctx.font = 'bold 24px Outfit, sans-serif';
    ctx.fillText('GeoVerity Controlled Camera Capture', 40, 260);
    ctx.font = '16px Inter, sans-serif';
    ctx.fillText('Original image content area is clean and untouched', 40, 295);

    // Draw dedicated footer
    ctx.fillStyle = '#0F172A';
    ctx.fillRect(0, 520, 600, 180);

    const vId = type === 'unregistered' 
      ? 'SGA-00000000-0000-0000-0000-000000000000' 
      : 'SGA-82F4D2A7-C34E-4621-91AB-5369A18DF50E';

    ctx.fillStyle = '#FFFFFF';
    ctx.font = 'bold 15px Outfit, sans-serif';
    ctx.fillText('Location: Karur, Tamil Nadu, India', 24, 555);
    ctx.font = '13px Inter, sans-serif';
    ctx.fillStyle = '#94A3B8';
    ctx.fillText('GPS: 10.785234, 78.125432', 24, 582);
    ctx.fillText('Trusted Date: 03 Sep 2026', 24, 607);
    ctx.fillText('Trusted Time: 02:35:12 PM IST', 24, 632);
    ctx.fillStyle = '#818CF8';
    ctx.font = 'bold 13px Inter, sans-serif';
    ctx.fillText(`Verification ID: ${vId}`, 24, 665);

    // Simulated QR area
    ctx.fillStyle = '#FFFFFF';
    ctx.fillRect(440, 535, 130, 130);
    ctx.fillStyle = '#0F172A';
    ctx.font = 'bold 12px monospace';
    ctx.fillText('QR CODE', 475, 605);
    ctx.font = '9px monospace';
    ctx.fillText(vId.substring(0, 12), 450, 625);

    canvas.toBlob(async (blob) => {
      if (!blob) return;
      const file = new File([blob], `geoverity_${type}_evidence.jpg`, { type: 'image/jpeg' });
      setSelectedFile(file);
      setPreviewUrl(URL.createObjectURL(file));

      // If backend is running, try real verify; else mock realistic demonstration
      try {
        const formData = new FormData();
        formData.append('file', file);
        const response = await fetch('/api/v1/verify', { method: 'POST', body: formData });
        if (response.ok) {
          const data = await response.json();
          setResult(data);
          setIsVerifying(false);
          return;
        }
      } catch (e) {
        // Fallback demo simulation
      }

      setTimeout(() => {
        if (type === 'authentic') {
          setResult({
            verificationId: 'SGA-82F4D2A7-C34E-4621-91AB-5369A18DF50E',
            status: 'AUTHENTIC',
            signatureValid: true,
            hashMatched: true,
            location: 'Karur, Tamil Nadu, India',
            gps: '10.785234, 78.125432',
            trustedTimestamp: '2026-09-03T14:35:12.000Z',
            deviceId: 'dev_pixel8_gv_984128',
            sha256Hash: 'a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8',
            canonicalMetadata: {
              appVersion: '1.0.0',
              deviceId: 'dev_pixel8_gv_984128',
              latitude: 10.785234,
              locationName: 'Karur, Tamil Nadu, India',
              longitude: 78.125432,
              trustedTimestamp: 1788440712000,
              verificationId: 'SGA-82F4D2A7-C34E-4621-91AB-5369A18DF50E'
            },
            verificationSteps: [
              'Received original image payload (124,892 bytes)',
              'Extracted Verification ID from embedded JPEG COM segment: SGA-82F4D2A7-C34E-4621-91AB-5369A18DF50E',
              'Retrieved authoritative record from PostgreSQL (Indexed Lookup in 4ms)',
              'Validated server ECDSA P-256 digital signature: VALID',
              'Recalculated SHA-256 over image bytes + canonical metadata: EXACT MATCH',
              'SUCCESS: Image is verified AUTHENTIC digital evidence'
            ]
          });
        } else if (type === 'tampered') {
          setResult({
            verificationId: 'SGA-82F4D2A7-C34E-4621-91AB-5369A18DF50E',
            status: 'NOT_AUTHENTIC',
            signatureValid: true,
            hashMatched: false,
            failureReason: 'Hash mismatch: The photograph content, metadata footer, or pixel bytes have been modified, cropped, compressed, or re-encoded.',
            verificationSteps: [
              'Received image payload (124,892 bytes)',
              'Extracted Verification ID: SGA-82F4D2A7-C34E-4621-91AB-5369A18DF50E',
              'Retrieved database record: Found',
              'Validated ECDSA P-256 signature: VALID',
              'FAILED: Recalculated SHA-256 (3f9a...) does not match stored authoritative hash (a7b8...)',
              'REJECTED: Digital evidence has been altered'
            ]
          });
        } else {
          setResult({
            status: 'NOT_AUTHENTIC',
            signatureValid: false,
            hashMatched: false,
            failureReason: 'Unknown Verification ID: No cryptographic evidence record exists for SGA-00000000-0000-0000-0000-000000000000',
            verificationSteps: [
              'Received image payload',
              'Extracted Verification ID: SGA-00000000-0000-0000-0000-000000000000',
              'FAILED: Verification ID not found in PostgreSQL registry'
            ]
          });
        }
        setIsVerifying(false);
      }, 600);
    }, 'image/jpeg', 0.95);
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8 animate-fade-in">
      
      {/* Hero Banner with White Base & Colorful Cards */}
      <div className="bg-white rounded-3xl p-8 border border-slate-200 shadow-sm relative overflow-hidden">
        <div className="absolute -right-16 -top-16 w-80 h-80 bg-gradient-to-br from-indigo-100/70 via-purple-100/40 to-transparent rounded-full blur-3xl pointer-events-none"></div>
        <div className="max-w-3xl relative z-10 space-y-4">
          <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-indigo-50 border border-indigo-200 text-indigo-700 text-xs font-semibold">
            <Lock className="w-3.5 h-3.5" />
            <span>Zero-Login Public Verification Authority</span>
          </div>
          <h1 className="text-3xl sm:text-4xl font-extrabold text-slate-900 tracking-tight font-['Outfit']">
            Verify Authentic Digital Evidence
          </h1>
          <p className="text-slate-600 text-base leading-relaxed">
            Upload an original photograph captured with the authorized GeoVerity mobile application. 
            The system verifies cryptographic authenticity directly against the server-issued 
            <span className="font-semibold text-slate-800"> ECDSA P-256 signature</span> and 
            <span className="font-semibold text-slate-800"> composite SHA-256 hash</span>.
          </p>

          {/* Quick Demo Test Buttons */}
          <div className="pt-2 flex flex-wrap items-center gap-3">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Try Demo Scenarios:</span>
            <button
              onClick={() => loadDemoSample('authentic')}
              className="px-3.5 py-1.5 rounded-xl bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 text-emerald-700 text-xs font-semibold flex items-center space-x-1.5 transition-colors shadow-sm"
            >
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
              <span>Authentic Evidence</span>
            </button>
            <button
              onClick={() => loadDemoSample('tampered')}
              className="px-3.5 py-1.5 rounded-xl bg-rose-50 hover:bg-rose-100 border border-rose-200 text-rose-700 text-xs font-semibold flex items-center space-x-1.5 transition-colors shadow-sm"
            >
              <XCircle className="w-3.5 h-3.5 text-rose-600" />
              <span>Tampered / Modified File</span>
            </button>
            <button
              onClick={() => loadDemoSample('unregistered')}
              className="px-3.5 py-1.5 rounded-xl bg-slate-100 hover:bg-slate-200 border border-slate-300 text-slate-700 text-xs font-semibold flex items-center space-x-1.5 transition-colors"
            >
              <AlertTriangle className="w-3.5 h-3.5 text-slate-600" />
              <span>Unregistered ID</span>
            </button>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        
        {/* Left Column: Upload Box & Image Preview */}
        <div className="lg:col-span-5 space-y-6">
          <div 
            onClick={() => fileInputRef.current?.click()}
            className="group bg-white rounded-3xl p-8 border-2 border-dashed border-indigo-200 hover:border-indigo-500 transition-all duration-300 cursor-pointer shadow-sm hover:shadow-md text-center space-y-4 relative overflow-hidden"
          >
            <input 
              ref={fileInputRef}
              type="file" 
              accept="image/jpeg,image/png,image/jpg" 
              className="hidden" 
              onChange={(e) => e.target.files?.[0] && handleFileChange(e.target.files[0])}
            />

            <div className="h-16 w-16 mx-auto rounded-2xl bg-indigo-50 group-hover:bg-indigo-600 text-indigo-600 group-hover:text-white flex items-center justify-center transition-all duration-300 shadow-sm">
              <UploadCloud className="w-8 h-8" />
            </div>

            <div className="space-y-1">
              <p className="font-['Outfit'] font-bold text-lg text-slate-800">
                Upload Original Evidence Photo
              </p>
              <p className="text-xs text-slate-500">
                Drag &amp; drop or click to browse. JPEG / PNG original files.
              </p>
            </div>

            <div className="pt-2 text-[11px] font-medium text-slate-400 bg-slate-50 p-2.5 rounded-xl border border-slate-100">
              ⚡ No login or manual Verification ID required. Auto-extracts from image bytes.
            </div>
          </div>

          {/* Image Preview */}
          {previewUrl && (
            <div className="bg-white rounded-3xl p-5 border border-slate-200 shadow-sm space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold uppercase tracking-wider text-slate-500 font-['Outfit']">Image Inspection</span>
                <span className="text-xs text-slate-400 font-mono">{selectedFile?.name}</span>
              </div>
              <div className="rounded-2xl overflow-hidden border border-slate-200 bg-slate-900 max-h-[380px] flex items-center justify-center">
                <img src={previewUrl} alt="Evidence Preview" className="max-h-[380px] w-auto object-contain" />
              </div>
            </div>
          )}
        </div>

        {/* Right Column: Verification Results & Cryptographic Pipeline */}
        <div className="lg:col-span-7 space-y-6">
          
          {/* Loading State */}
          {isVerifying && (
            <div className="bg-white rounded-3xl p-12 border border-slate-200 shadow-sm text-center space-y-4 animate-pulse">
              <div className="h-14 w-14 mx-auto rounded-2xl bg-indigo-100 text-indigo-600 flex items-center justify-center">
                <RefreshCw className="w-7 h-7 animate-spin" />
              </div>
              <h3 className="text-xl font-bold text-slate-900 font-['Outfit']">Executing Cryptographic Verification...</h3>
              <p className="text-xs text-slate-500 max-w-md mx-auto">
                Extracting embedded Verification ID, querying PostgreSQL registry, verifying ECDSA P-256 signature, and recomputing composite SHA-256 hash.
              </p>
            </div>
          )}

          {/* Error Message */}
          {error && (
            <div className="bg-rose-50 border border-rose-200 rounded-3xl p-6 text-rose-800 space-y-2">
              <div className="flex items-center space-x-2 font-bold font-['Outfit']">
                <XCircle className="w-5 h-5 text-rose-600" />
                <span>Verification Authority Error</span>
              </div>
              <p className="text-sm text-rose-700">{error}</p>
            </div>
          )}

          {/* Verification Result Card */}
          {result && (
            <div className="space-y-6 animate-scale-in">
              
              {result.status === 'AUTHENTIC' ? (
                /* SUCCESS CARD (Vibrant Emerald & Indigo White Card) */
                <div className="bg-white rounded-3xl p-8 border-2 border-emerald-500 shadow-lg shadow-emerald-500/10 relative overflow-hidden space-y-6">
                  <div className="absolute top-0 right-0 w-48 h-48 bg-emerald-50 rounded-full blur-2xl pointer-events-none"></div>

                  <div className="flex items-center justify-between border-b border-slate-100 pb-5">
                    <div className="flex items-center space-x-3">
                      <div className="h-12 w-12 rounded-2xl bg-emerald-500 text-white flex items-center justify-center shadow-md shadow-emerald-500/30">
                        <CheckCircle2 className="w-7 h-7" />
                      </div>
                      <div>
                        <div className="flex items-center space-x-2">
                          <span className="font-['Outfit'] font-black text-2xl text-emerald-600 tracking-tight">
                            ✓ AUTHENTIC GEOVERITY EVIDENCE
                          </span>
                        </div>
                        <p className="text-xs font-medium text-slate-500">
                          Digital integrity cryptographically guaranteed by Server Authority
                        </p>
                      </div>
                    </div>

                    <span className="hidden sm:inline-flex px-3 py-1 rounded-full bg-emerald-100 text-emerald-800 text-xs font-bold font-mono">
                      100% BIT MATCH
                    </span>
                  </div>

                  {/* Trusted Server Metadata Grid */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div className="bg-slate-50 p-4 rounded-2xl border border-slate-200/80 space-y-1">
                      <div className="flex items-center space-x-2 text-slate-500 text-xs font-semibold">
                        <MapPin className="w-3.5 h-3.5 text-indigo-500" />
                        <span>Verified Location</span>
                      </div>
                      <p className="font-bold text-slate-900 text-sm">{result.location}</p>
                      <p className="text-xs font-mono text-slate-500">GPS: {result.gps}</p>
                    </div>

                    <div className="bg-slate-50 p-4 rounded-2xl border border-slate-200/80 space-y-1">
                      <div className="flex items-center space-x-2 text-slate-500 text-xs font-semibold">
                        <Clock className="w-3.5 h-3.5 text-purple-500" />
                        <span>Trusted Server Timestamp</span>
                      </div>
                      <p className="font-bold text-slate-900 text-sm">
                        {new Date(result.trustedTimestamp || '').toLocaleString('en-US', { dateStyle: 'medium', timeStyle: 'medium' })}
                      </p>
                      <p className="text-xs font-mono text-slate-400">Time Authority Verified</p>
                    </div>

                    <div className="bg-slate-50 p-4 rounded-2xl border border-slate-200/80 space-y-1">
                      <div className="flex items-center space-x-2 text-slate-500 text-xs font-semibold">
                        <Smartphone className="w-3.5 h-3.5 text-cyan-500" />
                        <span>Originating Device</span>
                      </div>
                      <p className="font-bold font-mono text-xs text-slate-900">{result.deviceId}</p>
                      <p className="text-xs text-slate-400">GeoVerity Controlled Client v{result.canonicalMetadata?.appVersion}</p>
                    </div>

                    <div className="bg-slate-50 p-4 rounded-2xl border border-slate-200/80 space-y-1">
                      <div className="flex items-center space-x-2 text-slate-500 text-xs font-semibold">
                        <ShieldCheck className="w-3.5 h-3.5 text-emerald-500" />
                        <span>Cryptographic Attestation</span>
                      </div>
                      <p className="font-bold text-xs text-emerald-700">NOTARIZED AUTHENTIC EVIDENCE</p>
                      <p className="text-xs text-slate-400">ECDSA P-256 Server Authority Sealed</p>
                    </div>
                  </div>

                  {/* Cryptographic Badges */}
                  <div className="flex flex-wrap gap-3 pt-2">
                    <div className="flex items-center space-x-2 bg-indigo-50 border border-indigo-200 px-3 py-1.5 rounded-xl text-xs font-semibold text-indigo-800">
                      <ShieldCheck className="w-4 h-4 text-indigo-600" />
                      <span>Server ECDSA P-256 Signature: VALID</span>
                    </div>
                    <div className="flex items-center space-x-2 bg-emerald-50 border border-emerald-200 px-3 py-1.5 rounded-xl text-xs font-semibold text-emerald-800">
                      <Hash className="w-4 h-4 text-emerald-600" />
                      <span>SHA-256 Hash: MATCHED</span>
                    </div>
                  </div>
                </div>
              ) : (
                /* FAILURE CARD (Rose & Amber White Card) */
                <div className="bg-white rounded-3xl p-8 border-2 border-rose-500 shadow-lg shadow-rose-500/10 space-y-6">
                  <div className="flex items-center space-x-3 border-b border-slate-100 pb-5">
                    <div className="h-12 w-12 rounded-2xl bg-rose-500 text-white flex items-center justify-center shadow-md shadow-rose-500/30">
                      <XCircle className="w-7 h-7" />
                    </div>
                    <div>
                      <span className="font-['Outfit'] font-black text-2xl text-rose-600 tracking-tight">
                        ✕ NOT AUTHENTIC EVIDENCE
                      </span>
                      <p className="text-xs font-medium text-slate-500">
                        Cryptographic verification failed. Photograph has been altered or is unregistered.
                      </p>
                    </div>
                  </div>

                  <div className="bg-rose-50 border border-rose-200 p-5 rounded-2xl space-y-2">
                    <p className="text-xs font-bold uppercase tracking-wider text-rose-800 font-['Outfit']">Failure Reason</p>
                    <p className="text-sm font-semibold text-rose-900 leading-relaxed">{result.failureReason}</p>
                  </div>

                  <div className="text-xs text-slate-500 space-y-1">
                    <p className="font-semibold text-slate-700">Possible Causes:</p>
                    <ul className="list-disc list-inside space-y-1 pl-1">
                      <li>Image was cropped, resized, or modified in photo editing software</li>
                      <li>Image was sent over standard messaging (e.g., WhatsApp Photo mode) which re-encoded the pixels</li>
                      <li>Verification ID was deleted or missing from image byte markers</li>
                      <li>Capture failed server monotonic time validation (TIME_ANOMALY)</li>
                    </ul>
                  </div>
                </div>
              )}

              {/* Cryptographic Execution Trail */}
              {result.verificationSteps && result.verificationSteps.length > 0 && (
                <div className="bg-white rounded-3xl p-6 border border-slate-200 shadow-sm space-y-3">
                  <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500 font-['Outfit'] flex items-center space-x-2">
                    <FileText className="w-4 h-4 text-indigo-500" />
                    <span>Cryptographic Verification Audit Trail</span>
                  </h4>
                  <div className="bg-slate-900 rounded-2xl p-4 text-slate-200 font-mono text-xs space-y-2 overflow-x-auto">
                    {result.verificationSteps.map((step, idx) => (
                      <div key={idx} className="flex items-start space-x-2">
                        <span className="text-indigo-400 font-bold select-none">[{idx + 1}]</span>
                        <span className={step.startsWith('FAILED') || step.startsWith('REJECTED') ? 'text-rose-400 font-bold' : step.startsWith('SUCCESS') ? 'text-emerald-400 font-bold' : 'text-slate-300'}>
                          {step}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

            </div>
          )}

          {/* Initial Blank State Helper */}
          {!result && !isVerifying && !error && (
            <div className="bg-white rounded-3xl p-10 border border-slate-200 shadow-sm text-center space-y-4">
              <div className="h-16 w-16 mx-auto rounded-3xl bg-indigo-50 text-indigo-600 flex items-center justify-center">
                <ShieldCheck className="w-8 h-8" />
              </div>
              <h3 className="text-lg font-bold text-slate-800 font-['Outfit']">Ready for Evidence Verification</h3>
              <p className="text-xs text-slate-500 max-w-md mx-auto leading-relaxed">
                Select an original GeoVerity JPEG file or click one of the demo scenarios above. 
                The backend server will perform authoritative ECDSA P-256 verification and composite SHA-256 recalculation.
              </p>
            </div>
          )}

        </div>

      </div>
    </div>
  );
};
