package com.sdt.web_app.service.financial;

import com.sdt.web_app.dto.financial.FinancialDtos.*;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.enrollment.EnrollmentCourseItem;
import com.sdt.web_app.entities.enrollment.StudentEnrollment;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.financial.FeeTemplate;
import com.sdt.web_app.entities.financial.StudentAssessmentInvoice;
import com.sdt.web_app.entities.institution.AcademicYear;
import com.sdt.web_app.entities.institution.Course;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.entities.institution.TermType;
import com.sdt.web_app.entities.scheduling.ClassSection;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.enrollment.StudentEnrollmentRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.repositories.financial.FeeTemplateRepository;
import com.sdt.web_app.repositories.financial.StudentAccountLedgerRepository;
import com.sdt.web_app.repositories.financial.StudentAssessmentInvoiceRepository;
import com.sdt.web_app.repositories.institution.AcademicYearRepository;
import com.sdt.web_app.repositories.institution.CampusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FeeAssessmentServiceTest {

    @Mock
    private StudentEnrollmentRepository enrollmentRepository;
    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private StudentAssessmentInvoiceRepository invoiceRepository;
    @Mock
    private StudentAccountLedgerRepository ledgerRepository;
    @Mock
    private FeeTemplateRepository feeTemplateRepository;
    @Mock
    private AcademicYearRepository academicYearRepository;
    @Mock
    private CampusRepository campusRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FeeAssessmentService feeAssessmentService;

    private StudentProfile studentProfile;
    private StudentEnrollment enrollment;
    private Term term;
    private FeeTemplate feeTemplate;

    @BeforeEach
    void setUp() {
        AcademicYear ay = AcademicYear.builder().code("AY 2026-2027").build();
        term = Term.builder().academicYear(ay).termType(TermType.FIRST_SEM).build();

        User user = User.builder().username("juan_student").build();
        studentProfile = StudentProfile.builder()
                .studentNumber("2026-CS-0001")
                .user(user)
                .enrollmentStatus(StudentProfile.EnrollmentStatus.REGULAR)
                .build();

        Course course = Course.builder()
                .code("CS-101")
                .lectureUnits(new BigDecimal("3.00"))
                .labUnits(new BigDecimal("1.00"))
                .build();

        ClassSection section = ClassSection.builder()
                .course(course)
                .build();

        EnrollmentCourseItem item = EnrollmentCourseItem.builder()
                .section(section)
                .build();

        enrollment = StudentEnrollment.builder()
                .student(studentProfile)
                .term(term)
                .items(List.of(item))
                .totalCreditUnits(new BigDecimal("4.00"))
                .build();

        feeTemplate = FeeTemplate.builder()
                .tuitionPerUnit(new BigDecimal("250.00")) // 3 lec * 250 = 750
                .labFeePerUnit(new BigDecimal("500.00"))   // 1 lab * 500 = 500
                .miscellaneousFlatFee(new BigDecimal("1500.00"))
                .athleticFlatFee(new BigDecimal("300.00"))
                .build();
    }

    @Test
    @DisplayName("Phase 4: Fee Assessment automatically calculates tuition, lab fees, and 100% FHE subsidy for regular student")
    void testAssessEnrollment_FheEligible_Success() {
        given(enrollmentRepository.findById(100L)).willReturn(Optional.of(enrollment));
        given(invoiceRepository.findByStudentEnrollmentId(100L)).willReturn(Optional.empty());
        given(feeTemplateRepository.findFirstByAcademicYearIdAndActiveTrueOrderByCreatedAtDesc(any())).willReturn(Optional.of(feeTemplate));
        given(invoiceRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        StudentAssessmentInvoiceDto dto = feeAssessmentService.assessEnrollment(100L, 1L);

        assertThat(dto).isNotNull();
        assertThat(dto.totalTuitionFee()).isEqualByComparingTo("750.00");
        assertThat(dto.totalLabFee()).isEqualByComparingTo("500.00");
        assertThat(dto.totalMiscFee()).isEqualByComparingTo("1800.00"); // 1500 + 300
        assertThat(dto.totalGrossAssessment()).isEqualByComparingTo("3050.00"); // 750 + 500 + 1800
        assertThat(dto.fheSubsidyAmount()).isEqualByComparingTo("3050.00");
        assertThat(dto.netAssessedAmount()).isEqualByComparingTo("0.00");
        assertThat(dto.fheEligible()).isTrue();
        assertThat(dto.status()).isEqualTo("FHE_COVERED");
    }
}
