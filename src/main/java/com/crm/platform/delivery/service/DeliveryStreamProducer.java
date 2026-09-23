package com.crm.platform.delivery.service;

import java.util.List;

public interface DeliveryStreamProducer {

    void enqueueDelivery(Long campaignId, Long customerId);

    void enqueueDeliveries(Long campaignId, List<Long> customerIds);
}
