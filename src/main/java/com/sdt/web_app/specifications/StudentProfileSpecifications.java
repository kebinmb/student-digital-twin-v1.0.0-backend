package com.sdt.web_app.specifications;

import com.sdt.web_app.entities.enrollment.EnrollmentCourseItem;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;

public final class StudentProfileSpecifications {

    private StudentProfileSpecifications() {}

    public static Specification<StudentProfile> inCollege(Long collegeId) {
        return (root, query, cb) -> {
            if (collegeId == null) return cb.conjunction();
            Join<Object, Object> program = root.join("program", JoinType.INNER);
            Join<Object, Object> department = program.join("department", JoinType.INNER);
            return cb.or(
                    cb.equal(program.get("college").get("id"), collegeId),
                    cb.equal(department.get("id"), collegeId),
                    cb.equal(department.get("parentDepartment").get("id"), collegeId)
            );
        };
    }

    public static Specification<StudentProfile> inProgram(Long programId) {
        return (root, query, cb) -> programId == null ? cb.conjunction() : cb.equal(root.get("program").get("id"), programId);
    }

    public static Specification<StudentProfile> inPrograms(Collection<Long> programIds) {
        return (root, query, cb) -> {
            if (programIds == null || programIds.isEmpty()) return cb.disjunction();
            return root.get("program").get("id").in(programIds);
        };
    }

    public static Specification<StudentProfile> enrolledInSections(Collection<Long> sectionIds) {
        return (root, query, cb) -> {
            if (sectionIds == null || sectionIds.isEmpty()) return cb.disjunction();
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<EnrollmentCourseItem> itemRoot = subquery.from(EnrollmentCourseItem.class);
            subquery.select(itemRoot.get("enrollment").get("student").get("id"))
                    .where(itemRoot.get("section").get("id").in(sectionIds));
            return root.get("id").in(subquery);
        };
    }

    public static Specification<StudentProfile> searchKeyword(String queryStr) {
        return (root, query, cb) -> {
            if (queryStr == null || queryStr.trim().isEmpty()) return cb.conjunction();
            String pattern = "%" + queryStr.trim().toLowerCase() + "%";
            Join<Object, Object> user = root.join("user", JoinType.LEFT);
            Join<Object, Object> program = root.join("program", JoinType.LEFT);
            return cb.or(
                    cb.like(cb.lower(root.get("studentNumber")), pattern),
                    cb.like(cb.lower(user.get("username")), pattern),
                    cb.like(cb.lower(root.get("firstName")), pattern),
                    cb.like(cb.lower(root.get("lastName")), pattern),
                    cb.like(cb.lower(program.get("code")), pattern)
            );
        };
    }
}
