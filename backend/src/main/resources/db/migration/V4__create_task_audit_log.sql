create table task_audit_logs (
    id            bigserial primary key,
    task_id       bigint not null references tasks (id) on delete cascade,
    changed_by_id bigint not null references users (id),
    field         varchar(50) not null,
    old_value     text,
    new_value     text,
    changed_at    timestamptz not null
);

create index idx_task_audit_logs_task on task_audit_logs (task_id, changed_at desc);
