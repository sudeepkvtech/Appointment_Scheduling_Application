-- Create encounters table
CREATE TABLE encounters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    encounter_number VARCHAR(50) UNIQUE NOT NULL,
    appointment_id UUID UNIQUE NOT NULL,
    patient_id UUID NOT NULL,
    resource_id UUID NOT NULL,
    location_id UUID NOT NULL,
    encounter_type VARCHAR(100) NOT NULL,
    encounter_date TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PLANNED',
    chief_complaint TEXT,
    history_of_present_illness TEXT,
    physical_examination TEXT,
    assessment TEXT,
    plan TEXT,
    vital_signs JSONB,
    diagnosis_codes JSONB,
    procedure_codes JSONB,
    medications_prescribed JSONB,
    lab_orders JSONB,
    imaging_orders JSONB,
    referrals JSONB,
    follow_up_required BOOLEAN DEFAULT FALSE,
    follow_up_in_days INTEGER,
    follow_up_instructions TEXT,
    provider_signature VARCHAR(255),
    signed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_encounter_appointment ON encounters(appointment_id);
CREATE INDEX idx_encounter_patient ON encounters(patient_id);
CREATE INDEX idx_encounter_resource ON encounters(resource_id);
CREATE INDEX idx_encounter_number ON encounters(encounter_number);
CREATE INDEX idx_encounter_date ON encounters(encounter_date);
CREATE INDEX idx_encounter_status ON encounters(status);
