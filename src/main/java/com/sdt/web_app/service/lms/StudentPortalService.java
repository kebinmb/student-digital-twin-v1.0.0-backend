package com.sdt.web_app.service.lms;

import com.sdt.web_app.dto.lms.LmsDtos.*;
import com.sdt.web_app.entities.enrollment.StudentEnrollment;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.repositories.enrollment.StudentEnrollmentRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentPortalService {

    private final StudentProfileRepository profileRepository;
    private final StudentEnrollmentRepository enrollmentRepository;

    @Transactional(readOnly = true)
    public StudentSelfServiceSummaryDto getStudentPortalSummary(Long studentId) {
        StudentProfile student = profileRepository.findByIdWithProgramAndCurriculum(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found with id: " + studentId));

        List<StudentEnrollment> enrollments = enrollmentRepository.findByStudentIdWithDetails(student.getId());


        List<EnrolledCourseSummaryDto> currentCourses = enrollments.stream()
                .flatMap(se -> se.getItems().stream())
                .map(item -> {
                    String schedText = item.getSection().getSchedules().stream()
                            .map(s -> s.getDayOfWeek() + " " + s.getStartTime() + "-" + s.getEndTime() + " (" + s.getRoom().getCode() + ")")
                            .reduce((a, b) -> a + "; " + b).orElse("TBA");

                    String grade = item.getFinalNumericalGrade() != null ? String.format("%.2f", item.getFinalNumericalGrade()) : "In Progress";
                    return new EnrolledCourseSummaryDto(
                            item.getSection().getId(),
                            item.getSection().getSectionCode(),
                            item.getSection().getCourse().getCode(),
                            item.getSection().getCourse().getTitle(),
                            item.getSection().getCourse().getCreditUnits().toString(),
                            schedText,
                            item.getSection().getGradeStatus().name(),
                            grade
                    );
                })
                .toList();

        String studentName = student.getFullName();
        String gpa = student.getCumulativeGpa() != null ? String.format("%.2f", student.getCumulativeGpa()) : "N/A";
        String units = student.getTotalUnitsEarned() != null ? student.getTotalUnitsEarned().toString() : "0.00";

        return new StudentSelfServiceSummaryDto(
                student.getId(),
                student.getStudentNumber(),
                studentName,
                student.getProgram() != null ? student.getProgram().getCode() : "N/A",
                student.getYearLevel(),
                gpa,
                units,
                student.getFinancialClearance() != null ? student.getFinancialClearance().name() : "CLEARED",
                student.getDepartmentalClearance() != null ? student.getDepartmentalClearance().name() : "CLEARED",
                currentCourses
        );
    }
}
