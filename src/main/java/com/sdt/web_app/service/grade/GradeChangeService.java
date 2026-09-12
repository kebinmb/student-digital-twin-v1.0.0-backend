package com.sdt.web_app.service.grade;

import com.sdt.web_app.dto.grade.GradeChangeDtos.*;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.enrollment.GradeChangeRequest;
import com.sdt.web_app.entities.enrollment.StudentCourseGrade;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.institution.Course;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.enrollment.GradeChangeRequestRepository;
import com.sdt.web_app.repositories.enrollment.StudentCourseGradeRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.repositories.institution.CourseRepository;
import com.sdt.web_app.repositories.institution.TermRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GradeChangeService {

    private final GradeChangeRequestRepository requestRepository;
    private final StudentProfileRepository profileRepository;
    private final CourseRepository courseRepository;
    private final TermRepository termRepository;
    private final UserRepository userRepository;
    private final StudentCourseGradeRepository gradeRepository;

    @Transactional
    public GradeChangeResponse submitRequest(CreateGradeChangeRequest request, Long requestedByUserId) {
        StudentProfile student = profileRepository.findById(request.studentId())
                .orElseThrow(() -> new EntityNotFoundException("Student not found: " + request.studentId()));

        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new EntityNotFoundException("Course not found: " + request.courseId()));

        Term term = termRepository.findById(request.termId())
                .orElseThrow(() -> new EntityNotFoundException("Term not found: " + request.termId()));

        User requester = userRepository.findById(requestedByUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + requestedByUserId));

        GradeChangeRequest entity = GradeChangeRequest.builder()
                .student(student)
                .course(course)
                .term(term)
                .previousGrade(request.previousGrade())
                .newGrade(request.newGrade())
                .reason(request.reason())
                .status(GradeChangeRequest.Status.PENDING)
                .requestedBy(requester)
                .build();

        GradeChangeRequest saved = requestRepository.save(entity);
        log.info("Grade change request #{} submitted by user {} for student {}", saved.getId(), requestedByUserId, student.getStudentNumber());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<GradeChangeResponse> getPendingRequests() {
        return requestRepository.findByStatusWithDetails(GradeChangeRequest.Status.PENDING).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public GradeChangeResponse approveRequest(Long requestId, Long approvedByUserId) {
        GradeChangeRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Grade change request not found: " + requestId));

        if (request.getStatus() != GradeChangeRequest.Status.PENDING) {
            throw new IllegalStateException("Grade change request " + requestId + " is not in PENDING state.");
        }

        User approver = userRepository.findById(approvedByUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + approvedByUserId));

        request.approve(approver);

        // Update permanent grade record
        StudentProfile student = request.getStudent();
        Course course = request.getCourse();
        String completionStatus = request.getNewGrade().compareTo(new BigDecimal("3.00")) <= 0 ? "PASSED" : "FAILED";

        StudentCourseGrade historicalGrade = gradeRepository.findByStudentIdAndCourseId(student.getId(), course.getId())
                .orElseGet(() -> StudentCourseGrade.builder()
                        .student(student)
                        .course(course)
                        .term(request.getTerm())
                        .numericalGrade(request.getNewGrade())
                        .completionStatus(completionStatus)
                        .isCredited(true)
                        .build());

        historicalGrade.updateGrade(request.getNewGrade(), completionStatus);
        gradeRepository.save(historicalGrade);

        // Recalculate student GPA
        List<StudentCourseGrade> passedGrades = gradeRepository.findPassedGradesByStudentId(student.getId());
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

        student.updateProgress(totalUnits, gpa);
        profileRepository.save(student);

        GradeChangeRequest saved = requestRepository.save(request);
        log.info("Grade change request #{} APPROVED by user {}. Updated grade to {}", requestId, approvedByUserId, request.getNewGrade());
        return mapToResponse(saved);
    }

    @Transactional
    public GradeChangeResponse rejectRequest(Long requestId, Long rejectedByUserId) {
        GradeChangeRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Grade change request not found: " + requestId));

        if (request.getStatus() != GradeChangeRequest.Status.PENDING) {
            throw new IllegalStateException("Grade change request " + requestId + " is not in PENDING state.");
        }

        User rejector = userRepository.findById(rejectedByUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + rejectedByUserId));

        request.reject(rejector);
        GradeChangeRequest saved = requestRepository.save(request);
        log.info("Grade change request #{} REJECTED by user {}", requestId, rejectedByUserId);
        return mapToResponse(saved);
    }

    private GradeChangeResponse mapToResponse(GradeChangeRequest gcr) {
        String studentName = gcr.getStudent().getUser() != null ? gcr.getStudent().getUser().getUsername() : "Student #" + gcr.getStudent().getStudentNumber();
        return new GradeChangeResponse(
                gcr.getId(),
                gcr.getStudent().getId(),
                gcr.getStudent().getStudentNumber(),
                studentName,
                gcr.getCourse().getId(),
                gcr.getCourse().getCode(),
                gcr.getCourse().getTitle(),
                gcr.getTerm().getId(),
                gcr.getTerm().getTermType().name(),
                gcr.getPreviousGrade(),
                gcr.getNewGrade(),
                gcr.getReason(),
                gcr.getStatus().name(),
                gcr.getRequestedBy().getUsername(),
                gcr.getApprovedBy() != null ? gcr.getApprovedBy().getUsername() : null,
                gcr.getCreatedAt()
        );
    }
}
