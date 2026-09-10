package com.sdt.web_app.service.security;

import com.sdt.web_app.entities.institution.Department;
import com.sdt.web_app.entities.institution.Program;
import com.sdt.web_app.repositories.institution.DepartmentRepository;
import com.sdt.web_app.repositories.institution.ProgramRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DataScopingServiceTest {

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private ProgramRepository programRepository;

    @InjectMocks
    private DataScopingService dataScopingService;

    @Test
    @DisplayName("ADMIN has unrestricted scope (Optional.empty)")
    void getScopedProgramIds_Admin_ReturnsEmpty() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin", "pw", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        Optional<List<Long>> scoped = dataScopingService.getScopedProgramIds(auth);

        assertThat(scoped).isEmpty();
    }

    @Test
    @DisplayName("REGISTRAR has unrestricted scope (Optional.empty)")
    void getScopedProgramIds_Registrar_ReturnsEmpty() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "registrar_bob", "pw", List.of(new SimpleGrantedAuthority("ROLE_REGISTRAR")));

        Optional<List<Long>> scoped = dataScopingService.getScopedProgramIds(auth);

        assertThat(scoped).isEmpty();
    }

    @Test
    @DisplayName("DEAN is scoped to college and its departments' programs")
    void getScopedProgramIds_Dean_ReturnsCollegePrograms() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "dean_morris", "pw", List.of(new SimpleGrantedAuthority("ROLE_DEAN")));

        given(securityUtils.resolveUserId(auth)).willReturn(2L);

        Department college = Department.builder().code("CCS").name("College of Computer Studies").build();
        ReflectionTestUtils.setField(college, "id", 7L);

        Department childDept = Department.builder().code("IT_DEPT").name("IT Department").build();
        ReflectionTestUtils.setField(childDept, "id", 12L);

        given(departmentRepository.findByDeanUserId(2L)).willReturn(List.of(college));
        given(departmentRepository.findByParentDepartmentId(7L)).willReturn(List.of(childDept));

        Program p1 = Program.builder().code("BSIT").name("BS Info Tech").build();
        ReflectionTestUtils.setField(p1, "id", 101L);
        Program p2 = Program.builder().code("BSIS").name("BS Info Systems").build();
        ReflectionTestUtils.setField(p2, "id", 102L);

        given(programRepository.findByDepartmentIdIn(List.of(7L, 12L))).willReturn(List.of(p1, p2));

        Optional<List<Long>> scoped = dataScopingService.getScopedProgramIds(auth);

        assertThat(scoped).isPresent();
        assertThat(scoped.get()).containsExactlyInAnyOrder(101L, 102L);
    }

    @Test
    @DisplayName("CHAIRPERSON is scoped to assigned programs")
    void getScopedProgramIds_Chairperson_ReturnsChairpersonPrograms() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "chair_clark", "pw", List.of(new SimpleGrantedAuthority("ROLE_CHAIRPERSON")));

        given(securityUtils.resolveUserId(auth)).willReturn(3L);

        Program p1 = Program.builder().code("BSIT").name("BS Info Tech").build();
        ReflectionTestUtils.setField(p1, "id", 101L);

        given(programRepository.findByChairpersonUserId(3L)).willReturn(List.of(p1));

        Optional<List<Long>> scoped = dataScopingService.getScopedProgramIds(auth);

        assertThat(scoped).isPresent();
        assertThat(scoped.get()).containsExactly(101L);
    }

    @Test
    @DisplayName("isOnlyFaculty identifies standalone faculty correctly")
    void isOnlyFaculty_ChecksRolesCorrectly() {
        Authentication pureFaculty = new UsernamePasswordAuthenticationToken(
                "faculty_alice", "pw", List.of(new SimpleGrantedAuthority("ROLE_FACULTY")));
        assertThat(dataScopingService.isOnlyFaculty(pureFaculty)).isTrue();

        Authentication facultyWithAdmin = new UsernamePasswordAuthenticationToken(
                "admin_prof", "pw", List.of(new SimpleGrantedAuthority("ROLE_FACULTY"), new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertThat(dataScopingService.isOnlyFaculty(facultyWithAdmin)).isFalse();

        Authentication deanWithFaculty = new UsernamePasswordAuthenticationToken(
                "dean_morris", "pw", List.of(new SimpleGrantedAuthority("ROLE_DEAN"), new SimpleGrantedAuthority("ROLE_FACULTY")));
        assertThat(dataScopingService.isOnlyFaculty(deanWithFaculty)).isFalse();
    }
}
