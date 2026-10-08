package my.gov.imi.niise_demo;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper; // Added missing import
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import my.gov.imi.niise_demo.util.JwtUtil;

@RestController
@AllArgsConstructor
@RequestMapping("api/v1")
public class LoginController {

    private final JwtUtil jwtTokenUtil;
    private final LoginService loginService;
    private final ModelMapper mapper; // 1. Added missing ModelMapper field injection

    /**
     * Public authentication endpoint to log in user profiles.
     * Accessible via: POST http://localhost:8005/api/v1/login
     */
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<?> authenticate(@Valid @RequestBody LoginRequest req) throws Exception {
        Pengguna user;

        try {
            // 1. Verify credentials via your LoginService
            user = loginService.authenticate(req.getEmail(), req.getPassword());
        } catch (BadCredentialsException e) {
            // Return 401 Unauthorized cleanly for bad credentials
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Error: Incorrect username or password");
        }

        var userDetails = loginService.loadUserByUsername(user.getEmail());

        // 2. Generate the signed token string
        String jwt = jwtTokenUtil.generateToken(userDetails);

        // 3. Map the authenticated entity profile safely to your clean DTO format
        PenggunaDto userDto = mapper.map(user, PenggunaDto.class);

        // 4. Pass all three values into the constructor signature flawlessly
        return ResponseEntity.ok(new LoginResponse(jwt, "Bearer", userDto));
    }
}
