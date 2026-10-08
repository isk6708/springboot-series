package my.gov.imi.niise_demo;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder; // Added for password encryption
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@RestController
@RequestMapping("api/v1/pengguna")
public class PenggunaController {
    
    private final PenggunaService service;
    private final ModelMapper mapper;
    private final PasswordEncoder passwordEncoder; // 1. Injected the security password encoder

    // ===================================================================
    // PUBLIC AUTHENTICATION & PROFILE ENDPOINTS (Hides Database IDs)
    // ===================================================================

    /**
     * Public endpoint to search and view user listings without exposing primary key IDs.
     * Example: GET /api/v1/pengguna?nama=Ahmad&icno=95
     */
    @GetMapping
    public List<PenggunaDto> getPengguna(
            @RequestParam(value = "nama", required = false) String nama,
            @RequestParam(value = "icno", required = false) String icno) {
            
        return service.searchPengguna(nama, icno)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Public authentication endpoint to submit/register a new user account profile.
     * Utilizes the custom boolean check to prevent duplicate email registrations.
     */
    @PostMapping("/register")
    public ResponseEntity<?> postPengguna(@Valid @RequestBody PenggunaDto penggunaDto) {
        if (service.checkEmailExists(penggunaDto.getEmail())) {
            return ResponseEntity.badRequest().body("Error: Email is already taken!");
        }

        var entity = convertToEntity(penggunaDto);
        var savedPengguna = service.addPengguna(entity);

        var responseDto = convertToDto(savedPengguna);
        return ResponseEntity.ok(responseDto);
    }

    // ===================================================================
    // ADMINISTRATIVE ENDPOINTS (Exposes and Uses PenggunaAdminDto with IDs)
    // ===================================================================

    /**
     * Administrative dashboard endpoint to see and search all records with their backend IDs.
     * Example: GET /api/v1/pengguna/admin?icno=8801
     */
    @GetMapping("/admin")
    public List<PenggunaAdminDto> getPenggunaForAdmin(
            @RequestParam(value = "nama", required = false) String nama,
            @RequestParam(value = "icno", required = false) String icno) {
            
        return service.searchPengguna(nama, icno)
                .stream()
                .map(this::convertToAdminDto)
                .collect(Collectors.toList());
    }

    /**
     * Admin function to fetch a specific user record by its exact ID.
     */
    @GetMapping("/admin/{id}")
    public PenggunaAdminDto getPenggunaByIdForAdmin(@PathVariable("id") int id) {
        return convertToAdminDto(service.findPenggunaById(id));
    }

    /**
     * Admin function to completely update a user's fields using the ID specified in the request body.
     */
    @PutMapping("/admin")
    public PenggunaAdminDto updatePenggunaAdmin(@Valid @RequestBody PenggunaAdminDto adminDto) {
        var entity = convertToEntityFromAdmin(adminDto);
        var updatedEntity = service.updatePengguna(adminDto.getId(), entity);
        return convertToAdminDto(updatedEntity);
    }

    /**
     * Admin function to permanently delete a user record from the database.
     */
    @DeleteMapping("/admin/{id}")
    public void deletePenggunaByIdForAdmin(@PathVariable("id") int id) {
        service.removePenggunaById(id);
    }

    // ===================================================================
    // PRIVATE MODELMAPPER CONVERSION HELPERS
    // ===================================================================

    private PenggunaDto convertToDto(Pengguna entity) {
        PenggunaDto dto = mapper.map(entity, PenggunaDto.class);
        // dto.setPassword(null); // 2. CRUCIAL: Never leak the password back in public responses
        dto.setPassword(entity.getHashPassword()); 
        return dto;
    }

    private Pengguna convertToEntity(PenggunaDto dto) {
        Pengguna pengguna = mapper.map(dto, Pengguna.class);
        // 3. CRUCIAL: Automatically hashes the raw input text via BCrypt before hitting SQLite
        if (dto.getPassword() != null) {
            pengguna.setHashPassword(passwordEncoder.encode(dto.getPassword()));
        }
        return pengguna;
    }

    private PenggunaAdminDto convertToAdminDto(Pengguna entity) {
        PenggunaAdminDto dto = mapper.map(entity, PenggunaAdminDto.class);
        // dto.setPassword(null); // 4. CRUCIAL: Protect the password value on admin response payloads too
        dto.setPassword(entity.getHashPassword()); 
        return dto;
    }

    private Pengguna convertToEntityFromAdmin(PenggunaAdminDto adminDto) {
        Pengguna pengguna = mapper.map(adminDto, Pengguna.class);
        // 5. CRUCIAL: Encrypts the administrative update inputs safely
        if (adminDto.getPassword() != null && !adminDto.getPassword().isBlank()) {
            pengguna.setHashPassword(passwordEncoder.encode(adminDto.getPassword()));
        }
        return pengguna;
    }
}
