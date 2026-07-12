package com.talan.creditplatform.repository;

import com.talan.creditplatform.model.entity.StageResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StageResultRepository extends JpaRepository<StageResult, Long> {
    List<StageResult> findByEvaluationId(Long evaluationId);
}
