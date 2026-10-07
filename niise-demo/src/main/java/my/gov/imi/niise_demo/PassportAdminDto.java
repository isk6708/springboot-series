package my.gov.imi.niise_demo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PassportAdminDto extends PassportDto {

    private int id; // Or String / UUID depending on your Entity primary key type
}
