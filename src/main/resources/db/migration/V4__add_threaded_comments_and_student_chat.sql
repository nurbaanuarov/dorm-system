alter table post_comment add column author_user_id uuid;

update post_comment
set author_user_id = student_id
where author_user_id is null;

alter table post_comment alter column author_user_id set not null;
alter table post_comment
    add constraint fk_post_comment_author_user
    foreign key (author_user_id) references app_user(id);

alter table post_comment add column parent_comment_id uuid references post_comment(id) on delete cascade;
create index idx_post_comment_parent_created_at on post_comment(parent_comment_id, created_at asc);

alter table post_comment drop column student_id;

create table global_chat_message (
    id uuid primary key,
    author_user_id uuid not null references app_user(id),
    parent_message_id uuid references global_chat_message(id) on delete cascade,
    content varchar(2000) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_global_chat_message_created_at on global_chat_message(created_at desc);
create index idx_global_chat_message_parent_created_at on global_chat_message(parent_message_id, created_at asc);
