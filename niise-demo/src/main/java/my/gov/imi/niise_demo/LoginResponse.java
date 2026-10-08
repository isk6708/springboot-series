package my.gov.imi.niise_demo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor // Generates a constructor matching exactly: (String token, String type, PenggunaDto user)
public class LoginResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    // The generated stateless bearer token string
    private String token;

    // Modified: Removed 'final' so Lombok includes it cleanly in the AllArgsConstructor signature
    private String type = "Bearer";

    // Embedded user profile details (hides the database primary key)
    private PenggunaDto user;
}
