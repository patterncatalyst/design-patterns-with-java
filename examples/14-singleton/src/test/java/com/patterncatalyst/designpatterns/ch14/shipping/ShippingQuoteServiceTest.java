package com.patterncatalyst.designpatterns.ch14.shipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Proves the testability claim this chapter makes against
 * {@link WarehousePricingJob}: because {@link ShippingQuoteService} takes
 * its {@link RateLookup} through the constructor, a test substitutes a
 * Mockito double for it with no production code involved at all.
 */
@ExtendWith(MockitoExtension.class)
class ShippingQuoteServiceTest {

    @Mock
    private RateLookup rateLookup;

    @Test
    void quoteMultipliesTheInjectedBaseRateByWeight() {
        when(rateLookup.baseRateFor("GROUND")).thenReturn(Money.ofCents(200));
        ShippingQuoteService service = new ShippingQuoteService(rateLookup);

        Money quote = service.quote("GROUND", 5.0);

        assertThat(quote).isEqualTo(Money.ofCents(1_000));
        verify(rateLookup).baseRateFor("GROUND");
    }

    @Test
    void quoteRejectsNegativeWeight() {
        ShippingQuoteService service = new ShippingQuoteService(rateLookup);

        assertThatThrownBy(() -> service.quote("GROUND", -1.0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructorRejectsANullRateLookup() {
        assertThatThrownBy(() -> new ShippingQuoteService(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void sameServiceClassWorksUnchangedAgainstTheEnumSingleton() {
        ShippingQuoteService service = new ShippingQuoteService(CarrierRateTable.INSTANCE);

        assertThat(service.quote("GROUND", 10.0)).isEqualTo(Money.ofCents(1_800));
    }

    @Test
    void sameServiceClassWorksUnchangedAgainstTheHolderSingleton() {
        ShippingQuoteService service = new ShippingQuoteService(HolderCarrierRateTable.getInstance());

        assertThat(service.quote("AIR", 2.0)).isEqualTo(Money.ofCents(1_280));
    }

    @Test
    void sameServiceClassWorksUnchangedAgainstAHandWrittenFake() {
        RateLookup fakeRate = carrierCode -> Money.ofCents(1);
        ShippingQuoteService service = new ShippingQuoteService(fakeRate);

        assertThat(service.quote("ANYTHING", 50.0)).isEqualTo(Money.ofCents(50));
    }
}
