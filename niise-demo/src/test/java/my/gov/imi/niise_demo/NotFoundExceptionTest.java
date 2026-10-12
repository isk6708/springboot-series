package my.gov.imi.niise_demo;

import static org.assertj.core.api.Assertions.assertThat;

import my.gov.imi.niise_demo.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Unit tests for {@link NotFoundException}.
 *
 * <p>Built on Chapter 9 principles:
 * <ul>
 *   <li>Pure JUnit 5 tests — no Spring context, no mocks required.</li>
 *   <li>AssertJ for fluent, readable assertions.</li>
 *   <li>Verifies constructor behavior and {@code @ResponseStatus} metadata.</li>
 * </ul>
 *
 * <p>Location: {@code src/test/java/my/gov/imi/niise_demo/NotFoundExceptionTest.java}
 */
@DisplayName("NotFoundException Unit Tests")
class NotFoundExceptionTest {

    // ===================================================================
    // CONSTRUCTOR BEHAVIOR
    // ===================================================================

    @Nested
    @DisplayName("Constructor: NotFoundException(String message)")
    class MessageConstructor {

        @Test
        @DisplayName("Should store the provided message")
        void storesMessage() {
            // given
            String message = "Passport 7 was not found";

            // when
            NotFoundException ex = new NotFoundException(message);

            // then
            assertThat(ex).isNotNull();
            assertThat(ex.getMessage()).isEqualTo(message);
            assertThat(ex.getCause()).isNull();
        }

        @Test
        @DisplayName("Should accept a null message without throwing")
        void acceptsNullMessage() {
            // when
            NotFoundException ex = new NotFoundException((String) null);

            // then
            assertThat(ex).isNotNull();
            assertThat(ex.getMessage()).isNull();
        }
    }

    @Nested
    @DisplayName("Constructor: NotFoundException(String message, Throwable cause)")
    class MessageAndCauseConstructor {

        @Test
        @DisplayName("Should store both message and cause")
        void storesMessageAndCause() {
            // given
            String message = "Passport 7 was not found";
            Throwable cause = new IllegalStateException("root cause");

            // when
            NotFoundException ex = new NotFoundException(message, cause);

            // then
            assertThat(ex.getMessage()).isEqualTo(message);
            assertThat(ex.getCause()).isSameAs(cause);
        }

        @Test
        @DisplayName("Should accept null cause")
        void acceptsNullCause() {
            // when
            NotFoundException ex = new NotFoundException("message", null);

            // then
            assertThat(ex.getMessage()).isEqualTo("message");
            assertThat(ex.getCause()).isNull();
        }
    }

    @Nested
    @DisplayName("Constructor: NotFoundException(Throwable cause)")
    class CauseOnlyConstructor {

        @Test
        @DisplayName("Should store the cause and derive message from it")
        void storesCause() {
            // given
            Throwable cause = new IllegalStateException("root cause");

            // when
            NotFoundException ex = new NotFoundException(cause);

            // then
            assertThat(ex.getCause()).isSameAs(cause);
            assertThat(ex.getMessage()).isEqualTo(cause.toString());
        }

        @Test
        @DisplayName("Should accept null cause")
        void acceptsNullCause() {
            // when
            NotFoundException ex = new NotFoundException((Throwable) null);

            // then
            assertThat(ex.getCause()).isNull();
            assertThat(ex.getMessage()).isNull();
        }
    }

    // ===================================================================
    // TYPE HIERARCHY
    // ===================================================================

    @Nested
    @DisplayName("Type hierarchy")
    class TypeHierarchy {

        @Test
        @DisplayName("Should be a RuntimeException (unchecked)")
        void isRuntimeException() {
            NotFoundException ex = new NotFoundException("any");

            assertThat(ex).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("Should be a Throwable")
        void isThrowable() {
            NotFoundException ex = new NotFoundException("any");

            assertThat(ex).isInstanceOf(Throwable.class);
        }
    }

    // ===================================================================
    // SPRING MVC METADATA (@ResponseStatus)
    // ===================================================================

    @Nested
    @DisplayName("Spring @ResponseStatus metadata")
    class ResponseStatusMetadata {

        @Test
        @DisplayName("Should be annotated with @ResponseStatus")
        void hasResponseStatusAnnotation() {
            ResponseStatus annotation =
                    NotFoundException.class.getAnnotation(ResponseStatus.class);

            assertThat(annotation)
                    .as("@ResponseStatus must be present on NotFoundException")
                    .isNotNull();
        }

        @Test
        @DisplayName("Should map to HttpStatus.NOT_FOUND (404)")
        void mapsToHttp404() {
            ResponseStatus annotation =
                    NotFoundException.class.getAnnotation(ResponseStatus.class);

            assertThat(annotation).isNotNull();
            assertThat(annotation.value()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    // ===================================================================
    // THROWABILITY
    // ===================================================================

    @Nested
    @DisplayName("Throwable behavior")
    class Throwability {

        @Test
        @DisplayName("Should be throwable and catchable")
        void canBeThrownAndCaught() {
            // when / then
            try {
                throw new NotFoundException("Passport not found");
            } catch (NotFoundException ex) {
                assertThat(ex.getMessage()).isEqualTo("Passport not found");
            }
        }

        @Test
        @DisplayName("Should be catchable as RuntimeException")
        void catchableAsRuntimeException() {
            try {
                throw new NotFoundException("any");
            } catch (RuntimeException ex) {
                assertThat(ex).isInstanceOf(NotFoundException.class);
            }
        }
    }
}