package com.talan.creditplatform.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ai_model")
public class AiModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stage_name", nullable = false)
    private String stageName;

    @Column(name = "model_name", nullable = false)
    private String modelName;

    @Column(name = "context_window_size", nullable = false)
    private Integer contextWindowSize;

    @Column(nullable = false)
    private Double temperature;

    @Column(name = "keep_alive_setting", nullable = false)
    private String keepAliveSetting;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    public AiModel() {}

    public AiModel(String stageName, String modelName, Integer contextWindowSize, Double temperature, String keepAliveSetting, Boolean active) {
        this.stageName = stageName;
        this.modelName = modelName;
        this.contextWindowSize = contextWindowSize;
        this.temperature = temperature;
        this.keepAliveSetting = keepAliveSetting;
        this.active = active;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public Integer getContextWindowSize() { return contextWindowSize; }
    public void setContextWindowSize(Integer contextWindowSize) { this.contextWindowSize = contextWindowSize; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public String getKeepAliveSetting() { return keepAliveSetting; }
    public void setKeepAliveSetting(String keepAliveSetting) { this.keepAliveSetting = keepAliveSetting; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
