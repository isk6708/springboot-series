package com.heitech.lab_raj.demo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component 
public class Student {
    @Autowired
    private Grades grades;

    // public Student(Grades mygrades) {
    //     this.grades = mygrades;
    // }
    public void retrieveGrades() {
        grades.getGrades();
    }
}
