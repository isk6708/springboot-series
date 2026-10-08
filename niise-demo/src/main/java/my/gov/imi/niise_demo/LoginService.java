package my.gov.imi.niise_demo;

import lombok.AllArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class LoginService implements UserDetailsService {

    private final PenggunaRepository penggunaRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Standard Spring Security interface method to resolve credentials by email.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var pengguna = penggunaRepository.findByEmail(email.trim())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        return new LoginPrincipal(pengguna);
    }

    /**
     * Custom authentication method that handles credential verification.
     */
    public Pengguna authenticate(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new BadCredentialsException("Unauthorized: Credentials cannot be empty");
        }

        // 1. Look up the user record by their unique email identifier
        var pengguna = penggunaRepository.findByEmail(email.trim())
                .orElseThrow(() -> new BadCredentialsException("Unauthorized: Invalid email or password"));

        // 2. Validate incoming plain text password against the salted database hash
        boolean verified = passwordEncoder.matches(password, pengguna.getHashPassword());

        if (!verified) {
            throw new BadCredentialsException("Unauthorized: Invalid email or password");
        }

        return pengguna;
    }
}
