-- V10__philippine_statutory_equity_profiling.sql
-- Subsystem: Statutory Philippine Equity Target Profiling (RA 10931 UniFAST, RA 8371 IPRA, RA 7277 PWD, RA 11861 Solo Parents, GIDA & First-Gen)

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS student_equity_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_profile_id BIGINT NOT NULL UNIQUE,
    
    -- RA 10931 / DSWD Classifications
    is_4ps_beneficiary BOOLEAN NOT NULL DEFAULT FALSE,
    household_4ps_id_number VARCHAR(60) NULL,
    is_listahanan_nhts BOOLEAN NOT NULL DEFAULT FALSE,
    unifast_tes_awardee BOOLEAN NOT NULL DEFAULT FALSE,
    unifast_tes_award_number VARCHAR(60) NULL,

    -- RA 8371 (Indigenous Peoples)
    is_indigenous_people BOOLEAN NOT NULL DEFAULT FALSE,
    ip_ethnic_group VARCHAR(100) NULL,
    ncip_certificate_number VARCHAR(100) NULL,

    -- RA 7277 / RA 9442 / RA 10754 (PWD)
    is_person_with_disability BOOLEAN NOT NULL DEFAULT FALSE,
    pwd_id_number VARCHAR(60) NULL,
    disability_type VARCHAR(60) NULL,

    -- RA 8972 / RA 11861 (Solo Parents - Explicit Separation)
    is_solo_parent BOOLEAN NOT NULL DEFAULT FALSE,
    is_raised_by_solo_parent BOOLEAN NOT NULL DEFAULT FALSE,
    solo_parent_id_number VARCHAR(60) NULL,

    -- DSWD Orphan Status
    is_orphan BOOLEAN NOT NULL DEFAULT FALSE,

    -- DOH AO 2020-0023 (GIDA)
    is_gida_resident BOOLEAN NOT NULL DEFAULT FALSE,
    gida_barangay_residence VARCHAR(150) NULL,

    -- RA 8435 / RA 11321 (Farmer / Fisherfolk)
    is_farmer_fisherfolk BOOLEAN NOT NULL DEFAULT FALSE,
    rsbsa_registration_number VARCHAR(60) NULL,

    -- EO 70 s. 2018 (Rebel Returnees / E-CLIP)
    is_rebel_returnee_family BOOLEAN NOT NULL DEFAULT FALSE,
    certificate_of_surrender_number VARCHAR(60) NULL,

    -- RA 10931 Sec 7/9 (Income Bracket & Bottom 40%)
    is_bottom_40_income_bracket BOOLEAN NOT NULL DEFAULT FALSE,
    monthly_household_income_bracket VARCHAR(40) NOT NULL DEFAULT 'POOR_BELOW_10K',

    -- First Generation College Student
    is_first_generation_college BOOLEAN NOT NULL DEFAULT FALSE,

    -- Verification & Governance Audit Metadata
    verification_status VARCHAR(25) NOT NULL DEFAULT 'SELF_DECLARED',
    verified_by_user_id BIGINT NULL,
    verified_at DATETIME NULL,
    verification_remarks VARCHAR(255) NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_equity_student_profile FOREIGN KEY (student_profile_id) REFERENCES student_profiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_equity_verified_by FOREIGN KEY (verified_by_user_id) REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_equity_composite_flags (
        is_4ps_beneficiary,
        is_indigenous_people,
        is_person_with_disability,
        is_gida_resident,
        is_farmer_fisherfolk,
        is_bottom_40_income_bracket
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
