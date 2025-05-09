package com.mnp.p1.actors;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

public class ProductionLine extends AbstractBehavior<ProductionLine.Command> {

    public interface Command {
    }

    public record StartProduction(int orderId) implements Command {
        // orderId is the ID of the order to be produced
    }

    public record StopProduction(int orderId) implements Command {
        // orderId is the ID of the order to be stopped
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(context -> Behaviors.withTimers(timers -> new ProductionLine(context, timers)));
    }

    private final TimerScheduler<Command> timers;

    private ProductionLine(ActorContext<Command> context, TimerScheduler<Command> timers) {
        super(context);
        this.timers = timers;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(StartProduction.class, this::onStartProduction)
                .onMessage(StopProduction.class, this::onStopProduction)
                .build();
    }

    private Behavior<Command> onStartProduction(StartProduction command) {
        int orderId = command.orderId;
        getContext().getLog().info("Starting production for order: {}", orderId);

        int productionTime = (int) (Math.random() * 6 + 5);

        timers.startSingleTimer(new StopProduction(orderId), java.time.Duration.ofSeconds(productionTime));

        return this;
    }

    private Behavior<Command> onStopProduction(StopProduction command) {
        int orderId = command.orderId;
        getContext().getLog().info("Stopping production for order: {}", orderId);

        return this;
    }

    /*
     * • Produktionsstraße (ProductionLine)
     * – Produziere Karosserie: Die Karosserie wird durch den zugewiesenen Arbeiter in [5,10] Sekunden erstellt.
     * – Verbaue Spezialwünsche: Nachdem die Karosserie erstellt wurde,
     *   werden die 2 Spezialwünsche aus dem Lager geholt und eingebaut.
     * – Fertigstellung: Die Straße und die / der Arbeiter*in werden wieder freigegeben.
     */

}
