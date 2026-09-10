package com.sdt.web_app.specifications;

import com.sdt.web_app.entities.faculty.FacultyProfile;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;

public final class FacultyProfileSpecifications {

    private FacultyProfileSpecifications() {}

    public static Specification<FacultyProfile> inCollege(Long collegeId) {
        return (root, query, cb) -> {
            if (collegeId == null) return cb.conjunction();
            Join<Object, Object> user = root.join("user", JoinType.LEFT);
            return cb.or(
                    cb.equal(root.get("college").get("id"), collegeId),
                    cb.equal(user.get("college").get("id"), collegeId)
            );
        };
    }

    public static Specification<FacultyProfile> inProgram(Long programId) {
        return (root, query, cb) -> {
            if (programId == null) return cb.conjunction();
            Join<Object, Object> user = root.join("user", JoinType.LEFT);
            return cb.or(
                    cb.equal(root.get("program").get("id"), programId),
                    cb.equal(user.get("program").get("id"), programId)
            );
        };
    }

    public static Specification<FacultyProfile> inPrograms(Collection<Long> programIds) {
        return (root, query, cb) -> {
            if (programIds == null || programIds.isEmpty()) return cb.disjunction();
            Join<Object, Object> user = root.join("user", JoinType.LEFT);
            return cb.or(
                    root.get("program").get("id").in(programIds),
                    user.get("program").get("id").in(programIds)
            );
        };
    }
}
