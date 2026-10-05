package com.coderslab.lab.lab1;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component    
public class Student {

    // private final Grades grades;

    // // Dependency Injection happens here
    // public Student(Grades grades) {
    //     this.grades = grades;
    // }
    @Autowired
    private  Grades grades;
    
    // @Autowired
    // public void setGrades(Grades grades) {
    //     this.grades = grades;
    // }

    // public Grades getGrades() {
    //     return grades;
    // }

    public void retrieveGrades() {
        grades.getGrades();
    }
}
