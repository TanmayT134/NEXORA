ALTER TABLE interview_sessions
  ADD COLUMN resume_context_snapshot LONGTEXT NULL,
  ADD COLUMN termination_reason VARCHAR(40) NULL,
  ADD COLUMN termination_details TEXT NULL;

CREATE TABLE IF NOT EXISTS interview_integrity_events (
  id BIGINT NOT NULL AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  event_type VARCHAR(40) NOT NULL,
  severity VARCHAR(20) NOT NULL,
  details TEXT NULL,
  occurred_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_integrity_session_time (session_id, occurred_at),
  CONSTRAINT fk_integrity_event_session
    FOREIGN KEY (session_id) REFERENCES interview_sessions(id)
);
