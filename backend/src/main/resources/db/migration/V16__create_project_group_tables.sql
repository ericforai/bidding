create table if not exists project_groups (
    id bigserial primary key,
    group_code varchar(100) not null unique,
    group_name varchar(200) not null,
    manager_user_id bigint,
    visibility varchar(30) not null,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null
);

create index if not exists idx_project_groups_manager_user_id
    on project_groups (manager_user_id);

create table if not exists project_group_members (
    project_group_id bigint not null,
    user_id bigint not null,
    primary key (project_group_id, user_id)
);

create table if not exists project_group_role_access (
    project_group_id bigint not null,
    role_code varchar(50) not null,
    primary key (project_group_id, role_code)
);

create table if not exists project_group_projects (
    project_group_id bigint not null,
    project_id bigint not null,
    primary key (project_group_id, project_id)
);

create index if not exists idx_project_group_projects_project_id
    on project_group_projects (project_id);

alter table if exists project_groups
    add constraint fk_project_groups_manager_user
    foreign key (manager_user_id) references users (id);

alter table if exists project_group_members
    add constraint fk_project_group_members_group
    foreign key (project_group_id) references project_groups (id) on delete cascade;

alter table if exists project_group_members
    add constraint fk_project_group_members_user
    foreign key (user_id) references users (id);

alter table if exists project_group_role_access
    add constraint fk_project_group_role_access_group
    foreign key (project_group_id) references project_groups (id) on delete cascade;

alter table if exists project_group_projects
    add constraint fk_project_group_projects_group
    foreign key (project_group_id) references project_groups (id) on delete cascade;

alter table if exists project_group_projects
    add constraint fk_project_group_projects_project
    foreign key (project_id) references projects (id);
