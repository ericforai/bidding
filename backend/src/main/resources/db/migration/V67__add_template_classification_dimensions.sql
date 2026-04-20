alter table templates
    add column if not exists product_type varchar(64);

alter table templates
    add column if not exists industry varchar(64);

alter table templates
    add column if not exists document_type varchar(64);

create index if not exists idx_templates_product_type
    on templates (product_type);

create index if not exists idx_templates_industry
    on templates (industry);

create index if not exists idx_templates_document_type
    on templates (document_type);

create index if not exists idx_templates_catalog_filter
    on templates (product_type, industry, document_type);
