package com.sdt.web_app.service.institution;

import com.sdt.web_app.dto.institution.HonorRollDtos.*;
import com.sdt.web_app.entities.enrollment.EnrollmentCourseItem;
import com.sdt.web_app.entities.enrollment.StudentEnrollment;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.repositories.enrollment.StudentEnrollmentRepository;
import com.sdt.web_app.repositories.institution.TermRepository;
import com.sdt.web_app.service.lms.StudentNotificationPublisherService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HonorRollComputationService {

    private final StudentEnrollmentRepository enrollmentRepository;
    private final TermRepository termRepository;
    private final StudentNotificationPublisherService studentNotificationPublisherService;
    private final com.sdt.web_app.service.webhook.InstitutionalWebhookService institutionalWebhookService;

    @Transactional(readOnly = true)
    public TermHonorRollReportDto computeTermHonorRoll(Long termId, Long programId) {
        Term term = termRepository.findById(termId)
                .orElseThrow(() -> new EntityNotFoundException("Term not found with ID: " + termId));

        List<StudentEnrollment> enrollments = enrollmentRepository.findByTermIdWithDetails(termId);

        List<HonorStudentCandidate> candidates = new ArrayList<>();
        int evaluatedCount = 0;

        for (StudentEnrollment enrollment : enrollments) {
            StudentProfile student = enrollment.getStudent();
            if (student == null) continue;

            if (programId != null && (student.getProgram() == null || !programId.equals(student.getProgram().getId()))) {
                continue;
            }

            evaluatedCount++;

            // Must have enrolled items
            if (enrollment.getItems() == null || enrollment.getItems().isEmpty()) {
                continue;
            }

            BigDecimal totalUnits = BigDecimal.ZERO;
            BigDecimal weightedGradeSum = BigDecimal.ZERO;
            boolean hasFailingOrIncomplete = false;
            double maxGrade = 1.00; // Philippine grading: 1.00 is highest, 3.00 is passing, 5.00 is fail

            for (EnrollmentCourseItem item : enrollment.getItems()) {
                if (item.getCompletionStatus() == EnrollmentCourseItem.CompletionStatus.DROPPED) {
                    continue;
                }

                if (item.getFinalNumericalGrade() == null) {
                    hasFailingOrIncomplete = true;
                    break;
                }

                double gradeVal = item.getFinalNumericalGrade().doubleValue();
                if (gradeVal > 3.00 || item.getCompletionStatus() == EnrollmentCourseItem.CompletionStatus.FAILED) {
                    hasFailingOrIncomplete = true;
                    break;
                }

                if (gradeVal > maxGrade) {
                    maxGrade = gradeVal;
                }

                BigDecimal units = (item.getSection() != null && item.getSection().getCourse() != null && item.getSection().getCourse().getCreditUnits() != null)
                        ? item.getSection().getCourse().getCreditUnits()
                        : BigDecimal.valueOf(3.0);

                totalUnits = totalUnits.add(units);
                weightedGradeSum = weightedGradeSum.add(item.getFinalNumericalGrade().multiply(units));
            }

            // Regular academic load requirement: at least 15 units, no incompletes or failing grades
            if (hasFailingOrIncomplete || totalUnits.compareTo(BigDecimal.valueOf(15.0)) < 0) {
                continue;
            }

            BigDecimal gpa = weightedGradeSum.divide(totalUnits, 2, RoundingMode.HALF_UP);
            double gpaVal = gpa.doubleValue();

            String honorCategory = null;
            if (gpaVal <= 1.25 && maxGrade <= 1.50) {
                honorCategory = "PRESIDENTS_LIST";
            } else if (gpaVal <= 1.75 && maxGrade <= 2.00) {
                honorCategory = "DEANS_LIST";
            } else if (gpaVal <= 2.00) {
                honorCategory = "HONORABLE_MENTION";
            }

            if (honorCategory != null) {
                candidates.add(new HonorStudentCandidate(
                        student.getId(),
                        student.getStudentNumber(),
                        student.getFullName(),
                        student.getProgram() != null ? student.getProgram().getCode() : "N/A",
                        honorCategory,
                        gpaVal,
                        totalUnits
                ));

                // Send real-time honor recognition notification to student
                if (studentNotificationPublisherService != null) {
                    studentNotificationPublisherService.publishStandingUpdatedEvent(
                            student.getId(),
                            gpaVal,
                            totalUnits,
                            honorCategory
                    );
                }

                // Dispatch external webhook event to institutional SIS
                if (institutionalWebhookService != null) {
                    institutionalWebhookService.dispatchEvent(
                            "STUDENT_HONOR_AWARDED",
                            java.util.Map.of(
                                    "studentId", student.getId(),
                                    "studentNumber", student.getStudentNumber(),
                                    "termId", term.getId(),
                                    "honorCategory", honorCategory,
                                    "gpa", gpaVal
                            )
                    );
                }
            }
        }

        // Sort candidates by GPA ascending (lower number is higher academic honor in PH)
        candidates.sort(Comparator.comparingDouble(HonorStudentCandidate::termGpa));

        List<HonorStudentDto> honorees = new ArrayList<>();
        int rank = 1;
        for (HonorStudentCandidate c : candidates) {
            honorees.add(new HonorStudentDto(
                    c.studentId(),
                    c.studentNumber(),
                    c.fullName(),
                    c.programCode(),
                    c.honorCategory(),
                    c.termGpa(),
                    c.totalUnits(),
                    rank++
            ));
        }

        String termLabel = (term.getAcademicYear() != null ? term.getAcademicYear().getCode() : "") + " " +
                (term.getTermType() != null ? term.getTermType().name() : "");

        return new TermHonorRollReportDto(
                termId,
                termLabel.trim(),
                programId,
                evaluatedCount,
                honorees.size(),
                honorees
        );
    }

    private record HonorStudentCandidate(
            Long studentId,
            String studentNumber,
            String fullName,
            String programCode,
            String honorCategory,
            Double termGpa,
            BigDecimal totalUnits
    ) {}
}
