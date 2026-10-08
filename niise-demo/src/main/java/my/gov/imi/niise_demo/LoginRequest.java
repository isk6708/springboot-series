package my.gov.imi.niise_demo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;

@Getter
@Setter
public class LoginRequest implements Serializable {

    // Ensures safe version compatibility tracking during structural transmissions
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
