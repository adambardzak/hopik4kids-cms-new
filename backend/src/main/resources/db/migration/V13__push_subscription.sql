-- Web Push subscriptions for PWA notifications (new registration alerts to owners/admins).
CREATE TABLE push_subscription (
    id         VARCHAR(36)   NOT NULL PRIMARY KEY,
    user_id    VARCHAR(36)   NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    endpoint   VARCHAR(1000) NOT NULL,
    p256dh     VARCHAR(255)  NOT NULL,
    auth       VARCHAR(255)  NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL,
    updated_at TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uq_push_endpoint UNIQUE (endpoint)
);

CREATE INDEX idx_push_subscription_user ON push_subscription (user_id);
