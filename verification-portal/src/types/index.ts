export interface CanonicalMetadata {
  appVersion: string;
  deviceId: string;
  latitude: number;
  locationName: string;
  longitude: number;
  trustedTimestamp: number;
  verificationId: string;
}

export interface VerificationResponse {
  verificationId?: string;
  status: 'AUTHENTIC' | 'NOT_AUTHENTIC';
  signatureValid: boolean;
  hashMatched: boolean;
  location?: string;
  gps?: string;
  trustedTimestamp?: string;
  deviceId?: string;
  sha256Hash?: string;
  canonicalMetadata?: CanonicalMetadata;
  failureReason?: string;
  verificationSteps?: string[];
}

export interface ApiClient {
  id: string;
  clientName: string;
  apiKeyPrefix: string;
  rawApiKey?: string;
  permissions: string;
  status: 'ACTIVE' | 'REVOKED' | 'DISABLED';
  createdAt: string;
  lastUsedAt?: string;
}

export interface VerificationRecordEntity {
  id: string;
  verificationId: string;
  sha256Hash: string;
  canonicalMetadata: string;
  trustedServerTimestamp: string;
  ecdsaSignature: string;
  deviceId: string;
  status: string;
  createdAt: string;
}

export interface AuditLog {
  id: string;
  eventType: string;
  clientIp?: string;
  apiClientId?: string;
  verificationId?: string;
  status: string;
  details: string;
  createdAt: string;
}

export interface AdminStats {
  totalRecords: number;
  authenticatedRecords: number;
  totalApiClients: number;
  activeApiClients: number;
  totalAuditEvents: number;
  timeAnomalies: number;
}
