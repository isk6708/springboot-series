package com.heitech.lab_raj.repositories;

import com.heitech.lab_raj.models.UserEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Integer> {

    List<UserEntity> findByFullnameContainingIgnoreCaseAndEmailContainingIgnoreCase(String fullname, String email);
    boolean existsByEmail(String email);

    Optional<UserEntity> findByEmail(String email);
}
