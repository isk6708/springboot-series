package my.gov.imi.niise_demo;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PassportDto {

    @NotNull(message = "Full name is required")
    private String fullname;

    @NotNull(message = "IC number is required")
    private String icno;
}
