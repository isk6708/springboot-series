package my.gov.imi.niise_demo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PassportRepository extends JpaRepository<Passport, Integer> {
    // Standard CRUD operations are automatically included
}