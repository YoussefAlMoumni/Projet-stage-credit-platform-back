package com.talan.creditplatform.repository;

import com.talan.creditplatform.model.entity.AiPrompt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiPromptRepository extends JpaRepository<AiPrompt, Long> {
    Optional<AiPrompt> findFirstByAiModelIdOrderByUpdatedAtDesc(Long aiModelId);
}
