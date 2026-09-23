package com.crm.platform.reporting.dto;

import java.math.BigDecimal;
import java.util.List;

public class CustomerOverviewReportResponse {

    private long totalActiveCustomers;
    private BigDecimal grossCustomerSpend;
    private BigDecimal averageSpendPerCustomer;
    private long totalVisits;
    private double averageVisitsPerCustomer;
    private List<LocationMetricDto> topLocations;

    public CustomerOverviewReportResponse() {
    }

    public CustomerOverviewReportResponse(long totalActiveCustomers, BigDecimal grossCustomerSpend,
                                          BigDecimal averageSpendPerCustomer, long totalVisits,
                                          double averageVisitsPerCustomer, List<LocationMetricDto> topLocations) {
        this.totalActiveCustomers = totalActiveCustomers;
        this.grossCustomerSpend = grossCustomerSpend;
        this.averageSpendPerCustomer = averageSpendPerCustomer;
        this.totalVisits = totalVisits;
        this.averageVisitsPerCustomer = averageVisitsPerCustomer;
        this.topLocations = topLocations;
    }

    public static class LocationMetricDto {
        private String city;
        private long customerCount;

        public LocationMetricDto() {}
        public LocationMetricDto(String city, long customerCount) {
            this.city = city;
            this.customerCount = customerCount;
        }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public long getCustomerCount() { return customerCount; }
        public void setCustomerCount(long customerCount) { this.customerCount = customerCount; }
    }

    public long getTotalActiveCustomers() { return totalActiveCustomers; }
    public void setTotalActiveCustomers(long totalActiveCustomers) { this.totalActiveCustomers = totalActiveCustomers; }
    public BigDecimal getGrossCustomerSpend() { return grossCustomerSpend; }
    public void setGrossCustomerSpend(BigDecimal grossCustomerSpend) { this.grossCustomerSpend = grossCustomerSpend; }
    public BigDecimal getAverageSpendPerCustomer() { return averageSpendPerCustomer; }
    public void setAverageSpendPerCustomer(BigDecimal averageSpendPerCustomer) { this.averageSpendPerCustomer = averageSpendPerCustomer; }
    public long getTotalVisits() { return totalVisits; }
    public void setTotalVisits(long totalVisits) { this.totalVisits = totalVisits; }
    public double getAverageVisitsPerCustomer() { return averageVisitsPerCustomer; }
    public void setAverageVisitsPerCustomer(double averageVisitsPerCustomer) { this.averageVisitsPerCustomer = averageVisitsPerCustomer; }
    public List<LocationMetricDto> getTopLocations() { return topLocations; }
    public void setTopLocations(List<LocationMetricDto> topLocations) { this.topLocations = topLocations; }
}
