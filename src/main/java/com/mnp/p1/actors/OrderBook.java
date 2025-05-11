package com.mnp.p1.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class OrderBook extends AbstractBehavior<OrderBook.Message> {

    public interface Message {
    }

    public static final class NewOrder implements Message {
        public final int orderId;

        public NewOrder(int orderId) {
            this.orderId = orderId;
        }
    }

    public static final class ProductionLineAvailable implements Message {
        public final ActorRef<ProductionLine.Message> productionLine;

        public ProductionLineAvailable(ActorRef<ProductionLine.Message> productionLine) {
            this.productionLine = productionLine;
        }
    }

    public static Behavior<Message> create() {
        return Behaviors.setup(ctx ->
                Behaviors.withTimers(timers -> new OrderBook(ctx, timers))
        );
    }

    private final TimerScheduler<Message> timers;
    private final Queue<Integer> orders = new LinkedList<>();
    private final Set<ActorRef<ProductionLine.Message>> availableLines = new HashSet<>();

    private OrderBook(ActorContext<Message> context, TimerScheduler<Message> timers) {
        super(context);
        this.timers = timers;
    }

    @Override
    public Receive<Message> createReceive() {
        return newReceiveBuilder()
                .onMessage(NewOrder.class, this::onNewOrder)
                .onMessage(ProductionLineAvailable.class, this::onProductionLineAvailable)
                .build();
    }

    private Behavior<Message> onNewOrder(NewOrder msg) {
        orders.add(msg.orderId);
        getContext().getLog().info("OrderBook: new order #{}", msg.orderId);
        assignOrders();
        return this;
    }

    private Behavior<Message> onProductionLineAvailable(ProductionLineAvailable msg) {
        getContext().getLog().info("OrderBook: production line {} is available", msg.productionLine.path().name());
        availableLines.add(msg.productionLine);
        assignOrders();
        return this;
    }

    private void assignOrders() {
        while (!orders.isEmpty() && !availableLines.isEmpty()) {
            int orderId = orders.poll();
            ActorRef<ProductionLine.Message> line = availableLines.iterator().next();
            availableLines.remove(line);
            getContext().getLog().info("OrderBook: assigning order #{} to line {}", orderId, line.path().name());
            line.tell(new ProductionLine.StartProduction(orderId));
        }
    }
}
