create table users (
    id                bigint generated always as identity primary key,
    telegram_id       bigint      not null unique check (telegram_id > 0),
    active_profile_id bigint,
    created_at        timestamptz not null default now()
);

create table learning_profile (
    id            bigint generated always as identity primary key,
    user_id       bigint      not null references users (id) on delete cascade,
    target_lang   varchar(8)  not null,
    native_lang   varchar(8)  not null,
    level         varchar(2)  not null check (level in ('A1', 'A2', 'B1', 'B2', 'C1', 'C2')),
    reminder_time time,
    created_at    timestamptz not null default now(),
    constraint learning_profile_user_target_lang_uq unique (user_id, target_lang),
    constraint learning_profile_langs_differ_ck check (target_lang <> native_lang),
    -- lets users.active_profile_id reference a profile of the same user
    constraint learning_profile_id_user_uq unique (id, user_id)
);

alter table users
    add constraint users_active_profile_fk
        foreign key (active_profile_id, id) references learning_profile (id, user_id)
            on delete set null (active_profile_id);
