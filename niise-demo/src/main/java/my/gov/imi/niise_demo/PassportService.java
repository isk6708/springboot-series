package my.gov.imi.niise_demo;

import my.gov.imi.niise_demo.Passport; // Your Entity class
import my.gov.imi.niise_demo.PassportRepository;
import my.gov.imi.niise_demo.exception.NotFoundException; // Assuming you create this custom exception
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class PassportService {

    private final PassportRepository repo;

    public Iterable<Passport> findAllPassports() {
        return repo.findAll();
    }

    public Passport findPassportById(int id) {
        return findOrThrow(id);
    }

    @Transactional
    public void removePassportById(int id) {
        // Safe check to throw a clean 404 error if an admin tries to delete a non-existent ID
        if (!repo.existsById(id)) {
            throw new NotFoundException("Passport by id " + id + " was not found");
        }
        repo.deleteById(id);
    }

    @Transactional
    public Passport addPassport(Passport passport) {
        return repo.save(passport);
    }

    /**
     * Updates an existing passport and returns the newly saved entity state.
     * Fixed from 'void' to 'Passport' to fix controller compilation errors.
     */
    @Transactional
    public Passport updatePassport(int id, Passport incomingPassport) {
        // 1. Verify the record exists first (throws 404 if missing)
        Passport existingPassport = findOrThrow(id);
        
        // 2. Explicitly bind the target ID onto the incoming object state
        incomingPassport.setId(id);
        
        // 3. Persist and return the updated entity tracking state back up to the controller
        return repo.save(incomingPassport);
    }

    private Passport findOrThrow(final int id) {
        return repo
                .findById(id)
                .orElseThrow(
                        () -> new NotFoundException("Passport by id " + id + " was not found")
                );
    }
}
