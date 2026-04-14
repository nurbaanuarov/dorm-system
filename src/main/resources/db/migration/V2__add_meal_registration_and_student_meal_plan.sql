create table meal_registration_rule (
    id uuid primary key,
    meal_type varchar(16) not null,
    gender_scope varchar(16) not null,
    registration_date date not null,
    active boolean not null default true,
    created_by_user_id uuid not null references app_user(id),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_meal_registration_rule unique (meal_type, gender_scope, registration_date)
);

create table student_meal_plan (
    id uuid primary key,
    student_id uuid not null references app_user(id) on delete cascade,
    meal_type varchar(16) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_student_meal_plan unique (student_id, meal_type)
);

create index idx_meal_registration_rule_scope_date on meal_registration_rule(gender_scope, registration_date);
create index idx_student_meal_plan_student on student_meal_plan(student_id);
