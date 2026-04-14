create table app_user (
    id uuid primary key,
    email varchar(255) not null unique,
    role varchar(32) not null,
    name varchar(120) not null,
    surname varchar(120) not null,
    student_identifier varchar(64),
    gender varchar(16),
    password_hash varchar(255) not null,
    enabled boolean not null default true,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_app_user_student_identifier unique (student_identifier)
);

create table floor_unit (
    id uuid primary key,
    block_code varchar(4) not null,
    floor_number integer not null,
    active boolean not null default true,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_floor_block_floor unique (block_code, floor_number)
);

create table room_unit (
    id uuid primary key,
    floor_id uuid not null references floor_unit(id),
    room_number integer not null,
    capacity integer not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_room_floor_room unique (floor_id, room_number)
);

create table bed_unit (
    id uuid primary key,
    room_id uuid not null references room_unit(id),
    bed_number integer not null,
    occupant_id uuid references app_user(id),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_bed_room_bed unique (room_id, bed_number),
    constraint uk_bed_occupant unique (occupant_id)
);

create table meal_slot (
    id uuid primary key,
    meal_type varchar(16) not null,
    gender_scope varchar(16) not null,
    slot_date date not null,
    start_time time not null,
    end_time time not null,
    capacity integer not null,
    active boolean not null default true,
    created_by_user_id uuid not null references app_user(id),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_meal_slot unique (meal_type, gender_scope, slot_date, start_time)
);

create table meal_booking (
    id uuid primary key,
    slot_id uuid not null references meal_slot(id) on delete cascade,
    student_id uuid not null references app_user(id),
    meal_type varchar(16) not null,
    slot_date date not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_meal_booking_slot_student unique (slot_id, student_id),
    constraint uk_meal_booking_student_date_type unique (student_id, slot_date, meal_type)
);

create table post_item (
    id uuid primary key,
    title varchar(255) not null,
    description varchar(4000) not null,
    audience varchar(16) not null,
    created_by_user_id uuid not null references app_user(id),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table post_photo (
    id uuid primary key,
    post_id uuid not null references post_item(id) on delete cascade,
    photo_url varchar(1000) not null,
    sort_order integer not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table post_comment (
    id uuid primary key,
    post_id uuid not null references post_item(id) on delete cascade,
    student_id uuid not null references app_user(id),
    content varchar(2000) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_room_floor on room_unit(floor_id);
create index idx_bed_room on bed_unit(room_id);
create index idx_meal_slot_scope_date on meal_slot(gender_scope, slot_date, meal_type);
create index idx_post_item_audience_created_at on post_item(audience, created_at desc);
create index idx_post_comment_post_created_at on post_comment(post_id, created_at desc);
