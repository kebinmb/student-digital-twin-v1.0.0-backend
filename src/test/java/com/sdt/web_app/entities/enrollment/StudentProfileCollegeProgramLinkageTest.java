package com.sdt.web_app.entities.enrollment;

import com.sdt.web_app.dto.enrollment.EnrollmentDtos.StudentProfileResponse;
import com.sdt.web_app.entities.authentication.Roles;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.institution.Curriculum;
import com.sdt.web_app.entities.institution.Department;
import com.sdt.web_app.entities.institution.DepartmentType;
import com.sdt.web_app.entities.institution.Program;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class StudentProfileCollegeProgramLinkageTest {

    @Test
    @DisplayName("User and StudentProfile properly store and expose college and program references")
    void userAndStudentProfile_CollegeAndProgramLinkage() {
        Department college = Department.builder()
                .code("CCS")
                .name("College of Computer Studies")
                .type(DepartmentType.COLLEGE)
                .build();

        Program program = Program.builder()
                .code("BSIT")
                .name("Bachelor of Science in Information Technology")
                .department(college)
                .college(college)
                .build();

        Curriculum curriculum = Curriculum.builder()
                .code("BSIT-2026")
                .program(program)
                .build();

        User user = User.builder()
                .username("student_01")
                .email("student01@university.edu.ph")
                .program(program)
                .college(college)
                .build();
        user.addRole(Roles.STUDENT);

        StudentProfile profile = StudentProfile.builder()
                .user(user)
                .studentNumber("2026-00001")
                .firstName("Maria")
                .lastName("Santos")
                .program(program)
                .college(college)
                .curriculum(curriculum)
                .yearLevel(1)
                .build();

        assertThat(user.getRoles()).contains(Roles.STUDENT);
        assertThat(user.getProgram()).isEqualTo(program);
        assertThat(user.getCollege()).isEqualTo(college);

        assertThat(profile.getProgram()).isEqualTo(program);
        assertThat(profile.getCollege()).isEqualTo(college);
        assertThat(profile.getCollege().getName()).isEqualTo("College of Computer Studies");

        // Verify StudentProfileResponse mapping includes collegeId and collegeName
        StudentProfileResponse response = new StudentProfileResponse(
                profile.getId(),
                profile.getStudentNumber(),
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                profile.getFirstName(),
                profile.getMiddleName(),
                profile.getLastName(),
                profile.getSuffix(),
                profile.getFullName(),
                college.getId(),
                college.getName(),
                program.getId(),
                program.getCode(),
                program.getName(),
                curriculum.getId(),
                curriculum.getCode(),
                "CONTINUING",
                1,
                "REGULAR",
                false,
                BigDecimal.ZERO,
                null,
                "CLEARED",
                "CLEARED"
        );

        assertThat(response.collegeName()).isEqualTo("College of Computer Studies");
        assertThat(response.programCode()).isEqualTo("BSIT");
    }
}
