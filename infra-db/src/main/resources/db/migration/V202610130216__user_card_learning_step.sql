-- FSRS needs the current (re)learning step and the time of the last review.
alter table user_card
    add column step             integer not null default 0 check (step >= 0),
    add column last_reviewed_at timestamptz;
