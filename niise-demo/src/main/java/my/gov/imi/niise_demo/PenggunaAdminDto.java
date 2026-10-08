package my.gov.imi.niise_demo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PenggunaAdminDto extends PenggunaDto {
    
    // Aligns perfectly with the int primary key type defined inside the Pengguna entity
    private int id;
}
