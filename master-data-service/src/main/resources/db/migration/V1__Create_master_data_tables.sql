-- Create appointment_types table
CREATE TABLE appointment_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    duration_minutes INTEGER NOT NULL,
    color VARCHAR(7),
    requires_preparation BOOLEAN DEFAULT FALSE,
    preparation_instructions TEXT,
    is_virtual_available BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    category VARCHAR(100),
    default_price DECIMAL(10, 2),
    requires_referral BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_appt_type_code ON appointment_types(code);
CREATE INDEX idx_appt_type_active ON appointment_types(is_active);
CREATE INDEX idx_appt_type_category ON appointment_types(category);

-- Create resources table (providers/practitioners)
CREATE TABLE resources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    title VARCHAR(100),
    specialty VARCHAR(255),
    license_number VARCHAR(100),
    email VARCHAR(255),
    phone VARCHAR(20),
    is_active BOOLEAN DEFAULT TRUE,
    avatar_url VARCHAR(500),
    bio TEXT,
    qualifications JSONB,
    languages JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_resource_code ON resources(code);
CREATE INDEX idx_resource_active ON resources(is_active);
CREATE INDEX idx_resource_specialty ON resources(specialty);

-- Create locations table (facilities/clinics)
CREATE TABLE locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(50) NOT NULL,
    zip_code VARCHAR(20) NOT NULL,
    country VARCHAR(50) DEFAULT 'USA',
    phone VARCHAR(20),
    email VARCHAR(255),
    timezone VARCHAR(50) NOT NULL DEFAULT 'America/New_York',
    is_active BOOLEAN DEFAULT TRUE,
    operating_hours JSONB,
    facilities JSONB,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_location_code ON locations(code);
CREATE INDEX idx_location_active ON locations(is_active);
CREATE INDEX idx_location_city ON locations(city);
CREATE INDEX idx_location_state ON locations(state);
