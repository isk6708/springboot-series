package com.coderslab.lab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.coderslab.lab.lab1.Student;
// import com.coderslab.lab.lab1.Grades;

@SpringBootApplication
public class LabApplication {

	public static void main(String[] args) {
		SpringApplication.run(LabApplication.class, args);
		// ApplicationContext context =  SpringApplication.run(LabApplication.class, args);
		ApplicationContext context =  SpringApplication.run(LabApplication.class, args);
		// ApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");

		Student student = context.getBean(Student.class);

		// Student student = new Student(new Grades());
		// Student student = new Student();
		student.retrieveGrades();
	}

}
