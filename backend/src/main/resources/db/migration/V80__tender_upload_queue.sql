create table if not exists tender_file (
    id bigserial primary key,
    upload_id varchar(64) not null,
    user_id bigint not null,
    file_path varchar(1000) not null,
    file_sha256 varchar(64),
    file_size bigint,
    page_count integer,
    upload_status varchar(20) not null default 'INITIATED',
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint uk_tender_file_upload_id unique (upload_id),
    constraint fk_tender_file_user foreign key (user_id) references users(id)
);

create unique index if not exists uk_tender_file_user_sha256
    on tender_file(user_id, file_sha256)
    where file_sha256 is not null;

create index if not exists idx_tender_file_user_status_created
    on tender_file(user_id, upload_status, created_at);

create table if not exists tender_task (
    id bigserial primary key,
    file_id bigint not null,
    status varchar(20) not null,
    priority integer not null default 5,
    attempts integer not null default 0,
    available_at timestamp not null default now(),
    locked_by varchar(100),
    locked_at timestamp,
    error_code varchar(100),
    error_message varchar(1000),
    started_at timestamp,
    finished_at timestamp,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint uk_tender_task_file_id unique (file_id),
    constraint fk_tender_task_file foreign key (file_id) references tender_file(id)
);

create index if not exists idx_tender_task_status_available_priority
    on tender_task(status, available_at, priority, created_at);
create index if not exists idx_tender_task_locked_status
    on tender_task(locked_by, status);

create table if not exists tender_task_dlq (
    id bigserial primary key,
    task_id bigint not null,
    file_id bigint not null,
    failed_at timestamp not null default now(),
    error_code varchar(100),
    error_message varchar(1000),
    payload text,
    created_at timestamp not null default now(),
    constraint fk_tender_task_dlq_task foreign key (task_id) references tender_task(id),
    constraint fk_tender_task_dlq_file foreign key (file_id) references tender_file(id)
);

create index if not exists idx_tender_task_dlq_failed_at on tender_task_dlq(failed_at);
