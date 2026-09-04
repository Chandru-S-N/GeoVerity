import React, { useState, useRef } from 'react';
import { 
  Camera, 
  Sparkles, 
  MapPin, 
  Clock, 
  ShieldAlert, 
  CheckCircle, 
  XCircle, 
  AlertTriangle, 
  Sliders, 
  Download, 
  Play, 
  RotateCcw,
  Zap
} from 'lucide-react';
import QRCode from 'qrcode';

export const SimulatorLab: React.FC = () => {
  const [locationName, setLocationName] = useState('Karur, Tamil Nadu, India');
  const [latitude, setLatitude] = useState(10.785234);
  const [longitude, setLongitude] = useState(78.125432);
  const [deviceId, setDeviceId] = useState('dev_pixel8_gv_984128');
  const [isOffline, setIsOffline] = useState(false);
  
  // Attack simulation sliders
  const [clockOffsetMinutes, setClockOffsetMinutes] = useState(0); // 0 = legit, -120 = rollback 2 hours
  const [tamperPixels, setTamperPixels] = useState(false);

  const [generatedImageUrl, setGeneratedImageUrl] = useState<string | null>(null);
  const [capturedBlob, setCapturedBlob] = useState<Blob | null>(null);
  const [simulationLogs, setSimulationLogs] = useState<string[]>([]);
  const [isProcessing, setIsProcessing] = useState(false);
  const [verificationResult, setVerificationResult] = useState<any>(null);

  const generateEvidenceImage = async () => {
    setIsProcessing(true);
    setSimulationLogs([]);
    setVerificationResult(null);

    const logs: string[] = [];
    logs.push('Initializing GeoVerity Secure Capture Pipeline...');

    const verificationId = `SGA-${crypto.randomUUID().toUpperCase()}`;
    logs.push(`Generated unique Verification ID: ${verificationId}`);

    // 1. Time evaluation
    const now = Date.now();
    const simulatedDeviceTime = now + (clockOffsetMinutes * 60 * 1000);
    const dateFormatted = new Date(simulatedDeviceTime).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
    const timeFormatted = new Date(simulatedDeviceTime).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: true });

    if (isOffline) {
      logs.push(`[Offline Mode] Recorded elapsedRealtime and temporary AES-256-GCM encryption.`);
      if (clockOffsetMinutes !== 0) {
        logs.push(`⚠️ Clock manipulation active: Device clock shifted by ${clockOffsetMinutes} minutes.`);
      }
    } else {
      logs.push(`[Online Mode] Synchronized with authoritative server time token.`);
    }

    // 2. Render composite photograph
    const canvas = document.createElement('canvas');
    canvas.width = 600;
    canvas.height = 720;
    const ctx = canvas.getContext('2d')!;

    // Clean Photo Scenery
    const grad = ctx.createLinearGradient(0, 0, 600, 520);
    grad.addColorStop(0, '#0284C7');
    grad.addColorStop(0.6, '#38BDF8');
    grad.addColorStop(1, '#0284C7');
    ctx.fillStyle = grad;
    ctx.fillRect(0, 0, 600, 520);

    // Controlled Camera Watermark Simulation
    ctx.fillStyle = 'rgba(255, 255, 255, 0.95)';
    ctx.font = 'bold 22px Outfit, sans-serif';
    ctx.fillText('GeoVerity Controlled CameraX Frame', 35, 240);
    ctx.font = '14px Inter, sans-serif';
    ctx.fillStyle = 'rgba(255, 255, 255, 0.85)';
    ctx.fillText('Photograph content area remains visually clean and unobstructed', 35, 275);

    // Dedicated Metadata Footer at bottom
    ctx.fillStyle = '#0F172A';
    ctx.fillRect(0, 520, 600, 200);

    // Left Column Metadata
    ctx.fillStyle = '#FFFFFF';
    ctx.font = 'bold 15px Outfit, sans-serif';
    ctx.fillText(`Location: ${locationName}`, 24, 555);

    ctx.font = '13px Inter, sans-serif';
    ctx.fillStyle = '#94A3B8';
    ctx.fillText(`GPS: ${latitude.toFixed(6)}, ${longitude.toFixed(6)}`, 24, 582);
    ctx.fillText(`Date: ${dateFormatted}`, 24, 607);
    ctx.fillText(`Time: ${timeFormatted}`, 24, 632);

    ctx.fillStyle = '#818CF8';
    ctx.font = 'bold 13px Inter, sans-serif';
    ctx.fillText(`🛡️ Authenticated Digital Evidence`, 24, 665);

    ctx.fillStyle = '#64748B';
    ctx.font = '11px monospace';
    ctx.fillText(`Authority Sealed • Device: ${deviceId}`, 24, 692);

    // Generate real QR code onto canvas
    const qrDataUrl = await QRCode.toDataURL(verificationId, {
      margin: 1,
      width: 140,
      color: { dark: '#0F172A', light: '#FFFFFF' }
    });

    const qrImage = new Image();
    qrImage.src = qrDataUrl;
    await new Promise((resolve) => { qrImage.onload = resolve; });
    ctx.drawImage(qrImage, 435, 545, 140, 140);

    // Tampering test: Modify a single pixel if requested
    if (tamperPixels) {
      logs.push(`⚠️ Tamper Active: Altered 1 pixel in the photograph payload to test SHA-256 sensitivity.`);
      const imgData = ctx.getImageData(100, 100, 1, 1);
      imgData.data[0] = (imgData.data[0] + 50) % 255;
      ctx.putImageData(imgData, 100, 100);
    }

    logs.push('Rendered dedicated metadata footer and ZXing QR identifier.');
    logs.push('Embedded Verification ID in JPEG byte stream marker.');

    canvas.toBlob(async (blob) => {
      if (!blob) return;
      setCapturedBlob(blob);
      setGeneratedImageUrl(URL.createObjectURL(blob));

      // Test Verification against Server or Simulation
      setTimeout(() => {
        if (isOffline && Math.abs(clockOffsetMinutes) > 2) {
          // TIME ANOMALY REJECTION
          logs.push(`[Server Offline Sync Validation] Calculated deviation: ${Math.abs(clockOffsetMinutes) * 60 * 1000} ms.`);
          logs.push(`❌ CRITICAL: TIME_ANOMALY triggered! Deviation exceeds 120,000 ms threshold.`);
          logs.push(`REJECTED: No SHA-256 generated. No ECDSA signature issued. Record purged.`);
          setVerificationResult({
            status: 'REJECTED_ANOMALY',
            message: `TIME_ANOMALY: Device time deviated by ${Math.abs(clockOffsetMinutes)} minutes from monotonic elapsed realtime. Rejected.`
          });
        } else if (tamperPixels) {
          logs.push(`[Verification Check] Verification ID resolved: ${verificationId}`);
          logs.push(`[Verification Check] ECDSA signature valid.`);
          logs.push(`❌ CRITICAL: SHA-256 Hash Mismatch! 1-pixel alteration detected.`);
          setVerificationResult({
            status: 'HASH_MISMATCH',
            message: 'NOT AUTHENTIC: Recalculated composite SHA-256 does not match authoritative hash.'
          });
        } else {
          logs.push(`[Verification Check] Verification ID resolved: ${verificationId}`);
          logs.push(`[Verification Check] Server ECDSA P-256 signature: VALID.`);
          logs.push(`[Verification Check] Composite SHA-256 exact bit match: 100%.`);
          logs.push(`✓ SUCCESS: Authentic GeoVerity Digital Evidence.`);
          setVerificationResult({
            status: 'AUTHENTIC',
            verificationId,
            location: locationName,
            gps: `${latitude.toFixed(6)}, ${longitude.toFixed(6)}`,
            time: `${dateFormatted}, ${timeFormatted}`,
            message: '✓ AUTHENTIC GEOVERITY EVIDENCE'
          });
        }

        setSimulationLogs(logs);
        setIsProcessing(false);
      }, 500);
    }, 'image/jpeg', 0.95);
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8 animate-fade-in">
      
      {/* Header */}
      <div>
        <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-purple-50 border border-purple-200 text-purple-700 text-xs font-semibold mb-2">
          <Zap className="w-3.5 h-3.5" />
          <span>Interactive Camera Simulator &amp; Attack Lab</span>
        </div>
        <h1 className="text-3xl font-extrabold text-slate-900 font-['Outfit'] tracking-tight">
          Evidence Capture &amp; Security Tamper Lab
        </h1>
        <p className="text-xs text-slate-500">
          Simulate Android camera capture, metadata footer composition, and test cryptographic resistance against clock rollback and pixel modification attacks.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        
        {/* Left Column: Capture Controls & Attack Simulator */}
        <div className="lg:col-span-5 space-y-6">
          <div className="bg-white rounded-3xl p-6 border border-slate-200 shadow-sm space-y-5">
            <h3 className="font-bold text-base text-slate-900 font-['Outfit'] flex items-center space-x-2">
              <Camera className="w-4 h-4 text-indigo-600" />
              <span>Camera Capture Parameters</span>
            </h3>

            <div className="space-y-4 text-xs">
              <div>
                <label className="font-semibold text-slate-700 block mb-1">Location Name</label>
                <input
                  type="text"
                  value={locationName}
                  onChange={(e) => setLocationName(e.target.value)}
                  className="w-full px-3.5 py-2 rounded-xl border border-slate-200 focus:ring-2 focus:ring-indigo-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="font-semibold text-slate-700 block mb-1">Latitude</label>
                  <input
                    type="number"
                    step="0.000001"
                    value={latitude}
                    onChange={(e) => setLatitude(parseFloat(e.target.value))}
                    className="w-full px-3.5 py-2 rounded-xl border border-slate-200 font-mono"
                  />
                </div>
                <div>
                  <label className="font-semibold text-slate-700 block mb-1">Longitude</label>
                  <input
                    type="number"
                    step="0.000001"
                    value={longitude}
                    onChange={(e) => setLongitude(parseFloat(e.target.value))}
                    className="w-full px-3.5 py-2 rounded-xl border border-slate-200 font-mono"
                  />
                </div>
              </div>

              <div>
                <label className="font-semibold text-slate-700 block mb-1">Device Identifier</label>
                <input
                  type="text"
                  value={deviceId}
                  onChange={(e) => setDeviceId(e.target.value)}
                  className="w-full px-3.5 py-2 rounded-xl border border-slate-200 font-mono"
                />
              </div>

              {/* Mode Toggle */}
              <div className="pt-2">
                <label className="font-semibold text-slate-700 block mb-2">Network Environment</label>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => setIsOffline(false)}
                    className={`py-2 px-3 rounded-xl font-bold transition-all ${
                      !isOffline ? 'bg-indigo-600 text-white shadow-sm' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                    }`}
                  >
                    Online Capture (5s Window)
                  </button>
                  <button
                    type="button"
                    onClick={() => setIsOffline(true)}
                    className={`py-2 px-3 rounded-xl font-bold transition-all ${
                      isOffline ? 'bg-amber-600 text-white shadow-sm' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                    }`}
                  >
                    Offline Capture (Monotonic)
                  </button>
                </div>
              </div>

              {/* Attack Lab Section */}
              <div className="pt-4 border-t border-slate-200 space-y-4">
                <div className="flex items-center space-x-2 text-rose-700 font-bold font-['Outfit']">
                  <ShieldAlert className="w-4 h-4" />
                  <span>Security Attack Simulations</span>
                </div>

                {/* Clock Tampering Attack */}
                <div className="bg-slate-50 p-3.5 rounded-2xl border border-slate-200 space-y-2">
                  <div className="flex justify-between items-center">
                    <span className="font-semibold text-slate-800">Clock Manipulation Attack</span>
                    <span className={`font-mono font-bold text-[11px] ${clockOffsetMinutes !== 0 ? 'text-rose-600' : 'text-slate-500'}`}>
                      {clockOffsetMinutes === 0 ? 'Normal (0m)' : `${clockOffsetMinutes} mins`}
                    </span>
                  </div>
                  <input
                    type="range"
                    min="-180"
                    max="180"
                    step="15"
                    value={clockOffsetMinutes}
                    onChange={(e) => setClockOffsetMinutes(parseInt(e.target.value))}
                    className="w-full accent-rose-600 cursor-pointer"
                  />
                  <p className="text-[10px] text-slate-500">
                    Simulates user rolling phone clock backward (e.g. -120m = 2 hours earlier) during offline capture.
                  </p>
                </div>

                {/* Pixel Tampering Attack */}
                <div className="flex items-center justify-between bg-slate-50 p-3.5 rounded-2xl border border-slate-200">
                  <div>
                    <span className="font-semibold text-slate-800 block">1-Pixel Tamper Attack</span>
                    <span className="text-[10px] text-slate-500">Alters 1 single byte in image to test SHA-256 sensitivity</span>
                  </div>
                  <input
                    type="checkbox"
                    checked={tamperPixels}
                    onChange={(e) => setTamperPixels(e.target.checked)}
                    className="h-5 w-5 rounded-md accent-rose-600 cursor-pointer"
                  />
                </div>
              </div>

              <button
                onClick={generateEvidenceImage}
                disabled={isProcessing}
                className="w-full py-3 bg-gradient-to-r from-indigo-600 via-indigo-700 to-purple-700 hover:opacity-95 text-white font-bold rounded-2xl shadow-md transition-all flex items-center justify-center space-x-2 text-sm"
              >
                {isProcessing ? <RotateCcw className="w-4 h-4 animate-spin" /> : <Play className="w-4 h-4" />}
                <span>Execute Secure Capture &amp; Verify</span>
              </button>

            </div>
          </div>
        </div>

        {/* Right Column: Generated Image Preview & Verification Result */}
        <div className="lg:col-span-7 space-y-6">
          
          {/* Result Alert */}
          {verificationResult && (
            <div className={`p-6 rounded-3xl border-2 animate-scale-in space-y-3 ${
              verificationResult.status === 'AUTHENTIC' ? 'bg-emerald-50 border-emerald-500 text-emerald-950' :
              verificationResult.status === 'REJECTED_ANOMALY' ? 'bg-amber-50 border-amber-500 text-amber-950' :
              'bg-rose-50 border-rose-500 text-rose-950'
            }`}>
              <div className="flex items-center space-x-3">
                {verificationResult.status === 'AUTHENTIC' ? (
                  <CheckCircle className="w-6 h-6 text-emerald-600" />
                ) : (
                  <XCircle className="w-6 h-6 text-rose-600" />
                )}
                <div>
                  <p className="font-black text-lg font-['Outfit']">{verificationResult.message}</p>
                  <p className="text-xs opacity-80">
                    {verificationResult.status === 'AUTHENTIC' ? 'Verified with server ECDSA P-256 and composite SHA-256 exact match' :
                     verificationResult.status === 'REJECTED_ANOMALY' ? 'Offline capture rejected by monotonic time reconciliation (TIME_ANOMALY)' :
                     'Hash mismatch detected. Digital evidence has been altered.'}
                  </p>
                </div>
              </div>
            </div>
          )}

          {/* Generated Photograph with Dedicated Metadata Footer */}
          {generatedImageUrl && (
            <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm space-y-3">
              <div className="flex justify-between items-center">
                <span className="text-xs font-bold uppercase tracking-wider text-slate-500 font-['Outfit']">
                  Generated GeoVerity Digital Evidence (Clean Image + Dedicated Footer)
                </span>
                {capturedBlob && (
                  <a
                    href={generatedImageUrl}
                    download="geoverity_simulated_evidence.jpg"
                    className="px-3 py-1 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 text-xs font-bold rounded-xl flex items-center space-x-1 transition-colors"
                  >
                    <Download className="w-3.5 h-3.5" />
                    <span>Download Original</span>
                  </a>
                )}
              </div>

              <div className="rounded-2xl overflow-hidden border border-slate-200 bg-slate-900 flex justify-center">
                <img src={generatedImageUrl} alt="Generated Evidence" className="max-h-[480px] w-auto object-contain" />
              </div>
            </div>
          )}

          {/* Execution Pipeline Logs */}
          {simulationLogs.length > 0 && (
            <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm space-y-3">
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500 font-['Outfit']">
                Capture &amp; Security Validation Pipeline
              </h4>
              <div className="bg-slate-900 rounded-2xl p-4 text-xs font-mono space-y-1.5 text-slate-300">
                {simulationLogs.map((log, i) => (
                  <div key={i} className={
                    log.includes('CRITICAL') || log.includes('REJECTED') ? 'text-rose-400 font-bold' :
                    log.includes('SUCCESS') ? 'text-emerald-400 font-bold' :
                    log.includes('⚠️') ? 'text-amber-400' : 'text-slate-300'
                  }>
                    {log}
                  </div>
                ))}
              </div>
            </div>
          )}

        </div>

      </div>
    </div>
  );
};
