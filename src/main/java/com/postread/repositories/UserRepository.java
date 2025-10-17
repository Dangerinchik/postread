package com.postread.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.postread.security.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByName(String username);

    boolean existsUserByName(String username);

    boolean existsUserByEmail(String email);
//    Optional<User> findById(Long authorId);
}
