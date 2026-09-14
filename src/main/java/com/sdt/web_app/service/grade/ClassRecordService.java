package com.sdt.web_app.service.grade;

import com.sdt.web_app.dto.grade.ClassRecordDtos.*;
import com.sdt.web_app.entities.enrollment.EnrollmentCourseItem;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.grade.ClassRecordItem;
import com.sdt.web_app.entities.grade.SectionGradingCategory;
import com.sdt.web_app.entities.grade.SectionGradingConfig;
import com.sdt.web_app.entities.grade.StudentAssessmentScore;
import com.sdt.web_app.entities.institution.GradingScale;
import com.sdt.web_app.entities.scheduling.ClassSection;
import com.sdt.web_app.repositories.enrollment.EnrollmentCourseItemRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.repositories.grade.ClassRecordItemRepository;
import com.sdt.web_app.repositories.grade.SectionGradingCategoryRepository;
import com.sdt.web_app.repositories.grade.SectionGradingConfigRepository;
import com.sdt.web_app.repositories.grade.StudentAssessmentScoreRepository;
import com.sdt.web_app.repositories.scheduling.ClassSectionRepository;
import com.sdt.web_app.service.institution.GradeTransmutationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassRecordService {

    private final ClassSectionRepository sectionRepository;
    private final SectionGradingConfigRepository configRepository;
    private final SectionGradingCategoryRepository categoryRepository;
    private final ClassRecordItemRepository itemRepository;
    private final StudentAssessmentScoreRepository scoreRepository;
    private final EnrollmentCourseItemRepository enrollmentItemRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final GradeTransmutationService transmutationService;

    @Transactional
    public SectionGradingConfigResponse getGradingConfig(Long sectionId) {
        ClassSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new EntityNotFoundException("Class section not found with ID: " + sectionId));

        SectionGradingConfig config = configRepository.findBySectionIdWithDetails(sectionId)
                .orElseGet(() -> createDefaultConfig(section));

        return mapToConfigResponse(config);
    }

    @Transactional
    public SectionGradingConfigResponse updateGradingConfig(Long sectionId, UpdateSectionGradingConfigRequest request, Long actorUserId) {
        ClassSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new EntityNotFoundException("Class section not found with ID: " + sectionId));

        assertSectionEditable(section);

        SectionGradingConfig config = configRepository.findBySectionIdWithDetails(sectionId)
                .orElseGet(() -> createDefaultConfig(section));

        // Validate weights
        if (request.midtermWeight().add(request.finalWeight()).compareTo(new BigDecimal("100.00")) != 0) {
            throw new IllegalArgumentException("Midterm weight (" + request.midtermWeight() + "%) and Final weight (" + request.finalWeight() + "%) must sum to 100.00%");
        }

        config.updateWeights(request.midtermWeight(), request.finalWeight());

        if (request.categories() != null && !request.categories().isEmpty()) {
            // Validate category weight sums per term period
            BigDecimal midtermCatSum = BigDecimal.ZERO;
            BigDecimal finalCatSum = BigDecimal.ZERO;

            for (CategoryWeightRequest catReq : request.categories()) {
                if ("MIDTERM".equalsIgnoreCase(catReq.termPeriod())) {
                    midtermCatSum = midtermCatSum.add(catReq.weightPercentage());
                } else if ("FINAL".equalsIgnoreCase(catReq.termPeriod())) {
                    finalCatSum = finalCatSum.add(catReq.weightPercentage());
                }
            }

            if (midtermCatSum.compareTo(new BigDecimal("100.00")) != 0) {
                throw new IllegalArgumentException("Midterm category weights must sum to 100.00% (Current: " + midtermCatSum + "%)");
            }
            if (finalCatSum.compareTo(new BigDecimal("100.00")) != 0) {
                throw new IllegalArgumentException("Final category weights must sum to 100.00% (Current: " + finalCatSum + "%)");
            }

            // Sync existing or new categories
            Map<Long, SectionGradingCategory> existingMap = config.getCategories().stream()
                    .collect(Collectors.toMap(SectionGradingCategory::getId, c -> c));

            List<SectionGradingCategory> updatedCategories = new ArrayList<>();

            for (CategoryWeightRequest catReq : request.categories()) {
                SectionGradingCategory.TermPeriod termPeriod = SectionGradingCategory.TermPeriod.valueOf(catReq.termPeriod().toUpperCase());
                if (catReq.id() != null && existingMap.containsKey(catReq.id())) {
                    SectionGradingCategory cat = existingMap.get(catReq.id());
                    cat.updateDetails(catReq.categoryName(), catReq.weightPercentage(), termPeriod, catReq.displayOrder());
                    updatedCategories.add(cat);
                } else {
                    SectionGradingCategory newCat = SectionGradingCategory.builder()
                            .config(config)
                            .categoryName(catReq.categoryName().trim())
                            .weightPercentage(catReq.weightPercentage())
                            .termPeriod(termPeriod)
                            .displayOrder(catReq.displayOrder())
                            .build();
                    updatedCategories.add(newCat);
                }
            }

            config.getCategories().clear();
            config.getCategories().addAll(updatedCategories);
        }

        SectionGradingConfig saved = configRepository.save(config);
        log.info("Updated section grading config for section ID: {} by user ID: {}", sectionId, actorUserId);
        return mapToConfigResponse(saved);
    }

    @Transactional
    public ClassRecordItemDto addAssessmentItem(Long sectionId, CreateClassRecordItemRequest request, Long actorUserId) {
        if (sectionId == null) {
            throw new IllegalArgumentException("Section ID cannot be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("Create assessment item request payload cannot be null");
        }
        if (request.itemTitle() == null || request.itemTitle().isBlank()) {
            throw new IllegalArgumentException("Assessment item title cannot be blank");
        }
        if (request.maxPoints() == null || request.maxPoints().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Assessment item max points must be greater than 0");
        }

        ClassSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new EntityNotFoundException("Class section not found with ID: " + sectionId));

        assertSectionEditable(section);

        SectionGradingCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new EntityNotFoundException("Grading category not found with ID: " + request.categoryId()));

        if (!category.getConfig().getSection().getId().equals(sectionId)) {
            throw new IllegalArgumentException("Grading category " + request.categoryId() + " does not belong to section " + sectionId);
        }

        ClassRecordItem item = ClassRecordItem.builder()
                .category(category)
                .itemTitle(request.itemTitle().trim())
                .maxPoints(request.maxPoints())
                .sequenceOrder(request.sequenceOrder())
                .build();

        ClassRecordItem saved = itemRepository.save(item);
        log.info("Added class record item '{}' ({}) to section {} by user {}", saved.getItemTitle(), saved.getMaxPoints(), sectionId, actorUserId);

        return new ClassRecordItemDto(saved.getId(), category.getId(), saved.getItemTitle(), saved.getMaxPoints(), saved.getSequenceOrder());
    }

    @Transactional
    public void deleteAssessmentItem(Long itemId, Long actorUserId) {
        if (itemId == null) {
            throw new IllegalArgumentException("Assessment item ID cannot be null");
        }
        ClassRecordItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Assessment item not found with ID: " + itemId));

        assertSectionEditable(item.getCategory().getConfig().getSection());

        Long sectionId = item.getCategory().getConfig().getSection().getId();
        scoreRepository.deleteByItemId(itemId);
        itemRepository.delete(item);
        log.info("Deleted assessment item ID: {} by user ID: {}", itemId, actorUserId);

        recalculateAndSyncSectionGrades(sectionId, actorUserId);
    }

    @Transactional
    public ClassRecordMatrixResponse getScoreMatrix(Long sectionId) {
        if (sectionId == null) {
            throw new IllegalArgumentException("Section ID cannot be null");
        }
        ClassSection section = sectionRepository.findByIdWithSchedules(sectionId)
                .orElseThrow(() -> new EntityNotFoundException("Class section not found with ID: " + sectionId));

        SectionGradingConfig config = configRepository.findBySectionIdWithDetails(sectionId)
                .orElseGet(() -> createDefaultConfig(section));

        List<EnrollmentCourseItem> enrollmentItems = enrollmentItemRepository.findBySectionIdWithStudentDetails(sectionId);
        List<ClassRecordItem> sectionItems = itemRepository.findBySectionId(sectionId);
        List<StudentAssessmentScore> allScores = scoreRepository.findBySectionId(sectionId);

        Map<String, StudentAssessmentScore> scoreMap = allScores.stream()
                .filter(s -> s != null && s.getItem() != null && s.getStudent() != null)
                .collect(Collectors.toMap(
                        s -> s.getItem().getId() + "_" + s.getStudent().getId(),
                        s -> s,
                        (s1, s2) -> s1
                ));

        List<StudentScoreMatrixRowDto> rows = new ArrayList<>();

        for (EnrollmentCourseItem item : enrollmentItems) {
            if (item == null || item.getCompletionStatus() == EnrollmentCourseItem.CompletionStatus.DROPPED) {
                continue;
            }
            if (item.getEnrollment() == null || item.getEnrollment().getStudent() == null) {
                continue;
            }

            StudentProfile sp = item.getEnrollment().getStudent();
            List<StudentScoreEntryDto> studentScores = new ArrayList<>();

            for (ClassRecordItem cri : sectionItems) {
                StudentAssessmentScore sas = scoreMap.get(cri.getId() + "_" + sp.getId());
                if (sas != null) {
                    studentScores.add(new StudentScoreEntryDto(cri.getId(), sp.getId(), sas.getScoreEarned(), sas.isExcused()));
                } else {
                    studentScores.add(new StudentScoreEntryDto(cri.getId(), sp.getId(), null, false));
                }
            }

            // Progressive weighted calculations
            BigDecimal midtermPct = calculateTermPercentage(config, sectionItems, studentScores, SectionGradingCategory.TermPeriod.MIDTERM);
            BigDecimal finalPct = calculateTermPercentage(config, sectionItems, studentScores, SectionGradingCategory.TermPeriod.FINAL);

            BigDecimal totalRawPct = BigDecimal.ZERO;
            BigDecimal assessedTermWeightSum = BigDecimal.ZERO;

            if (midtermPct != null) {
                totalRawPct = totalRawPct.add(midtermPct.multiply(config.getMidtermWeight()).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
                assessedTermWeightSum = assessedTermWeightSum.add(config.getMidtermWeight());
            }
            if (finalPct != null) {
                totalRawPct = totalRawPct.add(finalPct.multiply(config.getFinalWeight()).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
                assessedTermWeightSum = assessedTermWeightSum.add(config.getFinalWeight());
            }

            if (assessedTermWeightSum.compareTo(BigDecimal.ZERO) > 0) {
                totalRawPct = totalRawPct.divide(assessedTermWeightSum, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
            }

            BigDecimal transmutedGrade = null;
            String status = item.getCompletionStatus() != null ? item.getCompletionStatus().name() : "IN_PROGRESS";

            if (midtermPct != null || finalPct != null) {
                try {
                    GradingScale scale = transmutationService.transmutePercentage(totalRawPct);
                    if (scale != null) {
                        transmutedGrade = scale.getNumericGrade();
                        if (transmutedGrade != null) {
                            if (finalPct != null && section.getGradeStatus() != ClassSection.GradeStatus.DRAFT) {
                                status = transmutedGrade.compareTo(new BigDecimal("3.00")) <= 0 ? "PASSED" : "FAILED";
                            } else {
                                status = "IN_PROGRESS";
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("Transmutation not matching scale for raw percentage {}: {}", totalRawPct, e.getMessage());
                }
            }

            rows.add(new StudentScoreMatrixRowDto(
                    sp.getId(),
                    sp.getStudentNumber(),
                    sp.getUser() != null ? sp.getUser().getUsername() : "Student " + (sp.getStudentNumber() != null ? sp.getStudentNumber() : sp.getId()),
                    sp.getProgram() != null ? sp.getProgram().getCode() : "N/A",
                    sp.getYearLevel(),
                    studentScores,
                    midtermPct,
                    finalPct,
                    totalRawPct,
                    transmutedGrade,
                    status
            ));
        }

        return new ClassRecordMatrixResponse(
                section.getId(),
                section.getSectionCode(),
                section.getCourse() != null ? section.getCourse().getCode() : "N/A",
                section.getCourse() != null ? section.getCourse().getTitle() : "N/A",
                mapToConfigResponse(config),
                rows
        );
    }

    @Transactional
    public ClassRecordMatrixResponse batchSaveScores(Long sectionId, BatchSaveScoresRequest request, Long actorUserId) {
        if (sectionId == null) {
            throw new IllegalArgumentException("Section ID cannot be null");
        }
        if (request == null || request.scores() == null) {
            throw new IllegalArgumentException("Batch save scores payload or scores list cannot be null");
        }

        ClassSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new EntityNotFoundException("Class section not found with ID: " + sectionId));

        assertSectionEditable(section);

        List<StudentScoreEntryDto> validEntries = request.scores().stream()
                .filter(e -> e != null && e.itemId() != null && e.studentId() != null)
                .toList();

        if (validEntries.isEmpty()) {
            recalculateAndSyncSectionGrades(sectionId, actorUserId);
            return getScoreMatrix(sectionId);
        }

        Set<Long> itemIds = validEntries.stream().map(StudentScoreEntryDto::itemId).collect(Collectors.toSet());
        Set<Long> studentIds = validEntries.stream().map(StudentScoreEntryDto::studentId).collect(Collectors.toSet());

        Map<Long, ClassRecordItem> itemMap = itemRepository.findAllById(itemIds).stream()
                .collect(Collectors.toMap(ClassRecordItem::getId, Function.identity()));

        Map<Long, StudentProfile> studentMap = studentProfileRepository.findAllById(studentIds).stream()
                .collect(Collectors.toMap(StudentProfile::getId, Function.identity()));

        Map<String, StudentAssessmentScore> existingScores = scoreRepository.findBySectionId(sectionId).stream()
                .filter(s -> s != null && s.getItem() != null && s.getStudent() != null)
                .collect(Collectors.toMap(
                        s -> s.getItem().getId() + "_" + s.getStudent().getId(),
                        Function.identity(),
                        (s1, s2) -> s1
                ));

        Map<String, StudentAssessmentScore> toSaveMap = new LinkedHashMap<>();

        for (StudentScoreEntryDto entry : validEntries) {
            ClassRecordItem item = itemMap.get(entry.itemId());
            if (item == null) {
                throw new EntityNotFoundException("Assessment item not found with ID: " + entry.itemId());
            }

            if (entry.scoreEarned() != null && !entry.isExcused()) {
                if (entry.scoreEarned().compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("Score earned (" + entry.scoreEarned() + ") for item '" + item.getItemTitle() + "' cannot be negative.");
                }
                if (entry.scoreEarned().compareTo(item.getMaxPoints()) > 0) {
                    throw new IllegalArgumentException("Score earned (" + entry.scoreEarned() + ") for item '" + item.getItemTitle() + "' exceeds activity maximum points (" + item.getMaxPoints() + ").");
                }
            }

            StudentProfile student = studentMap.get(entry.studentId());
            if (student == null) {
                throw new EntityNotFoundException("Student not found with ID: " + entry.studentId());
            }

            String key = entry.itemId() + "_" + entry.studentId();
            StudentAssessmentScore score = toSaveMap.get(key);
            if (score == null) {
                score = existingScores.get(key);
            }
            if (score == null) {
                score = StudentAssessmentScore.builder()
                        .item(item)
                        .student(student)
                        .build();
            }

            score.updateScore(entry.scoreEarned(), entry.isExcused());
            toSaveMap.put(key, score);
        }

        scoreRepository.saveAll(toSaveMap.values());
        log.info("Saved batch raw scores for section {} by user {}", sectionId, actorUserId);

        // Recalculate raw averages and push to EnrollmentCourseItem
        recalculateAndSyncSectionGrades(sectionId, actorUserId);

        return getScoreMatrix(sectionId);
    }

    @Transactional
    public void recalculateAndSyncSectionGrades(Long sectionId, Long actorUserId) {
        ClassSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new EntityNotFoundException("Class section not found with ID: " + sectionId));

        assertSectionEditable(section);

        ClassRecordMatrixResponse matrix = getScoreMatrix(sectionId);

        List<EnrollmentCourseItem> enrollmentItems = enrollmentItemRepository.findBySectionIdWithStudentDetails(sectionId);
        Map<Long, EnrollmentCourseItem> itemMap = enrollmentItems.stream()
                .collect(Collectors.toMap(i -> i.getEnrollment().getStudent().getId(), i -> i));

        List<EnrollmentCourseItem> itemsToUpdate = new ArrayList<>();
        for (StudentScoreMatrixRowDto row : matrix.rows()) {
            EnrollmentCourseItem item = itemMap.get(row.studentId());
            if (item != null && row.transmutedGrade() != null) {
                EnrollmentCourseItem.CompletionStatus status;
                if (row.transmutedGrade().compareTo(new BigDecimal("3.00")) <= 0) {
                    status = EnrollmentCourseItem.CompletionStatus.PASSED;
                } else if (row.transmutedGrade().compareTo(new BigDecimal("4.00")) == 0) {
                    status = EnrollmentCourseItem.CompletionStatus.INCOMPLETE;
                } else {
                    status = EnrollmentCourseItem.CompletionStatus.FAILED;
                }

                item.updateGrade(row.transmutedGrade(), status);
                itemsToUpdate.add(item);
            }
        }
        if (!itemsToUpdate.isEmpty()) {
            enrollmentItemRepository.saveAll(itemsToUpdate);
        }

        log.info("Synced transmuted grades to EnrollmentCourseItems for section {} by user {}", sectionId, actorUserId);
    }

    private BigDecimal calculateTermPercentage(
            SectionGradingConfig config,
            List<ClassRecordItem> sectionItems,
            List<StudentScoreEntryDto> studentScores,
            SectionGradingCategory.TermPeriod termPeriod
    ) {
        List<SectionGradingCategory> termCategories = config.getCategories().stream()
                .filter(c -> c.getTermPeriod() == termPeriod)
                .toList();

        if (termCategories.isEmpty()) return null;

        Map<Long, StudentScoreEntryDto> scoreMap = studentScores.stream()
                .filter(s -> s != null && s.itemId() != null)
                .collect(Collectors.toMap(
                        StudentScoreEntryDto::itemId,
                        s -> s,
                        (s1, s2) -> s1
                ));

        BigDecimal termWeightedSum = BigDecimal.ZERO;
        BigDecimal assessedCategoryWeightSum = BigDecimal.ZERO;
        boolean hasScoresInTerm = false;

        for (SectionGradingCategory cat : termCategories) {
            List<ClassRecordItem> catItems = sectionItems.stream()
                    .filter(i -> i != null && i.getCategory() != null && i.getCategory().getId() != null && i.getCategory().getId().equals(cat.getId()))
                    .toList();

            BigDecimal totalMax = BigDecimal.ZERO;
            BigDecimal totalEarned = BigDecimal.ZERO;

            for (ClassRecordItem item : catItems) {
                StudentScoreEntryDto scoreDto = scoreMap.get(item.getId());
                if (scoreDto != null && scoreDto.scoreEarned() != null && !scoreDto.isExcused()) {
                    totalEarned = totalEarned.add(scoreDto.scoreEarned());
                    totalMax = totalMax.add(item.getMaxPoints());
                    hasScoresInTerm = true;
                }
            }

            if (totalMax.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal catPct = totalEarned.divide(totalMax, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
                termWeightedSum = termWeightedSum.add(catPct.multiply(cat.getWeightPercentage()).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
                assessedCategoryWeightSum = assessedCategoryWeightSum.add(cat.getWeightPercentage());
            }
        }

        if (!hasScoresInTerm || assessedCategoryWeightSum.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        BigDecimal normalizedTermPct = termWeightedSum
                .divide(assessedCategoryWeightSum, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        return normalizedTermPct.setScale(2, RoundingMode.HALF_UP);
    }

    private SectionGradingConfig createDefaultConfig(ClassSection section) {
        SectionGradingConfig config = SectionGradingConfig.builder()
                .section(section)
                .midtermWeight(new BigDecimal("50.00"))
                .finalWeight(new BigDecimal("50.00"))
                .categories(new ArrayList<>())
                .build();

        List<SectionGradingCategory> defaults = List.of(
                SectionGradingCategory.builder().config(config).categoryName("Quizzes").weightPercentage(new BigDecimal("20.00")).termPeriod(SectionGradingCategory.TermPeriod.MIDTERM).displayOrder(1).build(),
                SectionGradingCategory.builder().config(config).categoryName("Seatwork").weightPercentage(new BigDecimal("15.00")).termPeriod(SectionGradingCategory.TermPeriod.MIDTERM).displayOrder(2).build(),
                SectionGradingCategory.builder().config(config).categoryName("Assignments").weightPercentage(new BigDecimal("15.00")).termPeriod(SectionGradingCategory.TermPeriod.MIDTERM).displayOrder(3).build(),
                SectionGradingCategory.builder().config(config).categoryName("Major Exam").weightPercentage(new BigDecimal("50.00")).termPeriod(SectionGradingCategory.TermPeriod.MIDTERM).displayOrder(4).build(),
                SectionGradingCategory.builder().config(config).categoryName("Quizzes").weightPercentage(new BigDecimal("20.00")).termPeriod(SectionGradingCategory.TermPeriod.FINAL).displayOrder(1).build(),
                SectionGradingCategory.builder().config(config).categoryName("Seatwork").weightPercentage(new BigDecimal("15.00")).termPeriod(SectionGradingCategory.TermPeriod.FINAL).displayOrder(2).build(),
                SectionGradingCategory.builder().config(config).categoryName("Assignments").weightPercentage(new BigDecimal("15.00")).termPeriod(SectionGradingCategory.TermPeriod.FINAL).displayOrder(3).build(),
                SectionGradingCategory.builder().config(config).categoryName("Major Exam").weightPercentage(new BigDecimal("50.00")).termPeriod(SectionGradingCategory.TermPeriod.FINAL).displayOrder(4).build()
        );

        config.getCategories().addAll(defaults);
        return configRepository.save(config);
    }

    private SectionGradingConfigResponse mapToConfigResponse(SectionGradingConfig config) {
        List<SectionGradingCategoryDto> catDtos = (config.getCategories() != null ? config.getCategories() : Collections.<SectionGradingCategory>emptyList()).stream()
                .sorted(Comparator.comparingInt(SectionGradingCategory::getDisplayOrder)
                        .thenComparing(c -> c.getId() != null ? c.getId() : 0L))
                .map(cat -> new SectionGradingCategoryDto(
                        cat.getId(),
                        cat.getCategoryName(),
                        cat.getWeightPercentage(),
                        cat.getTermPeriod().name(),
                        cat.getDisplayOrder(),
                        (cat.getItems() != null ? cat.getItems() : Collections.<ClassRecordItem>emptyList()).stream()
                                .sorted(Comparator.comparingInt(ClassRecordItem::getSequenceOrder)
                                        .thenComparing(i -> i.getId() != null ? i.getId() : 0L))
                                .map(item -> new ClassRecordItemDto(item.getId(), cat.getId(), item.getItemTitle(), item.getMaxPoints(), item.getSequenceOrder()))
                                .toList()
                ))
                .toList();

        return new SectionGradingConfigResponse(
                config.getId(),
                config.getSection().getId(),
                config.getMidtermWeight(),
                config.getFinalWeight(),
                config.isLocked(),
                catDtos
        );
    }

    private void assertSectionEditable(ClassSection section) {
        if (section.getGradeStatus() == ClassSection.GradeStatus.SUBMITTED ||
            section.getGradeStatus() == ClassSection.GradeStatus.VERIFIED ||
            section.getGradeStatus() == ClassSection.GradeStatus.SEALED) {
            throw new IllegalStateException("Class record modifications blocked: Section " + section.getSectionCode() + " is in " + section.getGradeStatus() + " status.");
        }
    }
}
