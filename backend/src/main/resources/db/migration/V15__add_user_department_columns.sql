alter table users
    add column if not exists department_code varchar(100);

alter table users
    add column if not exists department_name varchar(100);
