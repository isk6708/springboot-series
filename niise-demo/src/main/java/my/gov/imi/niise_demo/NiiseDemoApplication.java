package my.gov.imi.niise_demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class NiiseDemoApplication {

	public static void main(String[] args) {
		//SpringApplication.run(NiiseDemoApplication.class, args);
		// Student student = new Student(new Grades());
		
		ApplicationContext context =  SpringApplication.run(NiiseDemoApplication.class, args);
		Student student = context.getBean(Student.class);
		student.retrieveGrades();

	}

}
