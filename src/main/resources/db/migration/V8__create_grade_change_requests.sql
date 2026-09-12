CREATE TABLE IF NOT EXISTS grade_change_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    term_id BIGINT NOT NULL,
    previous_grade DECIMAL(3,2) NOT NULL,
    new_grade DECIMAL(3,2) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_by_id BIGINT NOT NULL,
    approved_by_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_gcr_student FOREIGN KEY (student_id) REFERENCES student_profiles (id),
    CONSTRAINT fk_gcr_course FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT fk_gcr_term FOREIGN KEY (term_id) REFERENCES terms (id),
    CONSTRAINT fk_gcr_requested_by FOREIGN KEY (requested_by_id) REFERENCES users (id),
    CONSTRAINT fk_gcr_approved_by FOREIGN KEY (approved_by_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
