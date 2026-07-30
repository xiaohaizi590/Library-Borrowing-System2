package net.togogo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "revinfo")
@RevisionEntity(net.togogo.audit.AuditRevisionListener.class)
public class AuditRevisionEntity {

    @Id
    @RevisionNumber
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @RevisionTimestamp
    @Column(name = "revtstmp")
    private Long timestamp;

    @Column(length = 100)
    private String operator;

    @Column(name = "operator_ip", length = 50)
    private String operatorIp;

    @Column(name = "operation_time")
    private LocalDateTime operationTime;

    @PrePersist
    protected void onCreate() {
        if (operationTime == null) {
            operationTime = LocalDateTime.now();
        }
    }
}
