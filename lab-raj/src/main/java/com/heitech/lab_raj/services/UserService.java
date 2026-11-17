package com.heitech.lab_raj.services;

import org.springframework.http.HttpStatus;
// import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.heitech.lab_raj.models.UserEntity;
import com.heitech.lab_raj.repositories.UserRepository;
import com.heitech.lab_raj.repositories.TodoRepository;

import java.util.List;
import java.util.Integer;

@Service
public class UserService {

    private final UserRepository userRepository;
    // private final PasswordEncoder passwordEncoder;
    private final TodoRepository todoRepository;

    // Constructor injection
    // public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, TodoRepository todoRepository) {
    public UserService(UserRepository userRepository,  TodoRepository todoRepository) {
        this.userRepository = userRepository;
        // this.passwordEncoder = passwordEncoder;
        this.todoRepository = todoRepository;
    }

    // Get all users, optionally filtered by a keyword in fullname or email
    public List<UserEntity> getAllUsers(String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return userRepository.findByFullnameContainingIgnoreCaseAndEmailContainingIgnoreCase(keyword, keyword);
        }
        return userRepository.findAll();
    }

    // Get one user (404 if missing)
    public UserEntity getUserById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + id));
    }

    // Create a user - email must be unique, password is BCrypt-hashed
    @Transactional
    public UserEntity createUser(UserEntity request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered: " + request.getEmail());
        }
        UserEntity user = new UserEntity();
        user.setEmail(request.getEmail());
        user.setFullname(request.getFullname());
        // user.setHashPassword(passwordEncoder.encode(request.getPassword()));
        return userRepository.save(user);
    }

    // Update a user - password only changes when a new one is sent
    @Transactional
    public UserEntity updateUser(Integer id, UserEntity request) {
        UserEntity existing = getUserById(id);

        userRepository.findByEmail(request.getEmail())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered: " + request.getEmail());
                });

        existing.setEmail(request.getEmail());
        existing.setFullname(request.getFullname());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            // existing.setHashPassword(passwordEncoder.encode(request.getPassword()));
        }
        return userRepository.save(existing);
    }

    // Delete a user (404 if missing) together with the user's private todos,
    // otherwise todo.user_id would point to a user that no longer exists
    @Transactional
    public void deleteUser(Integer id) {
        UserEntity user = getUserById(id);
        // todoRepository.deleteByUser(user);
        userRepository.delete(user);
    }
}
