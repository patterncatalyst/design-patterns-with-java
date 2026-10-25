package com.patterncatalyst.designpatterns.ch04.notification;

/**
 * The capability of turning a template ID and an event into message text. A
 * channel that cannot do this, such as a carrier that only ever accepts
 * plain text, never implements this interface.
 */
public interface TemplateRenderer {
    String render(String templateId, OrderEvent event);
}
