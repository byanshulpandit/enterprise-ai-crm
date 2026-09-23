package com.crm.platform.campaign.util;

import com.crm.platform.customer.entity.Customer;

public final class MessageTemplateRenderer {

    private MessageTemplateRenderer() {
    }

    public static String render(String template, Customer customer) {
        if (template == null) {
            return "";
        }
        if (customer == null) {
            return template;
        }

        String rendered = template;
        rendered = rendered.replace("{{firstName}}", customer.getFirstName() != null ? customer.getFirstName() : "");
        rendered = rendered.replace("{{lastName}}", customer.getLastName() != null ? customer.getLastName() : "");
        rendered = rendered.replace("{{email}}", customer.getEmail() != null ? customer.getEmail() : "");
        rendered = rendered.replace("{{phone}}", customer.getPhone() != null ? customer.getPhone() : "");
        rendered = rendered.replace("{{city}}", customer.getCity() != null ? customer.getCity() : "");
        rendered = rendered.replace("{{country}}", customer.getCountry() != null ? customer.getCountry() : "");
        rendered = rendered.replace("{{totalSpend}}", customer.getTotalSpend() != null ? customer.getTotalSpend().toString() : "0.00");
        rendered = rendered.replace("{{visitCount}}", customer.getVisitCount() != null ? customer.getVisitCount().toString() : "0");
        return rendered;
    }
}
