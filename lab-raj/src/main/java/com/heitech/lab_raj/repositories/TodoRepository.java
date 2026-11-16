package com.heitech.lab_raj.repositories;

import com.heitech.lab_raj.models.TodoEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TodoRepository extends JpaRepository<TodoEntity, Integer> {

    // Fetch all public todos (where user_id is null)
    List<TodoEntity> findByUserIdIsNull();

    // Fetch all private todos belonging to a specific user
    List<TodoEntity> findByUserId(Integer userId);

    // Fetch a specific todo owned by a specific user (useful for secure update/delete)
    Optional<TodoEntity> findByIdAndUserId(Integer id, Integer userId);
}
