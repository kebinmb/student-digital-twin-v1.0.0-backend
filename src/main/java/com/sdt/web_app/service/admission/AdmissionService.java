package com.sdt.web_app.service.admission;

import com.sdt.web_app.dto.admission.AdmissionDtos.*;
import com.sdt.web_app.entities.admission.AdmissionApplication;
import com.sdt.web_app.entities.admission.AdmissionConfig;
import com.sdt.web_app.entities.admission.EntranceExamSlot;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.institution.Program;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.repositories.admission.AdmissionApplicationRepository;
import com.sdt.web_app.repositories.admission.AdmissionConfigRepository;
import com.sdt.web_app.repositories.admission.EntranceExamSlotRepository;
import com.sdt.web_app.repositories.institution.ProgramRepository;
import com.sdt.web_app.repositories.institution.TermRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.sdt.web_app.exceptions.QueueSessionExpiredException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdmissionService {

    private final AdmissionApplicationRepository admissionApplicationRepository;
    private final EntranceExamSlotRepository entranceExamSlotRepository;
    private final AdmissionConfigRepository admissionConfigRepository;
    private final ProgramRepository programRepository;
    private final TermRepository termRepository;
    private final com.sdt.web_app.service.institution.TermService termService;
    private final AdmissionQueueService admissionQueueService;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional(readOnly = true)
    public List<PublicProgramDto> getPublicPrograms() {
        return programRepository.findAll().stream()
                .filter(Program::isActive)
                .map(p -> new PublicProgramDto(p.getId(), p.getCode(), p.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicTermDto> getPublicTerms() {
        return termService.getAllTerms().stream()
                .map(t -> new PublicTermDto(
                        t.getId(),
                        t.getAcademicYear() != null ? t.getAcademicYear().getCode() : "AY",
                        t.getTermType() != null ? t.getTermType().name() : "TERM",
                        t.isActive()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdmissionConfigDto getAdmissionConfig(Long termId) {
        Long resolvedTermId = termId != null ? termId : getActiveTermId();
        AdmissionConfig config = admissionConfigRepository.findByTermId(resolvedTermId)
                .orElseGet(() -> {
                    Term term = null;
                    try {
                        term = termService.getTermById(resolvedTermId);
                    } catch (Exception ignored) {}
                    return AdmissionConfig.builder()
                            .term(term)
                            .isActive(false)
                            .dailySlotLimit(1000)
                            .totalOpenedSlots(20000)
                            .daysOpen(20)
                            .build();
                });

        return mapToConfigDto(config);
    }

    @Transactional
    public AdmissionConfigDto updateAdmissionConfig(UpdateAdmissionConfigRequest request) {
        Term term = termService.getTermById(request.termId());

        AdmissionConfig config = admissionConfigRepository.findByTermId(term.getId())
                .orElseGet(() -> AdmissionConfig.builder().term(term).build());

        config.setActive(request.isActive());
        config.setDailySlotLimit(request.dailySlotLimit());
        config.setTotalOpenedSlots(request.totalOpenedSlots());
        config.recalculateDaysOpen();
        if (request.startDate() != null && !request.startDate().isBlank()) {
            config.setStartDate(LocalDate.parse(request.startDate()));
        }
        if (request.endDate() != null && !request.endDate().isBlank()) {
            config.setEndDate(LocalDate.parse(request.endDate()));
        }

        AdmissionConfig saved = admissionConfigRepository.save(config);
        log.info("Updated admission config for term {}: active={}, dailyLimit={}, totalSlots={}",
                term.getName(), saved.isActive(), saved.getDailySlotLimit(), saved.getTotalOpenedSlots());
        return mapToConfigDto(saved);
    }

    @Transactional(readOnly = true)
    public List<EntranceExamSlotResponse> getAvailableExamSlots(Long termId) {
        Long resolvedTermId = termId != null ? termId : getActiveTermId();
        Optional<AdmissionConfig> configOpt = admissionConfigRepository.findByTermId(resolvedTermId);
        if (configOpt.isEmpty() || !configOpt.get().isActive()) {
            return List.of();
        }

        List<EntranceExamSlot> slots = entranceExamSlotRepository
                .findByTermIdAndStatusOrderByExamDateAscStartTimeAsc(resolvedTermId, EntranceExamSlot.SlotStatus.OPEN);

        return slots.stream()
                .map(this::mapToSlotResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EntranceExamSlotResponse> getAllExamSlotsForAdmin(Long termId) {
        Long resolvedTermId = termId != null ? termId : getActiveTermId();
        List<EntranceExamSlot> slots = entranceExamSlotRepository.findByTermId(resolvedTermId);
        return slots.stream()
                .map(this::mapToSlotResponse)
                .toList();
    }

    @Transactional
    public EntranceExamSlotResponse createExamSlot(CreateExamSlotRequest request) {
        Term term = termService.getTermById(request.termId());

        LocalTime start = parseLocalTime(request.startTime());
        LocalTime end = parseLocalTime(request.endTime());

        EntranceExamSlot slot = EntranceExamSlot.builder()
                .term(term)
                .examDate(request.examDate())
                .startTime(start)
                .endTime(end)
                .venueRoom(request.venueRoom().trim())
                .maxCapacity(request.maxCapacity())
                .reservedCount(0)
                .status(EntranceExamSlot.SlotStatus.OPEN)
                .build();

        EntranceExamSlot saved = entranceExamSlotRepository.save(slot);
        log.info("Created entrance exam slot ID={} on {} at {}", saved.getId(), saved.getExamDate(), saved.getVenueRoom());
        return mapToSlotResponse(saved);
    }

    @Transactional
    public EntranceExamSlotResponse updateExamSlotStatus(Long slotId, String status) {
        EntranceExamSlot slot = entranceExamSlotRepository.findById(slotId)
                .orElseThrow(() -> new EntityNotFoundException("Entrance exam slot not found with ID: " + slotId));

        EntranceExamSlot.SlotStatus newStatus = EntranceExamSlot.SlotStatus.valueOf(status.trim().toUpperCase());
        slot.setStatus(newStatus);
        EntranceExamSlot updated = entranceExamSlotRepository.save(slot);
        log.info("Updated exam slot ID={} status to {}", slotId, newStatus);
        return mapToSlotResponse(updated);
    }

    @Transactional
    public void deleteExamSlot(Long slotId) {
        EntranceExamSlot slot = entranceExamSlotRepository.findById(slotId)
                .orElseThrow(() -> new EntityNotFoundException("Entrance exam slot not found with ID: " + slotId));

        if (slot.getReservedCount() > 0) {
            throw new IllegalStateException("Cannot delete exam slot ID " + slotId + " because " + slot.getReservedCount() + " applicant(s) have already booked this schedule. Change status to CANCELLED instead.");
        }

        entranceExamSlotRepository.delete(slot);
        log.info("Deleted unbooked exam slot ID={}", slotId);
    }

    private LocalTime parseLocalTime(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) return LocalTime.of(8, 0);
        String clean = timeStr.trim().toUpperCase();
        try {
            if (clean.contains("AM") || clean.contains("PM")) {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("hh:mm a");
                return LocalTime.parse(clean, fmt);
            }
            return LocalTime.parse(clean);
        } catch (Exception e) {
            return LocalTime.of(8, 0);
        }
    }

    @Transactional
    public AdmissionApplicationResponse submitApplication(SubmitAdmissionRequest request) {
        // 1. Term & Module Lock Check
        Term term = termService.getTermById(request.termId());

        Optional<AdmissionConfig> configOpt = admissionConfigRepository.findByTermId(term.getId());
        if (configOpt.isEmpty() || !configOpt.get().isActive()) {
            throw new IllegalStateException("Public admission applications are currently closed. Guidance / Admin has not activated this admission period.");
        }

        long countSubmitted = admissionApplicationRepository.countByTermId(term.getId());
        if (countSubmitted >= configOpt.get().getTotalOpenedSlots()) {
            throw new IllegalStateException("Admission slot limit reached. Total capacity of " + configOpt.get().getTotalOpenedSlots() + " applications has been met.");
        }

        // 2. Queue Token Validation
        if (request.queueToken() != null && !request.queueToken().isBlank()) {
            final String token = request.queueToken().trim();
            boolean valid = admissionQueueService.validateToken(token);
            if (!valid) {
                throw new QueueSessionExpiredException("QUEUE_SESSION_EXPIRED: Your queuing session has expired or is invalid. Please rejoin the queue to submit your application.");
            }
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        admissionQueueService.consumeToken(token);
                    }
                });
            } else {
                admissionQueueService.consumeToken(token);
            }
        }

        // 3. Program Validation
        Program program = programRepository.findById(request.targetProgramId())
                .orElseThrow(() -> new EntityNotFoundException("Target Academic Program not found with ID: " + request.targetProgramId()));

        // 4. Duplicate Application Check
        String email = request.email().trim();
        if (admissionApplicationRepository.existsByEmailIgnoreCaseAndTermId(email, term.getId())) {
            throw new IllegalStateException("An admission application with email '" + email + "' has already been submitted for " + term.getName() + ".");
        }

        if (request.lrnNumber() != null && !request.lrnNumber().isBlank()) {
            String lrn = request.lrnNumber().trim();
            if (admissionApplicationRepository.existsByLrnNumberAndTermId(lrn, term.getId())) {
                throw new IllegalStateException("An admission application with LRN '" + lrn + "' has already been submitted for " + term.getName() + ".");
            }
        }

        // 5. Atomic Entrance Exam Slot Reservation
        EntranceExamSlot reservedSlot = null;
        if (request.examSlotId() != null) {
            int updatedRows = entranceExamSlotRepository.incrementReservedCountIfOpen(request.examSlotId());
            if (updatedRows == 0) {
                throw new IllegalStateException("The selected entrance exam schedule slot is fully booked or no longer open. Please select another slot.");
            }
            reservedSlot = entranceExamSlotRepository.findById(request.examSlotId())
                    .orElseThrow(() -> new EntityNotFoundException("Exam slot not found with ID: " + request.examSlotId()));
        }

        // 6. Generate Application Number
        String appNum = generateUniqueApplicationNumber(term);

        // 7. Build & Persist Admission Application
        AdmissionApplication app = AdmissionApplication.builder()
                .applicationNumber(appNum)
                .targetProgram(program)
                .term(term)
                .examSlot(reservedSlot)
                .firstName(request.firstName().trim())
                .middleName(request.middleName() != null ? request.middleName().trim() : null)
                .lastName(request.lastName().trim())
                .suffix(request.suffix() != null ? request.suffix().trim() : null)
                .birthDate(request.birthDate())
                .birthPlace(request.birthPlace() != null ? request.birthPlace().trim() : null)
                .gender(request.gender() != null ? request.gender().trim().toUpperCase() : "FEMALE")
                .genderIdentity(request.genderIdentity() != null ? request.genderIdentity().trim() : null)
                .civilStatus(request.civilStatus() != null ? request.civilStatus().trim().toUpperCase() : "SINGLE")
                .citizenship(request.citizenship() != null ? request.citizenship().trim().toUpperCase() : "FILIPINO")
                .mobileNumber(request.mobileNumber().trim())
                .email(email)
                .lrnNumber(request.lrnNumber() != null ? request.lrnNumber().trim() : null)
                .highSchoolName(request.highSchoolName().trim())
                .depedSchoolId(request.depedSchoolId() != null ? request.depedSchoolId().trim() : null)
                .highSchoolType(request.highSchoolType() != null ? request.highSchoolType().trim().toUpperCase() : "PUBLIC")
                .shsTrackAndStrand(request.shsTrackAndStrand() != null ? request.shsTrackAndStrand().trim() : null)
                .highSchoolGwa(request.highSchoolGwa())
                .shsYearGraduated(request.shsYearGraduated())
                .streetAddress(request.streetAddress().trim())
                .barangay(request.barangay().trim())
                .cityMunicipality(request.cityMunicipality().trim())
                .province(request.province().trim())
                .zipCode(request.zipCode() != null ? request.zipCode().trim() : null)
                .permRegion(request.permRegion() != null ? request.permRegion().trim() : null)
                .permProvince(request.permProvince() != null ? request.permProvince().trim() : null)
                .permCityMunicipality(request.permCityMunicipality() != null ? request.permCityMunicipality().trim() : null)
                .permBarangay(request.permBarangay() != null ? request.permBarangay().trim() : null)
                .permZipCode(request.permZipCode() != null ? request.permZipCode().trim() : null)
                .permStreetAddress(request.permStreetAddress() != null ? request.permStreetAddress().trim() : null)
                .emergencyContactName(request.emergencyContactName().trim())
                .emergencyContactRelationship(request.emergencyContactRelationship().trim())
                .emergencyContactNumber(request.emergencyContactNumber().trim())
                .emergencyContactEmail(request.emergencyContactEmail() != null ? request.emergencyContactEmail().trim() : null)
                .is4psBeneficiary(Boolean.TRUE.equals(request.is4psBeneficiary()))
                .household4psIdNumber(request.household4psIdNumber() != null ? request.household4psIdNumber().trim() : null)
                .isIndigenousPeople(Boolean.TRUE.equals(request.isIndigenousPeople()))
                .ipEthnicGroup(request.ipEthnicGroup() != null ? request.ipEthnicGroup().trim() : null)
                .ncipCertificateNumber(request.ncipCertificateNumber() != null ? request.ncipCertificateNumber().trim() : null)
                .isPersonWithDisability(Boolean.TRUE.equals(request.isPersonWithDisability()))
                .disabilityType(request.disabilityType() != null ? request.disabilityType().trim() : null)
                .pwdIdNumber(request.pwdIdNumber() != null ? request.pwdIdNumber().trim() : null)
                .isSoloParent(Boolean.TRUE.equals(request.isSoloParent()))
                .isRaisedBySoloParent(Boolean.TRUE.equals(request.isRaisedBySoloParent()))
                .soloParentIdNumber(request.soloParentIdNumber() != null ? request.soloParentIdNumber().trim() : null)
                .isOrphan(Boolean.TRUE.equals(request.isOrphan()))
                .isGidaResident(Boolean.TRUE.equals(request.isGidaResident()))
                .gidaBarangayResidence(request.gidaBarangayResidence() != null ? request.gidaBarangayResidence().trim() : null)
                .isFarmerFisherfolk(Boolean.TRUE.equals(request.isFarmerFisherfolk()))
                .rsbsaRegistrationNumber(request.rsbsaRegistrationNumber() != null ? request.rsbsaRegistrationNumber().trim() : null)
                .isRebelReturneeFamily(Boolean.TRUE.equals(request.isRebelReturneeFamily()))
                .certificateOfSurrenderNumber(request.certificateOfSurrenderNumber() != null ? request.certificateOfSurrenderNumber().trim() : null)
                .isBottom40IncomeBracket(Boolean.TRUE.equals(request.isBottom40IncomeBracket()))
                .monthlyHouseholdIncomeBracket(request.monthlyHouseholdIncomeBracket() != null ? request.monthlyHouseholdIncomeBracket().trim() : "POOR_BELOW_10K")
                .isFirstGenerationCollege(Boolean.TRUE.equals(request.isFirstGenerationCollege()))
                .isUnderprivilegedHomeless(Boolean.TRUE.equals(request.isUnderprivilegedHomeless()))
                .scholarshipGrantType(request.scholarshipGrantType() != null ? request.scholarshipGrantType().trim() : null)
                .queueToken(request.queueToken())
                .applicationStatus(AdmissionApplication.ApplicationStatus.SUBMITTED)
                .build();

        AdmissionApplication savedApp = admissionApplicationRepository.save(app);
        log.info("Successfully submitted public admission application {} for {} {}", savedApp.getApplicationNumber(), savedApp.getFirstName(), savedApp.getLastName());

        return mapToApplicationResponse(savedApp);
    }

    @Transactional(readOnly = true)
    public AdmissionApplicationResponse getApplicationByNumber(String applicationNumber) {
        String cleanNum = applicationNumber != null ? applicationNumber.trim() : "";
        AdmissionApplication app = admissionApplicationRepository.findByApplicationNumber(cleanNum)
                .orElseThrow(() -> new EntityNotFoundException("Admission Application not found with number: " + cleanNum));
        return mapToApplicationResponse(app);
    }

    @Transactional(readOnly = true)
    public List<AdmissionApplicationResponse> getAllApplications(Long termId, String status) {
        Long resolvedTermId = termId != null ? termId : getActiveTermId();
        List<AdmissionApplication> apps;
        if (status != null && !status.isBlank()) {
            if ("UNCLAIMED".equalsIgnoreCase(status.trim())) {
                return getUnclaimedApprovedApplications(resolvedTermId);
            }
            AdmissionApplication.ApplicationStatus appStatus = AdmissionApplication.ApplicationStatus.valueOf(status.trim().toUpperCase());
            apps = admissionApplicationRepository.findByTermIdAndApplicationStatus(resolvedTermId, appStatus);
        } else {
            apps = admissionApplicationRepository.findByTermId(resolvedTermId);
        }
        return apps.stream().map(this::mapToApplicationResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AdmissionApplicationResponse> getUnclaimedApprovedApplications(Long termId) {
        Long resolvedTermId = termId != null ? termId : getActiveTermId();
        List<AdmissionApplication> apps = admissionApplicationRepository.findUnclaimedApprovedApplications(resolvedTermId);
        return apps.stream().map(this::mapToApplicationResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AdmissionApplicationResponse> getApplicationsForProgram(Long programId, String status) {
        Long resolvedTermId = getActiveTermId();
        List<AdmissionApplication> apps = admissionApplicationRepository.findByTermId(resolvedTermId).stream()
                .filter(a -> a.getTargetProgram().getId().equals(programId))
                .filter(a -> {
                    if (status == null || status.isBlank()) return true;
                    return a.getApplicationStatus().name().equalsIgnoreCase(status.trim());
                })
                .toList();

        return apps.stream().map(this::mapToApplicationResponse).toList();
    }

    @Transactional
    public AdmissionApplicationResponse evaluateExam(Long id, EvaluateExamRequest request, User evaluator) {
        AdmissionApplication app = admissionApplicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Admission Application not found with ID: " + id));

        AdmissionApplication.ApplicationStatus decisionStatus = AdmissionApplication.ApplicationStatus.valueOf(request.status().trim().toUpperCase());
        if (decisionStatus != AdmissionApplication.ApplicationStatus.EXAM_PASSED && decisionStatus != AdmissionApplication.ApplicationStatus.EXAM_FAILED) {
            throw new IllegalArgumentException("Invalid exam decision status. Must be EXAM_PASSED or EXAM_FAILED.");
        }

        app.setExamScore(request.examScore());
        app.setExamRemarks(request.examRemarks() != null ? request.examRemarks().trim() : null);
        app.setApplicationStatus(decisionStatus);
        app.setEvaluatedBy(evaluator);

        AdmissionApplication updated = admissionApplicationRepository.save(app);
        log.info("Guidance evaluated exam for application {}: score={}, status={}", updated.getApplicationNumber(), request.examScore(), decisionStatus);
        return mapToApplicationResponse(updated);
    }

    @Transactional
    public AdmissionApplicationResponse evaluateInterview(Long id, EvaluateInterviewRequest request, User interviewer) {
        AdmissionApplication app = admissionApplicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Admission Application not found with ID: " + id));

        if (app.getApplicationStatus() != AdmissionApplication.ApplicationStatus.EXAM_PASSED) {
            throw new IllegalStateException("Application must be in EXAM_PASSED status before Program Chair interview evaluation.");
        }

        AdmissionApplication.ApplicationStatus decisionStatus = AdmissionApplication.ApplicationStatus.valueOf(request.status().trim().toUpperCase());
        if (decisionStatus != AdmissionApplication.ApplicationStatus.INTERVIEW_ACCEPTED && decisionStatus != AdmissionApplication.ApplicationStatus.REJECTED) {
            throw new IllegalArgumentException("Invalid interview decision status. Must be INTERVIEW_ACCEPTED or REJECTED.");
        }

        app.setInterviewScore(request.interviewScore());
        app.setInterviewRemarks(request.interviewRemarks() != null ? request.interviewRemarks().trim() : null);
        app.setApplicationStatus(decisionStatus == AdmissionApplication.ApplicationStatus.INTERVIEW_ACCEPTED
                ? AdmissionApplication.ApplicationStatus.ELIGIBLE_FOR_ENROLLMENT
                : AdmissionApplication.ApplicationStatus.REJECTED);
        app.setInterviewedBy(interviewer);

        AdmissionApplication updated = admissionApplicationRepository.save(app);
        log.info("Program Chair evaluated interview for application {}: score={}, status={}", updated.getApplicationNumber(), request.interviewScore(), updated.getApplicationStatus());
        return mapToApplicationResponse(updated);
    }

    @Transactional
    public AdmissionApplicationResponse updateApplicationStatus(Long id, UpdateAdmissionStatusRequest request) {
        AdmissionApplication app = admissionApplicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Admission Application not found with ID: " + id));

        AdmissionApplication.ApplicationStatus newStatus = AdmissionApplication.ApplicationStatus.valueOf(request.applicationStatus().trim().toUpperCase());
        app.setApplicationStatus(newStatus);
        AdmissionApplication updated = admissionApplicationRepository.save(app);
        log.info("Updated admission application {} status to {}", updated.getApplicationNumber(), newStatus);
        return mapToApplicationResponse(updated);
    }

    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email, Long termId) {
        if (email == null || email.isBlank()) {
            return false;
        }
        Long resolvedTermId = termId != null ? termId : getActiveTermId();
        return !admissionApplicationRepository.existsByEmailIgnoreCaseAndTermId(email.trim(), resolvedTermId);
    }

    private Long getActiveTermId() {
        return termRepository.findByIsActiveTrue()
                .map(Term::getId)
                .orElse(4L);
    }

    private String generateUniqueApplicationNumber(Term term) {
        int year = LocalDate.now().getYear();
        for (int attempt = 0; attempt < 10; attempt++) {
            int randomSeq = 10000 + RANDOM.nextInt(90000);
            String candidate = "ADM-" + year + "-" + randomSeq;
            if (admissionApplicationRepository.findByApplicationNumber(candidate).isEmpty()) {
                return candidate;
            }
        }
        return "ADM-" + year + "-" + System.currentTimeMillis() % 100000;
    }

    private AdmissionConfigDto mapToConfigDto(AdmissionConfig cfg) {
        return new AdmissionConfigDto(
                cfg.getId(),
                cfg.getTerm() != null ? cfg.getTerm().getId() : null,
                cfg.getTerm() != null ? cfg.getTerm().getName() : "Term",
                cfg.isActive(),
                cfg.getDailySlotLimit(),
                cfg.getTotalOpenedSlots(),
                cfg.getDaysOpen(),
                cfg.getStartDate() != null ? cfg.getStartDate().toString() : null,
                cfg.getEndDate() != null ? cfg.getEndDate().toString() : null
        );
    }

    private EntranceExamSlotResponse mapToSlotResponse(EntranceExamSlot slot) {
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("hh:mm a");
        return new EntranceExamSlotResponse(
                slot.getId(),
                slot.getTerm().getId(),
                slot.getExamDate(),
                slot.getStartTime() != null ? slot.getStartTime().format(timeFmt) : "",
                slot.getEndTime() != null ? slot.getEndTime().format(timeFmt) : "",
                slot.getVenueRoom(),
                slot.getMaxCapacity(),
                slot.getReservedCount(),
                slot.getAvailableSeats(),
                slot.getStatus().name()
        );
    }

    private AdmissionApplicationResponse mapToApplicationResponse(AdmissionApplication app) {
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("hh:mm a");
        EntranceExamSlot slot = app.getExamSlot();
        return new AdmissionApplicationResponse(
                app.getId(),
                app.getApplicationNumber(),
                app.getTargetProgram().getId(),
                app.getTargetProgram().getCode(),
                app.getTargetProgram().getName(),
                app.getTerm().getId(),
                app.getTerm().getName(),
                slot != null ? slot.getId() : null,
                slot != null ? slot.getExamDate() : null,
                (slot != null && slot.getStartTime() != null) ? slot.getStartTime().format(timeFmt) : null,
                (slot != null && slot.getEndTime() != null) ? slot.getEndTime().format(timeFmt) : null,
                slot != null ? slot.getVenueRoom() : null,
                app.getFirstName(),
                app.getMiddleName(),
                app.getLastName(),
                app.getSuffix(),
                app.getFullName(),
                app.getBirthDate(),
                app.getBirthPlace(),
                app.getGender(),
                app.getGenderIdentity(),
                app.getCivilStatus(),
                app.getCitizenship(),
                app.getMobileNumber(),
                app.getEmail(),
                app.getLrnNumber(),
                app.getHighSchoolName(),
                app.getDepedSchoolId(),
                app.getHighSchoolType(),
                app.getShsTrackAndStrand(),
                app.getHighSchoolGwa(),
                app.getShsYearGraduated(),
                app.getStreetAddress(),
                app.getBarangay(),
                app.getCityMunicipality(),
                app.getProvince(),
                app.getZipCode(),
                app.getPermRegion(),
                app.getPermProvince(),
                app.getPermCityMunicipality(),
                app.getPermBarangay(),
                app.getPermZipCode(),
                app.getPermStreetAddress(),
                app.getEmergencyContactName(),
                app.getEmergencyContactRelationship(),
                app.getEmergencyContactNumber(),
                app.getEmergencyContactEmail(),
                app.is4psBeneficiary(),
                app.getHousehold4psIdNumber(),
                app.isIndigenousPeople(),
                app.getIpEthnicGroup(),
                app.getNcipCertificateNumber(),
                app.isPersonWithDisability(),
                app.getDisabilityType(),
                app.getPwdIdNumber(),
                app.isSoloParent(),
                app.isRaisedBySoloParent(),
                app.getSoloParentIdNumber(),
                app.isOrphan(),
                app.isGidaResident(),
                app.getGidaBarangayResidence(),
                app.isFarmerFisherfolk(),
                app.getRsbsaRegistrationNumber(),
                app.isRebelReturneeFamily(),
                app.getCertificateOfSurrenderNumber(),
                app.isBottom40IncomeBracket(),
                app.getMonthlyHouseholdIncomeBracket(),
                app.isFirstGenerationCollege(),
                app.isUnderprivilegedHomeless(),
                app.getScholarshipGrantType(),
                app.getQueueToken(),
                app.getApplicationStatus().name(),
                app.getExamScore(),
                app.getExamRemarks(),
                app.getInterviewScore(),
                app.getInterviewRemarks(),
                app.getEvaluatedBy() != null ? app.getEvaluatedBy().getUsername() : null,
                app.getInterviewedBy() != null ? app.getInterviewedBy().getUsername() : null,
                app.getCreatedAt() != null ? app.getCreatedAt().toString() : null,
                app.isEnrolled(),
                app.getStudentProfileId(),
                app.getEnrolledAt() != null ? app.getEnrolledAt().toString() : null
        );
    }
}
