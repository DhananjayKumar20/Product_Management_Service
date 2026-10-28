-- V2: tasks table (title, description, status, due date, owner association)
CREATE TABLE tasks (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    title       VARCHAR(150)  NOT NULL,
    description VARCHAR(2000) NULL,
    status      VARCHAR(20)   NOT NULL,
    due_date    DATE          NOT NULL,
    owner_id    BIGINT        NOT NULL,
    created_at  DATETIME(6)   NOT NULL,
    updated_at  DATETIME(6)   NOT NULL,
    version     BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_tasks PRIMARY KEY (id),
    CONSTRAINT fk_tasks_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,

    -- Index optimisation notes:
    -- 1) idx_tasks_owner_status_due : "my tasks" list, "my tasks by status", and sort by due_date
    --    (leftmost prefix owner_id also serves the FK, so no separate FK index is needed).
    -- 2) idx_tasks_owner_created    : default newest-first listing per owner and the
    --    duplicate-submit (double click) check on create.
    -- 3) idx_tasks_status_due       : ADMIN list filtered by status / sorted by due date.
    INDEX idx_tasks_owner_status_due (owner_id, status, due_date),
    INDEX idx_tasks_owner_created (owner_id, created_at),
    INDEX idx_tasks_status_due (status, due_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
