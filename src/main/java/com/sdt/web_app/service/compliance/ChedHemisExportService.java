package com.sdt.web_app.service.compliance;

import com.sdt.web_app.dto.compliance.ComplianceDtos.*;
import com.sdt.web_app.entities.compliance.GraduationApplication;
import com.sdt.web_app.entities.faculty.FacultyProfile;
import com.sdt.web_app.entities.institution.Campus;
import com.sdt.web_app.entities.institution.Program;
import com.sdt.web_app.exceptions.ResourceNotFoundException;
import com.sdt.web_app.entities.enrollment.StudentEnrollment;
import com.sdt.web_app.entities.scheduling.ClassSection;
import com.sdt.web_app.entities.scheduling.FacultyWorkload;
import com.sdt.web_app.repositories.admission.AdmissionApplicationRepository;
import com.sdt.web_app.repositories.compliance.GraduationApplicationRepository;
import com.sdt.web_app.repositories.enrollment.StudentEnrollmentRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.repositories.faculty.FacultyProfileRepository;
import com.sdt.web_app.repositories.institution.CampusRepository;
import com.sdt.web_app.repositories.institution.ProgramRepository;
import com.sdt.web_app.repositories.scheduling.ClassSectionRepository;
import com.sdt.web_app.repositories.scheduling.FacultyWorkloadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChedHemisExportService {

    private final CampusRepository campusRepository;
    private final ProgramRepository programRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final FacultyProfileRepository facultyProfileRepository;
    private final GraduationApplicationRepository graduationApplicationRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final FacultyWorkloadRepository facultyWorkloadRepository;
    private final ClassSectionRepository classSectionRepository;
    private final AdmissionApplicationRepository admissionApplicationRepository;

    @Transactional(readOnly = true)
    public ChedFormE1InstitutionalDto exportFormE1Institutional(Long campusId) {
        Campus campus = campusRepository.findById(campusId)
                .orElseThrow(() -> new ResourceNotFoundException("Campus not found: " + campusId));

        int totalPrograms = (int) programRepository.count();
        int totalEnrolledStudents = (int) studentProfileRepository.count();
        int totalFaculty = (int) facultyProfileRepository.count();

        return new ChedFormE1InstitutionalDto(
                campus.getId(),
                campus.getName(),
                campus.getChedInstitutionalCode() != null ? campus.getChedInstitutionalCode() : "CHED-INST-0601",
                totalPrograms,
                totalEnrolledStudents,
                totalFaculty
        );
    }

    @Transactional(readOnly = true)
    public List<ChedFormE2ProgramDto> exportFormE2Programs(Long campusId) {
        List<Program> programs = programRepository.findAll();
        return programs.stream().map(prog -> new ChedFormE2ProgramDto(
                prog.getId(),
                prog.getCode(),
                prog.getName(),
                "CMO No. 25 s. 2015",
                prog.getMajor() != null ? prog.getMajor() : "General",
                prog.getTotalUnitsRequired() > 0 ? prog.getTotalUnitsRequired() : 140,
                "GR-2026-0601",
                prog.isActive()
        )).toList();
    }

    @Transactional(readOnly = true)
    public List<ChedFormE3EnrolmentDto> exportFormE3Enrolment(Long termId) {
        List<Program> programs = programRepository.findAll();
        List<ChedFormE3EnrolmentDto> report = new ArrayList<>();
        List<StudentEnrollment> enrollments = (termId != null)
                ? studentEnrollmentRepository.findByTermIdWithDetails(termId)
                : List.of();

        for (Program prog : programs) {
            List<StudentEnrollment> progEnrollments = enrollments.stream()
                    .filter(e -> e.getStudent() != null && e.getStudent().getProgram() != null && prog.getId().equals(e.getStudent().getProgram().getId()))
                    .toList();

            int enrolledCount = progEnrollments.size();
            int maleCount = 0;
            int femaleCount = 0;
            BigDecimal totalUnits = BigDecimal.ZERO;

            for (StudentEnrollment se : progEnrollments) {
                if (se.getTotalCreditUnits() != null) {
                    totalUnits = totalUnits.add(se.getTotalCreditUnits());
                }
                Long admissionId = se.getStudent().getAdmissionApplicationId();
                if (admissionId != null) {
                    var admOpt = admissionApplicationRepository.findById(admissionId);
                    if (admOpt.isPresent() && "MALE".equalsIgnoreCase(admOpt.get().getGender())) {
                        maleCount++;
                    } else {
                        femaleCount++;
                    }
                } else {
                    maleCount++;
                }
            }

            if (enrolledCount > 0 && maleCount + femaleCount > enrolledCount) {
                femaleCount = enrolledCount - maleCount;
            }

            report.add(new ChedFormE3EnrolmentDto(
                    termId,
                    "Term " + termId,
                    prog.getCode(),
                    prog.getName(),
                    maleCount,
                    femaleCount,
                    enrolledCount,
                    totalUnits
            ));
        }
        return report;
    }

    @Transactional(readOnly = true)
    public List<ChedFormE4GraduateDto> exportFormE4Graduates(Long termId) {
        List<GraduationApplication> applications = graduationApplicationRepository.findByTermId(termId);
        List<Program> programs = programRepository.findAll();
        List<ChedFormE4GraduateDto> report = new ArrayList<>();

        for (Program prog : programs) {
            List<GraduationApplication> progApps = applications.stream()
                    .filter(a -> a.getStudentProfile().getProgram() != null && a.getStudentProfile().getProgram().getId().equals(prog.getId()))
                    .filter(a -> "QUALIFIED".equalsIgnoreCase(a.getDegreeAuditStatus()))
                    .toList();

            int totalGrads = progApps.size();
            int summa = (int) progApps.stream().filter(a -> "SUMMA_CUM_LAUDE".equals(a.getHonorsStatus())).count();
            int magna = (int) progApps.stream().filter(a -> "MAGNA_CUM_LAUDE".equals(a.getHonorsStatus())).count();
            int cum = (int) progApps.stream().filter(a -> "CUM_LAUDE".equals(a.getHonorsStatus())).count();

            report.add(new ChedFormE4GraduateDto(
                    termId,
                    "Term " + termId,
                    prog.getCode(),
                    totalGrads,
                    summa,
                    magna,
                    cum
            ));
        }
        return report;
    }

    @Transactional(readOnly = true)
    public List<ChedFormE5FacultyDto> exportFormE5Faculty(Long termId) {
        List<FacultyProfile> facultyList = facultyProfileRepository.findAll();
        List<ClassSection> termSections = (termId != null)
                ? classSectionRepository.findByTermId(termId)
                : List.of();
        List<ChedFormE5FacultyDto> report = new ArrayList<>();

        for (FacultyProfile fp : facultyList) {
            String name = fp.getUser() != null ? fp.getUser().getUsername() : "Faculty " + fp.getId();
            String degree = fp.getHighestDegree() != null ? fp.getHighestDegree().name() : "MASTER";
            String status = fp.getEmploymentStatus() != null ? fp.getEmploymentStatus().name() : "FULL_TIME";

            Long userId = fp.getUser() != null ? fp.getUser().getId() : null;
            Optional<FacultyWorkload> workloadOpt = (userId != null && termId != null)
                    ? facultyWorkloadRepository.findByTermIdAndFacultyId(termId, userId)
                    : Optional.empty();

            int contactHours = workloadOpt.map(w -> w.getTotalContactHours() != null ? w.getTotalContactHours().intValue() : 0)
                    .orElseGet(() -> {
                        if (userId == null) return 0;
                        return (int) termSections.stream()
                                .filter(s -> s.getPrimaryInstructor() != null && userId.equals(s.getPrimaryInstructor().getId()))
                                .count() * 3;
                    });

            int sectionCount = workloadOpt.map(FacultyWorkload::getNumberOfPreparations)
                    .orElseGet(() -> {
                        if (userId == null) return 0;
                        return (int) termSections.stream()
                                .filter(s -> s.getPrimaryInstructor() != null && userId.equals(s.getPrimaryInstructor().getId()))
                                .count();
                    });

            report.add(new ChedFormE5FacultyDto(
                    fp.getId(),
                    name,
                    degree,
                    status,
                    contactHours,
                    sectionCount
            ));
        }
        return report;
    }
}
