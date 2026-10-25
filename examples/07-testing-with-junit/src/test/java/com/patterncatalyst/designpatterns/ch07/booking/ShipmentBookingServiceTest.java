package com.patterncatalyst.designpatterns.ch07.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShipmentBookingService")
class ShipmentBookingServiceTest {

    // ShipmentValidator and FreightCostCalculator are plain, deterministic,
    // and side-effect free, so every test below uses the real thing. The
    // one collaborator with a side effect worth watching, RateLedger, is
    // the only one mocked here.
    private final ShipmentValidator validator = new ShipmentValidator();
    private final FreightCostCalculator costCalculator = new FreightCostCalculator();
    private final BookingIdGenerator idGenerator = () -> "BK-0001";

    @Mock
    private RateLedger ledger;

    private ShipmentBookingService service;

    @BeforeEach
    void setUp() {
        // lenient(): the capacity check this stubs is reached by every
        // accepted booking below, but never by a booking that fails
        // ShipmentValidator's checks first. Mockito's strict stubbing would
        // otherwise fail the validation-rejection tests with an
        // UnnecessaryStubbingException for a stub they never call.
        lenient().when(ledger.capacityRemaining()).thenReturn(10);
        service = new ShipmentBookingService(validator, costCalculator, ledger, idGenerator);
    }

    @Nested
    @DisplayName("when the shipment is accepted")
    class WhenAccepted {

        private final Shipment shipment = new Shipment(3.5, CargoClass.STANDARD, "SEA", 12_000L);

        @Test
        @DisplayName("records the priced booking on the ledger")
        void recordsThePricedBookingOnTheLedger() {
            BookingRecord bookingRecord = service.book(shipment);

            ArgumentCaptor<BookingRecord> captor = ArgumentCaptor.forClass(BookingRecord.class);
            verify(ledger).record(captor.capture());

            assertThat(captor.getValue()).isEqualTo(bookingRecord);
            assertThat(captor.getValue().amount()).isEqualTo(costCalculator.cost(shipment));
        }

        @Test
        @DisplayName("passes the generated booking id to the ledger, checked with an argument matcher")
        void passesTheGeneratedBookingIdToTheLedger() {
            service.book(shipment);

            verify(ledger).record(argThat(recorded -> recorded.bookingId().equals("BK-0001")));
        }
    }

    @Nested
    @DisplayName("when the shipment fails validation")
    class WhenValidationFails {

        @Test
        @DisplayName("never touches the ledger")
        void neverTouchesTheLedger() {
            Shipment malformed = new Shipment(-1.0, CargoClass.STANDARD, "SEA", 12_000L);

            assertThatThrownBy(() -> service.book(malformed)).isInstanceOf(InvalidShipmentException.class);

            verify(ledger, never()).record(any());
        }
    }

    @Nested
    @DisplayName("when the ledger is at capacity")
    class WhenLedgerIsAtCapacity {

        @Test
        @DisplayName("rejects the booking without recording it, proven on a real ledger behind a spy")
        void rejectsTheBookingWithoutRecordingIt() {
            // spy(), not mock(): InMemoryRateLedger is a real, working
            // collaborator. The only thing hard to produce naturally here
            // is a ledger that is already full, so the spy is stubbed on
            // exactly that one method; record(), if it were ever called,
            // would run the real implementation, not a canned answer.
            InMemoryRateLedger realLedger = new InMemoryRateLedger(1);
            RateLedger ledgerSpy = spy(realLedger);
            doReturn(0).when(ledgerSpy).capacityRemaining();

            ShipmentBookingService atCapacityService =
                    new ShipmentBookingService(validator, costCalculator, ledgerSpy, idGenerator);
            Shipment shipment = new Shipment(2.0, CargoClass.STANDARD, "SEA", 9_000L);

            assertThatThrownBy(() -> atCapacityService.book(shipment))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("capacity");

            verify(ledgerSpy, never()).record(any());
            assertThat(realLedger.recordedBookings()).isEmpty();
        }
    }
}
