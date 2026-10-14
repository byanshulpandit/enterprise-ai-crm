package com.crm.platform.frontend.util;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

/**
 * Enterprise toast notifications utility.
 */
public final class NotificationUtils {

    private NotificationUtils() {
    }

    public static void showSuccess(String message) {
        Notification notification = Notification.show(message, 4000, Notification.Position.TOP_END);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    public static void showWarning(String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.TOP_END);
        notification.addThemeVariants(NotificationVariant.LUMO_WARNING);
    }

    public static void showError(String message) {
        Notification notification = Notification.show(message, 6000, Notification.Position.TOP_END);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    public static void showInfo(String message) {
        Notification notification = Notification.show(message, 3500, Notification.Position.TOP_END);
        notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
    }
}
