package com.crm.platform.delivery.config;

import com.crm.platform.delivery.provider.DeliveryProvider;
import com.crm.platform.delivery.provider.SimulatedDeliveryProvider;
import com.crm.platform.delivery.provider.SmtpDeliveryProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class DeliveryProviderConfigTest {

    private DeliveryProviderConfig config;
    private SimulatedDeliveryProvider simulatedProvider;
    private SmtpDeliveryProvider smtpProvider;

    @BeforeEach
    void setUp() {
        config = new DeliveryProviderConfig();
        simulatedProvider = mock(SimulatedDeliveryProvider.class);
        smtpProvider = mock(SmtpDeliveryProvider.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"simulated", "SIMULATED", " Simulated "})
    @DisplayName("Provider selection 'simulated' returns SimulatedDeliveryProvider")
    void testProviderSelection_Simulated(String providerType) {
        DeliveryProvider provider = config.deliveryProvider(providerType, simulatedProvider, smtpProvider);
        assertThat(provider).isSameAs(simulatedProvider);
    }

    @ParameterizedTest
    @ValueSource(strings = {"smtp", "SMTP", " Smtp "})
    @DisplayName("Provider selection 'smtp' returns SmtpDeliveryProvider")
    void testProviderSelection_Smtp(String providerType) {
        DeliveryProvider provider = config.deliveryProvider(providerType, simulatedProvider, smtpProvider);
        assertThat(provider).isSameAs(smtpProvider);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t"})
    @DisplayName("Blank provider configuration throws clear IllegalStateException")
    void testProviderSelection_BlankThrowsException(String blank) {
        assertThatThrownBy(() -> config.deliveryProvider(blank, simulatedProvider, smtpProvider))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Property 'crm.delivery.provider' must not be blank");
    }

    @Test
    @DisplayName("Null provider configuration throws clear IllegalStateException")
    void testProviderSelection_NullThrowsException() {
        assertThatThrownBy(() -> config.deliveryProvider(null, simulatedProvider, smtpProvider))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Property 'crm.delivery.provider' must not be blank");
    }

    @ParameterizedTest
    @ValueSource(strings = {"kafka", "sendgrid", "mailgun", "invalid"})
    @DisplayName("Invalid provider configuration throws clear IllegalStateException and does not silently fall back")
    void testProviderSelection_InvalidThrowsException(String invalidType) {
        assertThatThrownBy(() -> config.deliveryProvider(invalidType, simulatedProvider, smtpProvider))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid delivery provider configured: '" + invalidType + "'")
                .hasMessageContaining("Allowed values: [simulated, smtp]");
    }

    @Test
    @DisplayName("Simulated provider regression: real SimulatedDeliveryProvider instance behaves identically")
    void testSimulatedProvider_RegressionBehavior() {
        SimulatedDeliveryProvider realSimulated = new SimulatedDeliveryProvider();
        DeliveryProvider selected = config.deliveryProvider("simulated", realSimulated, smtpProvider);
        assertThat(selected).isSameAs(realSimulated);

        com.crm.platform.delivery.provider.DeliveryRequest request = new com.crm.platform.delivery.provider.DeliveryRequest(
                "IDEM-SIM-REGRESSION",
                1L,
                1L,
                "test@example.com",
                "Hello"
        );
        com.crm.platform.delivery.provider.DeliveryResult result = selected.send(request);
        assertThat(result).isNotNull();
        assertThat(realSimulated.hasProcessed("IDEM-SIM-REGRESSION")).isTrue();
    }
}
