package com.crm.platform.frontend.client;

/**
 * Authoritative backend REST URI constants for the Enterprise AI-CRM Platform.
 */
public final class ApiEndpointConstants {

    private ApiEndpointConstants() {
    }

    // Auth
    public static final String AUTH_LOGIN = "/api/v1/auth/login";

    // Customers
    public static final String CUSTOMERS = "/api/v1/customers";
    public static final String CUSTOMER_BY_ID = "/api/v1/customers/{id}";

    // Segments
    public static final String SEGMENTS = "/api/v1/segments";
    public static final String SEGMENT_BY_ID = "/api/v1/segments/{id}";
    public static final String SEGMENT_PREVIEW = "/api/v1/segments/{id}/preview";
    public static final String SEGMENT_MEMBERS = "/api/v1/segments/{id}/members";

    // Campaigns & Delivery
    public static final String CAMPAIGNS = "/api/v1/campaigns";
    public static final String CAMPAIGN_BY_ID = "/api/v1/campaigns/{id}";
    public static final String CAMPAIGN_LAUNCH = "/api/v1/campaigns/{id}/launch";
    public static final String CAMPAIGN_DELIVERY_SUMMARY = "/api/v1/campaigns/{id}/delivery-summary";
    public static final String DELIVERIES_BY_CAMPAIGN = "/api/v1/deliveries/campaigns/{id}";

    // Bulk Ingestion
    public static final String UPLOADS_BULK = "/api/v1/uploads/bulk";
    public static final String UPLOADS_HISTORY = "/api/v1/uploads/history";

    // Reports
    public static final String REPORTS_CUSTOMER_OVERVIEW = "/api/v1/reports/customers/overview";
    public static final String REPORTS_CAMPAIGN_BY_ID = "/api/v1/reports/campaigns/{id}";
    public static final String REPORTS_CAMPAIGN_AI_SUMMARY = "/api/v1/reports/campaigns/{id}/ai-summary";

    // AI
    public static final String AI_GENERATE_RULES = "/api/v1/ai/segments/generate-rules";
    public static final String AI_AUDITS = "/api/v1/ai/segments/audits";

    // User Administration
    public static final String USERS = "/api/v1/users";
    public static final String USER_BY_ID = "/api/v1/users/{id}";
    public static final String USER_ROLE = "/api/v1/users/{id}/role";
    public static final String USER_STATUS = "/api/v1/users/{id}/status";
    public static final String USER_PASSWORD = "/api/v1/users/{id}/password";
}
