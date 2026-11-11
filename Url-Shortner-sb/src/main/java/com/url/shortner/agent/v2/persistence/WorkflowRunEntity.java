package com.url.shortner.agent.v2.persistence;
import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="agentic_workflow_runs") public class WorkflowRunEntity {
 @Id @Column(length=36) public String id;
 @Column(nullable=false,length=24) public String scenario;
 @Lob @Column(name="requirement_text",nullable=false) public String requirementText;
 @Column(nullable=false,length=40) public String status;
 @Column(name="requirement_version",nullable=false) public long requirementVersion=1;
 @Column(name="owner_id",nullable=false,length=160) public String ownerId;
 @Column(name="approval_hash",length=128) public String approvalHash;
 @Column(name="created_at",nullable=false) public Instant createdAt;
 @Column(name="updated_at",nullable=false) public Instant updatedAt;
 @Version public long version;
}
