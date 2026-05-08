-- N7 task comments + history timeline

CREATE TABLE task_comment (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    author_user_id BIGINT NOT NULL REFERENCES users(id),
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_task_comment_task_created
    ON task_comment(task_id, created_at DESC);

CREATE INDEX idx_task_comment_author
    ON task_comment(author_user_id);

CREATE TABLE task_history (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    actor_user_id BIGINT REFERENCES users(id),
    action VARCHAR(50) NOT NULL,
    snapshot_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    archived_at TIMESTAMP
);

CREATE INDEX idx_task_history_task_created
    ON task_history(task_id, created_at DESC);

CREATE INDEX idx_task_history_archive
    ON task_history(archived_at, created_at);
