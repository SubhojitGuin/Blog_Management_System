create sequence bookmarks_seq
    increment by 50;

alter sequence bookmarks_seq owner to root;

create sequence categories_seq
    increment by 50;

alter sequence categories_seq owner to root;

create sequence comments_seq
    increment by 50;

alter sequence comments_seq owner to root;

create sequence follows_seq
    increment by 50;

alter sequence follows_seq owner to root;

create sequence likes_seq
    increment by 50;

alter sequence likes_seq owner to root;

create sequence posts_seq
    increment by 50;

alter sequence posts_seq owner to root;

create sequence users_seq
    increment by 50;

alter sequence users_seq owner to root;
