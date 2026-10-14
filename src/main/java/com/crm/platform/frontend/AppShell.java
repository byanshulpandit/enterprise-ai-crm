package com.crm.platform.frontend;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.theme.Theme;

/**
 * Vaadin AppShell configuration for Enterprise AI-CRM Platform.
 * Configures the charcoal/graphite + warm amber crm-theme.
 */
@Theme(value = "crm-theme")
@Push(PushMode.DISABLED)
public class AppShell implements AppShellConfigurator {
}
