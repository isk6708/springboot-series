package my.gov.imi.niise_demo;

import my.gov.imi.niise_demo.Passport; // Your Entity class
import my.gov.imi.niise_demo.PassportRepository;
import my.gov.imi.niise_demo.exception.NotFoundException; // Assuming you create this custom exception
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

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

    public void removePassportById(int id) {
        repo.deleteById(id);
    }

    public Passport addPassport(Passport passport) {
        return repo.save(passport);
    }

    public void updatePassport(int id, Passport passport) {
        findOrThrow(id);
        repo.save(passport);
    }

    private Passport findOrThrow(final int id) {
        return repo
                .findById(id)
                .orElseThrow(
                        () -> new NotFoundException("Passport by id " + id + " was not found")
                );
    }
}
