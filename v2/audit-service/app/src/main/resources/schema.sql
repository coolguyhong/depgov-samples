CREATE TABLE IF NOT EXISTS audit_event (
    event_id VARCHAR(40) PRIMARY KEY,
    payload CLOB NOT NULL
);
