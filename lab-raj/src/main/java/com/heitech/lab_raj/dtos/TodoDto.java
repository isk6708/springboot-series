package com.heitech.lab_raj.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TodoDto {

    private Integer id;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private Boolean completed = false;

    private Integer userId;

}
