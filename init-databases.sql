-- Create databases for each microservice
CREATE DATABASE identity_db;
CREATE DATABASE scheduling_db;
CREATE DATABASE encounter_db;
CREATE DATABASE notification_db;
CREATE DATABASE master_data_db;

-- Grant privileges (optional, already granted to postgres user)
GRANT ALL PRIVILEGES ON DATABASE identity_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE scheduling_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE encounter_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE master_data_db TO postgres;
