package com.mnp.p1.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import java.util.*;
import java.util.stream.Collectors;

public class LocalStorage extends AbstractBehavior<LocalStorage.Message> {

    public interface Message {
    }

    public static record FetchRequests(int orderId, ActorRef<Worker.Message> worker,
                                       ActorRef<ProductionLine.Message> productionLine) implements Message {
    }

    public static final class Restock implements Message {
    }

    private final TimerScheduler<Message> timers;
    private final ActorRef<MainStorage.Message> mainStorage;
    private final Map<String, Integer> stock = new HashMap<>();
    private final Queue<FetchRequests> pending = new LinkedList<>();
    private final List<String> TYPES = List.of(
            "Ledersitze", "Klimaautomatik", "Elektrische Fensterheber", "Automatikgetriebe"
    );

    public LocalStorage(ActorContext<Message> context, TimerScheduler<Message> timers, ActorRef<MainStorage.Message> mainStorage) {
        super(context);
        this.timers = timers;
        this.mainStorage = mainStorage;
        TYPES.forEach(t -> stock.put(t, 4));
    }

    public static Behavior<Message> create(ActorRef<MainStorage.Message> mainStorage) {
        return Behaviors.setup(ctx ->
                Behaviors.withTimers(timers -> new LocalStorage(ctx, timers, mainStorage))
        );
    }

    @Override
    public Receive<Message> createReceive() {
        return newReceiveBuilder()
                .onMessage(FetchRequests.class, this::onFetchRequests)
                .onMessage(Restock.class, this::onRestock)
                .build();
    }

    private Behavior<Message> onFetchRequests(FetchRequests msg) {
        List<String> shuffled = new ArrayList<>(TYPES);
        Collections.shuffle(shuffled);
        List<String> chosen = shuffled.subList(0, 2);
        List<String> missing = chosen.stream()
                .filter(req -> stock.get(req) <= 0)
                .collect(Collectors.toList());
        if (missing.isEmpty()) {
            chosen.forEach(req -> stock.put(req, stock.get(req) - 1));
            getContext().getLog().info("LocalStorage: supplied {} for order #{}", chosen, msg.orderId);
            msg.worker.tell(new Worker.SpecialRequestsFetched(msg.orderId, msg.productionLine, chosen));
        } else {
            getContext().getLog().info("LocalStorage: {} missing, ordering from main storage", missing);
            pending.add(msg);
            mainStorage.tell(new MainStorage.OrderSupply(getContext().getSelf()));
        }
        return this;
    }

    private Behavior<Message> onRestock(Restock msg) {
        TYPES.forEach(t -> stock.put(t, stock.get(t) + 3));
        getContext().getLog().info("LocalStorage: restocked all items");
        while (!pending.isEmpty()) {
            onFetchRequests(pending.poll());
        }
        return this;
    }
}
