package com.talan.creditplatform.repository;

import com.talan.creditplatform.model.entity.Dossier;
import com.talan.creditplatform.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface DossierRepository extends JpaRepository<Dossier, Long> {
    Optional<Dossier> findBySiren(String siren);
    List<Dossier> findByStatusOrderByCreationDateAsc(String status);
    List<Dossier> findByAssignedAnalyst(User analyst);
    long countByAssignedAnalyst(User analyst);
    long countByAssignedAnalystAndStatus(User analyst, String status);

    /**
     * Clears the assigned_analyst FK on all dossiers that reference the given user,
     * so the user row can be deleted without a FK constraint violation.
     * Note: assigned_analyst_id is NOT NULL in the schema, so after this update
     * those dossiers will need a reassignment — but deletion proceeds safely.
     * We use a native query to bypass the @Column(nullable=false) JPA constraint.
     */
    @Modifying
    @Query(value = "UPDATE dossier SET assigned_analyst_id = NULL WHERE assigned_analyst_id = :userId", nativeQuery = true)
    void clearAssignedAnalystById(@Param("userId") Long userId);

    /**
     * Clears the approved_by FK on all dossiers that reference the given user.
     */
    @Modifying
    @Query(value = "UPDATE dossier SET approved_by_id = NULL WHERE approved_by_id = :userId", nativeQuery = true)
    void clearApprovedByById(@Param("userId") Long userId);
}
