create table if not exists refresh_sessions (
    id bigserial primary key,
    user_id bigint not null,
    token_hash varchar(64) not null unique,
    expires_at timestamp(6) not null,
    revoked_at timestamp(6),
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null
);

create index if not exists idx_refresh_sessions_user_id
    on refresh_sessions (user_id);

create index if not exists idx_refresh_sessions_expires_at
    on refresh_sessions (expires_at);

alter table refresh_sessions
    add constraint fk_refresh_sessions_user
    foreign key (user_id) references users (id);
