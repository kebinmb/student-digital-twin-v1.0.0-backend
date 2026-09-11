package com.sdt.web_app.repositories.authentication;

import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.authentication.Roles;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"roles", "college", "program"})
    @Override
    Optional<User> findById(Long id);

    @EntityGraph(attributePaths = {"roles", "college", "program"})
    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("""
        SELECT DISTINCT u FROM User u
        JOIN u.roles r
        WHERE u.enabled = true
          AND r IN :roles
        ORDER BY u.username ASC
    """)
    List<User> findByEnabledTrueAndRolesIn(@Param("roles") Collection<Roles> roles);
}
