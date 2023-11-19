package course.concurrency.m3_shared.immutable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static course.concurrency.m3_shared.immutable.Order.Status.IN_PROGRESS;
import static course.concurrency.m3_shared.immutable.Order.Status.NEW;

public final class Order {

    public enum Status {NEW, IN_PROGRESS, DELIVERED}

    private final Long id;
    private final List<Item> items;
    private final PaymentInfo paymentInfo;
    private final boolean isPacked;
    private final Status status;

    private Order(Long id, List<Item> items, PaymentInfo paymentInfo, boolean isPacked, Status status) {
        this.id = id;
        this.items = new ArrayList<>(items);
        this.paymentInfo = paymentInfo;
        this.isPacked = isPacked;
        this.status = status;
    }

    public Order(Long id, List<Item> items) {
        this(id, items, null, false, NEW);
    }

    public boolean checkStatus() {
        return items != null && !items.isEmpty() && paymentInfo != null && isPacked;
    }

    public Long getId() {
        return id;
    }

    public List<Item> getItems() {
        return Collections.unmodifiableList(items);
    }

    public PaymentInfo getPaymentInfo() {
        return paymentInfo;
    }

    public Order withPaymentInfo(PaymentInfo paymentInfo) {
        return new Order(this.id, this.items, paymentInfo, this.isPacked, IN_PROGRESS);
    }

    public boolean isPacked() {
        return isPacked;
    }

    public Order packed() {
        return new Order(this.id, this.items, this.paymentInfo, true, IN_PROGRESS);
    }

    public Status getStatus() {
        return status;
    }

    public Order withStatus(Status status) {
        return new Order(this.id, this.items, this.paymentInfo, this.isPacked, status);
    }
}
