package com.url.shortner.agent.v2.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface WorkflowAuditRepository extends JpaRepository<WorkflowAuditEntity, Long> {
    List<WorkflowAuditEntity> findByWorkflowIdOrderByOccurredAtAsc(String workflowId);

    @Query("select count(a) from WorkflowAuditEntity a, WorkflowRunEntity r " +
           "where a.workflowId = r.id and r.ownerId = :owner and a.eventType = :eventType")
    long countEventsByOwnerAndType(@Param("owner") String owner, @Param("eventType") String eventType);
}
