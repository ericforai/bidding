create table if not exists system_settings (
    id bigserial primary key,
    config_key varchar(100) not null unique,
    payload_json text not null,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null
);

create index if not exists idx_system_settings_config_key
    on system_settings (config_key);
