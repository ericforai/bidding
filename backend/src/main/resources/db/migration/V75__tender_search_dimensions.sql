alter table tenders
    add column if not exists region varchar(100);

alter table tenders
    add column if not exists industry varchar(100);

alter table tenders
    add column if not exists purchaser_name varchar(255);

alter table tenders
    add column if not exists purchaser_hash varchar(64);

alter table tenders
    add column if not exists publish_date date;

alter table tenders
    add column if not exists contact_name varchar(100);

alter table tenders
    add column if not exists contact_phone varchar(50);

alter table tenders
    add column if not exists description text;

alter table tenders
    add column if not exists tags text;

create index if not exists idx_tender_region
    on tenders (region);

create index if not exists idx_tender_industry
    on tenders (industry);

create index if not exists idx_tender_purchaser_hash
    on tenders (purchaser_hash);

create index if not exists idx_tender_status_region_industry
    on tenders (status, region, industry);
