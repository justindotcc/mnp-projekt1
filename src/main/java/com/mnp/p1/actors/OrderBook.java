package com.mnp.p1.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class OrderBook extends AbstractBehavior<OrderBook.Message> {

    public interface Message {
    }

    public static final class NewOrder implements Message {
        public final int orderId;

        public NewOrder(int orderId) {
            this.orderId = orderId;
        }
    }

    public static final class AssignOrder implements Message {
    }

    public static final class ProductionLineAvailable implements Message {
        public final ActorRef<ProductionLine.Message> productionLine;

        public ProductionLineAvailable(ActorRef<ProductionLine.Message> productionLine) {
            this.productionLine = productionLine;
        }
    }

    private final Queue<Integer> orders = new LinkedList<>();
    private final List<ActorRef<ProductionLine.Message>> productionLines;

    public static Behavior<Message> create(List<ActorRef<ProductionLine.Message>> productionLines) {
        return Behaviors.setup(ctx ->
                Behaviors.withTimers(timers -> new OrderBook(ctx, timers, productionLines))
        );
    }

    private final TimerScheduler<Message> timers;

    private OrderBook(ActorContext<Message> context, TimerScheduler<Message> timers, List<ActorRef<ProductionLine.Message>> productionLines) {
        super(context);
        this.timers = timers;
        this.productionLines = productionLines;
    }

    @Override
    public Receive<Message> createReceive() {
        return newReceiveBuilder()
                .onMessage(NewOrder.class, this::onNewOrder)
                .onMessage(ProductionLineAvailable.class, this::onProductionLineAvailable)
                .build();
    }

    private Behavior<Message> onNewOrder(NewOrder message) {
        orders.add(message.orderId);
        getContext().getLog().info("New order added: {}", message.orderId);
        assignNextOrder();
        return this;
    }

    private Behavior<Message> onProductionLineAvailable(ProductionLineAvailable message) {
        assignToLine(message.productionLine);
        return this;
    }

    private void assignNextOrder() {
        if (!orders.isEmpty()) {
            for (ActorRef<ProductionLine.Message> line : productionLines) {
                assignToLine(line);
            }
        }
    }

    private void assignToLine(ActorRef<ProductionLine.Message> line) {
        if (!orders.isEmpty()) {
            int nextOrder = orders.poll();
            line.tell(new ProductionLine.StartProduction(nextOrder));
            getContext().getLog().info("Assigned order #{} to line {}", nextOrder, line.path().name());
        }
    }
}