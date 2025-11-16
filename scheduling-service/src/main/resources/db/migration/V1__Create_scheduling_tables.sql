-- Create time_slots table
CREATE TABLE time_slots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resource_id UUID NOT NULL,
    location_id UUID NOT NULL,
    appointment_type_id UUID,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
    is_virtual BOOLEAN DEFAULT FALSE,
    max_bookings INTEGER DEFAULT 1,
    current_bookings INTEGER DEFAULT 0,
    block_reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_slot_time CHECK (end_time > start_time),
    CONSTRAINT valid_bookings CHECK (current_bookings <= max_bookings)
);

CREATE INDEX idx_timeslot_resource ON time_slots(resource_id);
CREATE INDEX idx_timeslot_location ON time_slots(location_id);
CREATE INDEX idx_timeslot_start_time ON time_slots(start_time);
CREATE INDEX idx_timeslot_status ON time_slots(status);
CREATE INDEX idx_timeslot_resource_time ON time_slots(resource_id, start_time, end_time);

-- Create appointments table
CREATE TABLE appointments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_number VARCHAR(50) UNIQUE NOT NULL,
    patient_id UUID NOT NULL,
    resource_id UUID NOT NULL,
    location_id UUID NOT NULL,
    appointment_type_id UUID NOT NULL,
    time_slot_id UUID,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED',
    is_virtual BOOLEAN DEFAULT FALSE,
    virtual_meeting_url VARCHAR(500),
    chief_complaint TEXT,
    notes TEXT,
    patient_notes TEXT,
    staff_notes TEXT,
    cancellation_reason TEXT,
    cancelled_at TIMESTAMP,
    cancelled_by UUID,
    checked_in_at TIMESTAMP,
    checked_out_at TIMESTAMP,
    reminder_sent_at TIMESTAMP,
    confirmation_sent_at TIMESTAMP,
    is_rescheduled BOOLEAN DEFAULT FALSE,
    rescheduled_from_appointment_id UUID,
    rescheduled_to_appointment_id UUID,
    rescheduled_at TIMESTAMP,
    rescheduled_by UUID,
    reschedule_reason TEXT,
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_appointment_time CHECK (end_time > start_time),
    CONSTRAINT fk_rescheduled_from FOREIGN KEY (rescheduled_from_appointment_id)
        REFERENCES appointments(id) ON DELETE SET NULL,
    CONSTRAINT fk_rescheduled_to FOREIGN KEY (rescheduled_to_appointment_id)
        REFERENCES appointments(id) ON DELETE SET NULL
);

CREATE INDEX idx_appointment_patient ON appointments(patient_id);
CREATE INDEX idx_appointment_resource ON appointments(resource_id);
CREATE INDEX idx_appointment_location ON appointments(location_id);
CREATE INDEX idx_appointment_start_time ON appointments(start_time);
CREATE INDEX idx_appointment_status ON appointments(status);
CREATE INDEX idx_appointment_number ON appointments(appointment_number);
CREATE INDEX idx_appointment_rescheduled_from ON appointments(rescheduled_from_appointment_id);

-- Create waitlist table
CREATE TABLE waitlist (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    appointment_type_id UUID NOT NULL,
    resource_id UUID,
    location_id UUID NOT NULL,
    preferred_date_start DATE,
    preferred_date_end DATE,
    preferred_time_start TIME,
    preferred_time_end TIME,
    priority INTEGER DEFAULT 0,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    notes TEXT,
    contacted_at TIMESTAMP,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_waitlist_patient ON waitlist(patient_id);
CREATE INDEX idx_waitlist_status ON waitlist(status);
CREATE INDEX idx_waitlist_priority ON waitlist(priority DESC);

-- Create appointment_history table
CREATE TABLE appointment_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    changed_by UUID,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    old_values JSONB,
    new_values JSONB,
    notes TEXT,
    CONSTRAINT fk_appointment FOREIGN KEY (appointment_id)
        REFERENCES appointments(id) ON DELETE CASCADE
);

CREATE INDEX idx_history_appointment ON appointment_history(appointment_id);
CREATE INDEX idx_history_changed_at ON appointment_history(changed_at);
