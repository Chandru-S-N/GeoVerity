package org.geoverity.repository;

import org.geoverity.entity.VerificationRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationRecordRepository extends JpaRepository<VerificationRecord, UUID> {
    Optional<VerificationRecord> findByVerificationId(String verificationId);
    Page<VerificationRecord> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByStatus(String status);
}
