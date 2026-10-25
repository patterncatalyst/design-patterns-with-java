package com.patterncatalyst.designpatterns.ch04.notification;

/**
 * The composition root: the one place in this example that constructs a
 * concrete {@link EmailGateway} or {@link SmsGateway}. Every other class in
 * this package depends only on {@link MessageSender}, {@link
 * TemplateRenderer}, or {@link DeliveryStatusProvider}; no DI container, no
 * annotation, no classpath scan wires this graph, just five constructor
 * calls read top to bottom.
 */
public final class Main {

    private Main() {}

    public static void main(String[] args) {
        EmailGateway emailGateway = new EmailGateway();
        SmsGateway smsGateway = new SmsGateway();

        OrderNotificationPolicy smsNotifier = new OrderNotificationPolicy(smsGateway);
        TemplatedOrderNotifier emailNotifier = new TemplatedOrderNotifier(emailGateway, emailGateway, "order-delayed");
        DeliveryAuditor auditor = new DeliveryAuditor(emailGateway);

        OrderEvent smsEvent = new OrderEvent("ORD-1001", "+1-555-0100", "Your order has shipped.");
        DispatchResult smsResult = smsNotifier.notifyCustomer(smsEvent);
        System.out.println("SMS dispatch (" + smsResult.dispatchId() + "): " + smsEvent.messageBody());

        OrderEvent emailEvent = new OrderEvent(
                "ORD-1002", "jordan@example.com", "it left the warehouse this morning");
        DispatchResult emailResult = emailNotifier.notifyCustomer(emailEvent);
        System.out.println("Email dispatch (" + emailResult.dispatchId() + "): rendered and sent");

        emailGateway.markDelivered(emailResult.dispatchId());
        System.out.println("Audit for " + emailResult.dispatchId() + ": " + auditor.auditDelivery(emailResult.dispatchId()));

        System.out.println();
        System.out.println("== Java 25: a sealed outcome that keeps the rejection reason ==");
        OrderEvent noContactEvent = new OrderEvent("ORD-1003", "", "Your order is on its way.");
        NotificationOutcome outcome = smsNotifier.tryNotifyCustomer(noContactEvent);
        String outcomeDescription = switch (outcome) {
            case NotificationOutcome.Notified notified -> "Notified: " + notified.result();
            case NotificationOutcome.Rejected rejected -> "Rejected: " + rejected.reason();
        };
        System.out.println(outcomeDescription);

        System.out.println();
        System.out.println("== Classic Java comparison: Optional<DispatchResult> loses the reason ==");
        System.out.println("Rejected event present in Optional? " + smsNotifier.tryNotifyCustomerOptional(noContactEvent).isPresent());
    }
}
