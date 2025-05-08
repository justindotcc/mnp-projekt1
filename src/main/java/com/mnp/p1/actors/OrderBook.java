package com.mnp.p1.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import java.util.LinkedList;
import java.util.Queue;

public class OrderBook extends AbstractBehavior<OrderBook.Command> {

    public interface Command {
    }

    public record NewOrder(int orderId) implements Command {
    }

    public record AssignOrder(ActorRef<ProductionLine.Command> productionLine) implements Command {
    }

    private final Queue<Integer> orders = new LinkedList<>();

    public static Behavior<Command> create() {
        return Behaviors.setup(OrderBook::new);
    }

    private OrderBook(ActorContext<Command> context) {
        super(context);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(NewOrder.class, this::onNewOrder)
                .onMessage(AssignOrder.class, this::onAssignOrder)
                .build();
    }


    private Behavior<Command> onNewOrder(NewOrder command) {
        orders.add(command.orderId);
        getContext().getLog().info("New order added: {}", command.orderId);
        return this;
    }

    private Behavior<Command> onAssignOrder(AssignOrder command) {
        if (!orders.isEmpty()) {
            int orderId = orders.poll();
            command.productionLine.tell(new ProductionLine.StartProduction(orderId));
            getContext().getLog().info("Order {} assigned to production line.", orderId);
        } else {
            getContext().scheduleOnce(
                    java.time.Duration.ofSeconds(10),
                    getContext().getSelf(),
                    command
            );
            getContext().getLog().info("No orders available. Retrying in 10 seconds.");
        }
        return this;
    }

    /*
     * • Auftragsbuch (OrderBook)
     * – Neuer Auftrag: Ein neuer Auftrag wird in das Auftragsbuch übernommen, dies kann einfach eine Nummer sein.
     * – Auftrag zuweisen: Der älteste nicht bearbeitete Auftrag wird der nächsten freien Produktionsstraße
     *  zugewiesen. Sollte keine vorliegen, wird 10 Sekunden gewartet und ein neuer Zuweisungsversuch unternommen.
     */

}
