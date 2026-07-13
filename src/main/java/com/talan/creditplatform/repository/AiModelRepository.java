package com.talan.creditplatform.repository;

import com.talan.creditplatform.model.entity.AiModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiModelRepository extends JpaRepository<AiModel, Long> {
    Optional<AiModel> findFirstByStageNameAndActiveTrue(String stageName);
}
