package com.talan.creditplatform.repository;

import com.talan.creditplatform.model.entity.Dossier;
import com.talan.creditplatform.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
