package com.sdt.web_app.service.enrollment;

import com.sdt.web_app.dto.enrollment.EnrollmentDtos.*;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.enrollment.CourseEquivalency;
import com.sdt.web_app.entities.enrollment.StudentCourseGrade;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.institution.Course;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.enrollment.CourseEquivalencyRepository;
import com.sdt.web_app.repositories.enrollment.StudentCourseGradeRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.repositories.institution.CourseRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransfereeCreditingService {

    private final StudentProfileRepository studentProfileRepository;
    private final CourseRepository courseRepository;
    private final CourseEquivalencyRepository equivalencyRepository;
    private final StudentCourseGradeRepository studentCourseGradeRepository;
    private final UserRepository userRepository;

    @Transactional
    public TransfereeCreditingSummaryResponse creditTransfereeCourses(
            Long studentId, CreditTransfereeCoursesRequest request, Long approverUserId) {
        StudentProfile student = studentProfileRepository.findByIdWithProgramAndCurriculum(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found with ID: " + studentId));

        User approver = null;
        if (approverUserId != null) {
            approver = userRepository.findById(approverUserId).orElse(null);
        }

        List<CourseEquivalencyDto> creditedDtos = new ArrayList<>();
        BigDecimal totalUnitsCredited = BigDecimal.ZERO;

        for (CreditCourseItemRequest item : request.items()) {
            Course internalCourse = courseRepository.findById(item.internalCourseId())
                    .orElseThrow(() -> new EntityNotFoundException("Internal course not found with ID: " + item.internalCourseId()));

            // 1. Create CourseEquivalency audit ledger
            CourseEquivalency equivalency = CourseEquivalency.builder()
                    .student(student)
                    .externalInstitution(item.externalInstitution().trim())
                    .externalCourseCode(item.externalCourseCode().trim())
                    .externalCourseTitle(item.externalCourseTitle().trim())
                    .internalCourse(internalCourse)
                    .externalNumericalGrade(item.externalNumericalGrade())
                    .creditsGranted(item.creditsGranted())
                    .status(CourseEquivalency.Status.APPROVED)
                    .approvedBy(approver)
                    .remarks(item.remarks() != null ? item.remarks().trim() : "Accredited transferee course")
                    .build();

            CourseEquivalency savedEquiv = equivalencyRepository.save(equivalency);

            // 2. Upsert StudentCourseGrade so prerequisite DAG recognizes the credited course
            StudentCourseGrade courseGrade = studentCourseGradeRepository
                    .findByStudentIdAndCourseId(student.getId(), internalCourse.getId())
                    .orElseGet(() -> StudentCourseGrade.builder()
                            .student(student)
                            .course(internalCourse)
                            .numericalGrade(item.externalNumericalGrade())
                            .completionStatus("PASSED")
                            .isCredited(true)
                            .build());

            studentCourseGradeRepository.save(courseGrade);
            totalUnitsCredited = totalUnitsCredited.add(item.creditsGranted());

            creditedDtos.add(mapToEquivalencyDto(savedEquiv));
            log.info("Credited transferee course: {} -> {} for student {}",
                    item.externalCourseCode(), internalCourse.getCode(), student.getStudentNumber());
        }

        // 3. Update total units earned on StudentProfile
        List<StudentCourseGrade> allPassed = studentCourseGradeRepository.findPassedGradesByStudentId(student.getId());

        BigDecimal totalUnitsEarned = BigDecimal.ZERO;
        BigDecimal totalGradePoints = BigDecimal.ZERO;

        for (StudentCourseGrade scg : allPassed) {
            if (scg.getCourse() != null && scg.getCourse().getCreditUnits() != null) {
                BigDecimal units = scg.getCourse().getCreditUnits();
                totalUnitsEarned = totalUnitsEarned.add(units);

                if (scg.getNumericalGrade() != null) {
                    totalGradePoints = totalGradePoints.add(scg.getNumericalGrade().multiply(units));
                }
            }
        }

        BigDecimal cumulativeGpa = totalUnitsEarned.compareTo(BigDecimal.ZERO) > 0
                ? totalGradePoints.divide(totalUnitsEarned, 2, java.math.RoundingMode.HALF_UP)
                : null;

        student.updateProgress(totalUnitsEarned, cumulativeGpa);
        studentProfileRepository.save(student);

        return new TransfereeCreditingSummaryResponse(
                student.getId(),
                student.getStudentNumber(),
                creditedDtos.size(),
                totalUnitsCredited,
                creditedDtos
        );
    }

    @Transactional(readOnly = true)
    public List<CourseEquivalencyDto> getStudentCourseEquivalencies(Long studentId) {
        return equivalencyRepository.findByStudentIdWithCourses(studentId).stream()
                .map(this::mapToEquivalencyDto)
                .toList();
    }

    private CourseEquivalencyDto mapToEquivalencyDto(CourseEquivalency ce) {
        return new CourseEquivalencyDto(
                ce.getId(),
                ce.getStudent().getId(),
                ce.getExternalInstitution(),
                ce.getExternalCourseCode(),
                ce.getExternalCourseTitle(),
                ce.getInternalCourse().getId(),
                ce.getInternalCourse().getCode(),
                ce.getInternalCourse().getTitle(),
                ce.getExternalNumericalGrade(),
                ce.getCreditsGranted(),
                ce.getStatus().name(),
                ce.getApprovedBy() != null ? ce.getApprovedBy().getUsername() : null,
                ce.getRemarks()
        );
    }
}
