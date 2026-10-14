package com.sdt.web_app.service.scheduling;

import com.sdt.web_app.config.WebSocketBroadcastService;
import com.sdt.web_app.config.WebSocketTopics;
import com.sdt.web_app.dto.scheduling.SchedulingOverrideDtos.*;
import com.sdt.web_app.entities.authentication.Roles;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.faculty.FacultyProfile;
import com.sdt.web_app.entities.institution.Department;
import com.sdt.web_app.entities.institution.Program;
import com.sdt.web_app.entities.scheduling.*;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.faculty.FacultyProfileRepository;
import com.sdt.web_app.repositories.institution.DepartmentRepository;
import com.sdt.web_app.repositories.institution.ProgramRepository;
import com.sdt.web_app.repositories.scheduling.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulingOverrideService {

    private final SchedulingOverrideRequestRepository requestRepository;
    private final SchedulingOverrideApprovalRepository approvalRepository;
    private final ClassSectionRepository sectionRepository;
    private final ClassScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final FacultyProfileRepository facultyProfileRepository;
    private final ProgramRepository programRepository;
    private final DepartmentRepository departmentRepository;
    private final RoomRepository roomRepository;
    private final FacultyWorkloadRepository workloadRepository;
    private final WebSocketBroadcastService broadcastService;

    /**
     * Inspect a prospective faculty assignment against target section.
     */
    @Transactional(readOnly = true)
    public OverrideValidationResponse validateAssignment(Long sectionId, Long facultyUserId) {
        ClassSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new EntityNotFoundException("Class section not found with id: " + sectionId));

        User facultyUser = userRepository.findById(facultyUserId)
                .orElseThrow(() -> new EntityNotFoundException("Faculty user not found with id: " + facultyUserId));

        FacultyProfile facultyProfile = facultyProfileRepository.findByUserId(facultyUserId).orElse(null);

        // Resolve Target Program and Target College
        Program targetProgram = null;
        if (section.getCurriculum() != null && section.getCurriculum().getProgram() != null) {
            targetProgram = section.getCurriculum().getProgram();
        }

        Department targetCollege = null;
        if (targetProgram != null) {
            targetCollege = targetProgram.getCollege();
            if (targetCollege == null && targetProgram.getDepartment() != null) {
                targetCollege = targetProgram.getDepartment();
                if (targetCollege.getParentDepartment() != null) {
                    targetCollege = targetCollege.getParentDepartment();
                }
            }
        }

        // Resolve Faculty Home Program and Home College
        Program homeProgram = null;
        Department homeCollege = null;

        if (facultyProfile != null) {
            homeProgram = facultyProfile.getProgram();
            homeCollege = facultyProfile.getCollege();
        }

        if (homeProgram == null && facultyUser.getProgram() != null) {
            homeProgram = facultyUser.getProgram();
        }
        if (homeCollege == null && facultyUser.getCollege() != null) {
            homeCollege = facultyUser.getCollege();
        }

        if (homeProgram != null && homeCollege == null) {
            homeCollege = homeProgram.getCollege();
            if (homeCollege == null && homeProgram.getDepartment() != null) {
                homeCollege = homeProgram.getDepartment();
                if (homeCollege.getParentDepartment() != null) {
                    homeCollege = homeCollege.getParentDepartment();
                }
            }
        }

        List<String> missingAuthorities = new ArrayList<>();
        if (homeProgram == null) {
            missingAuthorities.add("Faculty member has no designated home academic program.");
        }
        if (homeCollege == null) {
            missingAuthorities.add("Faculty member has no designated home college.");
        }

        // Resolve authorities
        User chairperson = null;
        if (homeProgram != null) {
            if (homeProgram.getChairpersonUserId() != null) {
                chairperson = userRepository.findById(homeProgram.getChairpersonUserId()).orElse(null);
            }
            if (chairperson == null) {
                missingAuthorities.add("Home program '" + homeProgram.getName() + "' has no assigned Program Chairperson.");
            }
        }

        User dean = null;
        if (homeCollege != null) {
            if (homeCollege.getDeanUserId() != null) {
                dean = userRepository.findById(homeCollege.getDeanUserId()).orElse(null);
            }
            if (dean == null) {
                missingAuthorities.add("Home college '" + homeCollege.getName() + "' has no assigned College Dean.");
            }
        }

        // Determine mismatch
        boolean isCrossProgram = false;
        boolean isCrossCollege = false;

        Long homeProgramId = homeProgram != null ? homeProgram.getId() : null;
        Long targetProgramId = targetProgram != null ? targetProgram.getId() : null;
        Long homeCollegeId = homeCollege != null ? homeCollege.getId() : null;
        Long targetCollegeId = targetCollege != null ? targetCollege.getId() : null;

        if (homeCollegeId != null && targetCollegeId != null && !homeCollegeId.equals(targetCollegeId)) {
            isCrossCollege = true;
        } else if (homeProgramId != null && targetProgramId != null && !homeProgramId.equals(targetProgramId)) {
            isCrossProgram = true;
        }

        boolean isMismatch = isCrossProgram || isCrossCollege;
        String overrideType = isCrossCollege ? "CROSS_COLLEGE" : (isCrossProgram ? "CROSS_PROGRAM" : "NONE");

        List<String> requiredSteps = new ArrayList<>();
        if (isCrossCollege) {
            requiredSteps.add("Home Program Chairperson Approval (" + (chairperson != null ? chairperson.getUsername() : "Unassigned") + ")");
            requiredSteps.add("Home College Dean Approval (" + (dean != null ? dean.getUsername() : "Unassigned") + ")");
            requiredSteps.add("Assigned Faculty Acceptance (" + resolveFacultyFullName(facultyUser, facultyProfile) + ")");
        } else if (isCrossProgram) {
            requiredSteps.add("Home Program Chairperson Approval (" + (chairperson != null ? chairperson.getUsername() : "Unassigned") + ")");
            requiredSteps.add("Assigned Faculty Acceptance (" + resolveFacultyFullName(facultyUser, facultyProfile) + ")");
        }

        return new OverrideValidationResponse(
                isMismatch,
                overrideType,
                facultyUser.getId(),
                resolveFacultyFullName(facultyUser, facultyProfile),
                facultyUser.getEmail(),
                homeProgramId,
                homeProgram != null ? homeProgram.getName() : null,
                homeCollegeId,
                homeCollege != null ? homeCollege.getName() : null,
                targetProgramId,
                targetProgram != null ? targetProgram.getName() : null,
                targetCollegeId,
                targetCollege != null ? targetCollege.getName() : null,
                chairperson != null ? chairperson.getId() : null,
                chairperson != null ? resolveUserFullName(chairperson) : null,
                dean != null ? dean.getId() : null,
                dean != null ? resolveUserFullName(dean) : null,
                !missingAuthorities.isEmpty(),
                missingAuthorities,
                requiredSteps
        );
    }

    /**
     * Create provisional override request and its approval steps.
     */
    @Transactional
    public SchedulingOverrideResponse createOverrideRequest(
            Long sectionId,
            Long scheduleId,
            Long facultyUserId,
            String justification,
            Long requestedByUserId) {

        ClassSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new EntityNotFoundException("Class section not found: " + sectionId));

        ClassSchedule schedule = null;
        if (scheduleId != null) {
            schedule = scheduleRepository.findById(scheduleId)
                    .orElseThrow(() -> new EntityNotFoundException("Class schedule not found: " + scheduleId));
        }

        User facultyUser = userRepository.findById(facultyUserId)
                .orElseThrow(() -> new EntityNotFoundException("Faculty user not found: " + facultyUserId));

        User requestedByUser = userRepository.findById(requestedByUserId)
                .orElseThrow(() -> new EntityNotFoundException("Requesting user not found: " + requestedByUserId));

        FacultyProfile facultyProfile = facultyProfileRepository.findByUserId(facultyUserId).orElse(null);

        OverrideValidationResponse validation = validateAssignment(sectionId, facultyUserId);
        if (!validation.isMismatch()) {
            throw new IllegalArgumentException("Assignment is within the faculty member's home program. No override is required.");
        }

        Program targetProgram = section.getCurriculum().getProgram();
        Department targetCollege = targetProgram.getCollege();
        if (targetCollege == null && targetProgram.getDepartment() != null) {
            targetCollege = targetProgram.getDepartment();
            if (targetCollege.getParentDepartment() != null) {
                targetCollege = targetCollege.getParentDepartment();
            }
        }

        Program homeProgram = validation.homeProgramId() != null
                ? programRepository.findById(validation.homeProgramId()).orElse(null)
                : null;
        Department homeCollege = validation.homeCollegeId() != null
                ? departmentRepository.findById(validation.homeCollegeId()).orElse(null)
                : null;

        OverrideRequestStatus initialStatus = validation.hasMissingAuthorities()
                ? OverrideRequestStatus.CONFIGURATION_REQUIRED
                : OverrideRequestStatus.PENDING_APPROVAL;

        OverrideType overrideType = "CROSS_COLLEGE".equals(validation.overrideType())
                ? OverrideType.CROSS_COLLEGE
                : OverrideType.CROSS_PROGRAM;

        // Invalidate/supersede any existing pending override requests for this section / schedule
        List<SchedulingOverrideRequest> existingPending = requestRepository.findBySectionIdAndStatus(sectionId, OverrideRequestStatus.PENDING_APPROVAL);
        for (SchedulingOverrideRequest oldReq : existingPending) {
            oldReq.updateStatus(OverrideRequestStatus.SUPERSEDED, "Superseded by new override request");
            for (SchedulingOverrideApproval step : oldReq.getApprovals()) {
                if (step.getStatus() == ApprovalStepStatus.PENDING) {
                    step.cancel("Superseded by new override request");
                }
            }
            requestRepository.save(oldReq);
        }

        SchedulingOverrideRequest overrideReq = SchedulingOverrideRequest.builder()
                .section(section)
                .schedule(schedule)
                .facultyUser(facultyUser)
                .facultyProfile(facultyProfile)
                .requestedByUser(requestedByUser)
                .overrideType(overrideType)
                .homeProgram(homeProgram)
                .homeCollege(homeCollege)
                .targetProgram(targetProgram)
                .targetCollege(targetCollege)
                .status(initialStatus)
                .justification(justification)
                .resolutionNotes(validation.hasMissingAuthorities() ? String.join("; ", validation.missingAuthorityMessages()) : null)
                .build();

        // Step 1: Home Program Chairperson Approval
        User chairpersonUser = validation.assignedChairpersonId() != null
                ? userRepository.findById(validation.assignedChairpersonId()).orElse(null)
                : null;

        SchedulingOverrideApproval chairStep = SchedulingOverrideApproval.builder()
                .approvalType(ApprovalStepType.CHAIRPERSON_APPROVAL)
                .stepOrder(1)
                .requiredRole("ROLE_CHAIRPERSON")
                .assignedApproverUser(chairpersonUser)
                .status(validation.hasMissingAuthorities() ? ApprovalStepStatus.SKIPPED : ApprovalStepStatus.PENDING)
                .build();
        overrideReq.addApproval(chairStep);

        // Step 2: Home College Dean Approval (only if CROSS_COLLEGE)
        if (overrideType == OverrideType.CROSS_COLLEGE) {
            User deanUser = validation.assignedDeanId() != null
                    ? userRepository.findById(validation.assignedDeanId()).orElse(null)
                    : null;

            SchedulingOverrideApproval deanStep = SchedulingOverrideApproval.builder()
                    .approvalType(ApprovalStepType.DEAN_APPROVAL)
                    .stepOrder(2)
                    .requiredRole("ROLE_DEAN")
                    .assignedApproverUser(deanUser)
                    .status(validation.hasMissingAuthorities() ? ApprovalStepStatus.SKIPPED : ApprovalStepStatus.PENDING)
                    .build();
            overrideReq.addApproval(deanStep);
        }

        // Step 3 / 2: Faculty Acceptance
        int facultyStepOrder = (overrideType == OverrideType.CROSS_COLLEGE) ? 3 : 2;
        SchedulingOverrideApproval facultyStep = SchedulingOverrideApproval.builder()
                .approvalType(ApprovalStepType.FACULTY_ACCEPTANCE)
                .stepOrder(facultyStepOrder)
                .requiredRole("ROLE_FACULTY")
                .assignedApproverUser(facultyUser)
                .status(validation.hasMissingAuthorities() ? ApprovalStepStatus.SKIPPED : ApprovalStepStatus.PENDING)
                .build();
        overrideReq.addApproval(facultyStep);

        // Lock schedule
        if (schedule != null) {
            schedule.lock(overrideType.name());
            scheduleRepository.save(schedule);
        }

        SchedulingOverrideRequest saved = requestRepository.save(overrideReq);
        log.info("Created SchedulingOverrideRequest {} for section {} and faculty {}", saved.getId(), section.getId(), facultyUser.getId());

        notifyParticipants(saved);

        return mapToResponse(saved);
    }

    /**
     * Approve a specific approval step.
     */
    @Transactional
    public SchedulingOverrideResponse approveStep(Long requestId, Long approvalId, Long decisionMakerUserId, String remarks) {
        SchedulingOverrideRequest request = requestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Override request not found: " + requestId));

        if (request.getStatus() != OverrideRequestStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Cannot approve step for request in status: " + request.getStatus());
        }

        User decisionMaker = userRepository.findById(decisionMakerUserId)
                .orElseThrow(() -> new EntityNotFoundException("Decision maker user not found: " + decisionMakerUserId));

        SchedulingOverrideApproval targetStep = request.getApprovals().stream()
                .filter(a -> a.getId().equals(approvalId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Approval step not found with ID: " + approvalId));

        if (targetStep.getStatus() != ApprovalStepStatus.PENDING) {
            throw new IllegalStateException("Approval step is already in status: " + targetStep.getStatus());
        }

        // Validate authority
        validateApproverAuthority(targetStep, decisionMaker);

        targetStep.approve(decisionMaker, remarks);
        approvalRepository.save(targetStep);

        // Check if all steps are approved
        boolean allApproved = request.getApprovals().stream()
                .allMatch(a -> a.getStatus() == ApprovalStepStatus.APPROVED || a.getStatus() == ApprovalStepStatus.SKIPPED);

        if (allApproved) {
            // Re-run conflict detection before final activation
            activateScheduleAndFinalize(request);
        }

        SchedulingOverrideRequest saved = requestRepository.save(request);
        notifyParticipants(saved);
        return mapToResponse(saved);
    }

    /**
     * Reject a specific approval step.
     */
    @Transactional
    public SchedulingOverrideResponse rejectStep(Long requestId, Long approvalId, Long decisionMakerUserId, String remarks) {
        SchedulingOverrideRequest request = requestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Override request not found: " + requestId));

        if (request.getStatus() != OverrideRequestStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Cannot reject step for request in status: " + request.getStatus());
        }

        User decisionMaker = userRepository.findById(decisionMakerUserId)
                .orElseThrow(() -> new EntityNotFoundException("Decision maker user not found: " + decisionMakerUserId));

        SchedulingOverrideApproval targetStep = request.getApprovals().stream()
                .filter(a -> a.getId().equals(approvalId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Approval step not found with ID: " + approvalId));

        if (targetStep.getStatus() != ApprovalStepStatus.PENDING) {
            throw new IllegalStateException("Approval step is already in status: " + targetStep.getStatus());
        }

        validateApproverAuthority(targetStep, decisionMaker);

        targetStep.reject(decisionMaker, remarks);
        approvalRepository.save(targetStep);

        // Request becomes REJECTED
        request.updateStatus(OverrideRequestStatus.REJECTED, remarks);

        // Cancel remaining pending steps
        for (SchedulingOverrideApproval step : request.getApprovals()) {
            if (step.getStatus() == ApprovalStepStatus.PENDING) {
                step.cancel("Rejected by " + decisionMaker.getUsername());
                approvalRepository.save(step);
            }
        }

        // Unlock schedule or keep locked? Schedule remains locked or unassigned
        if (request.getSchedule() != null) {
            request.getSchedule().lock("REJECTED");
            scheduleRepository.save(request.getSchedule());
        }

        SchedulingOverrideRequest saved = requestRepository.save(request);
        notifyParticipants(saved);
        return mapToResponse(saved);
    }

    /**
     * Cancel an override request by creator, registrar, or admin.
     */
    @Transactional
    public SchedulingOverrideResponse cancelRequest(Long requestId, Long actorUserId, String reason) {
        SchedulingOverrideRequest request = requestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Override request not found: " + requestId));

        if (request.getStatus() != OverrideRequestStatus.PENDING_APPROVAL
                && request.getStatus() != OverrideRequestStatus.CONFIGURATION_REQUIRED) {
            throw new IllegalStateException("Cannot cancel request in status: " + request.getStatus());
        }

        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + actorUserId));

        boolean isInitiator = request.getRequestedByUser().getId().equals(actorUserId);
        boolean isAdminOrRegistrar = actor.getRoles().stream().anyMatch(r ->
                r == Roles.ADMIN || r == Roles.SUPER_ADMIN || r == Roles.REGISTRAR);

        if (!isInitiator && !isAdminOrRegistrar) {
            throw new AccessDeniedException("Only the requester or an administrator/registrar can cancel this override request.");
        }

        request.updateStatus(OverrideRequestStatus.CANCELLED, reason);
        for (SchedulingOverrideApproval step : request.getApprovals()) {
            if (step.getStatus() == ApprovalStepStatus.PENDING) {
                step.cancel(reason);
                approvalRepository.save(step);
            }
        }

        if (request.getSchedule() != null) {
            request.getSchedule().lock("CANCELLED");
            scheduleRepository.save(request.getSchedule());
        }

        SchedulingOverrideRequest saved = requestRepository.save(request);
        notifyParticipants(saved);
        return mapToResponse(saved);
    }

    /**
     * Re-verify conflict checks and activate schedule.
     */
    private void activateScheduleAndFinalize(SchedulingOverrideRequest request) {
        ClassSchedule schedule = request.getSchedule();
        ClassSection section = request.getSection();

        if (schedule != null) {
            Long termId = section.getTerm().getId();
            Long roomId = schedule.getRoom().getId();
            Long instructorId = request.getFacultyUser().getId();
            String day = schedule.getDayOfWeek();

            // Re-verify Room Overlap excluding this schedule
            List<ClassSchedule> roomOverlaps = scheduleRepository.findActiveByRoomAndDay(roomId, termId, day);
            for (ClassSchedule existing : roomOverlaps) {
                if (!existing.getId().equals(schedule.getId())
                        && timesOverlap(schedule.getStartTime(), schedule.getEndTime(), existing.getStartTime(), existing.getEndTime())) {
                    throw new IllegalStateException(String.format(
                            "Cannot activate schedule: Room collision detected. Room '%s' is now occupied on %s between %s and %s.",
                            schedule.getRoom().getCode(), day, existing.getStartTime(), existing.getEndTime()));
                }
            }

            // Re-verify Faculty Overlap excluding this schedule
            List<ClassSchedule> facultyOverlaps = scheduleRepository.findActiveByInstructorAndDay(instructorId, termId, day);
            for (ClassSchedule existing : facultyOverlaps) {
                if (!existing.getId().equals(schedule.getId())
                        && timesOverlap(schedule.getStartTime(), schedule.getEndTime(), existing.getStartTime(), existing.getEndTime())) {
                    throw new IllegalStateException(String.format(
                            "Cannot activate schedule: Faculty collision detected. Instructor '%s' is already scheduled on %s between %s and %s.",
                            request.getFacultyUser().getUsername(), day, existing.getStartTime(), existing.getEndTime()));
                }
            }

            schedule.unlock();
            scheduleRepository.save(schedule);
        }

        // Set section primary instructor if not set
        if (section.getPrimaryInstructor() == null) {
            section.setPrimaryInstructor(request.getFacultyUser());
            sectionRepository.save(section);
        }

        request.updateStatus(OverrideRequestStatus.APPROVED, "All required approvals completed and validated.");
        log.info("Activated SchedulingOverrideRequest {} and unlocked schedule {}", request.getId(), schedule != null ? schedule.getId() : null);
    }

    private boolean timesOverlap(java.time.LocalTime s1, java.time.LocalTime e1, java.time.LocalTime s2, java.time.LocalTime e2) {
        return s1.isBefore(e2) && s2.isBefore(e1);
    }

    private void validateApproverAuthority(SchedulingOverrideApproval step, User decisionMaker) {
        boolean hasAdminBypass = decisionMaker.getRoles().stream()
                .anyMatch(r -> r == Roles.SUPER_ADMIN || r == Roles.ADMIN);

        if (hasAdminBypass) {
            return;
        }

        // If step is specifically assigned to a user
        if (step.getAssignedApproverUser() != null) {
            if (!step.getAssignedApproverUser().getId().equals(decisionMaker.getId())) {
                throw new AccessDeniedException("User " + decisionMaker.getUsername() + " is not the assigned authority for step: " + step.getApprovalType());
            }
            return;
        }

        // Validate by role
        String requiredRoleWithoutPrefix = step.getRequiredRole().replace("ROLE_", "");
        boolean hasRole = decisionMaker.getRoles().stream()
                .anyMatch(r -> r.name().equals(step.getRequiredRole()) || r.name().equals(requiredRoleWithoutPrefix));
        if (!hasRole) {
            throw new AccessDeniedException("User does not have the required role " + step.getRequiredRole() + " to decide step " + step.getApprovalType());
        }
    }

    private void notifyParticipants(SchedulingOverrideRequest request) {
        if (broadcastService == null) return;
        try {
            SchedulingOverrideResponse dto = mapToResponse(request);
            // Broadcast to section topic
            broadcastService.broadcast(WebSocketTopics.classSchedule(request.getSection().getId()), dto);
            // Broadcast to faculty topic
            broadcastService.broadcast(WebSocketTopics.faculty(request.getFacultyUser().getId()), dto);
            // Broadcast to admin
            broadcastService.broadcast("/topic/scheduling.overrides", dto);
        } catch (Exception e) {
            log.warn("Failed to broadcast override event: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public SchedulingOverrideResponse getRequestById(Long id) {
        SchedulingOverrideRequest req = requestRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Override request not found with ID: " + id));
        return mapToResponse(req);
    }

    @Transactional(readOnly = true)
    public List<SchedulingOverrideResponse> getRequestsBySection(Long sectionId) {
        return requestRepository.findBySectionIdWithDetails(sectionId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SchedulingOverrideResponse> getInbox(User currentUser) {
        boolean isAdminOrRegistrar = currentUser.getRoles().stream()
                .anyMatch(r -> r == Roles.ADMIN || r == Roles.SUPER_ADMIN || r == Roles.REGISTRAR);

        if (isAdminOrRegistrar) {
            return requestRepository.findAll().stream()
                    .map(r -> requestRepository.findByIdWithDetails(r.getId()).orElse(r))
                    .map(this::mapToResponse)
                    .toList();
        }

        Set<Long> requestIds = new LinkedHashSet<>();
        // 1. Pending as assigned approver
        requestRepository.findPendingRequestsForApprover(currentUser.getId()).forEach(r -> requestIds.add(r.getId()));

        // 2. Requests involving this user as assigned faculty
        requestRepository.findByFacultyUserIdWithDetails(currentUser.getId()).forEach(r -> requestIds.add(r.getId()));

        return requestIds.stream()
                .map(id -> requestRepository.findByIdWithDetails(id).orElse(null))
                .filter(Objects::nonNull)
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getPendingCount(Long userId) {
        return requestRepository.countPendingRequestsForApprover(userId);
    }

    public SchedulingOverrideResponse mapToResponse(SchedulingOverrideRequest r) {
        ClassSection sec = r.getSection();
        ClassSchedule sch = r.getSchedule();

        String scheduleSummary = null;
        boolean isLocked = false;
        if (sch != null) {
            scheduleSummary = sch.getDayOfWeek() + " " + sch.getStartTime() + " - " + sch.getEndTime()
                    + " (" + (sch.getRoom() != null ? sch.getRoom().getCode() : "TBA") + ")";
            isLocked = sch.isLocked();
        }

        List<ApprovalStepResponse> stepDtos = r.getApprovals().stream()
                .map(a -> new ApprovalStepResponse(
                        a.getId(),
                        a.getApprovalType().name(),
                        a.getStepOrder(),
                        a.getRequiredRole(),
                        a.getAssignedApproverUser() != null ? a.getAssignedApproverUser().getId() : null,
                        a.getAssignedApproverUser() != null ? resolveUserFullName(a.getAssignedApproverUser()) : null,
                        a.getDecisionByUser() != null ? a.getDecisionByUser().getId() : null,
                        a.getDecisionByUser() != null ? resolveUserFullName(a.getDecisionByUser()) : null,
                        a.getStatus().name(),
                        a.getDecisionAt(),
                        a.getRemarks()
                ))
                .toList();

        return new SchedulingOverrideResponse(
                r.getId(),
                sec.getId(),
                sec.getSectionCode(),
                sec.getCourse() != null ? sec.getCourse().getId() : null,
                sec.getCourse() != null ? sec.getCourse().getCode() : null,
                sec.getCourse() != null ? sec.getCourse().getTitle() : null,
                sec.getTerm() != null ? sec.getTerm().getId() : null,
                sec.getTerm() != null && sec.getTerm().getTermType() != null ? sec.getTerm().getTermType().name() : null,
                sch != null ? sch.getId() : null,
                scheduleSummary,
                r.getFacultyUser().getId(),
                resolveFacultyFullName(r.getFacultyUser(), r.getFacultyProfile()),
                r.getFacultyUser().getEmail(),
                r.getRequestedByUser().getId(),
                resolveUserFullName(r.getRequestedByUser()),
                r.getOverrideType().name(),
                r.getHomeProgram() != null ? r.getHomeProgram().getId() : null,
                r.getHomeProgram() != null ? r.getHomeProgram().getName() : null,
                r.getHomeCollege() != null ? r.getHomeCollege().getId() : null,
                r.getHomeCollege() != null ? r.getHomeCollege().getName() : null,
                r.getTargetProgram().getId(),
                r.getTargetProgram().getName(),
                r.getTargetCollege().getId(),
                r.getTargetCollege().getName(),
                r.getStatus().name(),
                r.getJustification(),
                r.getResolutionNotes(),
                r.getCreatedAt(),
                r.getCompletedAt(),
                stepDtos,
                isLocked
        );
    }

    private String resolveFacultyFullName(User user, FacultyProfile profile) {
        if (profile != null) {
            String name = profile.getFullName();
            if (name != null && !name.isBlank()) return name;
        }
        return user != null ? user.getUsername() : "N/A";
    }

    private String resolveUserFullName(User user) {
        if (user == null) return "N/A";
        FacultyProfile profile = facultyProfileRepository.findByUserId(user.getId()).orElse(null);
        if (profile != null) {
            String name = profile.getFullName();
            if (name != null && !name.isBlank()) return name;
        }
        return user.getUsername();
    }
}
