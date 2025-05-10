package com.mnp.p1.actors;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

/*
 * • Produktionsstraße (ProductionLine)
 * – Produziere Karosserie: Die Karosserie wird durch den zugewiesenen Arbeiter in [5,10] Sekunden erstellt.
 * – Verbaue Spezialwünsche: Nachdem die Karosserie erstellt wurde,
 *   werden die 2 Spezialwünsche aus dem Lager geholt und eingebaut.
 * – Fertigstellung: Die Straße und die / der Arbeiter*in werden wieder freigegeben.
 */

public class ProductionLine extends AbstractBehavior<ProductionLine.Message> {

    public interface Message {
    }

    public record StartProduction(int orderId) implements Message {
        // orderId is the ID of the order to be produced
    }

    public record StopProduction(int orderId) implements Message {
        // orderId is the ID of the order to be stopped
    }

    public static Behavior<Message> create() {
        return Behaviors.setup(context -> Behaviors.withTimers(timers -> new ProductionLine(context, timers)));
    }

    private final TimerScheduler<Message> timers;

    private ProductionLine(ActorContext<Message> context, TimerScheduler<Message> timers) {
        super(context);
        this.timers = timers;
    }

    @Override
    public Receive<Message> createReceive() {
        return newReceiveBuilder()
                .onMessage(StartProduction.class, this::onStartProduction)
                .onMessage(StopProduction.class, this::onStopProduction)
                .build();
    }

    private Behavior<Message> onStartProduction(StartProduction command) {
        int orderId = command.orderId;
        getContext().getLog().info("Starting production for order: {}", orderId);

        int productionTime = (int) (Math.random() * 6 + 5);

        timers.startSingleTimer(new StopProduction(orderId), java.time.Duration.ofSeconds(productionTime));

        return this;
    }

    private Behavior<Message> onStopProduction(StopProduction command) {
        int orderId = command.orderId;
        getContext().getLog().info("Stopping production for order: {}", orderId);

        return this;
    }
}
