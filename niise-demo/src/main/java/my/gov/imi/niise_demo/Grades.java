package my.gov.imi.niise_demo;

import org.springframework.stereotype.Component;

@Component
public class Grades {

    // A simple method that prints when called by the Student class
    public void getGrades() {
        System.out.println("Displaying student grades: Math: A, Science: B, English: A");
    }
}