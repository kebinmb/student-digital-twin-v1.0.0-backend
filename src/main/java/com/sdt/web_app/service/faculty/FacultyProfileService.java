package com.sdt.web_app.service.faculty;

import com.sdt.web_app.dto.faculty.FacultyDtos.*;
import com.sdt.web_app.entities.authentication.Roles;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.faculty.FacultyProfile;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.entities.scheduling.ClassSchedule;
import com.sdt.web_app.entities.scheduling.FacultyWorkload;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.faculty.FacultyProfileRepository;
import com.sdt.web_app.repositories.institution.TermRepository;
import com.sdt.web_app.repositories.scheduling.ClassScheduleRepository;
import com.sdt.web_app.repositories.scheduling.FacultyWorkloadRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacultyProfileService {

    private final FacultyProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final TermRepository termRepository;
    private final FacultyWorkloadRepository workloadRepository;
    private final ClassScheduleRepository scheduleRepository;

    @Transactional
    public FacultyProfileResponse getProfileByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        FacultyProfile profile = profileRepository.findByUserIdWithUser(userId)
                .orElseGet(() -> {
                    FacultyProfile defaultProfile = FacultyProfile.builder()
                            .user(user)
                            .facultyIdNumber("FAC-" + user.getId())
                            .highestDegree(FacultyProfile.HighestDegree.BACHELORS)
                            .academicRank(FacultyProfile.AcademicRank.INSTRUCTOR_I)
                            .employmentStatus(FacultyProfile.EmploymentStatus.FULL_TIME)
                            .isTenured(false)
                            .build();
                    return profileRepository.save(defaultProfile);
                });

        return mapToProfileResponse(profile);
    }

    @Transactional
    public FacultyProfileResponse updateProfile(Long userId, UpdateFacultyProfileRequest request) {
        FacultyProfile profile = profileRepository.findByUserIdWithUser(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
                    FacultyProfile defaultProfile = FacultyProfile.builder()
                            .user(user)
                            .facultyIdNumber("FAC-" + user.getId())
                            .build();
                    return profileRepository.save(defaultProfile);
                });

        FacultyProfile.HighestDegree degree = FacultyProfile.HighestDegree.valueOf(request.highestDegree().trim().toUpperCase());
        FacultyProfile.AcademicRank rank = FacultyProfile.AcademicRank.valueOf(request.academicRank().trim().toUpperCase());
        FacultyProfile.EmploymentStatus status = FacultyProfile.EmploymentStatus.valueOf(request.employmentStatus().trim().toUpperCase());

        profile.updateCredentials(degree, rank, request.prcLicenseNo(), status, request.isTenured());
        FacultyProfile saved = profileRepository.save(profile);
        log.info("Updated faculty credentials for user ID {}", userId);

        return mapToProfileResponse(saved);
    }

    @Transactional(readOnly = true)
    public ChedE5ReportResponse generateChedE5Report(Long termId) {
        Term term = termRepository.findById(termId)
                .orElseThrow(() -> new EntityNotFoundException("Term not found with ID: " + termId));

        List<User> facultyUsers = userRepository.findAll().stream()
                .filter(u -> u.isEnabled() && u.getRoles().stream()
                        .anyMatch(r -> r == Roles.FACULTY || r == Roles.CHAIRPERSON || r == Roles.DEAN))
                .toList();

        List<FacultyProfile> profiles = profileRepository.findAllWithUser();
        Map<Long, FacultyProfile> profileMap = profiles.stream()
                .collect(Collectors.toMap(fp -> fp.getUser().getId(), fp -> fp, (a, b) -> a));

        List<FacultyWorkload> workloads = workloadRepository.findByTermId(termId);
        Map<Long, FacultyWorkload> workloadMap = workloads.stream()
                .collect(Collectors.toMap(w -> w.getFaculty().getId(), w -> w, (a, b) -> a));

        List<ClassSchedule> termSchedules = scheduleRepository.findAll().stream()
                .filter(s -> s.getSection().getTerm().getId().equals(termId) && s.getInstructor() != null)
                .toList();
        Map<Long, List<ClassSchedule>> schedulesByFaculty = termSchedules.stream()
                .collect(Collectors.groupingBy(s -> s.getInstructor().getId()));

        List<ChedE5WorkloadSummaryDto> summaryList = new ArrayList<>();
        BigDecimal totalRegular = BigDecimal.ZERO;
        BigDecimal totalOverload = BigDecimal.ZERO;
        BigDecimal totalContact = BigDecimal.ZERO;

        for (User faculty : facultyUsers) {
            FacultyProfile fp = profileMap.get(faculty.getId());
            FacultyWorkload fw = workloadMap.get(faculty.getId());
            List<ClassSchedule> scheds = schedulesByFaculty.getOrDefault(faculty.getId(), List.of());

            List<String> assignedSectionCodes = scheds.stream()
                    .map(s -> s.getSection().getSectionCode())
                    .distinct()
                    .toList();

            BigDecimal regUnits = fw != null ? fw.getRegularUnits() : BigDecimal.ZERO;
            BigDecimal ovlUnits = fw != null ? fw.getOverloadUnits() : BigDecimal.ZERO;
            BigDecimal contactHours = fw != null ? fw.getTotalContactHours() : BigDecimal.ZERO;
            int preps = fw != null ? fw.getNumberOfPreparations() : 0;

            totalRegular = totalRegular.add(regUnits);
            totalOverload = totalOverload.add(ovlUnits);
            totalContact = totalContact.add(contactHours);

            summaryList.add(new ChedE5WorkloadSummaryDto(
                    faculty.getId(),
                    fp != null ? fp.getFacultyIdNumber() : "FAC-" + faculty.getId(),
                    faculty.getUsername(),
                    faculty.getEmail(),
                    fp != null ? fp.getHighestDegree().name() : "BACHELORS",
                    fp != null ? fp.getAcademicRank().name() : "INSTRUCTOR_I",
                    fp != null ? fp.getPrcLicenseNo() : null,
                    fp != null ? fp.getEmploymentStatus().name() : "FULL_TIME",
                    fp != null && fp.isTenured(),
                    regUnits,
                    ovlUnits,
                    contactHours,
                    preps,
                    assignedSectionCodes
            ));
        }

        return new ChedE5ReportResponse(
                term.getId(),
                term.getAcademicYear().getCode() + " " + term.getTermType().name(),
                facultyUsers.size(),
                totalRegular,
                totalOverload,
                totalContact,
                summaryList
        );
    }

    private FacultyProfileResponse mapToProfileResponse(FacultyProfile fp) {
        return new FacultyProfileResponse(
                fp.getId(),
                fp.getUser().getId(),
                fp.getUser().getUsername(),
                fp.getUser().getEmail(),
                fp.getFacultyIdNumber(),
                fp.getHighestDegree().name(),
                fp.getAcademicRank().name(),
                fp.getPrcLicenseNo(),
                fp.getEmploymentStatus().name(),
                fp.isTenured()
        );
    }
}
