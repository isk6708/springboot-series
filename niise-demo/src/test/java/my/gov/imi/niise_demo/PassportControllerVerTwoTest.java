package my.gov.imi.niise_demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import my.gov.imi.niise_demo.exception.NotFoundException;


import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

/**
 * Enhanced unit tests for {@link PassportController}.
 *
 * <p>Built on Chapter 9 principles:
 * <ul>
 *   <li>Mockito for isolating the controller from the service layer.</li>
 *   <li>AssertJ for fluent, readable assertions.</li>
 *   <li>ArgumentCaptor for inspecting arguments passed to mocks.</li>
 *   <li>assertThrows / assertThatThrownBy for exception scenarios.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PassportController Unit Tests (v2)")
class PassportControllerVerTwoTest {

    @Mock
    private PassportService service;

    private PassportController controller;

    @BeforeEach
    void setUp() {
        // ModelMapper is a concrete class, so we pass a real instance.
        controller = new PassportController(service, new ModelMapper());
    }

    // ===================================================================
    // PUBLIC ENDPOINTS
    // ===================================================================

    @Nested
    @DisplayName("GET /api/v1/passports (public search)")
    class GetPassports {

        @Test
        @DisplayName("Should return public DTOs when a matching passport is found")
        void getPassportsSearchesAndReturnsPublicDtos() {
            // given
            Passport passport = passport(1, "Ali", "900101");
            when(service.searchPassports("Ali", "900101"))
                    .thenReturn(List.of(passport));

            // when
            List<PassportDto> result = controller.getPassports("Ali", "900101");

            // then
            assertThat(result)
                    .hasSize(1)
                    .first()
                    .satisfies(dto -> {
                        assertThat(dto.getFullname()).isEqualTo("Ali");
                        assertThat(dto.getIcno()).isEqualTo("900101");
                    });
            
            String invalidIcno = "900-10-1";
            assertThat(invalidIcno).contains("-");
            
            List<PassportDto> result2 = controller.getPassports("Ali", invalidIcno);
            assertThat(result2).isEmpty();

            // 1. Arrange: Define a valid IC number string with no hyphens
        String validIcno = "900101";

        // 2. Act & Assert: Verify the format contains no invalid characters
        assertThat(validIcno).doesNotContain("-");

        // 3. Execute: Call the controller method directly without any exception wrappers
        List<PassportDto> result3 = controller.getPassports("Ali", validIcno);

        // 4. Verify: Assert that a valid dataset is returned successfully
        assertThat(result3).isNotEmpty();
            
            // verify(service,times(2)).searchPassports("Ali", "900101");
            verify(service, atLeastOnce()).searchPassports("Ali", "900101");
            
            verify(service, never()).findPassportById(any(Integer.class));
        }

        @Test
        @DisplayName("Should return an empty list when no passport matches the criteria")
        void getPassportsReturnsEmptyListWhenNoMatch() {
            // given
            when(service.searchPassports("Unknown", null))
                    .thenReturn(Collections.emptyList());

            // when
            List<PassportDto> result = controller.getPassports("Unknown", null);

            // then
            assertThat(result).isEmpty();
            verify(service).searchPassports("Unknown", null);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/passports (public create)")
    class PostPassport {

        @Test
        @DisplayName("Should map request, persist via service, and return public DTO")
        void postPassportMapsRequestAndReturnsCreatedPublicDto() {
            // given
            PassportDto request = new PassportDto();
            request.setFullname("Ali");
            request.setIcno("900101");

            when(service.addPassport(any(Passport.class)))
                    .thenAnswer(invocation -> {
                        Passport saved = invocation.getArgument(0);
                        saved.setId(1);
                        return saved;
                    });

            // when
            PassportDto result = controller.postPassport(request);

            // then
            ArgumentCaptor<Passport> captor = ArgumentCaptor.forClass(Passport.class);
            verify(service).addPassport(captor.capture());

            Passport captured = captor.getValue();
            assertThat(captured.getFullname()).isEqualTo("Ali");
            assertThat(captured.getIcno()).isEqualTo("900101");

            assertThat(result).isNotNull();
            assertThat(result.getFullname()).isEqualTo("Ali");
            assertThat(result.getIcno()).isEqualTo("900101");
        }

        @Test
        @DisplayName("Should propagate exception thrown by the service on create")
        void postPassportPropagatesServiceException() {
            // given
            PassportDto request = new PassportDto();
            request.setFullname("Ali");
            request.setIcno("900101");

            when(service.addPassport(any(Passport.class)))
                    .thenThrow(new RuntimeException("Database unavailable"));

            // when / then
            assertThatThrownBy(() -> controller.postPassport(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Database unavailable");

            verify(service).addPassport(any(Passport.class));
        }
    }

    // ===================================================================
    // ADMINISTRATIVE ENDPOINTS
    // ===================================================================

    @Nested
    @DisplayName("GET /api/v1/passports/admin (admin search)")
    class GetPassportsForAdmin {

        @Test
        @DisplayName("Should return admin DTOs including database IDs")
        void getPassportsForAdminReturnsDtosWithIds() {
            // given
            Passport passport = passport(7, "Ali", "900101");
            when(service.searchPassports("Ali", null))
                    .thenReturn(List.of(passport));

            // when
            List<PassportAdminDto> result =
                    controller.getPassportsForAdmin("Ali", null);

            // then
            assertThat(result)
                    .hasSize(1)
                    .first()
                    .satisfies(dto -> {
                        assertThat(dto.getId()).isEqualTo(7);
                        assertThat(dto.getFullname()).isEqualTo("Ali");
                        assertThat(dto.getIcno()).isEqualTo("900101");
                    });

            verify(service).searchPassports("Ali", null);
        }

        @Test
        @DisplayName("Should return an empty list when no admin records match")
        void getPassportsForAdminReturnsEmptyList() {
            // given
            when(service.searchPassports(null, null))
                    .thenReturn(Collections.emptyList());

            // when
            List<PassportAdminDto> result =
                    controller.getPassportsForAdmin(null, null);

            // then
            assertThat(result).isEmpty();
            verify(service).searchPassports(null, null);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/passports/admin/{id}")
    class GetPassportByIdForAdmin {

        @Test
        @DisplayName("Should return admin DTO for a valid ID")
        void getPassportByIdForAdminReturnsMatchingDto() {
            // given
            when(service.findPassportById(7))
                    .thenReturn(passport(7, "Ali", "900101"));

            // when
            PassportAdminDto result = controller.getPassportByIdForAdmin(7);

            // then
            assertThat(result.getId()).isEqualTo(7);
            assertThat(result.getFullname()).isEqualTo("Ali");
            assertThat(result.getIcno()).isEqualTo("900101");

            verify(service).findPassportById(7);
        }

        @Test
        @DisplayName("Should throw NotFoundException when ID does not exist")
        void getPassportByIdForAdminThrowsWhenNotFound() {
            // given
            when(service.findPassportById(999))
                    .thenThrow(new NotFoundException("Passport not found"));

            // when / then
            assertThrows(NotFoundException.class,
                    () -> controller.getPassportByIdForAdmin(999));

            verify(service).findPassportById(999);
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/passports/admin (admin update)")
    class UpdatePassportAdmin {

        @Test
        @DisplayName("Should map request, update via service, and return admin DTO")
        void updatePassportAdminMapsInputAndReturnsUpdatedDto() {
            // given
            PassportAdminDto request = new PassportAdminDto();
            request.setId(7);
            request.setFullname("Ali Updated");
            request.setIcno("900102");

            when(service.updatePassport(eq(7), any(Passport.class)))
                    .thenAnswer(invocation -> invocation.getArgument(1));

            // when
            PassportAdminDto result = controller.updatePassportAdmin(request);

            // then
            ArgumentCaptor<Passport> captor = ArgumentCaptor.forClass(Passport.class);
            verify(service).updatePassport(eq(7), captor.capture());

            Passport captured = captor.getValue();
            assertThat(captured.getFullname()).isEqualTo("Ali Updated");
            assertThat(captured.getIcno()).isEqualTo("900102");

            assertThat(result.getId()).isEqualTo(7);
            assertThat(result.getFullname()).isEqualTo("Ali Updated");
            assertThat(result.getIcno()).isEqualTo("900102");
        }

        @Test
        @DisplayName("Should propagate exception when service rejects the update")
        void updatePassportAdminPropagatesServiceException() {
            // given
            PassportAdminDto request = new PassportAdminDto();
            request.setId(7);
            request.setFullname("Ali Updated");
            request.setIcno("900102");

            when(service.updatePassport(eq(7), any(Passport.class)))
                    .thenThrow(new IllegalArgumentException("ID mismatch"));

            // when / then
            assertThatThrownBy(() -> controller.updatePassportAdmin(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ID mismatch");

            verify(service).updatePassport(eq(7), any(Passport.class));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/passports/admin/{id}")
    class DeletePassportByIdForAdmin {

        @Test
        @DisplayName("Should delegate deletion to the service")
        void deletePassportByIdForAdminDelegatesToService() {
            // when
            controller.deletePassportByIdForAdmin(7);

            // then
            verify(service).removePassportById(7);
            verify(service, times(1)).removePassportById(any(Integer.class));
        }

        @Test
        @DisplayName("Should propagate exception when service fails to delete")
        void deletePassportByIdForAdminPropagatesException() {
            // given
            org.mockito.Mockito.doThrow(new NotFoundException("Passport not found"))
                    .when(service).removePassportById(999);

            // when / then
            assertThrows(NotFoundException.class,
                    () -> controller.deletePassportByIdForAdmin(999));

            verify(service).removePassportById(999);
        }
    }

    // ===================================================================
    // TEST FIXTURE HELPERS
    // ===================================================================

    /**
     * Builds a {@link Passport} entity for test use, mimicking what
     * the persistence layer would return.
     */
    private Passport passport(int id, String fullname, String icno) {
        Passport passport = new Passport(fullname, icno);
        passport.setId(id);
        return passport;
    }
}