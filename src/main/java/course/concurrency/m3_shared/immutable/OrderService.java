package course.concurrency.m3_shared.immutable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static course.concurrency.m3_shared.immutable.Order.Status.DELIVERED;

public class OrderService {

    private final Map<Long, Order> currentOrders;
    private final AtomicLong nextId;

    public OrderService() {
        currentOrders = new ConcurrentHashMap<>();
        nextId = new AtomicLong();
    }

    private long nextId() {
        return nextId.getAndIncrement();
    }

    public long createOrder(List<Item> items) {
        return currentOrders.compute(nextId(), (id, order) -> new Order(id, items))
                .getId();
    }

    public void updatePaymentInfo(long orderId, PaymentInfo paymentInfo) {
        Order updatedOrder =
                currentOrders.compute(orderId, (id, order) -> order.withPaymentInfo(paymentInfo));

        if (updatedOrder.checkStatus()) {
            deliver(updatedOrder);
        }
    }

    public void setPacked(long orderId) {
        Order updatedOrder = currentOrders.compute(orderId, (id, order) -> order.packed());

        if (updatedOrder.checkStatus()) {
            deliver(updatedOrder);
        }
    }

    private void deliver(Order orderToDeliver) {
        currentOrders.compute(orderToDeliver.getId(), (id, order) -> order.withStatus(DELIVERED));
    }

    public boolean isDelivered(long orderId) {
        return currentOrders.get(orderId)
                .getStatus()
                .equals(DELIVERED);
    }
}
