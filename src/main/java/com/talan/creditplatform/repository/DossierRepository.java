package com.talan.creditplatform.repository;

import com.talan.creditplatform.model.entity.Dossier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DossierRepository extends JpaRepository<Dossier, String> {
}
