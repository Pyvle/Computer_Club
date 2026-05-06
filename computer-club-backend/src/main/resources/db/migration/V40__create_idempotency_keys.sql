CREATE TABLE idempotency_keys (
    id           VARCHAR(128)             NOT NULL,
    user_id      BIGINT                   NOT NULL REFERENCES users (id),
    endpoint     VARCHAR(200)             NOT NULL,
    request_hash VARCHAR(64)              NOT NULL,
    status_code  INTEGER                  NOT NULL,
    response_body JSONB                   NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_idempotency_keys_user_id ON idempotency_keys (user_id);
CREATE INDEX idx_idempotency_keys_expires_at ON idempotency_keys (expires_at);
