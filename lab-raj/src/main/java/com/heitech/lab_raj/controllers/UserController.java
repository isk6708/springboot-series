package com.heitech.lab_raj.controllers;

import com.heitech.lab_raj.dtos.UserDto;
import com.heitech.lab_raj.models.UserEntity;
import com.heitech.lab_raj.services.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService UserService;
    private final ModelMapper modelMapper;

    public UserController(UserService UserService, ModelMapper modelMapper) {
        this.UserService = UserService;
        this.modelMapper = modelMapper;
    }

    // ==================== PUBLIC TODOS (/api/v1/todos) ====================

    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers(
            @RequestParam(required = false) String keyword) {
        List<UserDto> users = UserService.getAllUsers(keyword)
                .stream()
                .map(entity -> modelMapper.map(entity, UserDto.class))
                .toList();
        return ResponseEntity.ok(users);
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Valid @RequestBody UserDto UserDto) {
        UserEntity entity = modelMapper.map(UserDto, UserEntity.class);
        UserEntity saved = UserService.createUser(entity);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(modelMapper.map(saved, UserDto.class));
    }
    
    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserDto UserDto) {
        UserEntity entity = modelMapper.map(UserDto, UserEntity.class);
        UserEntity saved = UserService.createUser(entity);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(modelMapper.map(saved, UserDto.class));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable Integer id,
            @Valid @RequestBody UserDto UserDto) {
        UserEntity entity = modelMapper.map(UserDto, UserEntity.class);
        UserEntity updated = UserService.updateUser(id, entity);
        return ResponseEntity.ok(modelMapper.map(updated, UserDto.class));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        UserService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    
}
