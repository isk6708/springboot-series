package my.gov.imi.niise_demo;

import my.gov.imi.niise_demo.Pengguna; // Your Entity class
import my.gov.imi.niise_demo.PenggunaRepository;
import my.gov.imi.niise_demo.exception.NotFoundException; // Extends your custom runtime exception handler
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class PenggunaService {

    private final PenggunaRepository repo;

    public Iterable<Pengguna> findAllPengguna() {
        return repo.findAll();
    }

    public Pengguna findPenggunaById(int id) {
        return findOrThrow(id);
    }

    @Transactional
    public void removePenggunaById(int id) {
        // Safe check to throw a clean 404 error if an admin tries to delete a non-existent ID
        if (!repo.existsById(id)) {
            throw new NotFoundException("Pengguna by id " + id + " was not found");
        }
        repo.deleteById(id);
    }

    @Transactional
    public Pengguna addPengguna(Pengguna pengguna) {
        return repo.save(pengguna);
    }

    /**
     * Updates an existing pengguna profile record and returns the newly persisted entity state.
     */
    @Transactional
    public Pengguna updatePengguna(int id, Pengguna incomingPengguna) {
        // 1. Verify the record exists first (throws 404 via helper if missing)
        findOrThrow(id);
        
        // 2. Explicitly bind the target tracking ID onto the incoming object state structure
        incomingPengguna.setId(id);
        
        // 3. Persist and return the updated entity tracking state back up to the controller layer
        return repo.save(incomingPengguna);
    }

    /**
     * Authentication workflow validation helper using your custom repository query method.
     */
    public boolean checkEmailExists(String email) {
        if (email == null) {
            return false;
        }
        return repo.selectExistsEmail(email.trim());
    }

    private Pengguna findOrThrow(final int id) {
        return repo
                .findById(id)
                .orElseThrow(
                        () -> new NotFoundException("Pengguna by id " + id + " was not found")
                );
    }

    /**
     * Conditional administrative and profile multi-field query system handler.
     */
    public List<Pengguna> searchPengguna(String nama, String icno) {
        boolean hasName = nama != null && !nama.trim().isEmpty();
        boolean hasIc = icno != null && !icno.trim().isEmpty();

        if (hasName && hasIc) {
            return repo.findByNamaContainingIgnoreCaseAndIcnoContainingIgnoreCase(nama.trim(), icno.trim());
        } else if (hasName) {
            return repo.findByNamaContainingIgnoreCase(nama.trim());
        } else if (hasIc) {
            return repo.findByIcnoContainingIgnoreCase(icno.trim());
        } else {
            return repo.findAll();
        }
    }
}
