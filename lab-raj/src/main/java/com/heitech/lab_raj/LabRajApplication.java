package com.heitech.lab_raj;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.heitech.lab_raj.demo.Student;
import com.heitech.lab_raj.demo.Grades;

@SpringBootApplication
public class LabRajApplication {

	public static void main(String[] args) {
		// SpringApplication.run(LabRajApplication.class, args);
		Student student = new Student(new Grades());
		student.retrieveGrades();
	}

}
