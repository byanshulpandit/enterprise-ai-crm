package com.crm.platform.delivery.config;

import com.crm.platform.delivery.provider.DeliveryProvider;
import com.crm.platform.delivery.provider.SimulatedDeliveryProvider;
import com.crm.platform.delivery.provider.SmtpDeliveryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DeliveryProviderConfig {

    private static final Logger log = LoggerFactory.getLogger(DeliveryProviderConfig.class);

    @Bean
    @Primary
    public DeliveryProvider deliveryProvider(
            @Value("${crm.delivery.provider:simulated}") String providerType,
            SimulatedDeliveryProvider simulatedProvider,
            SmtpDeliveryProvider smtpProvider) {

        if (providerType == null || providerType.trim().isEmpty()) {
            throw new IllegalStateException("Property 'crm.delivery.provider' must not be blank. Allowed values: [simulated, smtp]");
        }

        String normalized = providerType.trim().toLowerCase();
        return switch (normalized) {
            case "simulated" -> {
                log.info("Active DeliveryProvider configured: SimulatedDeliveryProvider");
                yield simulatedProvider;
            }
            case "smtp" -> {
                log.info("Active DeliveryProvider configured: SmtpDeliveryProvider");
                yield smtpProvider;
            }
            default -> throw new IllegalStateException(
                    "Invalid delivery provider configured: '" + providerType + "'. Allowed values: [simulated, smtp]"
            );
        };
    }
}
