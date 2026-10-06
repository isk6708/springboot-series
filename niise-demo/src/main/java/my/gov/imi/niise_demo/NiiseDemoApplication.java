package my.gov.imi.niise_demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import java.util.List;

@SpringBootApplication
public class NiiseDemoApplication {

	public static void main(String[] args) {
        // 1. Boot up the Spring application and capture the context
        ApplicationContext context = SpringApplication.run(NiiseDemoApplication.class, args);
        
        // 2. Fetch the PassportRepository bean managed by Spring
        // PassportRepository repository = context.getBean(PassportRepository.class);
		PassportService passportService = context.getBean(PassportService.class);
        
        // 3. Create a test passport record and save it
        Passport testPassport = new Passport("Hazieq", "010101-14-1234");
        // repository.save(testPassport);
		passportService.createPassport(testPassport);
        System.out.println(">>> Success: Saved test passport to SQLite database!");

        // 4. Query the database to verify it was written correctly
        // List<Passport> allPassports = repository.findAll();
		List<Passport> allPassports = passportService.getAllPassports();

        System.out.println(">>> Reading from SQLite database:");
        for (Passport p : allPassports) {
            System.out.println("ID: " + p.getId() + " | Name: " + p.getFullname() + " | IC: " + p.getIcno());
        }
    }

	// public static void main(String[] args) {
	// 	SpringApplication.run(NiiseDemoApplication.class, args);
	// 	// Student student = new Student(new Grades());
		
	// 	// ApplicationContext context =  SpringApplication.run(NiiseDemoApplication.class, args);
	// 	// Student student = context.getBean(Student.class);
	// 	// student.retrieveGrades();

	// }

}
