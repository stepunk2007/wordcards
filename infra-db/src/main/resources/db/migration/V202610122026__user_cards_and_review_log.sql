-- A lexeme in the deck of one learning profile with its FSRS state.
create table user_card (
    id          bigint generated always as identity primary key,
    profile_id  bigint           not null references learning_profile (id) on delete cascade,
    lexeme_id   bigint           not null references lexeme (id),
    state       varchar(20)      not null check (state in ('NEW', 'LEARNING', 'REVIEW', 'RELEARNING')),
    due_at      timestamptz      not null,
    stability   double precision not null default 0 check (stability >= 0),
    difficulty  double precision not null default 0 check (difficulty >= 0),
    reps        integer          not null default 0 check (reps >= 0),
    lapses      integer          not null default 0 check (lapses >= 0),
    created_at  timestamptz      not null default now(),
    constraint user_card_profile_lexeme_uq unique (profile_id, lexeme_id)
);

-- Finds cards due for review in a profile.
create index user_card_profile_due_at_idx on user_card (profile_id, due_at);

create table review_log (
    id           bigint generated always as identity primary key,
    user_card_id bigint      not null references user_card (id) on delete cascade,
    grade        varchar(10) not null check (grade in ('AGAIN', 'HARD', 'GOOD', 'EASY')),
    reviewed_at  timestamptz not null
);

create index review_log_user_card_reviewed_at_idx on review_log (user_card_id, reviewed_at);
