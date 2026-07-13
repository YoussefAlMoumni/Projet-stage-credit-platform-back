package com.talan.creditplatform.controller.rest;

import com.talan.creditplatform.model.entity.Dossier;
import com.talan.creditplatform.repository.DossierRepository;
import com.talan.creditplatform.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/dossiers")
public class DossierController {

    private final DossierRepository dossierRepository;
    private final UserRepository userRepository;

    public DossierController(DossierRepository dossierRepository, UserRepository userRepository) {
        this.dossierRepository = dossierRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<Dossier> createDossier(@RequestBody Dossier dossier) {
        assignDefaultAnalystIfMissing(dossier);
        Dossier saved = dossierRepository.save(dossier);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{siren}")
    public ResponseEntity<Dossier> getDossier(@PathVariable String siren) {
        Optional<Dossier> dossier = findBySirenOrId(siren);
        return dossier.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private Optional<Dossier> findBySirenOrId(String value) {
        Optional<Dossier> bySiren = dossierRepository.findBySiren(value);
        if (bySiren.isPresent()) {
            return bySiren;
        }
        try {
            return dossierRepository.findById(Long.valueOf(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private void assignDefaultAnalystIfMissing(Dossier dossier) {
        if (dossier.getAssignedAnalyst() != null) {
            return;
        }
        userRepository.findByUsername("analyst")
                .or(() -> userRepository.findByUsername("banker"))
                .or(() -> userRepository.findByUsername("admin"))
                .ifPresent(dossier::setAssignedAnalyst);
    }
}
