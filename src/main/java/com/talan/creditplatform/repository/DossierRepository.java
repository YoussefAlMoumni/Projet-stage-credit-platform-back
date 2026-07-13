package com.talan.creditplatform.repository;

import com.talan.creditplatform.model.entity.Dossier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DossierRepository extends JpaRepository<Dossier, Long> {
    Optional<Dossier> findBySiren(String siren);
}
