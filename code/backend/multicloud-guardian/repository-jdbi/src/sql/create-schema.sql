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

create table dbo.Credentials(
    credentials_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id INT REFERENCES dbo.Users(id) ON DELETE CASCADE ON UPDATE CASCADE,
    salt_id VARCHAR(256) not null,
    iterations int not null
);

create table dbo.Preferences (
    preferences_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id INT REFERENCES dbo.Users(id) ON DELETE CASCADE ON UPDATE CASCADE,
    location int NOT NULL check (location in (0,1,2,3)),
    cost int NOT NULL check(cost in (0,1,2)),
    storage_provider int not null check(storage_provider in (0,1,2,3))
);


create table dbo.Folders(
    folder_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id int REFERENCES dbo.Users(id) on delete cascade on update cascade, -- Owner of the folder
    parent_folder_id int REFERENCES dbo.Folders(folder_id) on delete cascade,
    folder_name VARCHAR(25) not null,
    size bigint not null,
    number_files int not null,
    created_at bigint not null,
    updated_at bigint not null,
    path VARCHAR(1024) not null,
    type int not null check (type in (0, 1)), -- 0 for private folder, 1 for shared folder
    constraint created_before_updated_at check (created_at <= updated_at),
    constraint created_at_is_valid check (created_at > 0),
    constraint updated_at_is_valid check (updated_at > 0),
    constraint folder_name_min_length check (char_length(folder_name) >= 2),
    constraint folder_name_max_length check (char_length(folder_name) <= 25)
);

create table dbo.Files(
    file_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id INT REFERENCES dbo.Users(id) on delete cascade on update cascade,
    folder_id INT REFERENCES dbo.Folders(folder_id) ON DELETE CASCADE,
    file_name VARCHAR(50) not null,
    file_fake_name VARCHAR(50) not null,
    path VARCHAR(255) not null,
    size bigint not null,
    content_type VARCHAR(200) not null,
    created_at bigint not null,
    encryption BOOLEAN not null,
    encryption_key VARCHAR(256),
    constraint created_at_is_valid check (created_at > 0)
);

create table dbo.Join_Folders(
    user_id INT REFERENCES dbo.Users(id) ON DELETE CASCADE ON UPDATE CASCADE,
    folder_id INT REFERENCES dbo.Folders(folder_id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, folder_id)
);

create table dbo.Invited_Folders(
  invite_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  inviter_id INT REFERENCES dbo.Users(id) ON DELETE CASCADE ON UPDATE CASCADE,
  guest_id INT REFERENCES dbo.Users(id) ON DELETE CASCADE ON UPDATE CASCADE,
  folder_id INT REFERENCES dbo.Folders(folder_id) ON DELETE CASCADE,
  status    int not null check (status in (0, 1, 2)) -- 0 for pending, 1 for accept, 2 for reject
);


-- Function to join a user to create a shared folder only if type is shared (type = 1)
create or replace function insert_owner_into_join_folders()
returns trigger as $$
begin
    -- Only insert if the folder is shared
    if NEW.type = 1 then
        insert into dbo.Join_Folders (user_id, folder_id)
        values (NEW.user_id, NEW.folder_id);
    end if;
    return NEW;
end;
$$ LANGUAGE plpgsql;

-- Create trigger to insert the owner into Join_Folders when a new shared folder is created
create trigger trigger_insert_owner_into_join_folders
after insert on dbo.Folders
for each row
execute function insert_owner_into_join_folders();



-- Function to insert a new member into a shared folder
create or replace function insert_new_member_into_shared_folder()
returns trigger as $$
begin
    -- Insert the new member into the Join_Folders table
    insert into dbo.Join_Folders (user_id, folder_id)
    values (NEW.guest_id, NEW.folder_id);
    return NEW;
end;
$$ LANGUAGE plpgsql;

-- Create trigger to insert a new member into a shared folder when an invitation is accepted
create trigger trigger_insert_new_member_into_shared_folder
after update on dbo.Invited_Folders
for each row
when (OLD.status = 0 and NEW.status = 1) -- When the invitation status changes from pending to accepted
execute function insert_new_member_into_shared_folder();



-- Function to delete previous invitation to a shared folder
create or replace function delete_previous_invite_to_shared_folder()
returns trigger as $$
begin
    -- Delete the previous invitation for the user to the shared folder
    delete from dbo.Invited_Folders
    where guest_id = OLD.user_id and folder_id = OLD.folder_id;
    return OLD;
end;
$$ LANGUAGE plpgsql;

-- Create trigger to delete previous invitation to a shared folder when a user is invited
create trigger trigger_delete_previous_invite_to_shared_folder
after delete on dbo.Join_Folders
for each row
execute function delete_previous_invite_to_shared_folder();


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



-- This view summarizes the total storage used by each user, grouping their files into categories:
-- 'Images', 'Video', 'Documents', or 'Others', and calculates the total size per category.
CREATE OR REPLACE VIEW user_file_storage_summary AS
WITH categorized_files AS (
  SELECT
    user_id,
    CASE
      WHEN content_type LIKE 'image/%' THEN 'Images'
      WHEN content_type LIKE 'video/%' THEN 'Video'
      WHEN content_type IN (
        'application/pdf',
        'application/msword',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'application/vnd.ms-excel',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        'application/vnd.ms-powerpoint',
        'application/vnd.openxmlformats-officedocument.presentationml.presentation',
        'text/plain'
      ) THEN 'Documents'
      ELSE 'Others'
    END AS category,
    size
  FROM dbo.Files
)
SELECT
  user_id,
  category,
  SUM(size) AS total_size
FROM categorized_files
GROUP BY user_id, category;
