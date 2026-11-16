package com.heitech.lab_raj.services;

import com.heitech.lab_raj.models.TodoEntity;
import com.heitech.lab_raj.repositories.TodoRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.heitech.lab_raj.exceptions.NotFoundException;

@Service
public class TodoService {

    private final TodoRepository todoRepository;

    // Constructor injection (recommended over @Autowired on fields)
    public TodoService(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    // ==================== PUBLIC TODOS ====================

    public List<TodoEntity> getAllPublicTodos() {
        // return todoRepository.findByUserIdIsNull();
        return todoRepository.findAll();
    }

    public TodoEntity createPublicTodo(TodoEntity todo) {
        todo.setId(null); // Ensure a new record is created
        todo.setUserId(null);
        return todoRepository.save(todo);
    }

    public TodoEntity updatePublicTodo(Integer id, TodoEntity updatedTodo) {
        // TodoEntity existing = todoRepository.findById(id)
        //         .filter(t -> t.getUserId() == null)
        //         .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Public todo not found"));
        TodoEntity existing = todoRepository.findById(id)
            .filter(t -> t.getUserId() == null)
            .orElseThrow(() -> new NotFoundException("Public todo not found with id: " + id));
    

        existing.setTitle(updatedTodo.getTitle());
        existing.setDescription(updatedTodo.getDescription());
        if (updatedTodo.getCompleted() != null) {
            existing.setCompleted(updatedTodo.getCompleted());
        }
        return todoRepository.save(existing);
    }

    public void deletePublicTodo(Integer id) {
        TodoEntity existing = todoRepository.findById(id)
                .filter(t -> t.getUserId() == null)
                .orElseThrow(() -> new NotFoundException("Public todo not found with id: " + id));
            

        todoRepository.delete(existing);
    }

    // ==================== PRIVATE USER TODOS ====================

    public List<TodoEntity> getUserTodos(Integer userId) {
        return todoRepository.findByUserId(userId);
    }

    public TodoEntity createUserTodo(Integer userId, TodoEntity todo) {
        todo.setId(null);
        todo.setUserId(userId);
        return todoRepository.save(todo);
    }

    public TodoEntity updateUserTodo(Integer id, Integer userId, TodoEntity updatedTodo) {
        TodoEntity existing = todoRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Todo not found for this user with id: " + id));

        existing.setTitle(updatedTodo.getTitle());
        existing.setDescription(updatedTodo.getDescription());
        if (updatedTodo.getCompleted() != null) {
            existing.setCompleted(updatedTodo.getCompleted());
        }
        return todoRepository.save(existing);
    }

    public void deleteUserTodo(Integer id, Integer userId) {
        TodoEntity existing = todoRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Todo not found for this user with id: " + id));

        todoRepository.delete(existing);
    }
}