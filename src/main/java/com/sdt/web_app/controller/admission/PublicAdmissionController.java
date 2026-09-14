package com.sdt.web_app.controller.admission;

import com.sdt.web_app.dto.admission.AdmissionDtos.*;
import com.sdt.web_app.service.admission.AdmissionQueueService;
import com.sdt.web_app.service.admission.AdmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/public/admission", "/api/public/admission"})
@RequiredArgsConstructor
public class PublicAdmissionController {

    private final AdmissionQueueService queueService;
    private final AdmissionService admissionService;

    @GetMapping("/config")
    public ResponseEntity<AdmissionConfigDto> getAdmissionConfig(@RequestParam(value = "termId", required = false) Long termId) {
        return ResponseEntity.ok(admissionService.getAdmissionConfig(termId));
    }

    @GetMapping("/check-email")
    public ResponseEntity<EmailAvailabilityResponse> checkEmail(
            @RequestParam("email") String email,
            @RequestParam(value = "termId", required = false) Long termId) {
        boolean available = admissionService.isEmailAvailable(email, termId);
        return ResponseEntity.ok(new EmailAvailabilityResponse(available));
    }

    @PostMapping("/queue/token")
    public ResponseEntity<QueueTokenResponse> requestQueueToken(@RequestBody(required = false) QueueTokenRequest request) {
        String clientId = request != null ? request.clientIdentifier() : null;
        QueueTokenResponse response = queueService.issueToken(clientId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/queue/status")
    public ResponseEntity<QueueTokenResponse> checkQueueStatus(@RequestParam("token") String token) {
        QueueTokenResponse response = queueService.checkTokenStatus(token);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/programs")
    public ResponseEntity<List<PublicProgramDto>> getPublicPrograms() {
        return ResponseEntity.ok(admissionService.getPublicPrograms());
    }

    @GetMapping("/terms")
    public ResponseEntity<List<PublicTermDto>> getPublicTerms() {
        return ResponseEntity.ok(admissionService.getPublicTerms());
    }

    @GetMapping("/exam-slots")
    public ResponseEntity<List<EntranceExamSlotResponse>> getAvailableExamSlots(@RequestParam(value = "termId", required = false) Long termId) {
        List<EntranceExamSlotResponse> slots = admissionService.getAvailableExamSlots(termId);
        return ResponseEntity.ok(slots);
    }

    @PostMapping("/apply")
    public ResponseEntity<AdmissionApplicationResponse> submitApplication(@Valid @RequestBody SubmitAdmissionRequest request) {
        AdmissionApplicationResponse response = admissionService.submitApplication(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/track/{applicationNumber}")
    public ResponseEntity<AdmissionApplicationResponse> trackApplication(@PathVariable("applicationNumber") String applicationNumber) {
        AdmissionApplicationResponse response = admissionService.getApplicationByNumber(applicationNumber);
        return ResponseEntity.ok(response);
    }
}
