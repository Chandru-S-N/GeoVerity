package org.geoverity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "verification_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "verification_id", nullable = false, unique = true, length = 64)
    private String verificationId;

    @Column(name = "sha256_hash", nullable = false, length = 64)
    private String sha256Hash;

    @Column(name = "canonical_metadata", nullable = false, columnDefinition = "TEXT")
    private String canonicalMetadata;

    @Column(name = "trusted_server_timestamp", nullable = false)
    private Instant trustedServerTimestamp;

    @Column(name = "ecdsa_signature", nullable = false, columnDefinition = "TEXT")
    private String ecdsaSignature;

    @Column(name = "device_id", nullable = false, length = 128)
    private String deviceId;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "api_client_id", nullable = false)
    private ApiClient apiClient;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "AUTHENTICATED"; // AUTHENTICATED, REVOKED

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
