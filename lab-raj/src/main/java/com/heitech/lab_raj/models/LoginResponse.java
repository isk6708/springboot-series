package com.heitech.lab_raj.models;

import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String email;
    private String fullname;
    private long userId;
}
