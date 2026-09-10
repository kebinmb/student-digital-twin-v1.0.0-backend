package com.sdt.web_app.specifications;

import com.sdt.web_app.entities.institution.Program;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;

public final class ProgramSpecifications {

    private ProgramSpecifications() {}

    public static Specification<Program> hasCollegeId(Long collegeId) {
        return (root, query, cb) -> {
            if (collegeId == null) return cb.conjunction();
            return cb.or(
                    cb.equal(root.get("college").get("id"), collegeId),
                    cb.equal(root.get("department").get("id"), collegeId),
                    cb.equal(root.get("department").get("parentDepartment").get("id"), collegeId)
            );
        };
    }

    public static Specification<Program> hasProgramId(Long programId) {
        return (root, query, cb) -> programId == null ? cb.conjunction() : cb.equal(root.get("id"), programId);
    }

    public static Specification<Program> hasProgramIds(Collection<Long> programIds) {
        return (root, query, cb) -> {
            if (programIds == null || programIds.isEmpty()) return cb.disjunction();
            return root.get("id").in(programIds);
        };
    }

    public static Specification<Program> isActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null ? cb.conjunction() : cb.equal(root.get("isActive"), isActive);
    }

    public static Specification<Program> hasDepartmentId(Long departmentId) {
        return (root, query, cb) -> departmentId == null ? cb.conjunction() : cb.equal(root.get("department").get("id"), departmentId);
    }
}
