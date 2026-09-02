create table users (
    id            bigserial primary key,
    name          varchar(255) not null,
    email         varchar(255) not null,
    password_hash varchar(255) not null,
    constraint uk_users_email unique (email)
);

create table projects (
    id          bigserial primary key,
    name        varchar(255) not null,
    description text,
    owner_id    bigint not null references users (id)
);

create table project_memberships (
    id         bigserial primary key,
    project_id bigint not null references projects (id) on delete cascade,
    user_id    bigint not null references users (id),
    role       varchar(20) not null,
    constraint uk_membership_project_user unique (project_id, user_id)
);

create table tasks (
    id          bigserial primary key,
    project_id  bigint not null references projects (id) on delete cascade,
    title       varchar(255) not null,
    description text,
    status      varchar(20) not null,
    priority    varchar(20) not null,
    created_at  timestamptz not null,
    updated_at  timestamptz not null,
    deadline    date,
    assignee_id bigint references users (id)
);

create index idx_project_memberships_user on project_memberships (user_id);
create index idx_tasks_project_status on tasks (project_id, status);

-- suporta a contagem do WIP limit (tarefas IN_PROGRESS por responsavel)
create index idx_tasks_assignee_status on tasks (assignee_id, status);
