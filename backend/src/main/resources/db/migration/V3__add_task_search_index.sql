-- acelera a busca textual por prefixo; para "contains" em escala ver o tradeoff no README (pg_trgm/full-text)
create index idx_tasks_title_lower on tasks (lower(title));
