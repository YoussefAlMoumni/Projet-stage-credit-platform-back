package com.talan.creditplatform.controller.rest;

import com.talan.creditplatform.model.entity.Dossier;
import com.talan.creditplatform.model.entity.User;
import com.talan.creditplatform.repository.DossierRepository;
import com.talan.creditplatform.repository.UserRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
@RequestMapping("/api")
public class DashboardController {

    private final UserRepository userRepository;
    private final DossierRepository dossierRepository;

    public DashboardController(UserRepository userRepository, DossierRepository dossierRepository) {
        this.userRepository = userRepository;
        this.dossierRepository = dossierRepository;
    }

    @GetMapping("/admin/employees")
    public List<User> getEmployeesForAdmin() {
        return userRepository.findAll();
    }

    @GetMapping("/manager/analysts")
    public List<User> getAnalystsForManager() {
        return userRepository.findByRoleOrderByLastNameAscFirstNameAsc("analyst");
    }

    @GetMapping("/analyst/dossiers/pending")
    public List<Dossier> getPendingDossiersForAnalyst() {
        return dossierRepository.findByStatusOrderByCreationDateAsc("in_progress");
    }
}
