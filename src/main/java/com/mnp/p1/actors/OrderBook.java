package com.mnp.p1.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/*
 * Auftragsbuch (OrderBook)
 * – Neuer Auftrag: Ein neuer Auftrag wird in das Auftragsbuch übernommen, dies kann einfach eine Nummer sein.
 * – Auftrag zuweisen: Der älteste nicht bearbeitete Auftrag wird der nächsten freien Produktionsstraße
 *  zugewiesen. Sollte keine vorliegen, wird 10 Sekunden gewartet und ein neuer Zuweisungsversuch unternommen.
 */

public class OrderBook extends AbstractBehavior<OrderBook.Message> {

    public interface Message {
    }

    public static class Create implements Message {
    }

    public record NewOrder(int orderId) implements Message {
    }

    public record AssignOrder() implements Message {
    }

    private final Queue<Integer> orders = new LinkedList<>();
    private final List<ActorRef<ProductionLine.Message>> productionLines;

    public static Behavior<Message> create(List<ActorRef<ProductionLine.Message>> productionLines) {
        return Behaviors.setup(context -> Behaviors.withTimers(timers -> new OrderBook(context, timers, productionLines)));

    }

    private final TimerScheduler<Message> timers;

    private OrderBook(ActorContext<Message> context, TimerScheduler<Message> timers, List<ActorRef<ProductionLine.Message>> productionLines) {
        super(context);
        this.productionLines = productionLines;
        this.timers = timers;
        getContext().getLog().info("OrderBook actor created.");
        getContext().getSelf().tell(new NewOrder(1));
        getContext().getSelf().tell(new AssignOrder());
    }

    @Override
    public Receive<Message> createReceive() {
        return newReceiveBuilder()
                .onMessage(Create.class, this::onCreate)
                .onMessage(NewOrder.class, this::onNewOrder)
                .onMessage(AssignOrder.class, this::onAssignOrder)
                .build();
    }

    private Behavior<Message> onCreate(Create message) {
        return this;
    }

    private Behavior<Message> onNewOrder(NewOrder message) {
        orders.add(message.orderId);
        this.getContext().getSelf().tell(new AssignOrder());

        getContext().getLog().info("New order added: {}", message.orderId);

        this.timers.startSingleTimer("NewOrderTimer",
                new NewOrder(message.orderId + 1),
                java.time.Duration.ofSeconds(15)
        );
        return this;
    }

    private Behavior<Message> onAssignOrder(AssignOrder command) {

        if (!orders.isEmpty()) {
            int orderId = orders.poll();
            getContext().getLog().info("Assigning order: {}", orderId);
        }


        return this;
    }


}