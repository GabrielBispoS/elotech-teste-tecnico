package com.elotech.taskmanager.task.domain;

import com.elotech.taskmanager.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "task_audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by_id", nullable = false)
    private User changedBy;

    @Column(nullable = false, length = 50)
    private String field;

    @Column(name = "old_value", columnDefinition = "text")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "text")
    private String newValue;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    public TaskAuditLog(Task task, User changedBy, String field, String oldValue, String newValue) {
        this.task = task;
        this.changedBy = changedBy;
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
}
