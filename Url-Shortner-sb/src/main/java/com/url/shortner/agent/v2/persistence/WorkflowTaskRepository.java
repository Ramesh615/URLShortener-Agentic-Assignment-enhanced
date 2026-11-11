package com.url.shortner.agent.v2.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WorkflowTaskRepository extends JpaRepository<WorkflowTaskEntity, String> {
    List<WorkflowTaskEntity> findByWorkflowIdOrderByTaskKey(String workflowId);
    Optional<WorkflowTaskEntity> findByWorkflowIdAndTaskKey(String workflowId, String taskKey);
}
