package com.talan.creditplatform.controller.rest;

import com.talan.creditplatform.model.entity.Dossier;
import com.talan.creditplatform.model.entity.User;
import com.talan.creditplatform.repository.DossierRepository;
import com.talan.creditplatform.repository.UserRepository;
import com.talan.creditplatform.security.CustomUserDetails;
import com.talan.creditplatform.service.DossierAuthorizationService;
import com.talan.creditplatform.service.EventService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for Dossier CRUD operations.
 *
 * <p>AI pipeline operations (trigger, stream, stop, get latest evaluation) are handled
 * separately by {@link AiPipelineController}.
 */
@RestController
@RequestMapping("/api/dossiers")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class DossierController {

    private static final Logger logger = LoggerFactory.getLogger(DossierController.class);

    private final DossierRepository           dossierRepository;
    private final UserRepository              userRepository;
    private final EventService                eventService;
    private final DossierAuthorizationService authorizationService;

    public DossierController(DossierRepository dossierRepository,
                             UserRepository userRepository,
                             EventService eventService,
                             DossierAuthorizationService authorizationService) {
        this.dossierRepository    = dossierRepository;
        this.userRepository       = userRepository;
        this.eventService         = eventService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public ResponseEntity<List<Dossier>> getAllDossiers(@AuthenticationPrincipal CustomUserDetails principal) {
        User caller = principal != null ? principal.getUser() : null;
        if (caller != null && "analyst".equalsIgnoreCase(caller.getRole())) {
            return ResponseEntity.ok(dossierRepository.findByAssignedAnalyst(caller));
        }
        return ResponseEntity.ok(dossierRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Dossier> createDossier(@Valid @RequestBody Dossier dossier,
                                                 @AuthenticationPrincipal CustomUserDetails principal) {
        if (dossier.getAssignedAnalyst() == null && principal != null) {
            dossier.setAssignedAnalyst(principal.getUser());
        }
        assignDefaultAnalystIfMissing(dossier);
        linkRelationships(dossier);
        Dossier saved = dossierRepository.save(dossier);
        eventService.emitDossiersChanged();
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Dossier> updateDossier(@PathVariable Long id, @Valid @RequestBody Dossier dossier) {
        Optional<Dossier> existingOpt = dossierRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Dossier existing = existingOpt.get();
        dossier.setId(id);
        dossier.setCreationDate(existing.getCreationDate());
        if (dossier.getAssignedAnalyst() == null) {
            dossier.setAssignedAnalyst(existing.getAssignedAnalyst());
        }
        if (dossier.getStatus() == null) {
            dossier.setStatus(existing.getStatus());
        }
        assignDefaultAnalystIfMissing(dossier);
        linkRelationships(dossier);
        Dossier saved = dossierRepository.save(dossier);
        eventService.emitDossiersChanged();
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{value}")
    public ResponseEntity<Dossier> getDossier(@PathVariable String value,
                                              @AuthenticationPrincipal CustomUserDetails principal) {
        Optional<Dossier> dossier = findBySirenOrId(value);
        if (dossier.isEmpty()) return ResponseEntity.notFound().build();
        if (!authorizationService.canAccess(dossier.get(), principal)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(dossier.get());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteDossier(@PathVariable Long id) {
        if (!dossierRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        dossierRepository.deleteById(id);
        eventService.emitDossiersChanged();
        return ResponseEntity.ok(Map.of("message", "Dossier deleted."));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateDossierStatus(@PathVariable Long id,
                                                 @RequestBody Map<String, String> body,
                                                 @AuthenticationPrincipal CustomUserDetails principal) {
        Optional<Dossier> dossierOpt = dossierRepository.findById(id);
        if (dossierOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!authorizationService.canAccess(dossierOpt.get(), principal)) {
            logger.warn("403 Forbidden: user {} attempted to modify dossier {}",
                    principal != null ? principal.getUsername() : "anonymous", id);
            return ResponseEntity.status(403).body(Map.of("message", "You do not have permission to modify this dossier."));
        }
        String status = body.get("status");
        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Status is required"));
        }
        Dossier dossier = dossierOpt.get();
        dossier.setStatus(status);
        dossierRepository.save(dossier);
        eventService.emitDossiersChanged();
        return ResponseEntity.ok(dossier);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Optional<Dossier> findBySirenOrId(String value) {
        Optional<Dossier> bySiren = dossierRepository.findBySiren(value);
        if (bySiren.isPresent()) return bySiren;
        try {
            return dossierRepository.findById(Long.valueOf(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private void assignDefaultAnalystIfMissing(Dossier dossier) {
        if (dossier.getAssignedAnalyst() != null) return;
        userRepository.findByUsername("analyst")
                .or(() -> userRepository.findByUsername("banker"))
                .or(() -> userRepository.findByUsername("admin"))
                .ifPresent(dossier::setAssignedAnalyst);
    }

    /** Ensures JPA bi-directional relationships are set before persist. */
    private void linkRelationships(Dossier dossier) {
        if (dossier.getLoans() != null) {
            dossier.getLoans().forEach(loan -> {
                loan.setDossier(dossier);
                if (loan.getCollaterals() != null) {
                    loan.getCollaterals().forEach(c -> c.setLoan(loan));
                }
            });
        }
        if (dossier.getCreditHistories() != null) {
            dossier.getCreditHistories().forEach(h -> h.setDossier(dossier));
        }
        if (dossier.getIndividual() != null) dossier.getIndividual().setDossier(dossier);
        if (dossier.getCorporate()  != null) dossier.getCorporate().setDossier(dossier);
    }
}
