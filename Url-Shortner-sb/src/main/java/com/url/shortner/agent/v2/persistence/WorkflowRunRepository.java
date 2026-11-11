package com.url.shortner.agent.v2.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WorkflowRunRepository extends JpaRepository<WorkflowRunEntity, String> {
    Optional<WorkflowRunEntity> findByIdAndOwnerId(String id, String ownerId);
    long countByOwnerId(String ownerId);
    long countByOwnerIdAndStatus(String ownerId, String status);
}
