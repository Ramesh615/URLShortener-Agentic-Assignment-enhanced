CREATE TABLE IF NOT EXISTS agentic_workflow_runs (
  id VARCHAR(36) PRIMARY KEY,
  scenario VARCHAR(24) NOT NULL,
  requirement_text LONGTEXT NOT NULL,
  status VARCHAR(40) NOT NULL,
  requirement_version BIGINT NOT NULL DEFAULT 1,
  owner_id VARCHAR(160) NOT NULL,
  approval_hash VARCHAR(128),
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  INDEX idx_workflow_owner_created (owner_id, created_at),
  INDEX idx_workflow_status_updated (status, updated_at)
);
CREATE TABLE IF NOT EXISTS agentic_workflow_tasks (
  id VARCHAR(36) PRIMARY KEY,
  workflow_id VARCHAR(36) NOT NULL,
  task_key VARCHAR(80) NOT NULL,
  agent_type VARCHAR(80) NOT NULL,
  dependencies_json LONGTEXT NOT NULL,
  state VARCHAR(40) NOT NULL,
  output_text LONGTEXT,
  attempts INT NOT NULL DEFAULT 0,
  max_attempts INT NOT NULL DEFAULT 3,
  lease_owner VARCHAR(160),
  lease_until TIMESTAMP(6) NULL,
  started_at TIMESTAMP(6) NULL,
  completed_at TIMESTAMP(6) NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_agentic_task_run FOREIGN KEY (workflow_id) REFERENCES agentic_workflow_runs(id),
  CONSTRAINT uq_agentic_task_key UNIQUE (workflow_id, task_key),
  INDEX idx_agentic_task_claim (state, lease_until)
);
CREATE TABLE IF NOT EXISTS agentic_workflow_audit (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  workflow_id VARCHAR(36) NOT NULL,
  actor VARCHAR(160) NOT NULL,
  event_type VARCHAR(100) NOT NULL,
  details LONGTEXT,
  occurred_at TIMESTAMP(6) NOT NULL,
  CONSTRAINT fk_agentic_audit_run FOREIGN KEY (workflow_id) REFERENCES agentic_workflow_runs(id),
  INDEX idx_agentic_audit_run_time (workflow_id, occurred_at)
);
