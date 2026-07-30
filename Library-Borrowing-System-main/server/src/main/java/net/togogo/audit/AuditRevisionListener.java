package net.togogo.audit;

import net.togogo.entity.AuditRevisionEntity;
import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class AuditRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        AuditRevisionEntity revision = (AuditRevisionEntity) revisionEntity;
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            Object principal = auth.getPrincipal();
            if (principal instanceof UserDetails) {
                revision.setOperator(((UserDetails) principal).getUsername());
            } else {
                revision.setOperator(principal.toString());
            }
        } else {
            revision.setOperator("SYSTEM");
        }
    }
}
