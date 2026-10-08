package my.gov.imi.niise_demo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PenggunaRepository extends JpaRepository<Pengguna, Integer> {

    // ===================================================================
    // AUTHENTICATION LOGIC LOOKUPS
    // ===================================================================

    /**
     * Finds a user profile by their exact email address.
     * Crucial for Spring Security to load profiles during login/JWT verification.
     */
    Optional<Pengguna> findByEmail(String email);

    /**
     * Custom query execution checking if a specific email exists.
     * Returns true if the count is greater than 0, otherwise returns false.
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Pengguna p WHERE p.email = :email")
    boolean selectExistsEmail(@Param("email") String email);

    // ===================================================================
    // ADMINISTRATIVE SEARCH AND FILTER FUNCTIONS
    // ===================================================================

    /**
     * Finds records matching both criteria if both are provided.
     */
    List<Pengguna> findByNamaContainingIgnoreCaseAndIcnoContainingIgnoreCase(String nama, String icno);

    /**
     * Finds records matching only the name field.
     */
    List<Pengguna> findByNamaContainingIgnoreCase(String nama);

    /**
     * Finds records matching only the IC number field.
     */
    List<Pengguna> findByIcnoContainingIgnoreCase(String icno);
}
