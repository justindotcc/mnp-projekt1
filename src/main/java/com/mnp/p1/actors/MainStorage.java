package com.mnp.p1.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import java.time.Duration;
import java.util.Random;

public class MainStorage extends AbstractBehavior<MainStorage.Message> {

    public interface Message {
    }

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

    private Behavior<Message> onOrderSupply(OrderSupply msg) {
        int delay = 10 + random.nextInt(6);
        getContext().getLog().info("MainStorage: received order, will arrive in {}s", delay);
        timers.startSingleTimer(new SupplyArrived(msg.localStorage), Duration.ofSeconds(delay));
        return this;
    }

    private Behavior<Message> onSupplyArrived(SupplyArrived msg) {
        getContext().getLog().info("MainStorage: supply arrived");
        msg.localStorage.tell(new LocalStorage.Restock());
        return this;
    }
}
