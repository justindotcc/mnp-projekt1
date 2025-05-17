/**
 * Isabelle Bille 156252
 * Justin Gottwald 201237
 * Ilia Orlov
 */

package com.mnp.p1.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import java.time.Duration;
import java.util.Random;

/**
 * Main storage.
 * Delivers orders to the LocalStorage.
 */

public class MainStorage extends AbstractBehavior<MainStorage.Message> {

    public interface Message {
    }

    //Order from the localStorage.
    public static record OrderSupply(ActorRef<LocalStorage.Message> localStorage) implements Message {
    }

    private static final class SupplyArrived implements Message {
        public final ActorRef<LocalStorage.Message> localStorage;

        public SupplyArrived(ActorRef<LocalStorage.Message> localStorage) {
            this.localStorage = localStorage;
        }
    }

    private final TimerScheduler<Message> timers;
    private final Random random = new Random();

    private MainStorage(ActorContext<Message> context, TimerScheduler<Message> timers) {
        super(context);
        this.timers = timers;
    }

    //Create the MainStorage-actor.
    public static Behavior<Message> create() {
        return Behaviors.setup(ctx ->
                Behaviors.withTimers(timers -> new MainStorage(ctx, timers))
        );
    }

    @Override
    public Receive<Message> createReceive() {
        return newReceiveBuilder()
                .onMessage(OrderSupply.class, this::onOrderSupply)
                .onMessage(SupplyArrived.class, this::onSupplyArrived)
                .build();
    }

    /**
     * Processes a replenishment order from the local warehouse.
     * Start a timer to simulate the time of delivery between 10 and 15 seconds.
     */

    private Behavior<Message> onOrderSupply(OrderSupply msg) {

        //Random delay in delivery between 10 and 15 seconds.
        int delay = 10 + random.nextInt(6);

        //Set timer to send SupplyArrived to the actor.
        getContext().getLog().info("MainStorage: received order, will arrive in {}s", delay);
        timers.startSingleTimer(new SupplyArrived(msg.localStorage), Duration.ofSeconds(delay));
        return this;
    }

    /**
     * Processes the arrival of the subsequent delivery.
     * Inform the localStorage to refill the storage.
     */

    private Behavior<Message> onSupplyArrived(SupplyArrived msg) {
        getContext().getLog().info("MainStorage: supply arrived");
        msg.localStorage.tell(new LocalStorage.Restock());
        return this;
    }
}
