package my.gov.imi.niise_demo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "passports") // Names the SQLite database table
public class Passport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Handles auto-increment for SQLite
    private int id;

    @Column(name = "fullname", length = 255, nullable = false)
    private String fullname;

    @Column(name = "icno", length = 255, nullable = false, unique = true)
    private String icno;

    // Default no-argument constructor (Required by JPA)
    public Passport() {
    }

    // Convenience constructor for creating new records
    public Passport(String fullname, String icno) {
        this.fullname = fullname;
        this.icno = icno;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getIcno() {
        return icno;
    }

    public void setIcno(String icno) {
        this.icno = icno;
    }
}