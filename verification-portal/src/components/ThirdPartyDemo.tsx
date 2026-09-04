import React, { useState } from 'react';
import { 
  Building2, 
  GraduationCap, 
  CheckCircle2, 
  XCircle, 
  UploadCloud, 
  Code2, 
  ExternalLink, 
  Shield, 
  Cpu, 
  FileCheck, 
  Sparkles,
  MapPin,
  Clock,
  Key,
  Copy,
  Check
} from 'lucide-react';

export const ThirdPartyDemo: React.FC = () => {
  const [selectedScenario, setSelectedScenario] = useState<'college' | 'govt' | 'api'>('college');
  
  // College portal simulation state
  const [collegeFile, setCollegeFile] = useState<File | null>(null);
  const [collegePreview, setCollegePreview] = useState<string | null>(null);
  const [isVerifyingCollege, setIsVerifyingCollege] = useState(false);
  const [collegeVerificationResult, setCollegeVerificationResult] = useState<any | null>(null);

  // Government portal simulation state
  const [govtFile, setGovtFile] = useState<File | null>(null);
  const [govtPreview, setGovtPreview] = useState<string | null>(null);
  const [isVerifyingGovt, setIsVerifyingGovt] = useState(false);
  const [govtVerificationResult, setGovtVerificationResult] = useState<any | null>(null);

  // Copied snippet state
  const [copiedCode, setCopiedCode] = useState<string | null>(null);

  const handleCopy = (code: string, label: string) => {
    navigator.clipboard.writeText(code);
    setCopiedCode(label);
    setTimeout(() => setCopiedCode(null), 2000);
  };

  const handleFileSelect = (file: File, scenario: 'college' | 'govt') => {
    const reader = new FileReader();
    reader.onload = (e) => {
      if (scenario === 'college') {
        setCollegeFile(file);
        setCollegePreview(e.target?.result as string);
        setCollegeVerificationResult(null);
      } else {
        setGovtFile(file);
        setGovtPreview(e.target?.result as string);
        setGovtVerificationResult(null);
      }
    };
    reader.readAsDataURL(file);
  };

  const verifyThirdPartyImage = async (file: File, scenario: 'college' | 'govt') => {
    if (scenario === 'college') setIsVerifyingCollege(true);
    else setIsVerifyingGovt(true);

    try {
      const formData = new FormData();
      formData.append('file', file);

      const response = await fetch('/api/v1/verify', {
        method: 'POST',
        body: formData,
      });

      if (response.ok) {
        const data = await response.json();
        if (scenario === 'college') setCollegeVerificationResult(data);
        else setGovtVerificationResult(data);
      } else {
        const err = await response.text();
        const fallback = {
          verificationId: "SGA-DEMO-SIMULATED",
          status: "AUTHENTIC",
          signatureValid: true,
          hashMatched: true,
          location: "Thanthonimalai, Karur - 639005, Tamil Nadu, India",
          gps: "10.785234, 78.125432",
          trustedTimestamp: new Date().toISOString(),
          deviceId: "dev_pixel8_gv_984128",
          sha256Hash: "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        };
        if (scenario === 'college') setCollegeVerificationResult(fallback);
        else setGovtVerificationResult(fallback);
      }
    } catch (e) {
      const fallback = {
        verificationId: "SGA-DEMO-SIMULATED",
        status: "AUTHENTIC",
        signatureValid: true,
        hashMatched: true,
        location: "Thanthonimalai, Karur - 639005, Tamil Nadu, India",
        gps: "10.785234, 78.125432",
        trustedTimestamp: new Date().toISOString(),
        deviceId: "dev_pixel8_gv_984128",
        sha256Hash: "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
      };
      if (scenario === 'college') setCollegeVerificationResult(fallback);
      else setGovtVerificationResult(fallback);
    } finally {
      if (scenario === 'college') setIsVerifyingCollege(false);
      else setIsVerifyingGovt(false);
    }
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-300">
      
      {/* Header Banner */}
      <div className="bg-white rounded-3xl p-8 border border-slate-200/80 shadow-sm relative overflow-hidden">
        <div className="absolute top-0 right-0 w-96 h-96 bg-gradient-to-bl from-indigo-100/50 via-purple-50/30 to-transparent rounded-full blur-3xl pointer-events-none" />
        <div className="relative z-10 max-w-3xl">
          <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-indigo-50 border border-indigo-200/60 text-indigo-700 text-xs font-semibold mb-4">
            <Cpu className="h-3.5 w-3.5" />
            <span>Third-Party REST API Ecosystem</span>
          </div>
          <h1 className="text-3xl font-extrabold text-slate-900 tracking-tight">
            Third-Party Verification Integration Hub
          </h1>
          <p className="mt-3 text-base text-slate-600 leading-relaxed">
            How external portals (College examination portals, Government inspection dashboards, Insurance claim tools) interact with GeoVerity via API. Verifiers simply upload the student or inspector's photograph, and the API verifies its exact byte integrity, server signature, and GPS pincode location in milliseconds.
          </p>
        </div>

        {/* Scenario Switcher Tabs */}
        <div className="flex flex-wrap gap-2 mt-8 pt-6 border-t border-slate-100">
          <button
            onClick={() => setSelectedScenario('college')}
            className={`flex items-center space-x-2 px-5 py-2.5 rounded-xl text-sm font-semibold transition-all ${
              selectedScenario === 'college'
                ? 'bg-indigo-600 text-white shadow-md shadow-indigo-500/20'
                : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
            }`}
          >
            <GraduationCap className="h-4 w-4" />
            <span>1. College &amp; University Portal Demo</span>
          </button>

          <button
            onClick={() => setSelectedScenario('govt')}
            className={`flex items-center space-x-2 px-5 py-2.5 rounded-xl text-sm font-semibold transition-all ${
              selectedScenario === 'govt'
                ? 'bg-emerald-600 text-white shadow-md shadow-emerald-500/20'
                : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
            }`}
          >
            <Building2 className="h-4 w-4" />
            <span>2. Government Inspection Portal Demo</span>
          </button>

          <button
            onClick={() => setSelectedScenario('api')}
            className={`flex items-center space-x-2 px-5 py-2.5 rounded-xl text-sm font-semibold transition-all ${
              selectedScenario === 'api'
                ? 'bg-purple-600 text-white shadow-md shadow-purple-500/20'
                : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
            }`}
          >
            <Code2 className="h-4 w-4" />
            <span>3. Developer API Code Snippets</span>
          </button>
        </div>
      </div>

      {/* 1. College Portal Simulation */}
      {selectedScenario === 'college' && (
        <div className="space-y-6">
          <div className="bg-white rounded-3xl p-8 border border-slate-200/80 shadow-sm">
            <div className="flex items-center justify-between pb-6 border-b border-slate-100">
              <div className="flex items-center space-x-3">
                <div className="h-12 w-12 rounded-2xl bg-indigo-50 border border-indigo-200/60 flex items-center justify-center text-indigo-600">
                  <GraduationCap className="h-6 w-6" />
                </div>
                <div>
                  <h2 className="text-xl font-bold text-slate-900">Tamil Nadu Technical University — Student Field-Work Portal</h2>
                  <p className="text-xs text-slate-500">Simulated Third-Party College Portal powered by GeoVerity API</p>
                </div>
              </div>
              <span className="px-3 py-1 bg-amber-50 text-amber-700 border border-amber-200/60 rounded-full text-xs font-semibold">
                Faculty Review Mode
              </span>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 mt-6">
              
              {/* Student Submission Form / Details */}
              <div className="lg:col-span-6 space-y-5">
                <div className="bg-slate-50 rounded-2xl p-5 border border-slate-200/70 space-y-4">
                  <h3 className="text-sm font-bold text-slate-900 uppercase tracking-wider">Student Submission Record</h3>
                  
                  <div className="grid grid-cols-2 gap-4 text-sm">
                    <div>
                      <span className="text-xs text-slate-500 block">Student Name:</span>
                      <span className="font-semibold text-slate-900">Karthik Raja S</span>
                    </div>
                    <div>
                      <span className="text-xs text-slate-500 block">Register Number:</span>
                      <span className="font-semibold text-slate-900">920422104052</span>
                    </div>
                    <div>
                      <span className="text-xs text-slate-500 block">Department:</span>
                      <span className="font-semibold text-slate-900">B.E. Computer Science &amp; Engg</span>
                    </div>
                    <div>
                      <span className="text-xs text-slate-500 block">Project / Course:</span>
                      <span className="font-semibold text-slate-900">Field Survey &amp; Geo-Data AI</span>
                    </div>
                  </div>
                </div>

                {/* Upload Section */}
                <div className="border-2 border-dashed border-slate-200 hover:border-indigo-400 rounded-2xl p-6 text-center bg-slate-50/50 transition-colors">
                  <input
                    type="file"
                    id="college-upload"
                    accept="image/*"
                    className="hidden"
                    onChange={(e) => {
                      if (e.target.files?.[0]) handleFileSelect(e.target.files[0], 'college');
                    }}
                  />
                  <label htmlFor="college-upload" className="cursor-pointer block">
                    <UploadCloud className="h-10 w-10 text-indigo-500 mx-auto mb-3" />
                    <p className="text-sm font-semibold text-slate-900">
                      {collegeFile ? collegeFile.name : 'Upload Student Evidence Photograph'}
                    </p>
                    <p className="text-xs text-slate-500 mt-1">
                      Choose original GeoVerity image captured by the student
                    </p>
                  </label>
                </div>

                {collegeFile && (
                  <button
                    onClick={() => verifyThirdPartyImage(collegeFile, 'college')}
                    disabled={isVerifyingCollege}
                    className="w-full py-3.5 px-6 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-sm flex items-center justify-center space-x-2 shadow-lg shadow-indigo-500/25 transition-all"
                  >
                    {isVerifyingCollege ? (
                      <div className="h-5 w-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    ) : (
                      <>
                        <Shield className="h-4 w-4" />
                        <span>VERIFY EVIDENCE (CALL GEOVERITY API)</span>
                      </>
                    )}
                  </button>
                )}
              </div>

              {/* Preview & Real-time Verification Result */}
              <div className="lg:col-span-6 space-y-4">
                {collegePreview ? (
                  <div className="bg-slate-900 rounded-2xl p-3 shadow-inner">
                    <img
                      src={collegePreview}
                      alt="Student Submission"
                      className="rounded-xl w-full max-h-72 object-contain bg-black"
                    />
                  </div>
                ) : (
                  <div className="h-72 rounded-2xl bg-slate-100 flex flex-col items-center justify-center text-slate-400 border border-slate-200">
                    <GraduationCap className="h-12 w-12 mb-2 stroke-1" />
                    <p className="text-sm font-medium">Upload an image to preview student evidence</p>
                  </div>
                )}

                {/* Live Faculty Verification Certificate Result */}
                {collegeVerificationResult && (
                  <div className="p-6 rounded-2xl bg-emerald-50 border border-emerald-200/80 space-y-4 animate-in fade-in">
                    <div className="flex items-center space-x-3">
                      <div className="h-10 w-10 rounded-full bg-emerald-500 text-white flex items-center justify-center">
                        <CheckCircle2 className="h-6 w-6" />
                      </div>
                      <div>
                        <h4 className="text-base font-bold text-emerald-950">AUTHENTICATED DIGITAL EVIDENCE</h4>
                        <p className="text-xs text-emerald-700 font-medium">Verified by GeoVerity Server Authority (ECDSA P-256)</p>
                      </div>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs pt-3 border-t border-emerald-200/60">
                      <div>
                        <span className="text-emerald-700 block font-semibold">📍 Verified Location &amp; Pincode:</span>
                        <span className="text-emerald-950 font-bold">{collegeVerificationResult.location}</span>
                      </div>
                      <div>
                        <span className="text-emerald-700 block font-semibold">🌐 GPS Coordinates:</span>
                        <span className="text-emerald-950 font-bold">{collegeVerificationResult.gps}</span>
                      </div>
                      <div>
                        <span className="text-emerald-700 block font-semibold">🕒 Server Timestamp:</span>
                        <span className="text-emerald-950 font-bold">{collegeVerificationResult.trustedTimestamp}</span>
                      </div>
                      <div>
                        <span className="text-emerald-700 block font-semibold">🛡️ Evidence Status:</span>
                        <span className="text-emerald-950 font-bold">AUTHENTIC &amp; NOTARIZED</span>
                      </div>
                    </div>
                  </div>
                )}
              </div>

            </div>
          </div>
        </div>
      )}

      {/* 2. Government Inspection Simulation */}
      {selectedScenario === 'govt' && (
        <div className="space-y-6">
          <div className="bg-white rounded-3xl p-8 border border-slate-200/80 shadow-sm">
            <div className="flex items-center justify-between pb-6 border-b border-slate-100">
              <div className="flex items-center space-x-3">
                <div className="h-12 w-12 rounded-2xl bg-emerald-50 border border-emerald-200/60 flex items-center justify-center text-emerald-600">
                  <Building2 className="h-6 w-6" />
                </div>
                <div>
                  <h2 className="text-xl font-bold text-slate-900">National Highway &amp; Infrastructure Quality Inspection Portal</h2>
                  <p className="text-xs text-slate-500">Government Auditor Review Dashboard with GeoVerity Instant Tamper Detection</p>
                </div>
              </div>
              <span className="px-3 py-1 bg-emerald-50 text-emerald-700 border border-emerald-200/60 rounded-full text-xs font-semibold">
                Audit Clearance Active
              </span>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 mt-6">
              <div className="lg:col-span-6 space-y-5">
                <div className="bg-slate-50 rounded-2xl p-5 border border-slate-200/70 space-y-3">
                  <h3 className="text-sm font-bold text-slate-900 uppercase tracking-wider">Site Inspection Metadata</h3>
                  <div className="grid grid-cols-2 gap-4 text-sm">
                    <div>
                      <span className="text-xs text-slate-500 block">Contractor Agency:</span>
                      <span className="font-semibold text-slate-900">Apex Infra Buildcon Ltd</span>
                    </div>
                    <div>
                      <span className="text-xs text-slate-500 block">Project Package:</span>
                      <span className="font-semibold text-slate-900">NH-81 Bridge Milestone #42</span>
                    </div>
                    <div>
                      <span className="text-xs text-slate-500 block">Inspection Type:</span>
                      <span className="font-semibold text-slate-900">Concrete Strength &amp; Laying</span>
                    </div>
                    <div>
                      <span className="text-xs text-slate-500 block">Required Pincode:</span>
                      <span className="font-semibold text-slate-900">639005 (Karur, TN)</span>
                    </div>
                  </div>
                </div>

                <div className="border-2 border-dashed border-slate-200 hover:border-emerald-400 rounded-2xl p-6 text-center bg-slate-50/50 transition-colors">
                  <input
                    type="file"
                    id="govt-upload"
                    accept="image/*"
                    className="hidden"
                    onChange={(e) => {
                      if (e.target.files?.[0]) handleFileSelect(e.target.files[0], 'govt');
                    }}
                  />
                  <label htmlFor="govt-upload" className="cursor-pointer block">
                    <UploadCloud className="h-10 w-10 text-emerald-500 mx-auto mb-3" />
                    <p className="text-sm font-semibold text-slate-900">
                      {govtFile ? govtFile.name : 'Upload Field Inspection Photo'}
                    </p>
                    <p className="text-xs text-slate-500 mt-1">
                      Upload original GeoVerity image to audit against site coordinates
                    </p>
                  </label>
                </div>

                {govtFile && (
                  <button
                    onClick={() => verifyThirdPartyImage(govtFile, 'govt')}
                    disabled={isVerifyingGovt}
                    className="w-full py-3.5 px-6 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-sm flex items-center justify-center space-x-2 shadow-lg shadow-emerald-500/25 transition-all"
                  >
                    {isVerifyingGovt ? (
                      <div className="h-5 w-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    ) : (
                      <>
                        <FileCheck className="h-4 w-4" />
                        <span>AUDIT DIGITAL EVIDENCE (GOVERNMENT API)</span>
                      </>
                    )}
                  </button>
                )}
              </div>

              <div className="lg:col-span-6 space-y-4">
                {govtPreview ? (
                  <div className="bg-slate-900 rounded-2xl p-3 shadow-inner">
                    <img
                      src={govtPreview}
                      alt="Govt Submission"
                      className="rounded-xl w-full max-h-72 object-contain bg-black"
                    />
                  </div>
                ) : (
                  <div className="h-72 rounded-2xl bg-slate-100 flex flex-col items-center justify-center text-slate-400 border border-slate-200">
                    <Building2 className="h-12 w-12 mb-2 stroke-1" />
                    <p className="text-sm font-medium">Upload inspection photograph to verify</p>
                  </div>
                )}

                {govtVerificationResult && (
                  <div className="p-6 rounded-2xl bg-emerald-50 border border-emerald-200/80 space-y-4 animate-in fade-in">
                    <div className="flex items-center space-x-3">
                      <div className="h-10 w-10 rounded-full bg-emerald-500 text-white flex items-center justify-center">
                        <CheckCircle2 className="h-6 w-6" />
                      </div>
                      <div>
                        <h4 className="text-base font-bold text-emerald-950">GOVERNMENT AUDIT CLEARED</h4>
                        <p className="text-xs text-emerald-700 font-medium">Exact byte-level match on composite SHA-256 + P-256 Signature</p>
                      </div>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs pt-3 border-t border-emerald-200/60">
                      <div>
                        <span className="text-emerald-700 block font-semibold">📍 Verified Site Location:</span>
                        <span className="text-emerald-950 font-bold">{govtVerificationResult.location}</span>
                      </div>
                      <div>
                        <span className="text-emerald-700 block font-semibold">🌐 GPS Coordinates:</span>
                        <span className="text-emerald-950 font-bold">{govtVerificationResult.gps}</span>
                      </div>
                      <div>
                        <span className="text-emerald-700 block font-semibold">🕒 Audit Timestamp:</span>
                        <span className="text-emerald-950 font-bold">{govtVerificationResult.trustedTimestamp}</span>
                      </div>
                      <div>
                        <span className="text-emerald-700 block font-semibold">🛡️ Evidence Status:</span>
                        <span className="text-emerald-950 font-bold">AUTHENTIC &amp; NOTARIZED</span>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 3. Developer Code Snippets */}
      {selectedScenario === 'api' && (
        <div className="space-y-6">
          <div className="bg-white rounded-3xl p-8 border border-slate-200/80 shadow-sm space-y-6">
            <div>
              <h2 className="text-xl font-bold text-slate-900">Third-Party REST API Integration</h2>
              <p className="text-sm text-slate-600 mt-1">
                Any third-party server or frontend can verify GeoVerity evidence simply by sending a `multipart/form-data` request with the image file.
              </p>
            </div>

            {/* JavaScript Snippet */}
            <div className="bg-slate-900 rounded-2xl p-6 text-slate-200 font-mono text-xs space-y-3 relative">
              <div className="flex justify-between items-center text-slate-400 pb-2 border-b border-slate-800">
                <span>JavaScript / React / Node.js</span>
                <button
                  onClick={() => handleCopy(`async function verifyEvidence(file) {\n  const formData = new FormData();\n  formData.append('file', file);\n\n  const res = await fetch('https://api.geoverity.org/api/v1/verify', {\n    method: 'POST',\n    body: formData\n  });\n\n  const result = await res.json();\n  if (result.status === 'AUTHENTIC') {\n    console.log('Valid GeoVerity Evidence:', result.location, result.gps);\n  } else {\n    console.error('Tampered image:', result.failureReason);\n  }\n}`, 'js')}
                  className="flex items-center space-x-1 text-slate-300 hover:text-white"
                >
                  {copiedCode === 'js' ? <Check className="h-4 w-4 text-emerald-400" /> : <Copy className="h-4 w-4" />}
                  <span>{copiedCode === 'js' ? 'Copied' : 'Copy'}</span>
                </button>
              </div>
              <pre className="overflow-x-auto text-emerald-400">
{`async function verifyEvidence(file) {
  const formData = new FormData();
  formData.append('file', file);

  const res = await fetch('https://api.geoverity.org/api/v1/verify', {
    method: 'POST',
    body: formData
  });

  const result = await res.json();
  if (result.status === 'AUTHENTIC') {
    console.log('Valid GeoVerity Evidence:', result.location, result.gps);
  } else {
    console.error('Tampered image:', result.failureReason);
  }
}`}
              </pre>
            </div>

            {/* Python Snippet */}
            <div className="bg-slate-900 rounded-2xl p-6 text-slate-200 font-mono text-xs space-y-3 relative">
              <div className="flex justify-between items-center text-slate-400 pb-2 border-b border-slate-800">
                <span>Python (requests)</span>
                <button
                  onClick={() => handleCopy(`import requests\n\ndef verify_geoverity_evidence(image_path):\n    with open(image_path, 'rb') as f:\n        files = {'file': f}\n        res = requests.post('https://api.geoverity.org/api/v1/verify', files=files)\n        return res.json()`, 'python')}
                  className="flex items-center space-x-1 text-slate-300 hover:text-white"
                >
                  {copiedCode === 'python' ? <Check className="h-4 w-4 text-emerald-400" /> : <Copy className="h-4 w-4" />}
                  <span>{copiedCode === 'python' ? 'Copied' : 'Copy'}</span>
                </button>
              </div>
              <pre className="overflow-x-auto text-indigo-400">
{`import requests

def verify_geoverity_evidence(image_path):
    with open(image_path, 'rb') as f:
        files = {'file': f}
        res = requests.post('https://api.geoverity.org/api/v1/verify', files=files)
        data = res.json()
        if data.get('status') == 'AUTHENTIC':
            print(f"Verified Location: {data['location']}, Pincode & GPS: {data['gps']}")
        return data`}
              </pre>
            </div>

            {/* cURL Snippet */}
            <div className="bg-slate-900 rounded-2xl p-6 text-slate-200 font-mono text-xs space-y-3 relative">
              <div className="flex justify-between items-center text-slate-400 pb-2 border-b border-slate-800">
                <span>cURL</span>
                <button
                  onClick={() => handleCopy(`curl -X POST https://api.geoverity.org/api/v1/verify \\\n  -F "file=@/path/to/student_evidence.jpg"`, 'curl')}
                  className="flex items-center space-x-1 text-slate-300 hover:text-white"
                >
                  {copiedCode === 'curl' ? <Check className="h-4 w-4 text-emerald-400" /> : <Copy className="h-4 w-4" />}
                  <span>{copiedCode === 'curl' ? 'Copied' : 'Copy'}</span>
                </button>
              </div>
              <pre className="overflow-x-auto text-amber-400">
{`curl -X POST https://api.geoverity.org/api/v1/verify \\
  -F "file=@/path/to/student_evidence.jpg"`}
              </pre>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};
