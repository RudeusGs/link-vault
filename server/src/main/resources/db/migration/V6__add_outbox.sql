create table if not exists outbox_events (
    id uuid primary key,
    aggregate_type varchar(50) not null,
    aggregate_id uuid,
    event_type varchar(100) not null,
    payload text not null,
    status varchar(20) not null,
    created_at timestamptz not null,
    processed_at timestamptz,
    error_message text
);

create index if not exists idx_outbox_events_status on outbox_events(status);
create index if not exists idx_outbox_events_created_at on outbox_events(created_at);
