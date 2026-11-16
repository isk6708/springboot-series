package com.heitech.lab_raj.demo;

public class Student {
    private Grades grades;

    public Student(Grades mygrades) {
        this.grades = mygrades;
    }
    public void retrieveGrades() {
        grades.getGrades();
    }
}
