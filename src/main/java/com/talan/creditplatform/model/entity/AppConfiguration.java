package com.talan.creditplatform.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;

@Entity
public class AppConfiguration {

    @Id
    @Column(name = "config_key", nullable = false)
    private String key;

    @Column(name = "config_value", nullable = false)
    private String value;

    public AppConfiguration() {
    }

    public AppConfiguration(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
