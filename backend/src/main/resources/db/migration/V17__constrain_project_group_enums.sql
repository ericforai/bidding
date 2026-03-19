alter table project_groups
    drop constraint if exists chk_project_groups_visibility;

alter table project_groups
    add constraint chk_project_groups_visibility
    check (visibility in ('ALL', 'MEMBERS', 'MANAGER', 'CUSTOM'));

alter table project_group_role_access
    drop constraint if exists chk_project_group_role_access_role_code;

alter table project_group_role_access
    add constraint chk_project_group_role_access_role_code
    check (role_code in ('ADMIN', 'MANAGER', 'STAFF'));
