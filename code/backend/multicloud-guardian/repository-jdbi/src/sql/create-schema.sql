create schema dbo;


create table dbo.Users(
    id int generated always as identity primary key,
    username VARCHAR(64) unique not null,
    email VARCHAR(100) unique not null,
    password_validation VARCHAR(256) not null,
    constraint email_is_valid check (email ~ '^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+$'),
    constraint username_min_length check (char_length(username) >= 5),
    constraint username_max_length check (char_length(username) <= 25)
);

create table dbo.Tokens(
    token_validation VARCHAR(256) primary key,
    user_id int REFERENCES dbo.Users(id) on delete cascade on update cascade,
    user_agent  VARCHAR(255) not null,
    created_at bigint not null,
    last_used_at bigint not null,
    constraint created_before_last_used check (created_at <= last_used_at),
    constraint created_at_is_valid check (created_at > 0),
    constraint last_used_at_is_valid check (last_used_at > 0)
);

create table dbo.Preferences (
    preferences_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id INT REFERENCES dbo.Users(id) ON DELETE CASCADE ON UPDATE CASCADE,
    location int NOT NULL check (location in (0,1,2,3)),
    performance int NOT NULL check(performance in (0,1,2)),
    storage_provider int not null check(storage_provider in (0,1,2,3))
);


create table dbo.Folders(
    folder_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id int REFERENCES dbo.Users(id) on delete cascade on update cascade,
    parent_folder_id int REFERENCES dbo.Folders(folder_id) on delete cascade,
    folder_name VARCHAR(25) not null,
    size bigint not null,
    number_files int not null,
    created_at bigint not null,
    updated_at bigint not null,
    path VARCHAR(255) not null,
    constraint created_before_updated_at check (created_at <= updated_at),
    constraint created_at_is_valid check (created_at > 0),
    constraint updated_at_is_valid check (updated_at > 0),
    constraint folder_name_min_length check (char_length(folder_name) >= 5),
    constraint folder_name_max_length check (char_length(folder_name) <= 25)
);

create table dbo.Files(
    file_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id INT REFERENCES dbo.Users(id) on delete cascade on update cascade,
    folder_id INT REFERENCES dbo.Folders(folder_id) on delete set null default null, 
    file_name VARCHAR(30) not null,
    checksum bigint not null,
    path VARCHAR(255) not null,
    size bigint not null,
    encryption_key VARCHAR(256),
    encryption BOOLEAN not null
);

create table dbo.Metadata(
    metadata_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    file_id int REFERENCES dbo.Files(file_id) on delete cascade on update cascade,
    content_type VARCHAR(200) not null,
    tags TEXT[] not null,
    created_at bigint not null,
    indexed_at bigint not null,
    constraint created_before_indexed_at check (created_at <= indexed_at),
    constraint created_at_is_valid check (created_at > 0),
    constraint indexed_at_is_valid check (indexed_at > 0)
);


-- Function to update folder size and number of files when a file is added, updated or deleted 

create or replace function update_folder_stats()
returns trigger as $$
begin
    -- If a file is inserted or moved to a different folder
    if (TG_OP = 'INSERT' OR (TG_OP = 'UPDATE' AND NEW.folder_id IS DISTINCT FROM OLD.folder_id)) THEN
        -- Increase folder size and file count
        UPDATE dbo.Folders
        SET 
            size = size + NEW.size,
            number_files = number_files + 1
        WHERE folder_id = NEW.folder_id;
    end if;

    -- If a file is moved from one folder to another
    if (TG_OP = 'UPDATE' AND OLD.folder_id IS DISTINCT FROM NEW.folder_id) THEN
        -- Decrease the previous folder's size and file count
        UPDATE dbo.Folders
        SET
            size = size - OLD.size,
            number_files = number_files - 1
        WHERE folder_id = OLD.folder_id;
    end if;

    -- If a file is deleted
    if (TG_OP = 'DELETE') THEN
        -- Decrease folder size and file count
        UPDATE dbo.Folders
        SET 
            size = size - OLD.size,
            number_files = number_files - 1
        WHERE folder_id = OLD.folder_id;
    end if;

    return null;
end;
$$ LANGUAGE plpgsql;

-- Create trigger to update folder statistics when a file is inserted, updated or deleted 
create trigger trigger_update_folder_stats
after insert or update or delete on dbo.Files
for each row
execute function update_folder_stats();