-- A project a member posts to collect price offers from other members (a tender).
CREATE TABLE projects (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(120) NOT NULL,
    category VARCHAR(20) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    city VARCHAR(100),
    budget BIGINT CHECK (budget > 0),
    status VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    closes_at TIMESTAMPTZ NOT NULL,
    closing_notified BOOLEAN NOT NULL DEFAULT FALSE,
    awarded_bid_id BIGINT
);

-- A provider's offer on a project. Each provider has at most one per project.
CREATE TABLE bids (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    bidder_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount BIGINT NOT NULL CHECK (amount > 0),
    note VARCHAR(500),
    status VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (project_id, bidder_id)
);

ALTER TABLE projects
    ADD CONSTRAINT fk_projects_awarded_bid FOREIGN KEY (awarded_bid_id) REFERENCES bids(id) ON DELETE SET NULL;

CREATE INDEX idx_projects_status_closes ON projects (status, closes_at);
CREATE INDEX idx_projects_owner ON projects (owner_id, created_at DESC);
CREATE INDEX idx_bids_bidder ON bids (bidder_id, created_at DESC);
