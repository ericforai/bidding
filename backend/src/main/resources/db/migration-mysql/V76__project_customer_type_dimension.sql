alter table projects
    add column customer_type varchar(100);

create index idx_project_customer_type
    on projects (customer_type);
