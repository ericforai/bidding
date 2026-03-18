create table if not exists export_tasks (
    id bigserial primary key,
    export_type varchar(20) not null,
    data_type varchar(50) not null,
    status varchar(20) not null,
    file_name varchar(255),
    file_path varchar(500),
    file_size bigint,
    error_message text,
    progress integer,
    created_by bigint not null,
    created_at timestamp(6) not null,
    completed_at timestamp(6),
    expires_at timestamp(6),
    export_params text
);

create index if not exists idx_export_user
    on export_tasks (created_by);

create index if not exists idx_export_status
    on export_tasks (status);
