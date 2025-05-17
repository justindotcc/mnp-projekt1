/**
 * Isabelle Bille 156252
 * Justin Gottwald 201237
 * Ilia Orlov 251287
 */

package com.mnp.p1.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import java.time.Duration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * Class to manage the orders and the availability of the ProductLines.
 * Wait (in necessary) if there is no available ProductLine.
 */

public class OrderBook extends AbstractBehavior<OrderBook.Message> {

    public interface Message {
    }

    //New order in the order book.
    public static final class NewOrder implements Message {
        public final int orderId;

        public NewOrder(int orderId) {
            this.orderId = orderId;
        }
    }

    //Message from the available ProductLine
    public static final class ProductionLineAvailable implements Message {
        public final ActorRef<ProductionLine.Message> productionLine;

        public ProductionLineAvailable(ActorRef<ProductionLine.Message> productionLine) {
            this.productionLine = productionLine;
        }
    }

    //Timer-message for another assignment.
    private static final class RetryAssignment implements Message {
    }

    //Create a new OderBook-actor.
    public static Behavior<Message> create() {
        return Behaviors.setup(ctx ->
                Behaviors.withTimers(timers -> new OrderBook(ctx, timers))
        );
    }

    private final TimerScheduler<Message> timers;

    //Queue for open orders.
    private final Queue<Integer> orders = new LinkedList<>();

    //Available ProductLines.
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
                .onMessage(RetryAssignment.class, msg -> {
                    assignOrders();
                    return this;
                })
                .build();
    }

    //Processing a new order.
    private Behavior<Message> onNewOrder(NewOrder msg) {
        orders.add(msg.orderId);
        getContext().getLog().info("OrderBook: new order #{}", msg.orderId);
        assignOrders();
        return this;
    }

    //Processing a message that one of the ProductLines is now available.
    private Behavior<Message> onProductionLineAvailable(ProductionLineAvailable msg) {
        getContext().getLog().info("OrderBook: production line {} is available", msg.productionLine.path().name());
        availableLines.add(msg.productionLine);
        assignOrders();
        return this;
    }

    /**
     * Attempts to distribute outstanding orders to available production lines.
     * Set a timer if there is no available ProductLine at the moment.
     */

    private void assignOrders() {

        //Distribute orders as long as there are orders.
        while (!orders.isEmpty() && !availableLines.isEmpty()) {
            int orderId = orders.poll();
            ActorRef<ProductionLine.Message> line = availableLines.iterator().next();
            availableLines.remove(line);

            //Send order to the ProductLine.
            getContext().getLog().info("OrderBook: assigning order #{} to line {}", orderId, line.path().name());
            line.tell(new ProductionLine.StartProduction(orderId));
        }

        //In case there are orders available and not distributed, try again after ten seconds.
        if (!orders.isEmpty() && availableLines.isEmpty()) {
            getContext().getLog().info("OrderBook: No available lines, retrying in 10s...");
            timers.startSingleTimer(new RetryAssignment(), Duration.ofSeconds(10));
        }
    }
}
