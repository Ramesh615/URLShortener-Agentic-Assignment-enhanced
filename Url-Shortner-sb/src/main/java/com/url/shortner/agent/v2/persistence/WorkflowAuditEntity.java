package com.url.shortner.agent.v2.persistence;
import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="agentic_workflow_audit") public class WorkflowAuditEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(name="workflow_id",nullable=false,length=36) public String workflowId;
 @Column(nullable=false,length=160) public String actor;
 @Column(name="event_type",nullable=false,length=100) public String eventType;
 @Lob public String details;
 @Column(name="occurred_at",nullable=false) public Instant occurredAt;
}
