package org.geoverity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType; // CAPTURE_REQUEST, AUTHENTICATION_SUCCESS, AUTHENTICATION_FAILURE, OFFLINE_SYNC, TIME_ANOMALY, VERIFY_SUCCESS, VERIFY_FAILURE, API_KEY_CREATED, API_KEY_REVOKED, API_KEY_ROTATED

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "api_client_id")
    private UUID apiClientId;

    @Column(name = "verification_id", length = 64)
    private String verificationId;

    @Column(nullable = false, length = 32)
    private String status; // SUCCESS, FAILURE, REJECTED, ANOMALY

    @Column(columnDefinition = "TEXT")
    private String details;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
