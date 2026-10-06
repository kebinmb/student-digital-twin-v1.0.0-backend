package com.sdt.web_app.service.financial;

import com.sdt.web_app.dto.financial.FinancialDtos.*;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.enrollment.EnrollmentCourseItem;
import com.sdt.web_app.entities.enrollment.StudentEnrollment;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.financial.FeeTemplate;
import com.sdt.web_app.entities.financial.StudentAccountLedger;
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

import java.util.Set;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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
    @Mock
    private com.sdt.web_app.service.webhook.InstitutionalWebhookService institutionalWebhookService;

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
                .items(Set.of(item))
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

    @Test
    @DisplayName("Fee Assessment automatically applies 100% tuition discount for President's List honor student when self-paying")
    void testAssessEnrollment_PresidentsList_FullTuitionDiscount() {
        StudentProfile nonFheStudent = StudentProfile.builder()
                .id(2L)
                .studentNumber("2026-CS-0002")
                .user(User.builder().username("maria_student").build())
                .enrollmentStatus(StudentProfile.EnrollmentStatus.PROBATION)
                .build();

        Term currentTerm = Term.builder()
                .id(200L)
                .academicYear(AcademicYear.builder().code("AY 2026-2027").build())
                .termType(TermType.FIRST_SEM)
                .build();

        Course course = Course.builder()
                .code("CS-101")
                .lectureUnits(new BigDecimal("3.00"))
                .labUnits(new BigDecimal("1.00"))
                .build();

        ClassSection section = ClassSection.builder().course(course).build();
        EnrollmentCourseItem currentItem = EnrollmentCourseItem.builder().section(section).build();

        StudentEnrollment currentEnrollment = StudentEnrollment.builder()
                .id(100L)
                .student(nonFheStudent)
                .term(currentTerm)
                .items(Set.of(currentItem))
                .build();

        // Prior term enrollment qualifying for President's List (GPA <= 1.25, 15 units)
        Term priorTerm = Term.builder()
                .id(199L)
                .academicYear(AcademicYear.builder().code("AY 2025-2026").build())
                .termType(TermType.SECOND_SEM)
                .build();

        Course c1 = Course.builder().code("MATH-101").creditUnits(new BigDecimal("3.00")).build();
        ClassSection s1 = ClassSection.builder().course(c1).build();

        Set<EnrollmentCourseItem> priorItems = new java.util.HashSet<>();
        for (int i = 0; i < 5; i++) {
            priorItems.add(EnrollmentCourseItem.builder()
                    .id((long) (i + 1))
                    .section(s1)
                    .finalNumericalGrade(new BigDecimal("1.25"))
                    .completionStatus(EnrollmentCourseItem.CompletionStatus.PASSED)
                    .build());
        }

        StudentEnrollment priorEnrollment = StudentEnrollment.builder()
                .id(50L)
                .student(nonFheStudent)
                .term(priorTerm)
                .items(priorItems)
                .build();

        given(enrollmentRepository.findById(100L)).willReturn(Optional.of(currentEnrollment));
        given(enrollmentRepository.findByStudentId(nonFheStudent.getId())).willReturn(List.of(priorEnrollment));
        given(invoiceRepository.findByStudentEnrollmentId(100L)).willReturn(Optional.empty());
        given(feeTemplateRepository.findFirstByAcademicYearIdAndActiveTrueOrderByCreatedAtDesc(any())).willReturn(Optional.of(feeTemplate));
        given(invoiceRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        StudentAssessmentInvoiceDto dto = feeAssessmentService.assessEnrollment(100L, 1L);

        assertThat(dto).isNotNull();
        assertThat(dto.totalTuitionFee()).isEqualByComparingTo("750.00");
        assertThat(dto.totalGrossAssessment()).isEqualByComparingTo("3050.00");
        assertThat(dto.scholarshipDiscountAmount()).isEqualByComparingTo("750.00"); // 100% of 750
        assertThat(dto.fheSubsidyAmount()).isEqualByComparingTo("0.00");
        assertThat(dto.netAssessedAmount()).isEqualByComparingTo("2300.00"); // 3050 - 750 = 2300
        assertThat(dto.status()).isEqualTo("UNPAID");

        verify(ledgerRepository).save(argThat(l -> 
                l.getTransactionType() == StudentAccountLedger.TransactionType.DISCOUNT &&
                l.getCreditAmount().compareTo(new BigDecimal("750.00")) == 0 &&
                l.getDescription().contains("President's List Academic Scholarship")
        ));
    }

    @Test
    @DisplayName("Fee Assessment automatically applies 50% tuition discount for Dean's List honor student when self-paying")
    void testAssessEnrollment_DeansList_HalfTuitionDiscount() {
        StudentProfile nonFheStudent = StudentProfile.builder()
                .id(3L)
                .studentNumber("2026-CS-0003")
                .user(User.builder().username("pedro_student").build())
                .enrollmentStatus(StudentProfile.EnrollmentStatus.PROBATION)
                .build();

        Term currentTerm = Term.builder()
                .id(200L)
                .academicYear(AcademicYear.builder().code("AY 2026-2027").build())
                .termType(TermType.FIRST_SEM)
                .build();

        Course course = Course.builder()
                .code("CS-101")
                .lectureUnits(new BigDecimal("3.00"))
                .labUnits(new BigDecimal("1.00"))
                .build();

        ClassSection section = ClassSection.builder().course(course).build();
        EnrollmentCourseItem currentItem = EnrollmentCourseItem.builder().section(section).build();

        StudentEnrollment currentEnrollment = StudentEnrollment.builder()
                .id(100L)
                .student(nonFheStudent)
                .term(currentTerm)
                .items(Set.of(currentItem))
                .build();

        Term priorTerm = Term.builder()
                .id(199L)
                .academicYear(AcademicYear.builder().code("AY 2025-2026").build())
                .termType(TermType.SECOND_SEM)
                .build();

        Course c1 = Course.builder().code("CS-102").creditUnits(new BigDecimal("3.00")).build();
        ClassSection s1 = ClassSection.builder().course(c1).build();

        Set<EnrollmentCourseItem> priorItems = new java.util.HashSet<>();
        for (int i = 0; i < 5; i++) {
            priorItems.add(EnrollmentCourseItem.builder()
                    .id((long) (i + 10))
                    .section(s1)
                    .finalNumericalGrade(new BigDecimal("1.60"))
                    .completionStatus(EnrollmentCourseItem.CompletionStatus.PASSED)
                    .build());
        }

        StudentEnrollment priorEnrollment = StudentEnrollment.builder()
                .id(60L)
                .student(nonFheStudent)
                .term(priorTerm)
                .items(priorItems)
                .build();

        given(enrollmentRepository.findById(100L)).willReturn(Optional.of(currentEnrollment));
        given(enrollmentRepository.findByStudentId(nonFheStudent.getId())).willReturn(List.of(priorEnrollment));
        given(invoiceRepository.findByStudentEnrollmentId(100L)).willReturn(Optional.empty());
        given(feeTemplateRepository.findFirstByAcademicYearIdAndActiveTrueOrderByCreatedAtDesc(any())).willReturn(Optional.of(feeTemplate));
        given(invoiceRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        StudentAssessmentInvoiceDto dto = feeAssessmentService.assessEnrollment(100L, 1L);

        assertThat(dto).isNotNull();
        assertThat(dto.totalTuitionFee()).isEqualByComparingTo("750.00");
        assertThat(dto.scholarshipDiscountAmount()).isEqualByComparingTo("375.00"); // 50% of 750
        assertThat(dto.netAssessedAmount()).isEqualByComparingTo("2675.00"); // 3050 - 375 = 2675

        verify(ledgerRepository).save(argThat(l -> 
                l.getTransactionType() == StudentAccountLedger.TransactionType.DISCOUNT &&
                l.getCreditAmount().compareTo(new BigDecimal("375.00")) == 0 &&
                l.getDescription().contains("Dean's List Academic Scholarship")
        ));
    }
}
