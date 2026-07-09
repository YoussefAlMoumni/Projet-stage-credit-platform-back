package com.talan.creditplatform.controller.rest;

import com.talan.creditplatform.model.entity.Dossier;
import com.talan.creditplatform.model.repository.DossierRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/dossiers")
public class DossierController {

    private final DossierRepository dossierRepository;

    public DossierController(DossierRepository dossierRepository) {
        this.dossierRepository = dossierRepository;
    }

    @PostMapping
    public ResponseEntity<Dossier> createDossier(@RequestBody Dossier dossier) {
        Dossier saved = dossierRepository.save(dossier);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{siren}")
    public ResponseEntity<Dossier> getDossier(@PathVariable String siren) {
        Optional<Dossier> dossier = dossierRepository.findById(siren);
        return dossier.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
