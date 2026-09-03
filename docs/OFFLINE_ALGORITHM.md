# GeoVerity - Offline Monotonic Time Algorithm

## 1. Problem Statement

When a mobile device is offline (disconnected from cellular/Wi-Fi), a user can manually alter the system wall-clock time in Android Settings (e.g. changing 2:00 PM to 12:00 PM) to forge digital evidence timestamps.

---

## 2. Monotonic Clock Defense Model

Android provides `SystemClock.elapsedRealtime()`, a hardware monotonic timer that counts milliseconds since device boot, including deep sleep. This clock **cannot be modified by user settings, timezone shifts, or NTP updates**.

### Stored Online Baseline:
When the app last communicates with the GeoVerity server, it records:
- $T_{server}$: `lastTrustedServerTimestamp` (authoritative epoch millis)
- $E_{server}$: `lastTrustedElapsedRealtime` (`SystemClock.elapsedRealtime()`)

---

## 3. Offline Capture Event

When offline capture occurs:
1. Record device wall-clock time: $T_{device}$
2. Record capture elapsed realtime: $E_{capture}$
3. Encrypt raw photo bytes locally using Android Keystore **AES-256-GCM**.
4. **Do NOT generate the final SHA-256 hash or render the footer yet.**

---

## 4. Reconciliation Upon Network Reconnection

When network connectivity returns, the client and server calculate:

$$\Delta E = E_{capture} - E_{server}$$
$$T_{expected} = T_{server} + \Delta E$$
$$\text{Deviation} = | T_{device} - T_{expected} |$$

### Decision Logic:
- **Case 1: Monotonic Regression** ($\Delta E < 0$):
  - Device rebooted or timer manipulated.
  - **Action**: Reject capture with `TIME_ANOMALY`.
- **Case 2: Deviation Exceeds Threshold** ($\text{Deviation} > 120,000\text{ ms}$):
  - Wall-clock time manipulation detected.
  - **Action**: Reject capture with `TIME_ANOMALY`. Do not sign. Purge temporary encrypted image.
- **Case 3: Deviation Within Tolerance** ($\text{Deviation} \le 120,000\text{ ms}$):
  - Authoritative capture timestamp set to $T_{expected}$.
  - Final image composed with metadata footer + QR code + embedded COM segment.
  - Compute $\text{SHA-256}(\text{finalImageBytes} + \text{canonicalMetadata})$.
  - Submit to `POST /api/v1/capture/offline-sync` for server ECDSA P-256 signature.
