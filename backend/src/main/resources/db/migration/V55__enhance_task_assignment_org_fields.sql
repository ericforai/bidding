alter table tasks
    add column if not exists assignee_dept_code varchar(100);

alter table tasks
    add column if not exists assignee_dept_name varchar(100);

alter table tasks
    add column if not exists assignee_role_code varchar(64);

alter table tasks
    add column if not exists assignee_role_name varchar(100);

create index if not exists idx_tasks_assignee_dept_code on tasks (assignee_dept_code);
create index if not exists idx_tasks_assignee_role_code on tasks (assignee_role_code);
