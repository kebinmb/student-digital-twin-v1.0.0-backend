package com.sdt.web_app.service.compliance;

import com.sdt.web_app.dto.common.SliceResponse;
import com.sdt.web_app.dto.compliance.EquityDtos.*;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.compliance.StudentEquityProfile;
import com.sdt.web_app.entities.compliance.StudentEquityProfile.EquityVerificationStatus;
import com.sdt.web_app.entities.compliance.StudentEquityProfile.HouseholdIncomeBracket;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.compliance.StudentEquityProfileRepository;
import com.sdt.web_app.service.security.StudentProfileL2CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentEquityProfileService {

    private final StudentEquityProfileRepository equityRepository;
    private final UserRepository userRepository;
    private final StudentProfileL2CacheService studentProfileL2CacheService;

    @Transactional(readOnly = true)
    public StudentEquityProfileDto getEquityProfileByStudentProfileId(Long studentProfileId) {
        StudentProfile sp = Optional.ofNullable(studentProfileL2CacheService.findByUserId(studentProfileId))
                .or(() -> Optional.ofNullable(studentProfileL2CacheService.findById(studentProfileId)))
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found: " + studentProfileId));
        StudentEquityProfile equity = equityRepository.findByStudentProfileId(sp.getId())
                .orElseGet(() -> buildDefaultProfileForStudent(sp));
        return mapToDto(equity);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "equityProfiles", key = "#userId")
    public StudentEquityProfileDto getEquityProfileForUser(Long userId) {
        StudentProfile studentProfile = Optional.ofNullable(studentProfileL2CacheService.findByUserId(userId))
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found for user ID: " + userId));
        return getEquityProfileByStudentProfileId(studentProfile.getId());
    }

    @Transactional
    @CacheEvict(value = "equityProfiles", allEntries = true)
    public StudentEquityProfileDto upsertEquityProfileForUser(Long userId, UpdateStudentEquityProfileRequest request) {
        StudentProfile studentProfile = Optional.ofNullable(studentProfileL2CacheService.findByUserId(userId))
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found for user ID: " + userId));

        StudentEquityProfile profile = equityRepository.findByStudentProfileId(studentProfile.getId())
                .orElse(StudentEquityProfile.builder()
                        .studentProfile(studentProfile)
                        .verificationStatus(EquityVerificationStatus.SELF_DECLARED)
                        .build());

        // 1. Person with Disability
        profile.setIsPersonWithDisability(request.getIsPersonWithDisability());
        profile.setPwdIdNumber(request.getPwdIdNumber());
        profile.setDisabilityType(request.getDisabilityType());

        // 2. Solo Parent Status (Explicit Separation)
        profile.setIsSoloParent(request.getIsSoloParent());
        profile.setIsRaisedBySoloParent(request.getIsRaisedBySoloParent());
        profile.setSoloParentIdNumber(request.getSoloParentIdNumber());

        // 3. 4Ps Beneficiary & UniFAST TES
        profile.setIs4psBeneficiary(request.getIs4psBeneficiary());
        profile.setHousehold4psIdNumber(request.getHousehold4psIdNumber());
        profile.setIsListahananNhts(request.getIsListahananNhts());
        profile.setUnifastTesAwardee(request.getUnifastTesAwardee());
        profile.setUnifastTesAwardNumber(request.getUnifastTesAwardNumber());

        // 4. Indigenous Peoples
        profile.setIsIndigenousPeople(request.getIsIndigenousPeople());
        profile.setIpEthnicGroup(request.getIpEthnicGroup());
        profile.setNcipCertificateNumber(request.getNcipCertificateNumber());

        // 5. Orphan Status
        profile.setIsOrphan(request.getIsOrphan());

        // 6. GIDA Resident
        profile.setIsGidaResident(request.getIsGidaResident());
        profile.setGidaBarangayResidence(request.getGidaBarangayResidence());

        // 7. Subsistence Farmer or Fisherfolk Family
        profile.setIsFarmerFisherfolk(request.getIsFarmerFisherfolk());
        profile.setRsbsaRegistrationNumber(request.getRsbsaRegistrationNumber());

        // 8. Rebel Returnees / E-CLIP
        profile.setIsRebelReturneeFamily(request.getIsRebelReturneeFamily());
        profile.setCertificateOfSurrenderNumber(request.getCertificateOfSurrenderNumber());

        // 9. Bottom 40% Household Income Bracket
        profile.setIsBottom40IncomeBracket(request.getIsBottom40IncomeBracket());
        profile.setMonthlyHouseholdIncomeBracket(request.getMonthlyHouseholdIncomeBracket());

        // 10. First Generation College Student
        profile.setIsFirstGenerationCollege(request.getIsFirstGenerationCollege());

        // Verification Status Transition:
        // If not already verified, set to PENDING_VERIFICATION if supporting IDs provided, else SELF_DECLARED
        if (profile.getVerificationStatus() != EquityVerificationStatus.VERIFIED) {
            boolean hasSupportingIds = (request.getHousehold4psIdNumber() != null && !request.getHousehold4psIdNumber().isBlank())
                    || (request.getNcipCertificateNumber() != null && !request.getNcipCertificateNumber().isBlank())
                    || (request.getPwdIdNumber() != null && !request.getPwdIdNumber().isBlank())
                    || (request.getSoloParentIdNumber() != null && !request.getSoloParentIdNumber().isBlank())
                    || (request.getUnifastTesAwardNumber() != null && !request.getUnifastTesAwardNumber().isBlank())
                    || (request.getRsbsaRegistrationNumber() != null && !request.getRsbsaRegistrationNumber().isBlank())
                    || (request.getCertificateOfSurrenderNumber() != null && !request.getCertificateOfSurrenderNumber().isBlank());
            profile.setVerificationStatus(hasSupportingIds ? EquityVerificationStatus.PENDING_VERIFICATION : EquityVerificationStatus.SELF_DECLARED);
        }

        StudentEquityProfile saved = equityRepository.save(profile);
        log.info("Successfully updated equity profile for studentProfileId: {}", studentProfile.getId());
        return mapToDto(saved);
    }

    @Transactional
    @CacheEvict(value = "equityProfiles", allEntries = true)
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
            Boolean isSoloParent,
            Boolean isFarmerFisherfolk,
            Boolean isBottom40,
            Pageable pageable) {

        Page<StudentEquityProfile> page = equityRepository.searchProfiles(
                search, status, is4ps, isIp, isPwd, isGida, isFirstGen, isSoloParent, isFarmerFisherfolk, isBottom40, pageable);
        return page.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public SliceResponse<StudentEquityProfileDto> searchEquityProfilesSlice(
            String search,
            EquityVerificationStatus status,
            Boolean is4ps,
            Boolean isIp,
            Boolean isPwd,
            Boolean isGida,
            Boolean isFirstGen,
            Boolean isSoloParent,
            Boolean isFarmerFisherfolk,
            Boolean isBottom40,
            Pageable pageable) {

        Slice<StudentEquityProfile> slice = equityRepository.searchProfilesSlice(
                search, status, is4ps, isIp, isPwd, isGida, isFirstGen, isSoloParent, isFarmerFisherfolk, isBottom40, pageable);
        Slice<StudentEquityProfileDto> dtoSlice = slice.map(this::mapToDto);
        return SliceResponse.from(dtoSlice);
    }

    @Transactional(readOnly = true)
    public EquityStatisticsSummaryDto getEquityStatisticsSummary() {
        long total = equityRepository.count();
        long countPwd = equityRepository.countByIsPersonWithDisabilityTrue();
        long countSolo = equityRepository.countByIsSoloParentTrue();
        long countRaisedBySolo = equityRepository.countByIsRaisedBySoloParentTrue();
        long count4ps = equityRepository.countByIs4psBeneficiaryTrue();
        long countNhts = equityRepository.countByIsListahananNhtsTrue();
        long countTes = equityRepository.countByUnifastTesAwardeeTrue();
        long countIp = equityRepository.countByIsIndigenousPeopleTrue();
        long countOrphan = equityRepository.countByIsOrphanTrue();
        long countGida = equityRepository.countByIsGidaResidentTrue();
        long countFarmer = equityRepository.countByIsFarmerFisherfolkTrue();
        long countRebel = equityRepository.countByIsRebelReturneeFamilyTrue();
        long countBottom40 = equityRepository.countByIsBottom40IncomeBracketTrue();
        long countFirstGen = equityRepository.countByIsFirstGenerationCollegeTrue();

        long countSelfDeclared = equityRepository.countByVerificationStatus(EquityVerificationStatus.SELF_DECLARED);
        long countPendingVerification = equityRepository.countByVerificationStatus(EquityVerificationStatus.PENDING_VERIFICATION);
        long countVerified = equityRepository.countByVerificationStatus(EquityVerificationStatus.VERIFIED);
        long countRejected = equityRepository.countByVerificationStatus(EquityVerificationStatus.REJECTED);

        return EquityStatisticsSummaryDto.builder()
                .totalProfilesCount(total)
                .countPersonsWithDisabilities(countPwd)
                .countSoloParents(countSolo)
                .countRaisedBySoloParents(countRaisedBySolo)
                .count4psBeneficiaries(count4ps)
                .countListahananNhts(countNhts)
                .countUnifastTesAwardees(countTes)
                .countIndigenousPeoples(countIp)
                .countOrphans(countOrphan)
                .countGidaResidents(countGida)
                .countFarmerFisherfolk(countFarmer)
                .countRebelReturneeFamilies(countRebel)
                .countBottom40IncomeBracket(countBottom40)
                .countFirstGenerationCollege(countFirstGen)
                .countSelfDeclared(countSelfDeclared)
                .countPendingVerification(countPendingVerification)
                .countVerified(countVerified)
                .countRejected(countRejected)
                .build();
    }

    private StudentEquityProfile buildDefaultProfileForStudent(StudentProfile sp) {
        return StudentEquityProfile.builder()
                .studentProfile(sp)
                .verificationStatus(EquityVerificationStatus.SELF_DECLARED)
                .isPersonWithDisability(false)
                .isSoloParent(false)
                .isRaisedBySoloParent(false)
                .is4psBeneficiary(false)
                .isListahananNhts(false)
                .unifastTesAwardee(false)
                .isIndigenousPeople(false)
                .isOrphan(false)
                .isGidaResident(false)
                .isFarmerFisherfolk(false)
                .isRebelReturneeFamily(false)
                .isBottom40IncomeBracket(false)
                .monthlyHouseholdIncomeBracket(HouseholdIncomeBracket.POOR_BELOW_10K)
                .isFirstGenerationCollege(false)
                .build();
    }

    private StudentEquityProfile createDefaultProfileForStudentId(Long studentProfileId) {
        StudentProfile sp = Optional.ofNullable(studentProfileL2CacheService.findByUserId(studentProfileId))
                .or(() -> Optional.ofNullable(studentProfileL2CacheService.findById(studentProfileId)))
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found: " + studentProfileId));
        return buildDefaultProfileForStudent(sp);
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
                .isPersonWithDisability(entity.getIsPersonWithDisability())
                .pwdIdNumber(entity.getPwdIdNumber())
                .disabilityType(entity.getDisabilityType())
                .isSoloParent(entity.getIsSoloParent())
                .isRaisedBySoloParent(entity.getIsRaisedBySoloParent())
                .soloParentIdNumber(entity.getSoloParentIdNumber())
                .is4psBeneficiary(entity.getIs4psBeneficiary())
                .household4psIdNumber(entity.getHousehold4psIdNumber())
                .isListahananNhts(entity.getIsListahananNhts())
                .unifastTesAwardee(entity.getUnifastTesAwardee())
                .unifastTesAwardNumber(entity.getUnifastTesAwardNumber())
                .isIndigenousPeople(entity.getIsIndigenousPeople())
                .ipEthnicGroup(entity.getIpEthnicGroup())
                .ncipCertificateNumber(entity.getNcipCertificateNumber())
                .isOrphan(entity.getIsOrphan())
                .isGidaResident(entity.getIsGidaResident())
                .gidaBarangayResidence(entity.getGidaBarangayResidence())
                .isFarmerFisherfolk(entity.getIsFarmerFisherfolk())
                .rsbsaRegistrationNumber(entity.getRsbsaRegistrationNumber())
                .isRebelReturneeFamily(entity.getIsRebelReturneeFamily())
                .certificateOfSurrenderNumber(entity.getCertificateOfSurrenderNumber())
                .isBottom40IncomeBracket(entity.getIsBottom40IncomeBracket())
                .monthlyHouseholdIncomeBracket(entity.getMonthlyHouseholdIncomeBracket())
                .isFirstGenerationCollege(entity.getIsFirstGenerationCollege())
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
