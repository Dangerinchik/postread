begin;
alter table multimedia add column file_name varchar(255),
    add column file_type varchar(20),
    add column position_in_text integer;
end;