-- V1__create_incidents.sql
-- Creates the 'incidents' table with UUID primary key and indexed project_name.
-- raw_log_hash stores the SHA-256 of the submitted log to enable deduplication
-- without persisting the raw log itself (per business rule: logs are NOT stored).

CREATE TABLE incidents (
    id            UUID        NOT NULL,
    project_name  VARCHAR(255) NOT NULL,
    service_name  VARCHAR(255) NOT NULL,
    raw_log_hash  VARCHAR(64)  NOT NULL,
    severity      VARCHAR(10)  NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL,

    CONSTRAINT pk_incidents PRIMARY KEY (id)
);

-- Index on project_name to support frequent queries scoped to a project
CREATE INDEX idx_incidents_project_name ON incidents (project_name);
