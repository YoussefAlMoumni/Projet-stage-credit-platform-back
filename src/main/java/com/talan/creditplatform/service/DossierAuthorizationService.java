package com.talan.creditplatform.service;

import com.talan.creditplatform.model.entity.Dossier;
import com.talan.creditplatform.model.entity.User;
import com.talan.creditplatform.security.CustomUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Encapsulates access-control decisions for Dossier resources.
 *
 * <p>Business rule: Analysts may only access dossiers they are assigned to.
 * Managers and Admins may access any dossier.
 */
@Service
public class DossierAuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(DossierAuthorizationService.class);

    /**
     * Returns {@code true} if the given principal may access the given dossier.
     *
     * @param dossier   the dossier to check
     * @param principal the authenticated user; may be null (returns false)
     */
    public boolean canAccess(Dossier dossier, CustomUserDetails principal) {
        if (principal == null) {
            logger.warn("canAccess: unauthenticated principal attempted access to dossier {}", dossier.getId());
            return false;
        }
        User caller = principal.getUser();
        if (!"analyst".equalsIgnoreCase(caller.getRole())) {
            return true; // managers and admins can see everything
        }
        User assigned = dossier.getAssignedAnalyst();
        if (assigned == null) {
            logger.warn("canAccess: dossier {} is not assigned to anyone", dossier.getId());
            return false;
        }
        boolean hasAccess = assigned.getId().equals(caller.getId());
        if (!hasAccess) {
            logger.warn("canAccess: analyst {} attempted access to dossier {} assigned to {}",
                    caller.getUsername(), dossier.getId(), assigned.getUsername());
        }
        return hasAccess;
    }
}
