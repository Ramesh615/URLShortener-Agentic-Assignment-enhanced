package com.url.shortner.agent.v2.persistence;
import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="agentic_workflow_tasks", uniqueConstraints=@UniqueConstraint(columnNames={"workflow_id","task_key"})) public class WorkflowTaskEntity {
 @Id @Column(length=36) public String id;
 @Column(name="workflow_id",nullable=false,length=36) public String workflowId;
 @Column(name="task_key",nullable=false,length=80) public String taskKey;
 @Column(name="agent_type",nullable=false,length=80) public String agentType;
 @Lob @Column(name="dependencies_json",nullable=false) public String dependenciesJson;
 @Column(nullable=false,length=40) public String state;
 @Lob @Column(name="output_text") public String outputText;
 @Column(nullable=false) public int attempts;
 @Column(name="max_attempts",nullable=false) public int maxAttempts=3;
 @Column(name="lease_owner",length=160) public String leaseOwner;
 @Column(name="lease_until") public Instant leaseUntil;
 @Column(name="started_at") public Instant startedAt;
 @Column(name="completed_at") public Instant completedAt;
 @Version public long version;
}
