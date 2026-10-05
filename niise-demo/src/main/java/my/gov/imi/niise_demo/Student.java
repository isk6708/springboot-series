package my.gov.imi.niise_demo;

public class Student {
    private Grades grades;

    public Student(Grades grades) {
        this.grades = grades;
    }
    public void retrieveGrades() {
        grades.getGrades();
    }
}
