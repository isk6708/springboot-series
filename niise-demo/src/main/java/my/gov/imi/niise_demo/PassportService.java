package my.gov.imi.niise_demo;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service 
public class PassportService {

    private final PassportRepository passportRepository;

    // Use constructor-based injection (Industry Standard recommendation)
    public PassportService(PassportRepository passportRepository) {
        this.passportRepository = passportRepository;
    }

    /**
     * Retrieve all passport records from the SQLite database.
     */
    public List<Passport> getAllPassports() {
        return passportRepository.findAll();
    }

    
    /**
     * Find a single passport by its ID.
     */
    public Optional<Passport> getPassportById(int id) {
        return passportRepository.findById(id);
    }

    /**
     * Create or save a new passport record.
     */
    public Passport createPassport(Passport passport) {
        return passportRepository.save(passport);
    }

    /**
     * Delete a passport record by its ID.
     */
    public void deletePassport(int id) {
        passportRepository.deleteById(id);
    }
}
