### Phase 1: Foundation Layer

**System Administration, RBAC & Master Setup**

- **Role-Based Access Control (RBAC):** Define permission scopes for Superadmins, Deans, Program Chairs, Faculty, Guidance Counselors, Cashiers, Registrars, and Students in strict compliance with RA 10173 (Philippine Data Privacy Act of 2012).
    
- **Institutional Master Tables:** Academic calendars, campuses, terms/semesters, colleges, academic departments, and institutional grading transmutation scales.
    
- **Financial Master Catalog:** CHED-sanctioned tuition rates, miscellaneous fee catalogs, and billing matrices for RA 10931 (Universal Access to Quality Tertiary Education Act / UniFAST) and Tertiary Education Subsidy (TES).

### Phase 2: Academic Backbone

**Curriculum & Outcome-Based Education (OBE) Architecture**

- **Program Registry:** Programs and majors mapped to their official CHED Memorandum Orders (CMOs) and government authority/permit numbers.
    
- **Course Catalog:** Subject codes, descriptive titles, lecture/laboratory credit units, contact hours, and strict prerequisite/co-requisite rule trees.
    
- **OBE Mapping:** Setup of Institutional Intended Learning Outcomes (IILOs), Program Intended Learning Outcomes (PILOs), and Course Intended Learning Outcomes (CILOs).

### Phase 3: Human Entities & Baseline Profiling

**User Identity, Equity Intake & Scheduling**

- **`Module 00` Equity Target Profiling:** Admissions intake profiling capturing household income brackets, 4Ps beneficiary status, Indigenous Peoples (IP) affiliation, PWD classification, solo parent dependents, and first-generation college student markers aligned with RA 10687 (UniFAST Act) equity goals.
    
- **Faculty Credential Management:** Faculty profiling, highest degree attained, PRC professional licenses, academic ranks, and teaching assignments for CHED Form E-5 compliance.
    
- **Facility & Scheduling Matrix:** Classroom and laboratory inventory, geofence coordinates for campus venues, section quotas, and conflict-free timetable generation.

### Phase 4: Operational Core

**Enrollment, Advising & Financial Billing**

- **Advising & Registration Engine:** Automated prerequisite checking, maximum unit load verification, subject adding/dropping, and generation of the official Certificate of Registration (COR).
    
- **Assessment & FHE Billing:** Real-time fee assessment, scholarship deduction logic, and generation of Free Higher Education (FHE) billing manifests for CHED/UniFAST reimbursement.
    
- **Class Roster Population:** Dispatches registered student lists into class databases to initialize attendance sheets and continuous assessment gradebooks.

### Phase 5: Daily Operations & Live Telemetry

**Attendance Tracking & Real-Time Performance**

- **`Module 01` Attendance Monitoring:** Secure attendance capture using dynamic rotating QR codes with temporal expiration and GPS geofencing to prevent proxy attendance, logging timestamped telemetry for both faculty and students.
    
- **Continuous Assessment Tracking:** Gradebook recording for formative assessments (quizzes, laboratory exercises, seatworks) and summative assessments (midterm/final examinations).
    
- **`Module 02` Academic Performance Dashboard:** Aggregates live attendance and continuous assessment scores into OBE-aligned weighted standings, diagnostic visual charts, and preliminary transmutation estimates for students, faculty, and academic advisors.

### Phase 6: Intelligence & Intervention Layer

**Diagnostic Risk Scoring & Machine Learning Forecasting**

- **`Module 03` Student Risk Assessment & Early Warning:** Rule-based diagnostic scoring combining live operational telemetry (attendance dips, failing formative marks) with baseline vulnerability flags from **Module 00** to produce a multidimensional Student Risk Index.
    
- **`Module 04` Predictive Analytics for Vulnerable Students:** Supervised classification and time-series ML models trained on historical cohort trends to detect subtle performance degradation and forecast failure or dropout risk weeks in advance.
    
- **Intervention Dispatch Workflow:** Automated alert routing to Guidance and Counseling offices, academic advisors, and Dean portals for timely academic and socioeconomic support.

### Phase 7: End-of-Term Processing & Records

**Registrar Operations, Transmutation & Credentials**

- **Grade Encoding & Sealing:** Final grade transmutation, electronic approval workflows from Faculty to Dean to Registrar, and grade locking with complete audit trails.
    
- **Academic Evaluation:** Running General Weighted Average (GWA) computation, academic honors verification, retention/probation tagging, and prerequisite clearance for the next cycle.
    
- **Official Records Issuance:** Generation of permanent Transcript of Records (TOR), Certifications of Grades, Honorable Dismissals, and CHED Special Order (SO) graduation audit packets.

### Phase 8: Statutory Regulatory Layer

**CHED HEMIS / HEIDA & Institutional Analytics**

- **HEMIS / CHECKS Generation:** Automated aggregation and export of standard electronic reporting tables:
    
    - **Form E-1 / E-2:** Institutional and program profile data.
        
    - **Form E-3:** Enrollment statistics disaggregated by program, year level, and sex.
        
    - **Form E-4:** Graduate statistics by program and academic year.
        
    - **Form E-5:** Faculty profile, employment status, workload, and educational credentials.
        
- **UniFAST & TES Auditing:** Compliance data extraction for national subsidies and equity monitoring audits.

|**Layer**|**Module Name**|**Direct Upstream Dependencies**|**Primary Downstream Consumers**|
|---|---|---|---|
|**01**|System Master Data & RBAC|_None (Root Configuration)_|All Modules|
|**02**|Curriculum & OBE Architecture|Layer 01|Advising, Scheduling, Dashboards|
|**03**|**00 Equity Target Profiling**|Layer 01 (Admissions Intake)|**03 Risk Assessment**, **04 Predictive Analytics**, UniFAST|
|**03**|Faculty & Scheduling|Layer 01, Layer 02|**01 Attendance**, Enrollment Rosters|
|**04**|Enrollment & Billing Assessment|Layer 02, Layer 03 (Profiles & Schedules)|**01 Attendance**, Registrar, Billing Extracts|
|**05**|**01 Attendance Monitoring**|Layer 03 (Geofences/Times), Layer 04 (Rosters)|**02 Dashboards**, **03 Risk Assessment**, **04 Predictive ML**|
|**05**|**02 Academic Performance Dashboard**|Layer 05 (**01 Attendance**), Faculty Gradebooks|**03 Risk Assessment**, Student/Faculty Portals|
|**06**|**03 Student Risk Assessment**|Layer 03 (**00 Equity**), Layer 05 (**01 Attendance**, **02 Dashboard**)|Guidance & Counseling Alerts, Advising|
|**06**|**04 Predictive Analytics**|Historical Data, Layer 03 (**00 Equity**), Layer 05 & 06 Telemetry|Proactive Interventions, Retention Planning|
|**07**|Registrar Operations (TOR / SO)|Layer 04 (Enrollment), Layer 05 (Final Grades)|Graduation Packets, Permanent Records|
|**08**|CHED HEMIS / HEIDA Extraction|Layers 01 through 07 (Full Master & Transactional Data)|CHED, UniFAST, Institutional Research|