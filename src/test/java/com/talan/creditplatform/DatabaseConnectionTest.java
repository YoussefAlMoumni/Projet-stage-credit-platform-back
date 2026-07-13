package com.talan.creditplatform;

import com.talan.creditplatform.model.entity.Dossier;
import com.talan.creditplatform.repository.DossierRepository;
import com.talan.creditplatform.repository.UserRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Scripted manual test for Deliverable 2: PostgreSQL connection wiring.
 * Run this test manually in Eclipse to verify the JPA entity round-trip.
 * Remove @Disabled to execute. Ensure PostgreSQL is running and credentials in application.properties match.
 */
@SpringBootTest
@Disabled("Manual JPA Round-Trip Test")
public class DatabaseConnectionTest {

    @Autowired
    private DossierRepository dossierRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testJpaRoundTrip() {
        System.out.println("Starting JPA Entity Round-Trip Test...");

        // Create
        Dossier dossier = new Dossier();
        dossier.setSiren("999999999");
        dossier.setName("Test Entity");
        dossier.setTypeClient("Personne Physique");
        dossier.setMontantDemande("10000.0");
        dossier.setRawData("{}");
        userRepository.findByUsername("analyst").ifPresent(dossier::setAssignedAnalyst);

        // Save
        Dossier savedDossier = dossierRepository.save(dossier);
        assertNotNull(savedDossier.getSiren());
        System.out.println("Saved dossier successfully: " + savedDossier.getSiren());

        // Retrieve
        Dossier retrievedDossier = dossierRepository.findBySiren(savedDossier.getSiren()).orElse(null);
        assertNotNull(retrievedDossier);
        assertEquals("Test Entity", retrievedDossier.getName());
        System.out.println("Retrieved dossier successfully: " + retrievedDossier.getName());

        // Cleanup
        dossierRepository.delete(retrievedDossier);
        System.out.println("Deleted dossier successfully.");
        
        System.out.println("JPA Round-Trip Test Passed!");
    }
}
