package com.sdt.web_app.specifications;

import com.sdt.web_app.entities.scheduling.ClassSchedule;
import com.sdt.web_app.entities.scheduling.ClassSection;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;

public final class ClassSectionSpecifications {

    private ClassSectionSpecifications() {}

    public static Specification<ClassSection> inTerm(Long termId) {
        return (root, query, cb) -> termId == null ? cb.conjunction() : cb.equal(root.get("term").get("id"), termId);
    }

    public static Specification<ClassSection> inCollege(Long collegeId) {
        return (root, query, cb) -> {
            if (collegeId == null) return cb.conjunction();
            Join<Object, Object> curriculum = root.join("curriculum", JoinType.INNER);
            Join<Object, Object> program = curriculum.join("program", JoinType.INNER);
            Join<Object, Object> department = program.join("department", JoinType.INNER);
            return cb.or(
                    cb.equal(program.get("college").get("id"), collegeId),
                    cb.equal(department.get("id"), collegeId),
                    cb.equal(department.get("parentDepartment").get("id"), collegeId)
            );
        };
    }

    public static Specification<ClassSection> inProgram(Long programId) {
        return (root, query, cb) -> {
            if (programId == null) return cb.conjunction();
            Join<Object, Object> curriculum = root.join("curriculum", JoinType.INNER);
            return cb.equal(curriculum.get("program").get("id"), programId);
        };
    }

    public static Specification<ClassSection> inPrograms(Collection<Long> programIds) {
        return (root, query, cb) -> {
            if (programIds == null || programIds.isEmpty()) return cb.disjunction();
            Join<Object, Object> curriculum = root.join("curriculum", JoinType.INNER);
            return curriculum.get("program").get("id").in(programIds);
        };
    }

    public static Specification<ClassSection> assignedToInstructor(Long instructorId) {
        return (root, query, cb) -> {
            if (instructorId == null) return cb.conjunction();
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<ClassSchedule> schedRoot = subquery.from(ClassSchedule.class);
            subquery.select(schedRoot.get("section").get("id"))
                    .where(cb.equal(schedRoot.get("instructor").get("id"), instructorId));

            return cb.or(
                    cb.equal(root.get("primaryInstructor").get("id"), instructorId),
                    root.get("id").in(subquery)
            );
        };
    }

    public static Specification<ClassSection> withSectionIds(Collection<Long> sectionIds) {
        return (root, query, cb) -> {
            if (sectionIds == null || sectionIds.isEmpty()) return cb.disjunction();
            return root.get("id").in(sectionIds);
        };
    }
}
