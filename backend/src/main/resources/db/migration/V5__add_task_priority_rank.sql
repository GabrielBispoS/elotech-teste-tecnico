-- ordem semantica da prioridade (CRITICAL > HIGH > MEDIUM > LOW); a coluna e derivada,
-- entao nao ha como divergir do enum gravado em priority
alter table tasks
    add column priority_rank smallint generated always as (
        case priority
            when 'CRITICAL' then 4
            when 'HIGH' then 3
            when 'MEDIUM' then 2
            else 1
        end
    ) stored;

create index idx_tasks_project_priority_rank on tasks (project_id, priority_rank);
