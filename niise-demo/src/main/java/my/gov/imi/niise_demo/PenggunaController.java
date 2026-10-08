package my.gov.imi.niise_demo;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
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
        // Clear out the password string from the response payload for data security
        responseDto.setPassword(null);

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
        return mapper.map(entity, PenggunaDto.class);
    }

    private Pengguna convertToEntity(PenggunaDto dto) {
        // Explicitly binds the DTO's plain text password field to the entity's hashPassword destination field
        Pengguna pengguna = mapper.map(dto, Pengguna.class);
        pengguna.setHashPassword(dto.getPassword());
        return pengguna;
    }

    private PenggunaAdminDto convertToAdminDto(Pengguna entity) {
        return mapper.map(entity, PenggunaAdminDto.class);
    }

    private Pengguna convertToEntityFromAdmin(PenggunaAdminDto adminDto) {
        // Explicitly binds the Admin DTO's password field to the entity's hashPassword destination field
        Pengguna pengguna = mapper.map(adminDto, Pengguna.class);
        pengguna.setHashPassword(adminDto.getPassword());
        return pengguna;
    }
}
