package com.heitech.lab_raj.controllers;

import com.heitech.lab_raj.dtos.TodoDto;
import com.heitech.lab_raj.models.TodoEntity;
import com.heitech.lab_raj.services.TodoService;
import jakarta.validation.Valid;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// @CrossOrigin(
//     origins = "http://localhost:4200",
//     methods = {
//         RequestMethod.GET,
//         RequestMethod.POST,
//         RequestMethod.PUT,
//         RequestMethod.DELETE,
//         RequestMethod.PATCH,
//         RequestMethod.OPTIONS
//     },
//     allowedHeaders = "*"
// )
@RestController
@RequestMapping("/api/v1/todos")
public class TodoController {

    private final TodoService todoService;
    private final ModelMapper modelMapper;

    public TodoController(TodoService todoService, ModelMapper modelMapper) {
        this.todoService = todoService;
        this.modelMapper = modelMapper;
    }

    // ==================== PUBLIC TODOS (/api/v1/todos) ====================

    @GetMapping
    public ResponseEntity<List<TodoDto>> getAllPublicTodos() {
        List<TodoDto> todos = todoService.getAllPublicTodos()
                .stream()
                .map(entity -> modelMapper.map(entity, TodoDto.class))
                .toList();
        return ResponseEntity.ok(todos);
    }

    @PostMapping
    public ResponseEntity<TodoDto> createPublicTodo(@Valid @RequestBody TodoDto todoDto) {
        TodoEntity entity = modelMapper.map(todoDto, TodoEntity.class);
        TodoEntity saved = todoService.createPublicTodo(entity);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(modelMapper.map(saved, TodoDto.class));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TodoDto> updatePublicTodo(
            @PathVariable Integer id,
            @Valid @RequestBody TodoDto todoDto) {
        TodoEntity entity = modelMapper.map(todoDto, TodoEntity.class);
        TodoEntity updated = todoService.updatePublicTodo(id, entity);
        return ResponseEntity.ok(modelMapper.map(updated, TodoDto.class));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePublicTodo(@PathVariable Integer id) {
        todoService.deletePublicTodo(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== PRIVATE TODOS (/api/v1/todos/protected) ====================

    @GetMapping("/protected")
    public ResponseEntity<List<TodoDto>> getUserTodos(@RequestParam Integer userId) {
        List<TodoDto> todos = todoService.getUserTodos(userId)
                .stream()
                .map(entity -> modelMapper.map(entity, TodoDto.class))
                .toList();
        return ResponseEntity.ok(todos);
    }

    @PostMapping("/protected")
    public ResponseEntity<TodoDto> createUserTodo(
            @RequestParam Integer userId,
            @Valid @RequestBody TodoDto todoDto) {
        TodoEntity entity = modelMapper.map(todoDto, TodoEntity.class);
        TodoEntity saved = todoService.createUserTodo(userId, entity);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(modelMapper.map(saved, TodoDto.class));
    }

    @PutMapping("/protected/{id}")
    public ResponseEntity<TodoDto> updateUserTodo(
            @PathVariable Integer id,
            @RequestParam Integer userId,
            @Valid @RequestBody TodoDto todoDto) {
        TodoEntity entity = modelMapper.map(todoDto, TodoEntity.class);
        TodoEntity updated = todoService.updateUserTodo(id, userId, entity);
        return ResponseEntity.ok(modelMapper.map(updated, TodoDto.class));
    }

    @DeleteMapping("/protected/{id}")
    public ResponseEntity<Void> deleteUserTodo(
            @PathVariable Integer id,
            @RequestParam Integer userId) {
        todoService.deleteUserTodo(id, userId);
        return ResponseEntity.noContent().build();
    }
}
