create table if not exists notification_outbox (
    id varchar(36) primary key,
    payload varchar(10000) not null
);
