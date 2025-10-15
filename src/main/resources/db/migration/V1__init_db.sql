create table users (
    id serial primary key,
    name varchar(50) not null,
    password varchar(255) not null,
    birth_date date,
    created_at timestamp default current_timestamp,
    icon varchar(255),
    description text,
    email varchar(100) not null unique,

    check(birth_date <= current_date)
);

create table subscriptions (
    subscriber_id integer,
    target_id integer,
    created_at timestamp default current_timestamp,

    check(subscriber_id != target_id),
    primary key(subscriber_id, target_id),
    foreign key(subscriber_id) references users(id) on delete cascade,
    foreign key(target_id) references users(id) on delete cascade
);
create index idx_subscriptions_target on subscriptions(target_id);

create table articles (
    id serial primary key,
    author_id integer not null,
    title varchar(70) not null,
    short_description varchar(100),
    article_text text,
    is_published boolean default false,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp,
    view_count integer default 0,

    foreign key(author_id) references users(id) on delete cascade
);
create index idx_articles_author on articles(author_id);

create type role_type as enum('ADMIN', 'USER');

create table roles (
    id serial primary key,
    type role_type not null unique
);

create table users_roles (
    user_id integer,
    role_id integer,

    primary key(user_id, role_id),
    foreign key(user_id) references users(id) on delete cascade,
    foreign key(role_id) references roles(id) on delete cascade
);
create index idx_users_roles_role on users_roles(role_id);

create table reactions (
       id serial primary key,
       user_id integer,
       article_id integer,
       type integer,

       check(type in(1,2,3,4,5,6,7,8)),
       foreign key(user_id) references users(id) on delete cascade,
       foreign key(article_id) references articles(id) on delete cascade,
       unique(user_id, article_id)
);
create index idx_reactions_article on reactions(article_id);

create table bookmarks (
       id serial primary key,
       user_id integer,
       article_id integer,
       added_at timestamp default current_timestamp,

       foreign key(user_id) references users(id) on delete cascade,
       foreign key(article_id) references articles(id) on delete cascade,
       unique(user_id, article_id)
);
create index idx_bookmarks_article on bookmarks(article_id);

create table tags (
       id serial primary key,
       name varchar(50) not null,
       slug varchar(30) unique not null
);

create table articles_tags (
       article_id integer,
       tag_id integer,

       primary key(article_id, tag_id),
       foreign key(article_id) references articles(id) on delete cascade,
       foreign key(tag_id) references tags(id) on delete cascade
);
create index idx_articles_tags_tag on articles_tags(tag_id);

create table comments (
       id serial primary key,
       user_id integer,
       article_id integer,
       comment_text text not null,
       created_at timestamp default current_timestamp,
       updated_at timestamp default current_timestamp,
       reference_id integer,

       check(reference_id is null or reference_id != id),
       foreign key(user_id) references users(id) on delete cascade,
       foreign key(article_id) references articles(id) on delete cascade,
       foreign key(reference_id) references comments(id) on delete cascade
);
create index idx_comments_article on comments(article_id);

create table multimedia (
       id serial primary key,
       article_id integer,
       url varchar(255),

       foreign key(article_id) references articles(id) on delete cascade
);
create index idx_multimedia_article on multimedia(article_id);

create table reviews(
       id serial primary key,
       article_id integer,
       author_id integer,
       title varchar(70),
       review_text text,
       is_published boolean default false,
       created_at timestamp default current_timestamp,
       updated_at timestamp default current_timestamp,
       view_count integer default 0,

       foreign key(article_id) references articles(id) on delete cascade,
       foreign key(author_id) references users(id) on delete cascade,
       unique(article_id, author_id)
);
create index idx_reviews_article on reviews(article_id);