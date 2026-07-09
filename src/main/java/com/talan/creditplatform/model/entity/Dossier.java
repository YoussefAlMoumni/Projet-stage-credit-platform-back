package com.talan.creditplatform.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "dossiers")
public class Dossier {

    @Id
    @Column(name = "siren", nullable = false, unique = true)
    private String siren;

    @Column(nullable = false)
    private String name;

    @Column(name = "type_client", nullable = false)
    private String typeClient;

    @Column(name = "montant_demande", nullable = false)
    private String montantDemande;

    @Column(name = "raw_data", columnDefinition = "TEXT")
    private String rawData;

    public Dossier() {}

    public String getSiren() {
        return siren;
    }

    public void setSiren(String siren) {
        this.siren = siren;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTypeClient() {
        return typeClient;
    }

    public void setTypeClient(String typeClient) {
        this.typeClient = typeClient;
    }

    public String getMontantDemande() {
        return montantDemande;
    }

    public void setMontantDemande(String montantDemande) {
        this.montantDemande = montantDemande;
    }

    public String getRawData() {
        return rawData;
    }

    public void setRawData(String rawData) {
        this.rawData = rawData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dossier dossier = (Dossier) o;
        return Objects.equals(siren, dossier.siren);
    }

    @Override
    public int hashCode() {
        return Objects.hash(siren);
    }
}
