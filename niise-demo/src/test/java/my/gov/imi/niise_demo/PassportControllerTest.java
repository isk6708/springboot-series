package my.gov.imi.niise_demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

@ExtendWith(MockitoExtension.class)
class PassportControllerTest {

    @Mock
    private PassportService service;

    private PassportController controller;

    @BeforeEach
    void setUp() {
        controller = new PassportController(service, new ModelMapper());
    }

    @Test
    void getPassportsSearchesAndReturnsPublicDtos() {
        Passport passport = passport(1, "Ali", "900101");
        when(service.searchPassports("Ali", "900101")).thenReturn(List.of(passport));

        List<PassportDto> result = controller.getPassports("Ali", "900101");

        assertEquals(1, result.size());
        assertEquals("Ali", result.get(0).getFullname());
        assertEquals("900101", result.get(0).getIcno());
        verify(service).searchPassports("Ali", "900101");
    }

    @Test
    void postPassportMapsRequestAndReturnsCreatedPublicDto() {
        PassportDto request = new PassportDto();
        request.setFullname("Ali");
        request.setIcno("900101");
        when(service.addPassport(org.mockito.ArgumentMatchers.any(Passport.class)))
                .thenAnswer(invocation -> {
                    Passport saved = invocation.getArgument(0);
                    saved.setId(1);
                    return saved;
                });

        PassportDto result = controller.postPassport(request);

        ArgumentCaptor<Passport> passportCaptor = ArgumentCaptor.forClass(Passport.class);
        verify(service).addPassport(passportCaptor.capture());
        assertEquals("Ali", passportCaptor.getValue().getFullname());
        assertEquals("900101", passportCaptor.getValue().getIcno());
        assertEquals("Ali", result.getFullname());
        assertEquals("900101", result.getIcno());
    }

    @Test
    void postPassportAdminNoDashMapsRequestAndReturnsAdminDto() {
        PassportAdminDto request = new PassportAdminDto();
        request.setFullname("Ali");
        request.setIcno("900-101");
        when(service.addPassport(org.mockito.ArgumentMatchers.any(Passport.class)))
                .thenAnswer(invocation -> {
                    Passport saved = invocation.getArgument(0);
                    saved.setId(5);
                    return saved;
                });

        PassportAdminDto result = controller.postPassportAdminNoDash(request);

        ArgumentCaptor<Passport> passportCaptor = ArgumentCaptor.forClass(Passport.class);
        verify(service).addPassport(passportCaptor.capture());
        assertEquals("900101", passportCaptor.getValue().getIcno());
        assertEquals(5, result.getId());
        assertEquals("Ali", result.getFullname());
        assertEquals("900101", result.getIcno());
    }

    @Test
    void getPassportsForAdminReturnsDtosWithIds() {
        Passport passport = passport(7, "Ali", "900101");
        when(service.searchPassports("Ali", null)).thenReturn(List.of(passport));

        List<PassportAdminDto> result = controller.getPassportsForAdmin("Ali", null);

        assertEquals(1, result.size());
        assertEquals(7, result.get(0).getId());
        assertEquals("Ali", result.get(0).getFullname());
        verify(service).searchPassports("Ali", null);
    }

    @Test
    void getPassportByIdForAdminReturnsMatchingDto() {
        when(service.findPassportById(7)).thenReturn(passport(7, "Ali", "900101"));

        PassportAdminDto result = controller.getPassportByIdForAdmin(7);

        assertEquals(7, result.getId());
        assertEquals("Ali", result.getFullname());
        assertEquals("900101", result.getIcno());
        verify(service).findPassportById(7);
    }

    @Test
    void updatePassportAdminMapsInputAndReturnsUpdatedDto() {
        PassportAdminDto request = new PassportAdminDto();
        request.setId(7);
        request.setFullname("Ali Updated");
        request.setIcno("900102");
        when(service.updatePassport(org.mockito.ArgumentMatchers.eq(7),
                org.mockito.ArgumentMatchers.any(Passport.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        PassportAdminDto result = controller.updatePassportAdmin(request);

        ArgumentCaptor<Passport> passportCaptor = ArgumentCaptor.forClass(Passport.class);
        verify(service).updatePassport(org.mockito.ArgumentMatchers.eq(7), passportCaptor.capture());
        assertEquals("Ali Updated", passportCaptor.getValue().getFullname());
        assertEquals("900102", passportCaptor.getValue().getIcno());
        assertEquals(7, result.getId());
        assertEquals("Ali Updated", result.getFullname());
        assertEquals("900102", result.getIcno());
    }

    @Test
    void deletePassportByIdForAdminDelegatesToService() {
        controller.deletePassportByIdForAdmin(7);

        verify(service).removePassportById(7);
    }

    private Passport passport(int id, String fullname, String icno) {
        Passport passport = new Passport(fullname, icno);
        passport.setId(id);
        return passport;
    }
}
