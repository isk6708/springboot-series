package my.gov.imi.niise_demo;

import my.gov.imi.niise_demo.PassportDto;
import my.gov.imi.niise_demo.Passport; // Your Entity class
import my.gov.imi.niise_demo.PassportService;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@AllArgsConstructor
@RestController
@RequestMapping("api/v1/passports")
public class PassportController {
    
    private final PassportService service;
    private final ModelMapper mapper;

    @GetMapping
    public List<PassportDto> getPassports() {
        var passportList = StreamSupport
                .stream(service.findAllPassports().spliterator(), false)
                .collect(Collectors.toList());

        return passportList
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public PassportDto getPassportById(@PathVariable("id") int id) {
        return convertToDto(service.findPassportById(id));
    }

    @DeleteMapping("/{id}")
    public void deletePassportById(@PathVariable("id") int id) {
        service.removePassportById(id);
    }

    @PostMapping
    public PassportDto postPassport(@Valid @RequestBody PassportDto passportDto) {
        var entity = convertToEntity(passportDto);
        var passport = service.addPassport(entity);

        return convertToDto(passport);
    }

    @PutMapping("/{id}")
    public void putPassport(
            @PathVariable("id") int id,
            @Valid @RequestBody PassportDto passportDto
    ) {
        // Convert to entity and manually attach the ID from the path variable
        var passportEntity = convertToEntity(passportDto);
        passportEntity.setId(id); 
        
        service.updatePassport(id, passportEntity);
    }

    private PassportDto convertToDto(Passport entity) {
        return mapper.map(entity, PassportDto.class);
    }

    private Passport convertToEntity(PassportDto dto) {
        return mapper.map(dto, Passport.class);
    }
}
