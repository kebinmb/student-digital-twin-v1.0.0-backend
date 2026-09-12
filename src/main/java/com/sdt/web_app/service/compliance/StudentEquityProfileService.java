package com.sdt.web_app.service.compliance;

import com.sdt.web_app.dto.compliance.EquityDtos.*;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.compliance.StudentEquityProfile;
import com.sdt.web_app.entities.compliance.StudentEquityProfile.EquityVerificationStatus;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.compliance.StudentEquityProfileRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentEquityProfileService {

    private final StudentEquityProfileRepository equityRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public StudentEquityProfileDto getEquityProfileByStudentProfileId(Long studentProfileId) {
        StudentEquityProfile equity = equityRepository.findByStudentProfileId(studentProfileId)
                .orElseGet(() -> createDefaultProfileForStudentId(studentProfileId));
        return mapToDto(equity);
    }

    @Transactional(readOnly = true)
    public StudentEquityProfileDto getEquityProfileForUser(Long userId) {
        StudentProfile studentProfile = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found for user ID: " + userId));
        return getEquityProfileByStudentProfileId(studentProfile.getId());
    }

    @Transactional
    public StudentEquityProfileDto upsertEquityProfileForUser(Long userId, UpdateStudentEquityProfileRequest request) {
        StudentProfile studentProfile = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found for user ID: " + userId));

        StudentEquityProfile profile = equityRepository.findByStudentProfileId(studentProfile.getId())
                .orElse(StudentEquityProfile.builder()
                        .studentProfile(studentProfile)
                        .verificationStatus(EquityVerificationStatus.SELF_DECLARED)
                        .build());

        profile.setIs4psBeneficiary(request.getIs4psBeneficiary());
        profile.setHousehold4psIdNumber(request.getHousehold4psIdNumber());
        profile.setIsListahananNhts(request.getIsListahananNhts());
        profile.setUnifastTesAwardee(request.getUnifastTesAwardee());
        profile.setUnifastTesAwardNumber(request.getUnifastTesAwardNumber());

        profile.setIsIndigenousPeople(request.getIsIndigenousPeople());
        profile.setIpEthnicGroup(request.getIpEthnicGroup());
        profile.setNcipCertificateNumber(request.getNcipCertificateNumber());

        profile.setIsPersonWithDisability(request.getIsPersonWithDisability());
        profile.setPwdIdNumber(request.getPwdIdNumber());
        profile.setDisabilityType(request.getDisabilityType());

        profile.setIsSoloParentOrDependent(request.getIsSoloParentOrDependent());
        profile.setSoloParentIdNumber(request.getSoloParentIdNumber());

        profile.setIsFirstGenerationCollege(request.getIsFirstGenerationCollege());
        profile.setIsGidaResident(request.getIsGidaResident());
        profile.setMonthlyHouseholdIncomeBracket(request.getMonthlyHouseholdIncomeBracket());

        // If updated by student, status returns to DOCUMENTED if IDs present, else SELF_DECLARED unless already VERIFIED
        if (profile.getVerificationStatus() != EquityVerificationStatus.VERIFIED) {
            boolean hasSupportingIds = (request.getHousehold4psIdNumber() != null && !request.getHousehold4psIdNumber().isBlank())
                    || (request.getNcipCertificateNumber() != null && !request.getNcipCertificateNumber().isBlank())
                    || (request.getPwdIdNumber() != null && !request.getPwdIdNumber().isBlank())
                    || (request.getSoloParentIdNumber() != null && !request.getSoloParentIdNumber().isBlank())
                    || (request.getUnifastTesAwardNumber() != null && !request.getUnifastTesAwardNumber().isBlank());
            profile.setVerificationStatus(hasSupportingIds ? EquityVerificationStatus.DOCUMENTED : EquityVerificationStatus.SELF_DECLARED);
        }

        StudentEquityProfile saved = equityRepository.save(profile);
        log.info("Successfully updated equity profile for studentProfileId: {}", studentProfile.getId());
        return mapToDto(saved);
    }

    @Transactional
    public StudentEquityProfileDto verifyEquityProfile(Long profileId, Long verifierUserId, VerifyEquityProfileRequest request) {
        StudentEquityProfile profile = equityRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Equity profile not found with ID: " + profileId));

        User verifier = userRepository.findById(verifierUserId)
                .orElseThrow(() -> new IllegalArgumentException("Verifier user not found with ID: " + verifierUserId));

        profile.setVerificationStatus(request.getVerificationStatus());
        profile.setVerifiedBy(verifier);
        profile.setVerifiedAt(LocalDateTime.now());
        profile.setVerificationRemarks(request.getVerificationRemarks());

        StudentEquityProfile saved = equityRepository.save(profile);
        log.info("Verified equity profile ID {} status set to {} by user {}", profileId, request.getVerificationStatus(), verifierUserId);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<StudentEquityProfileDto> searchEquityProfiles(
            String search,
            EquityVerificationStatus status,
            Boolean is4ps,
            Boolean isIp,
            Boolean isPwd,
            Boolean isGida,
            Boolean isFirstGen,
            Pageable pageable) {

        Page<StudentEquityProfile> page = equityRepository.searchProfiles(
                search, status, is4ps, isIp, isPwd, isGida, isFirstGen, pageable);
        return page.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public EquityStatisticsSummaryDto getEquityStatisticsSummary() {
        long total = equityRepository.count();
        long count4ps = equityRepository.countByIs4psBeneficiaryTrue();
        long countNhts = equityRepository.countByIsListahananNhtsTrue();
        long countTes = equityRepository.countByUnifastTesAwardeeTrue();
        long countIp = equityRepository.countByIsIndigenousPeopleTrue();
        long countPwd = equityRepository.countByIsPersonWithDisabilityTrue();
        long countSolo = equityRepository.countByIsSoloParentOrDependentTrue();
        long countFirstGen = equityRepository.countByIsFirstGenerationCollegeTrue();
        long countGida = equityRepository.countByIsGidaResidentTrue();

        long countSelfDeclared = equityRepository.countByVerificationStatus(EquityVerificationStatus.SELF_DECLARED);
        long countDocumented = equityRepository.countByVerificationStatus(EquityVerificationStatus.DOCUMENTED);
        long countVerified = equityRepository.countByVerificationStatus(EquityVerificationStatus.VERIFIED);
        long countRejected = equityRepository.countByVerificationStatus(EquityVerificationStatus.REJECTED);

        return EquityStatisticsSummaryDto.builder()
                .totalProfilesCount(total)
                .count4psBeneficiaries(count4ps)
                .countListahananNhts(countNhts)
                .countUnifastTesAwardees(countTes)
                .countIndigenousPeoples(countIp)
                .countPersonsWithDisabilities(countPwd)
                .countSoloParents(countSolo)
                .countFirstGenerationCollege(countFirstGen)
                .countGidaResidents(countGida)
                .countSelfDeclared(countSelfDeclared)
                .countDocumented(countDocumented)
                .countVerified(countVerified)
                .countRejected(countRejected)
                .build();
    }

    private StudentEquityProfile createDefaultProfileForStudentId(Long studentProfileId) {
        StudentProfile sp = studentProfileRepository.findById(studentProfileId)
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found: " + studentProfileId));
        StudentEquityProfile defaultProfile = StudentEquityProfile.builder()
                .studentProfile(sp)
                .verificationStatus(EquityVerificationStatus.SELF_DECLARED)
                .build();
        return equityRepository.save(defaultProfile);
    }

    private StudentEquityProfileDto mapToDto(StudentEquityProfile entity) {
        StudentProfile sp = entity.getStudentProfile();
        User user = sp != null ? sp.getUser() : null;

        return StudentEquityProfileDto.builder()
                .id(entity.getId())
                .studentProfileId(sp != null ? sp.getId() : null)
                .studentNumber(sp != null ? sp.getStudentNumber() : null)
                .studentName(user != null ? user.getUsername() : null)
                .programCode(sp != null && sp.getProgram() != null ? sp.getProgram().getCode() : null)
                .programName(sp != null && sp.getProgram() != null ? sp.getProgram().getName() : null)
                .is4psBeneficiary(entity.getIs4psBeneficiary())
                .household4psIdNumber(entity.getHousehold4psIdNumber())
                .isListahananNhts(entity.getIsListahananNhts())
                .unifastTesAwardee(entity.getUnifastTesAwardee())
                .unifastTesAwardNumber(entity.getUnifastTesAwardNumber())
                .isIndigenousPeople(entity.getIsIndigenousPeople())
                .ipEthnicGroup(entity.getIpEthnicGroup())
                .ncipCertificateNumber(entity.getNcipCertificateNumber())
                .isPersonWithDisability(entity.getIsPersonWithDisability())
                .pwdIdNumber(entity.getPwdIdNumber())
                .disabilityType(entity.getDisabilityType())
                .isSoloParentOrDependent(entity.getIsSoloParentOrDependent())
                .soloParentIdNumber(entity.getSoloParentIdNumber())
                .isFirstGenerationCollege(entity.getIsFirstGenerationCollege())
                .isGidaResident(entity.getIsGidaResident())
                .monthlyHouseholdIncomeBracket(entity.getMonthlyHouseholdIncomeBracket())
                .verificationStatus(entity.getVerificationStatus())
                .verifiedByUserId(entity.getVerifiedBy() != null ? entity.getVerifiedBy().getId() : null)
                .verifiedByUsername(entity.getVerifiedBy() != null ? entity.getVerifiedBy().getUsername() : null)
                .verifiedAt(entity.getVerifiedAt())
                .verificationRemarks(entity.getVerificationRemarks())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
