package com.sdt.web_app.controller.institution;

import com.sdt.web_app.annotation.Auditable;
import com.sdt.web_app.dto.institution.GradingScaleDtos.*;
import com.sdt.web_app.entities.institution.GradingScale;
import com.sdt.web_app.service.institution.GradingScaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/grading-scales")
@RequiredArgsConstructor
public class GradingScaleController {

    private final GradingScaleService gradingScaleService;

    @Auditable(action = "READ_ALL_GRADING_SCALES", entityName = "GradingScale")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<List<GradingScaleResponse>> getAllGradingScales() {
        List<GradingScaleResponse> list = gradingScaleService.getAllGradingScales().stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    @Auditable(action = "READ_GRADING_SCALE", entityName = "GradingScale", entityId = "#id")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<GradingScaleResponse> getGradingScaleById(@PathVariable Long id) {
        return ResponseEntity.ok(mapToResponse(gradingScaleService.getGradingScaleById(id)));
    }

    @Auditable(action = "CREATE_GRADING_SCALE", entityName = "GradingScale")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'REGISTRAR')")
    public ResponseEntity<GradingScaleResponse> createGradingScale(@Valid @RequestBody CreateGradingScaleRequest request) {
        GradingScale scale = gradingScaleService.createGradingScale(
                request.code(),
                request.numericGrade(),
                request.percentageMin(),
                request.percentageMax(),
                request.transmutedGrade(),
                request.remarks(),
                request.isPassing(),
                request.isNonNumeric()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(scale));
    }

    @Auditable(action = "UPDATE_GRADING_SCALE", entityName = "GradingScale", entityId = "#id")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'REGISTRAR')")
    public ResponseEntity<GradingScaleResponse> updateGradingScale(
            @PathVariable Long id,
            @Valid @RequestBody UpdateGradingScaleRequest request
    ) {
        GradingScale scale = gradingScaleService.updateGradingScale(
                id,
                request.percentageMin(),
                request.percentageMax(),
                request.remarks(),
                request.isPassing()
        );
        return ResponseEntity.ok(mapToResponse(scale));
    }

    @Auditable(action = "DELETE_GRADING_SCALE", entityName = "GradingScale", entityId = "#id")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Void> deleteGradingScale(@PathVariable Long id) {
        gradingScaleService.deleteGradingScale(id);
        return ResponseEntity.noContent().build();
    }

    private GradingScaleResponse mapToResponse(GradingScale g) {
        return new GradingScaleResponse(
                g.getId(),
                g.getCode(),
                g.getNumericGrade(),
                g.getPercentageMin(),
                g.getPercentageMax(),
                g.getTransmutedGrade(),
                g.getRemarks(),
                g.isPassing(),
                g.isNonNumeric()
        );
    }
}
