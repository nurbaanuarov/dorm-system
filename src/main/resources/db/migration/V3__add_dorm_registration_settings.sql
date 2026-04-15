create table dorm_registration_settings (
    id uuid primary key,
    start_date date not null,
    end_date date not null,
    created_by_user_id uuid not null references app_user(id),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table dorm_registration_meal_option (
    id uuid primary key,
    settings_id uuid not null references dorm_registration_settings(id) on delete cascade,
    meal_type varchar(16) not null,
    available boolean not null default false,
    included_in_price boolean not null default false,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_dorm_registration_meal_option unique (settings_id, meal_type)
);

create index idx_dorm_registration_settings_updated_at on dorm_registration_settings(updated_at desc);
