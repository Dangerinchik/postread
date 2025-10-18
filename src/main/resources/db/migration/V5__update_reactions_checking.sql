begin;

-- Затем используйте полученное имя
ALTER TABLE reactions DROP CONSTRAINT reactions_type_check;
ALTER TABLE reactions ADD CONSTRAINT reactions_type_check
    CHECK (type IN(1,2,3,4,5,6,7,8,9,10,11,12));
end;