package com.crm.platform.delivery.service;

public interface DeliveryWorkerService {

    boolean processDelivery(Long campaignId, Long customerId);
}
