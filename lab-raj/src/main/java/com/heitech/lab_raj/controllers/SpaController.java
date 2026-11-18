package com.heitech.lab_raj.controllers;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Sends Angular routes (e.g. /login, /register, /todos, /users) to index.html.
 * The pattern matches ONE path segment without a dot, so:
 *   /todos          → index.html   (Angular route)
 *   /main-AB12.js   → not matched  (real static file, has a dot)
 *   /api/v1/todos   → not matched  (more than one segment → REST controller)
 */
@Controller
public class SpaController {

    @GetMapping("/{path:[^\\.]*}")
    public String forwardToAngular() {
        return "forward:/index.html";
    }
}
