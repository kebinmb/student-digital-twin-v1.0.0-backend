package com.sdt.web_app.service.grade;

import com.sdt.web_app.dto.grade.GradeDtos.*;
import com.sdt.web_app.entities.enrollment.EnrollmentCourseItem;
import com.sdt.web_app.entities.enrollment.StudentCourseGrade;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.scheduling.ClassSection;
import com.sdt.web_app.repositories.enrollment.EnrollmentCourseItemRepository;
import com.sdt.web_app.repositories.enrollment.StudentCourseGradeRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.repositories.scheduling.ClassSectionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class GradeService {

    private final ClassSectionRepository sectionRepository;
    private final EnrollmentCourseItemRepository itemRepository;
    private final StudentCourseGradeRepository gradeRepository;
    private final StudentProfileRepository profileRepository;

    @Transactional(readOnly = true)
    public SectionRosterResponse getSectionRoster(Long sectionId) {
        ClassSection section = sectionRepository.findByIdWithSchedules(sectionId)
                .orElseThrow(() -> new EntityNotFoundException("Class section not found with ID: " + sectionId));

        List<EnrollmentCourseItem> items = itemRepository.findBySectionIdWithStudentDetails(sectionId);

        List<RosterStudentDto> students = items.stream()
                .filter(item -> item.getCompletionStatus() != EnrollmentCourseItem.CompletionStatus.DROPPED)
                .map(item -> {
                    StudentProfile sp = item.getEnrollment().getStudent();
                    return new RosterStudentDto(
                            item.getId(),
                            sp.getId(),
                            sp.getStudentNumber(),
                            sp.getUser() != null ? sp.getUser().getUsername() : "Student " + sp.getStudentNumber(),
                            sp.getProgram() != null ? sp.getProgram().getCode() : "N/A",
                            sp.getYearLevel(),
                            item.getFinalNumericalGrade(),
                            item.getCompletionStatus().name()
                    );
                })
                .toList();

        String instructorName = section.getPrimaryInstructor() != null
                ? section.getPrimaryInstructor().getUsername()
                : (section.getSchedules().stream()
                        .filter(s -> s.getInstructor() != null)
                        .map(s -> s.getInstructor().getUsername())
                        .findFirst().orElse("Unassigned"));

        return new SectionRosterResponse(
                section.getId(),
                section.getSectionCode(),
                section.getCourse().getId(),
                section.getCourse().getCode(),
                section.getCourse().getTitle(),
                section.getCourse().getCreditUnits(),
                section.getTerm().getId(),
                section.getTerm().getTermType().name(),
                section.getGradeStatus().name(),
                section.getPrimaryInstructor() != null ? section.getPrimaryInstructor().getId() : null,
                instructorName,
                section.getEnrolledCount(),
                section.getMaxCapacity(),
                students
        );
    }

    @Transactional
    public GradeActionResponse saveGrades(Long sectionId, SaveSectionGradesRequest request, Long actorUserId) {
        ClassSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new EntityNotFoundException("Class section not found with ID: " + sectionId));

        if (section.getGradeStatus() == ClassSection.GradeStatus.SEALED) {
            throw new IllegalStateException("Cannot update grades: section " + section.getSectionCode() + " is already SEALED.");
        }

        int updatedCount = 0;
        if (request.grades() != null) {
            for (GradeEntryDto entry : request.grades()) {
                EnrollmentCourseItem item = itemRepository.findById(entry.enrollmentItemId())
                        .orElseThrow(() -> new EntityNotFoundException("Enrollment item not found: " + entry.enrollmentItemId()));

                if (!item.getSection().getId().equals(sectionId)) {
                    throw new IllegalArgumentException("Item " + entry.enrollmentItemId() + " does not belong to section " + sectionId);
                }

                EnrollmentCourseItem.CompletionStatus status = EnrollmentCourseItem.CompletionStatus.ENROLLED;
                if (entry.completionStatus() != null && !entry.completionStatus().isBlank()) {
                    status = EnrollmentCourseItem.CompletionStatus.valueOf(entry.completionStatus().toUpperCase());
                } else if (entry.finalNumericalGrade() != null) {
                    status = entry.finalNumericalGrade().compareTo(new BigDecimal("3.00")) <= 0
                            ? EnrollmentCourseItem.CompletionStatus.PASSED
                            : EnrollmentCourseItem.CompletionStatus.FAILED;
                }

                item.updateGrade(entry.finalNumericalGrade(), status);
                itemRepository.save(item);
                updatedCount++;
            }
        }

        if (request.submitForVerification()) {
            section.updateGradeStatus(ClassSection.GradeStatus.SUBMITTED);
            sectionRepository.save(section);
            log.info("Section {} grades submitted for verification by user {}", section.getSectionCode(), actorUserId);
            return new GradeActionResponse(
                    section.getId(),
                    section.getSectionCode(),
                    section.getGradeStatus().name(),
                    updatedCount,
                    "Grades successfully submitted for Dean/Chairperson verification."
            );
        }

        return new GradeActionResponse(
                section.getId(),
                section.getSectionCode(),
                section.getGradeStatus().name(),
                updatedCount,
                "Grades successfully saved as draft."
        );
    }

    @Transactional
    public GradeActionResponse verifyGrades(Long sectionId, Long approverUserId) {
        ClassSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new EntityNotFoundException("Class section not found with ID: " + sectionId));

        if (section.getGradeStatus() != ClassSection.GradeStatus.SUBMITTED) {
            throw new IllegalStateException("Cannot verify grades: section must be in SUBMITTED status. Current status: " + section.getGradeStatus());
        }

        section.updateGradeStatus(ClassSection.GradeStatus.VERIFIED);
        sectionRepository.save(section);
        log.info("Section {} grades verified by user {}", section.getSectionCode(), approverUserId);

        return new GradeActionResponse(
                section.getId(),
                section.getSectionCode(),
                section.getGradeStatus().name(),
                0,
                "Grades verified and endorsed for Registrar sealing."
        );
    }

    @Transactional
    public GradeActionResponse sealGrades(Long sectionId, Long registrarUserId) {
        ClassSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new EntityNotFoundException("Class section not found with ID: " + sectionId));

        if (section.getGradeStatus() != ClassSection.GradeStatus.VERIFIED) {
            throw new IllegalStateException("Cannot seal grades: section must be in VERIFIED status before sealing. Current status: " + section.getGradeStatus());
        }

        List<EnrollmentCourseItem> items = itemRepository.findBySectionIdWithStudentDetails(sectionId);
        Set<StudentProfile> affectedStudents = new HashSet<>();

        for (EnrollmentCourseItem item : items) {
            if (item.getCompletionStatus() == EnrollmentCourseItem.CompletionStatus.DROPPED) {
                continue;
            }

            StudentProfile sp = item.getEnrollment().getStudent();
            if (item.getFinalNumericalGrade() != null) {
                StudentCourseGrade historicalGrade = gradeRepository
                        .findByStudentIdAndCourseId(sp.getId(), section.getCourse().getId())
                        .orElseGet(() -> StudentCourseGrade.builder()
                                .student(sp)
                                .course(section.getCourse())
                                .term(section.getTerm())
                                .numericalGrade(item.getFinalNumericalGrade())
                                .completionStatus(item.getCompletionStatus().name())
                                .isCredited(false)
                                .build());

                historicalGrade.updateGrade(item.getFinalNumericalGrade(), item.getCompletionStatus().name());
                gradeRepository.save(historicalGrade);
                affectedStudents.add(sp);
            }
        }

        // Recalculate units and GPA for each affected student
        for (StudentProfile sp : affectedStudents) {
            List<StudentCourseGrade> passedGrades = gradeRepository.findPassedGradesByStudentId(sp.getId());
            BigDecimal totalUnits = BigDecimal.ZERO;
            BigDecimal weightedGradeSum = BigDecimal.ZERO;

            for (StudentCourseGrade g : passedGrades) {
                BigDecimal units = g.getCourse().getCreditUnits();
                totalUnits = totalUnits.add(units);
                weightedGradeSum = weightedGradeSum.add(g.getNumericalGrade().multiply(units));
            }

            BigDecimal gpa = totalUnits.compareTo(BigDecimal.ZERO) > 0
                    ? weightedGradeSum.divide(totalUnits, 2, RoundingMode.HALF_UP)
                    : null;

            sp.updateProgress(totalUnits, gpa);
            profileRepository.save(sp);
        }

        section.updateGradeStatus(ClassSection.GradeStatus.SEALED);
        sectionRepository.save(section);
        log.info("Section {} grades permanently SEALED into academic transcripts by registrar {}", section.getSectionCode(), registrarUserId);

        return new GradeActionResponse(
                section.getId(),
                section.getSectionCode(),
                section.getGradeStatus().name(),
                items.size(),
                "Grades officially sealed into permanent transcripts and prerequisite records updated."
        );
    }
}
