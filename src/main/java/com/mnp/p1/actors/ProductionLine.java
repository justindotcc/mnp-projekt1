package com.mnp.p1.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import java.util.List;

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

    public record Create(List<ActorRef<Worker.Message>> workers) implements Message {
    }

    public record StartProduction(int orderId) implements Message {
        // orderId is the ID of the order to be produced
    }

    public record StopProduction(int orderId) implements Message {
        // orderId is the ID of the order to be stopped
    }

    public static Behavior<Message> create(List<ActorRef<Worker.Message>> workers, ActorRef<LocalStorage.Message> localStorage) {
        return Behaviors.setup(context -> Behaviors.withTimers(timers -> new ProductionLine(context, timers, workers, localStorage)));
    }

    private final TimerScheduler<Message> timers;
    private final List<ActorRef<Worker.Message>> workers;
    private boolean isFree = true;

    private ProductionLine(ActorContext<Message> context, TimerScheduler<Message> timers, List<ActorRef<Worker.Message>> workers, ActorRef<LocalStorage.Message> localStorage) {
        super(context);
        this.timers = timers;
        this.workers = workers;
    }

    @Override
    public Receive<Message> createReceive() {
        return newReceiveBuilder()
                .onMessage(Create.class, this::onCreate)
                .onMessage(StartProduction.class, this::onStartProduction)
                .onMessage(StopProduction.class, this::onStopProduction)
                .build();
    }

    private Behavior<Message> onCreate(Create command) {
        getContext().getLog().info("ProductionLine actor created.");
        return this;
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
