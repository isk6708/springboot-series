package my.gov.imi.niise_demo;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@RestController
@RequestMapping("api/v1/passports")
public class PassportController {
    
    private final PassportService service;
    private final ModelMapper mapper;

    // ===================================================================
    // PUBLIC ENDPOINTS (Hides Database IDs)
    // ===================================================================

    /**
     * Public endpoint to view and search passports without exposing backend IDs.
     * Example: GET /api/v1/passports?fullname=Ali&icno=90
     */
    @GetMapping
    public List<PassportDto> getPassports(
            @RequestParam(value = "fullname", required = false) String fullname,
            @RequestParam(value = "icno", required = false) String icno) {
            
        return service.searchPassports(fullname, icno)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Public endpoint to submit/create a new passport record.
     */
    @PostMapping
    public PassportDto postPassport(@Valid @RequestBody PassportDto passportDto) {
        var entity = convertToEntity(passportDto);
        var passport = service.addPassport(entity);

        return convertToDto(passport);
    }

    // ===================================================================
    // ADMINISTRATIVE ENDPOINTS (Exposes and Uses PassportAdminDto with IDs)
    // ===================================================================

    /**
     * Administrative dashboard endpoint to see and search records with their backend IDs.
     * Example: GET /api/v1/passports/admin?icno=8801
     */
    @GetMapping("/admin")
    public List<PassportAdminDto> getPassportsForAdmin(
            @RequestParam(value = "fullname", required = false) String fullname,
            @RequestParam(value = "icno", required = false) String icno) {
            
        return service.searchPassports(fullname, icno)
                .stream()
                .map(this::convertToAdminDto)
                .collect(Collectors.toList());
    }

    /**
     * Admin function to fetch a specific passport record by its exact ID.
     */
    @GetMapping("/admin/{id}")
    public PassportAdminDto getPassportByIdForAdmin(@PathVariable("id") int id) {
        return convertToAdminDto(service.findPassportById(id));
    }

    /**
     * Admin function to completely update a passport's fields using the ID specified in the body.
     */
    @PutMapping("/admin")
    public PassportAdminDto updatePassportAdmin(@Valid @RequestBody PassportAdminDto adminDto) {
        var entity = convertToEntityFromAdmin(adminDto);
        var updatedEntity = service.updatePassport(adminDto.getId(), entity);
        return convertToAdminDto(updatedEntity);
    }

    /**
     * Admin function to permanently delete a passport record from the system.
     */
    @DeleteMapping("/admin/{id}")
    public void deletePassportByIdForAdmin(@PathVariable("id") int id) {
        service.removePassportById(id);
    }

    // ===================================================================
    // PRIVATE MODELMAPPER CONVERSION HELPERS
    // ===================================================================

    private PassportDto convertToDto(Passport entity) {
        return mapper.map(entity, PassportDto.class);
    }

    private Passport convertToEntity(PassportDto dto) {
        return mapper.map(dto, Passport.class);
    }

    private PassportAdminDto convertToAdminDto(Passport entity) {
        return mapper.map(entity, PassportAdminDto.class);
    }

    private Passport convertToEntityFromAdmin(PassportAdminDto adminDto) {
        return mapper.map(adminDto, Passport.class);
    }
}
