package com.sdt.web_app.service.institution;

import com.sdt.web_app.dto.institution.MajorDtos.*;
import java.util.List;

public interface MajorService {
    MajorDetailResponse createMajor(CreateMajorRequest request);
    MajorDetailResponse updateMajor(Long id, UpdateMajorRequest request);
    List<MajorSummaryResponse> getMajorsByProgram(Long programId);
    List<MajorSummaryResponse> getActiveMajorsByProgram(Long programId);
    MajorDetailResponse getMajorById(Long id);
    void deleteMajor(Long id);
}
