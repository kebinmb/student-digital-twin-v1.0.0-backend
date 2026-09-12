package com.sdt.web_app.service.compliance;

import com.sdt.web_app.dto.compliance.ComplianceDtos.*;
import com.sdt.web_app.entities.compliance.GraduationApplication;
import com.sdt.web_app.entities.enrollment.StudentCourseGrade;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.institution.Curriculum;
import com.sdt.web_app.entities.institution.CurriculumCourse;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.exceptions.ResourceNotFoundException;
import com.sdt.web_app.repositories.compliance.GraduationApplicationRepository;
import com.sdt.web_app.repositories.enrollment.StudentCourseGradeRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.repositories.institution.CurriculumCourseRepository;
import com.sdt.web_app.repositories.institution.TermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DegreeAuditService {

    private final GraduationApplicationRepository graduationApplicationRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final CurriculumCourseRepository curriculumCourseRepository;
    private final StudentCourseGradeRepository studentCourseGradeRepository;
    private final TermRepository termRepository;

    @Transactional(readOnly = true)
    public DegreeAuditResultDto evaluateDegreeAudit(Long studentProfileId) {
        StudentProfile student = studentProfileRepository.findById(studentProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Student Profile not found: " + studentProfileId));

        Curriculum curriculum = student.getCurriculum();
        if (curriculum == null) {
            throw new IllegalStateException("Student is not assigned to an active curriculum.");
        }

        List<CurriculumCourse> curriculumCourses = curriculumCourseRepository.findByCurriculumId(curriculum.getId());
        List<StudentCourseGrade> studentGrades = studentCourseGradeRepository.findByStudentId(student.getId());

        Map<Long, StudentCourseGrade> gradeMap = studentGrades.stream()
                .collect(Collectors.toMap(
                        g -> g.getCourse().getId(),
                        g -> g,
                        (g1, g2) -> g1.getNumericalGrade().compareTo(g2.getNumericalGrade()) <= 0 ? g1 : g2
                ));

        BigDecimal totalCurriculumUnits = BigDecimal.ZERO;
        BigDecimal totalUnitsEarned = BigDecimal.ZERO;
        BigDecimal totalGradePoints = BigDecimal.ZERO;
        BigDecimal totalGradedUnits = BigDecimal.ZERO;
        boolean hasFailingGrade = false;

        List<CourseAuditItemDto> auditedCourses = new ArrayList<>();

        for (CurriculumCourse cc : curriculumCourses) {
            BigDecimal units = cc.getCourse() != null ? cc.getCourse().getCreditUnits() : BigDecimal.ZERO;
            totalCurriculumUnits = totalCurriculumUnits.add(units);

            StudentCourseGrade grade = gradeMap.get(cc.getCourse().getId());
            boolean completed = false;
            BigDecimal gradeEarned = null;
            String status = "PENDING";

            if (grade != null) {
                gradeEarned = grade.getNumericalGrade();
                if ("PASSED".equalsIgnoreCase(grade.getCompletionStatus()) || (gradeEarned != null && gradeEarned.compareTo(BigDecimal.valueOf(3.00)) <= 0)) {
                    completed = true;
                    status = "PASSED";
                    totalUnitsEarned = totalUnitsEarned.add(units);

                    if (gradeEarned != null) {
                        totalGradePoints = totalGradePoints.add(gradeEarned.multiply(units));
                        totalGradedUnits = totalGradedUnits.add(units);
                    }
                } else {
                    status = "FAILED";
                    hasFailingGrade = true;
                }
            }

            auditedCourses.add(new CourseAuditItemDto(
                    cc.getCourse().getId(),
                    cc.getCourse().getCode(),
                    cc.getCourse().getTitle(),
                    units,
                    completed,
                    gradeEarned,
                    status
            ));
        }

        BigDecimal gpa = totalGradedUnits.compareTo(BigDecimal.ZERO) > 0
                ? totalGradePoints.divide(totalGradedUnits, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        boolean residencyMet = totalUnitsEarned.compareTo(totalCurriculumUnits.multiply(BigDecimal.valueOf(0.50))) >= 0;
        boolean qualifiedForGraduation = totalUnitsEarned.compareTo(totalCurriculumUnits) >= 0 && !hasFailingGrade && residencyMet;

        String honorsStatus = "NONE";
        if (qualifiedForGraduation && !hasFailingGrade) {
            if (gpa.compareTo(BigDecimal.valueOf(1.20)) <= 0) {
                honorsStatus = "SUMMA_CUM_LAUDE";
            } else if (gpa.compareTo(BigDecimal.valueOf(1.45)) <= 0) {
                honorsStatus = "MAGNA_CUM_LAUDE";
            } else if (gpa.compareTo(BigDecimal.valueOf(1.75)) <= 0) {
                honorsStatus = "CUM_LAUDE";
            }
        }

        String studentName = student.getUser() != null ? student.getUser().getUsername() : "N/A";
        String programCode = student.getProgram() != null ? student.getProgram().getCode() : "N/A";

        return new DegreeAuditResultDto(
                student.getId(),
                student.getStudentNumber(),
                studentName,
                programCode,
                curriculum.getCode(),
                totalCurriculumUnits,
                totalUnitsEarned,
                gpa,
                residencyMet,
                qualifiedForGraduation,
                honorsStatus,
                auditedCourses
        );
    }

    @Transactional
    public GraduationApplicationDto applyForGraduation(ApplyForGraduationRequest request, Long actorUserId) {
        StudentProfile student = studentProfileRepository.findById(request.studentProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Student Profile not found: " + request.studentProfileId()));

        Term term = termRepository.findById(request.termId())
                .orElseThrow(() -> new ResourceNotFoundException("Term not found: " + request.termId()));

        DegreeAuditResultDto audit = evaluateDegreeAudit(student.getId());

        graduationApplicationRepository.findByStudentProfileIdAndTermId(student.getId(), term.getId())
                .ifPresent(existing -> {
                    throw new IllegalStateException("Graduation application already submitted for this term.");
                });

        GraduationApplication app = GraduationApplication.builder()
                .studentProfile(student)
                .curriculum(student.getCurriculum())
                .term(term)
                .applicationDate(LocalDate.now())
                .degreeAuditStatus(audit.qualifiedForGraduation() ? "QUALIFIED" : "INCOMPLETE")
                .totalUnitsCompleted(audit.totalUnitsEarned())
                .cumulativeGpa(audit.cumulativeGpa())
                .honorsStatus(audit.honorsEligible())
                .build();

        GraduationApplication saved = graduationApplicationRepository.save(app);

        // Update student profile status
        if (audit.qualifiedForGraduation()) {
            student.updateAcademicStanding(student.getYearLevel(), student.getEnrollmentStatus(), true);
            studentProfileRepository.save(student);
        }

        return mapToGraduationDto(saved);
    }

    @Transactional
    public GraduationApplicationDto issueSpecialOrder(Long graduationApplicationId, String specialOrderNumber, Long actorUserId) {
        GraduationApplication app = graduationApplicationRepository.findById(graduationApplicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Graduation Application not found: " + graduationApplicationId));

        if (!"QUALIFIED".equalsIgnoreCase(app.getDegreeAuditStatus())) {
            throw new IllegalStateException("Cannot issue CHED Special Order for an unqualified graduation application.");
        }

        app.setSpecialOrderNumber(specialOrderNumber.trim().toUpperCase());
        app.setSpecialOrderIssuedAt(LocalDate.now());

        GraduationApplication updated = graduationApplicationRepository.save(app);
        return mapToGraduationDto(updated);
    }

    @Transactional(readOnly = true)
    public List<GraduationApplicationDto> getGraduationApplicationsByTerm(Long termId) {
        return graduationApplicationRepository.findByTermId(termId)
                .stream()
                .map(this::mapToGraduationDto)
                .toList();
    }

    private GraduationApplicationDto mapToGraduationDto(GraduationApplication entity) {
        String studentName = entity.getStudentProfile().getUser() != null
                ? entity.getStudentProfile().getUser().getUsername()
                : "N/A";

        String termName = entity.getTerm().getTermType() != null ? entity.getTerm().getTermType().name() : "Term " + entity.getTerm().getId();

        return new GraduationApplicationDto(
                entity.getId(),
                entity.getStudentProfile().getId(),
                entity.getStudentProfile().getStudentNumber(),
                studentName,
                entity.getCurriculum().getId(),
                entity.getCurriculum().getCode(),
                entity.getTerm().getId(),
                termName,
                entity.getApplicationDate().toString(),
                entity.getDegreeAuditStatus(),
                entity.getTotalUnitsCompleted(),
                entity.getCumulativeGpa(),
                entity.getHonorsStatus(),
                entity.getSpecialOrderNumber(),
                entity.getSpecialOrderIssuedAt() != null ? entity.getSpecialOrderIssuedAt().toString() : null
        );
    }
}
