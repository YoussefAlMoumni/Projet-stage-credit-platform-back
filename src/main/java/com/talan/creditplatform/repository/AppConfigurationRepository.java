package com.talan.creditplatform.repository;

import com.talan.creditplatform.model.entity.AppConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppConfigurationRepository extends JpaRepository<AppConfiguration, String> {
}
