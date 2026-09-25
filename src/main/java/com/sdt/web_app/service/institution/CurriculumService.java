package com.sdt.web_app.service.institution;

import com.sdt.web_app.dto.institution.CurriculumCourseDtos.*;
import com.sdt.web_app.dto.institution.CurriculumDtos.*;
import com.sdt.web_app.entities.institution.Course;
import com.sdt.web_app.entities.institution.Curriculum;
import com.sdt.web_app.entities.institution.CurriculumCourse;
import com.sdt.web_app.entities.institution.Program;
import com.sdt.web_app.entities.institution.Major;
import com.sdt.web_app.repositories.institution.CourseRepository;
import com.sdt.web_app.repositories.institution.CurriculumCourseRepository;
import com.sdt.web_app.repositories.institution.CurriculumRepository;
import com.sdt.web_app.repositories.institution.MajorRepository;
import com.sdt.web_app.repositories.institution.ProgramRepository;
import com.sdt.web_app.config.CacheConfig;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CurriculumService {

    private final CurriculumRepository curriculumRepository;
    private final CurriculumCourseRepository curriculumCourseRepository;
    private final ProgramRepository programRepository;
    private final CourseRepository courseRepository;
    private final MajorRepository majorRepository;

    @CacheEvict(value = CacheConfig.CACHE_CURRICULA_BY_PROGRAM, allEntries = true)
    public CurriculumResponse createCurriculum(CreateCurriculumRequest request) {
        Program program = programRepository.findById(request.programId())
                .orElseThrow(() -> new IllegalArgumentException("Program not found with ID: " + request.programId()));

        if (curriculumRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Curriculum with code already exists: " + request.code());
        }

        Major major = null;
        if (request.majorId() != null) {
            major = majorRepository.findById(request.majorId())
                    .orElseThrow(() -> new IllegalArgumentException("Major not found with ID: " + request.majorId()));
            if (!major.getProgram().getId().equals(program.getId())) {
                throw new IllegalArgumentException("Major does not belong to program ID: " + program.getId());
            }
        }

        Curriculum curriculum = Curriculum.builder()
                .program(program)
                .major(major)
                .code(request.code().trim().toUpperCase())
                .name(request.name().trim())
                .effectiveAcademicYear(request.effectiveAcademicYear().trim())
                .status(Curriculum.Status.DRAFT)
                .versionNumber(1)
                .isActive(true)
                .build();

        Curriculum saved = curriculumRepository.save(curriculum);
        return mapToResponse(saved);
    }

    @CacheEvict(value = CacheConfig.CACHE_CURRICULA_BY_PROGRAM, allEntries = true)
    public CurriculumResponse updateCurriculum(Long id, UpdateCurriculumRequest request) {
        Curriculum curriculum = findCurriculumById(id);
        curriculum.updateDetails(request.name(), request.effectiveAcademicYear());
        return mapToResponse(curriculum);
    }

    @CacheEvict(value = CacheConfig.CACHE_CURRICULA_BY_PROGRAM, allEntries = true)
    public CurriculumCourseResponse assignCourseToCurriculum(Long curriculumId, AssignCourseToCurriculumRequest request) {
        Curriculum curriculum = findCurriculumById(curriculumId);
        if (!curriculum.isEditable()) {
            throw new IllegalStateException("Curriculum is locked under status: " + curriculum.getStatus());
        }

        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found with ID: " + request.courseId()));

        if (curriculumCourseRepository.existsByCurriculumIdAndCourseId(curriculumId, request.courseId())) {
            throw new IllegalArgumentException("Course is already assigned to this curriculum: " + course.getCode());
        }

        int seqOrder;
        if (request.sequenceOrder() != null && request.sequenceOrder() > 0) {
            seqOrder = request.sequenceOrder();
        } else {
            int maxSeq = curriculumCourseRepository
                    .findByCurriculumIdAndYearLevelAndSemester(curriculumId, request.yearLevel(), request.semester())
                    .stream()
                    .mapToInt(CurriculumCourse::getSequenceOrder)
                    .max()
                    .orElse(0);
            seqOrder = maxSeq + 1;
        }

        String category = (request.category() != null && !request.category().isBlank())
                ? request.category()
                : "PROFESSIONAL_MAJOR";

        CurriculumCourse curriculumCourse = CurriculumCourse.builder()
                .curriculum(curriculum)
                .course(course)
                .creditUnits(course.getCreditUnits())
                .yearLevel(request.yearLevel())
                .semester(request.semester())
                .sequenceOrder(seqOrder)
                .category(category)
                .build();

        CurriculumCourse saved = curriculumCourseRepository.save(curriculumCourse);
        return mapToCourseResponse(saved);
    }

    @CacheEvict(value = CacheConfig.CACHE_CURRICULA_BY_PROGRAM, allEntries = true)
    public void removeCourseFromCurriculum(Long curriculumId, Long curriculumCourseId) {
        Curriculum curriculum = findCurriculumById(curriculumId);
        if (!curriculum.isEditable()) {
            throw new IllegalStateException("Curriculum is locked under status: " + curriculum.getStatus());
        }

        CurriculumCourse curriculumCourse = curriculumCourseRepository.findById(curriculumCourseId)
                .orElseThrow(() -> new EntityNotFoundException("CurriculumCourse not found with ID: " + curriculumCourseId));

        if (!curriculumCourse.getCurriculum().getId().equals(curriculumId)) {
            throw new IllegalArgumentException("Course mapping does not belong to curriculum ID: " + curriculumId);
        }

        curriculumCourseRepository.delete(curriculumCourse);
    }

    @Transactional(readOnly = true)
    public CurriculumResponse getCurriculumById(Long id) {
        return mapToResponse(findCurriculumById(id));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_CURRICULA_BY_PROGRAM, key = "#programId")
    public List<CurriculumResponse> getCurriculaByProgramId(Long programId) {
        if (!programRepository.existsById(programId)) {
            throw new IllegalArgumentException("Program not found with ID: " + programId);
        }
        return curriculumRepository.findByProgramId(programId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CurriculumCourseResponse> getCurriculumCourses(Long curriculumId) {
        if (!curriculumRepository.existsById(curriculumId)) {
            throw new EntityNotFoundException("Curriculum not found with ID: " + curriculumId);
        }
        return curriculumCourseRepository.findByCurriculumId(curriculumId).stream()
                .map(this::mapToCourseResponse)
                .toList();
    }

    @CacheEvict(value = CacheConfig.CACHE_CURRICULA_BY_PROGRAM, allEntries = true)
    public CurriculumResponse transitionStatus(Long id, Curriculum.Status targetStatus) {
        Curriculum curriculum = findCurriculumById(id);
        curriculum.transitionTo(targetStatus);
        return mapToResponse(curriculum);
    }

    @CacheEvict(value = CacheConfig.CACHE_CURRICULA_BY_PROGRAM, allEntries = true)
    public void deleteCurriculum(Long id) {
        Curriculum curriculum = findCurriculumById(id);
        if (curriculum.getStatus() == Curriculum.Status.ACTIVE) {
            curriculum.transitionTo(Curriculum.Status.ARCHIVED);
        } else {
            curriculumRepository.delete(curriculum);
        }
    }

    @Transactional(readOnly = true)
    public List<CurriculumResponse> getCurriculaByProgramAndMajor(Long programId, Long majorId) {
        if (!programRepository.existsById(programId)) {
            throw new IllegalArgumentException("Program not found with ID: " + programId);
        }
        List<Curriculum> list;
        if (majorId != null) {
            list = curriculumRepository.findByProgramIdAndMajorIdAndIsActiveTrueOrderByCodeAsc(programId, majorId);
        } else {
            list = curriculumRepository.findByProgramIdAndIsActiveTrueOrderByCodeAsc(programId);
        }
        return list.stream().map(this::mapToResponse).toList();
    }

    private Curriculum findCurriculumById(Long id) {
        return curriculumRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Curriculum not found with ID: " + id));
    }

    private CurriculumResponse mapToResponse(Curriculum c) {
        Long majorId = c.getMajor() != null ? c.getMajor().getId() : null;
        String majorName = c.getMajor() != null ? c.getMajor().getName() : null;
        return new CurriculumResponse(
                c.getId(),
                c.getProgram().getId(),
                c.getProgram().getCode(),
                c.getProgram().getName(),
                majorId,
                majorName,
                c.getCode(),
                c.getName(),
                c.getEffectiveAcademicYear(),
                c.getStatus().name(),
                c.getVersionNumber(),
                c.isActive()
        );
    }

    private CurriculumCourseResponse mapToCourseResponse(CurriculumCourse cc) {
        return new CurriculumCourseResponse(
                cc.getId(),
                cc.getCurriculum().getId(),
                cc.getCurriculum().getCode(),
                cc.getCourse().getId(),
                cc.getCourse().getCode(),
                cc.getCourse().getTitle(),
                cc.getCourse().getCreditUnits(),
                cc.getYearLevel(),
                cc.getSemester(),
                cc.getSequenceOrder(),
                cc.getCategory()
        );
    }
}
