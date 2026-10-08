package my.gov.imi.niise_demo;

import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@AllArgsConstructor
public class LoginPrincipal implements UserDetails {

    // Core entity backing the authenticated context wrapper
    private final Pengguna pengguna;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Return null for basic stateless configuration setup, or collections for authorization mapping
        return null; 
    }

    @Override
    public String getPassword() {
        // Returns the encrypted BCrypt hash string stored in your SQLite database
        return this.pengguna.getHashPassword();
    }

    @Override
    public String getUsername() {
        // Aligns with your custom email tracking identifier requirement
        return this.pengguna.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        // MUST BE true: Allows the user entity to successfully pass login filter processing
        return true; 
    }

    @Override
    public boolean isAccountNonLocked() {
        // MUST BE true: Guards account profiles from throwing locked account context crashes
        return true; 
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // MUST BE true: Prevents credential credential expiration workflow prompts
        return true; 
    }

    @Override
    public boolean isEnabled() {
        // MUST BE true: Activates the runtime security context capability for the user profile
        return true; 
    }
}
