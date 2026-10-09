package com.sdt.web_app.service.analytics;

import com.sdt.web_app.BaseIntegrationTest;
import com.sdt.web_app.dto.analytics.AcknowledgeInterventionRequest;
import com.sdt.web_app.entities.analytics.StudentIntervention;
import com.sdt.web_app.entities.analytics.StudentIntervention.InterventionStatus;
import com.sdt.web_app.entities.analytics.StudentIntervention.InterventionType;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.repositories.analytics.StudentInterventionRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class InterventionAcknowledgeTest extends BaseIntegrationTest {

    @Autowired
    private StudentInterventionService interventionService;

    @Autowired
    private DigitalTwinRiskService digitalTwinRiskService;

    @Autowired
    private StudentInterventionRepository interventionRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    private StudentProfile testStudent;
    private StudentIntervention testIntervention;
    private Long studentUserId;

    @BeforeEach
    void setUp() {
        testStudent = studentProfileRepository.findAll().stream()
                .filter(s -> s.getUser() != null)
                .findFirst()
                .orElse(null);

        if (testStudent != null) {
            studentUserId = testStudent.getUser().getId();
            testIntervention = interventionRepository.save(
                    StudentIntervention.builder()
                            .student(testStudent)
                            .interventionType(InterventionType.ACADEMIC_TUTORING)
                            .status(InterventionStatus.DISPATCHED)
                            .triggerFactor("Midterm risk evaluation flag")
                            .caseNotes("Initial counseling review")
                            .build()
            );
        }
    }

    @Test
    @DisplayName("STUDENT can acknowledge own intervention via StudentInterventionService")
    void studentShouldBeAbleToAcknowledgeOwnIntervention() {
        if (testIntervention == null) return;

        AcknowledgeInterventionRequest request =
                new AcknowledgeInterventionRequest("I acknowledge this feedback.");

        assertThatNoException().isThrownBy(() ->
                interventionService.acknowledgeIntervention(testIntervention.getId(), studentUserId, request)
        );

        StudentIntervention updated = interventionRepository.findById(testIntervention.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(InterventionStatus.ACKNOWLEDGED);
        assertThat(updated.getAcknowledgedAt()).isNotNull();
        assertThat(updated.getStudentResponse()).contains("I acknowledge this feedback.");
    }

    @Test
    @DisplayName("STUDENT can acknowledge own intervention via DigitalTwinRiskService")
    void studentShouldBeAbleToAcknowledgeOwnInterventionViaRiskService() {
        if (testIntervention == null) return;

        AcknowledgeInterventionRequest request =
                new AcknowledgeInterventionRequest("Acknowledged via digital twin telemetry portal.");

        assertThatNoException().isThrownBy(() ->
                digitalTwinRiskService.acknowledgeIntervention(testIntervention.getId(), studentUserId, request)
        );

        StudentIntervention updated = interventionRepository.findById(testIntervention.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(InterventionStatus.ACKNOWLEDGED);
        assertThat(updated.getAcknowledgedAt()).isNotNull();
    }

    @Test
    @DisplayName("STUDENT should not be able to acknowledge another student's intervention")
    void studentShouldNotBeAbleToAcknowledgeAnotherStudentsIntervention() {
        if (testIntervention == null) return;

        Long wrongStudentId = 999999L; // Not the owner

        assertThatThrownBy(() ->
                interventionService.acknowledgeIntervention(
                        testIntervention.getId(), wrongStudentId, new AcknowledgeInterventionRequest("Unauthorized attempt"))
        ).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unauthorized: intervention does not belong to student.");
    }

    @Test
    @DisplayName("Acknowledging already acknowledged intervention should be idempotent and return without error")
    void acknowledgingAlreadyAcknowledgedInterventionShouldReturn200NotThrow409() {
        if (testIntervention == null) return;

        // First acknowledgement
        interventionService.acknowledgeIntervention(
                testIntervention.getId(), studentUserId, new AcknowledgeInterventionRequest("Initial acknowledge"));

        // Second acknowledgement must NOT throw 409 Conflict — should be idempotent
        assertThatNoException().isThrownBy(() ->
                interventionService.acknowledgeIntervention(
                        testIntervention.getId(), studentUserId, new AcknowledgeInterventionRequest("Acknowledging again"))
        );

        StudentIntervention updated = interventionRepository.findById(testIntervention.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(InterventionStatus.ACKNOWLEDGED);
    }

    @Test
    @DisplayName("Intervention status should transition to ACKNOWLEDGED with timestamp")
    void interventionStatusShouldBeAcknowledgedAfterStudentAcknowledges() {
        if (testIntervention == null) return;

        interventionService.acknowledgeIntervention(
                testIntervention.getId(), studentUserId, new AcknowledgeInterventionRequest("OK"));

        StudentIntervention result = interventionRepository.findById(testIntervention.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(InterventionStatus.ACKNOWLEDGED);
        assertThat(result.getAcknowledgedAt()).isNotNull();
    }

    @Test
    @DisplayName("Acknowledge with empty response should succeed when response text is optional")
    void acknowledgeWithEmptyResponseShouldSucceedIfResponseIsOptional() {
        if (testIntervention == null) return;

        assertThatNoException().isThrownBy(() ->
                interventionService.acknowledgeIntervention(
                        testIntervention.getId(), studentUserId, new AcknowledgeInterventionRequest(""))
        );

        StudentIntervention result = interventionRepository.findById(testIntervention.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(InterventionStatus.ACKNOWLEDGED);
    }
}
