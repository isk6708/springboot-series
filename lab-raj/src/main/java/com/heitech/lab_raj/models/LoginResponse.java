package com.heitech.lab_raj.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;

import com.heitech.lab_raj.dtos.UserDto;

@Getter
@Setter
@AllArgsConstructor 
public class LoginResponse implements Serializable {

    private String token;

    private String type = "Bearer";

    
    private UserDto user;
}
