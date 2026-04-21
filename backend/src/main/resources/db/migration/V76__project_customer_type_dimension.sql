alter table projects
    add column if not exists customer_type varchar(100);

create index if not exists idx_project_customer_type
    on projects (customer_type);
