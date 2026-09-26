package com.sdt.web_app.service.institution;

import com.sdt.web_app.dto.institution.CurriculumDesignerDtos;
import com.sdt.web_app.entities.institution.*;
import com.sdt.web_app.repositories.institution.CoursePrerequisiteRepository;
import com.sdt.web_app.repositories.institution.CurriculumCourseRepository;
import com.sdt.web_app.repositories.institution.CurriculumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CurriculumValidationService {
    private final CurriculumRepository curriculumRepository;
    private final CurriculumCourseRepository curriculumCourseRepository;
    private final CoursePrerequisiteRepository coursePrerequisiteRepository;


    public CurriculumDesignerDtos.ValidationReportDto validateCurriculum(Long curriculumId) {
        Curriculum curriculum = curriculumRepository.findById(curriculumId)
                .orElseThrow(() -> new IllegalArgumentException("Curriculum not found with ID: " + curriculumId));

        Program program = curriculum.getProgram();
        List<CurriculumCourse> curriculumCourses = curriculumCourseRepository.findByCurriculumId(curriculumId);

        List<CurriculumDesignerDtos.DiagnosticMessage> errors = new ArrayList<>();
        List<CurriculumDesignerDtos.DiagnosticMessage> warnings = new ArrayList<>();

        // 1. Total Unit Verification
        BigDecimal totalUnits = curriculumCourses.stream()
                .map(cc -> cc.getCourse().getCreditUnits())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal unitDeficit = BigDecimal.valueOf(program.getTotalUnitsRequired()).subtract(totalUnits);
        if (unitDeficit.compareTo(BigDecimal.ZERO) != 0) {
            errors.add(new CurriculumDesignerDtos.DiagnosticMessage(
                    "UNIT_REQUIREMENT_MISMATCH",
                    "ERROR",
                    String.format("Curriculum units (%s) do not match Program '%s' requirement (%d units).",
                            totalUnits, program.getCode(), program.getTotalUnitsRequired()),
                    null, null, null
            ));
        }
        // 2. CHED Contact Hour Rules & Semester Load Audits
        Map<String, List<CurriculumCourse>> groupedByTerm = curriculumCourses.stream()
                .collect(Collectors.groupingBy(cc -> cc.getYearLevel() + "_" + cc.getSemester()));

        for (Map.Entry<String, List<CurriculumCourse>> entry : groupedByTerm.entrySet()) {
            List<CurriculumCourse> termCourses = entry.getValue();
            int year = termCourses.getFirst().getYearLevel();
            String sem = termCourses.getFirst().getSemester();

            BigDecimal termUnits = termCourses.stream()
                    .map(cc -> cc.getCourse().getCreditUnits())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int termHours = termCourses.stream()
                    .mapToInt(cc -> cc.getCourse().getContactHoursLec() + cc.getCourse().getContactHoursLab())
                    .sum();

            if (termUnits.compareTo(new BigDecimal("24.00")) > 0) {
                warnings.add(new CurriculumDesignerDtos.DiagnosticMessage(
                        "SEMESTER_UNIT_OVERLOAD",
                        "WARNING",
                        String.format("Year %d %s total units (%s) exceeds recommended max limit of 24.0 units.", year, sem, termUnits),
                        null, year, sem
                ));
            }

            if (termHours > 30) {
                warnings.add(new CurriculumDesignerDtos.DiagnosticMessage(
                        "CONTACT_HOUR_OVERLOAD",
                        "WARNING",
                        String.format("Year %d %s contact hours (%d hrs/wk) exceeds CHED threshold (30 hrs/wk).", year, sem, termHours),
                        null, year, sem
                ));
            }

            for (CurriculumCourse cc : termCourses) {
                Course c = cc.getCourse();
                int expLec = c.getLectureUnits().intValue();
                int expLab = c.getLabUnits().intValue() * 3;
                if (c.getContactHoursLec() != expLec || c.getContactHoursLab() != expLab) {
                    warnings.add(new CurriculumDesignerDtos.DiagnosticMessage(
                            "CHED_CONTACT_HOUR_CONVERSION_ANOMALY",
                            "WARNING",
                            String.format("Course %s lecture/lab hours do not follow the standard 1:1 and 1:3 CHED ratios.", c.getCode()),
                            c.getCode(), year, sem
                    ));
                }
            }
        }
        // 3. Prerequisite Graph Cycle Detection (DFS)
        detectCycles(curriculumCourses, errors);

        boolean isValid = errors.isEmpty();
        CurriculumDesignerDtos.ValidationSummary summary = new CurriculumDesignerDtos.ValidationSummary(totalUnits, program.getTotalUnitsRequired(), unitDeficit);
        return new CurriculumDesignerDtos.ValidationReportDto(isValid, summary, errors, warnings);
    }

    private void detectCycles(List<CurriculumCourse> curriculumCourses, List<CurriculumDesignerDtos.DiagnosticMessage> errors) {
        Map<Long, Course> courseMap = curriculumCourses.stream()
                .map(CurriculumCourse::getCourse)
                .collect(Collectors.toMap(Course::getId, c -> c, (a, b) -> a));

        Map<Long, List<Course>> adjList = new HashMap<>();
        if (!courseMap.isEmpty()) {
            List<CoursePrerequisite> allPrereqs = coursePrerequisiteRepository.findPrerequisitesForCourseIds(courseMap.keySet());
            for (CoursePrerequisite cp : allPrereqs) {
                if ("HARD".equalsIgnoreCase(cp.getRuleType())) {
                    adjList.computeIfAbsent(cp.getCourse().getId(), k -> new ArrayList<>())
                            .add(cp.getPrerequisiteCourse());
                }
            }
        }

        Map<Long, Integer> state = new HashMap<>(); // 0: White (unvisited), 1: Grey (visiting), 2: Black (visited)
        Deque<String> trace = new ArrayDeque<>();

        for (Long cId : courseMap.keySet()) {
            if (state.getOrDefault(cId, 0) == 0) {
                dfs(cId, adjList, courseMap, state, trace, errors);
            }
        }
    }

    private void dfs(Long curr, Map<Long, List<Course>> adj, Map<Long, Course> map,
                     Map<Long, Integer> state, Deque<String> trace, List<CurriculumDesignerDtos.DiagnosticMessage> errors) {
        state.put(curr, 1);
        trace.push(map.get(curr).getCode());

        for (Course neighbor : adj.getOrDefault(curr, Collections.emptyList())) {
            int nState = state.getOrDefault(neighbor.getId(), 0);
            if (nState == 1) {
                List<String> path = new ArrayList<>(trace);
                Collections.reverse(path);
                path.add(neighbor.getCode());
                errors.add(new CurriculumDesignerDtos.DiagnosticMessage(
                        "CIRCULAR_DEPENDENCY_DETECTED",
                        "ERROR",
                        "Circular prerequisite dependency detected: " + String.join(" -> ", path),
                        map.get(curr).getCode(), null, null
                ));
            } else if (nState == 0 && map.containsKey(neighbor.getId())) {
                dfs(neighbor.getId(), adj, map, state, trace, errors);
            }
        }
        state.put(curr, 2);
        trace.pop();
    }
}
