alter table cases
    add column if not exists product_line varchar(255);

alter table cases
    add column if not exists source_project_id bigint;

alter table cases
    add column if not exists archive_summary TEXT;

alter table cases
    add column if not exists price_strategy TEXT;

alter table cases
    add column if not exists document_snapshot_text TEXT;

alter table cases
    add column if not exists status varchar(30) not null default 'DRAFT';

alter table cases
    add column if not exists published_at timestamp(6);

alter table cases
    add column if not exists visibility varchar(30) not null default 'INTERNAL';

alter table cases
    add column if not exists search_document TEXT;

update cases
set search_document = concat_ws(' ', title, description, customer_name, location_name, project_period, product_line, archive_summary, price_strategy, document_snapshot_text)
where search_document is null or trim(search_document) = '';

create table if not exists case_success_factors (
    case_id bigint not null,
    success_factor varchar(1000)
);

create table if not exists case_lessons_learned (
    case_id bigint not null,
    lesson_learned varchar(1000)
);

create table if not exists case_attachment_names (
    case_id bigint not null,
    attachment_name varchar(255)
);

create index if not exists idx_cases_product_line
    on cases (product_line);

create index if not exists idx_cases_source_project_id
    on cases (source_project_id);

create index if not exists idx_cases_status
    on cases (status);

create index if not exists idx_cases_visibility
    on cases (visibility);

create index if not exists idx_cases_published_at
    on cases (published_at desc);
