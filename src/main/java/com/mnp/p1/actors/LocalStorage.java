/**
 * Isabelle Bille 156252
 * Justin Gottwald 201237
 * Ilia Orlov 251287
 */

package com.mnp.p1.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Local storage to control the stock due to special wishes.
 * LocalStorage delivers requested parts or asks for another delivery from the MainStorage.
 */

public class LocalStorage extends AbstractBehavior<LocalStorage.Message> {

    public interface Message {
    }

    //Request from a worker to provide two special requests for a job.
    public static record FetchRequests(int orderId, ActorRef<Worker.Message> worker,
                                       ActorRef<ProductionLine.Message> productionLine) implements Message {
    }

    //Message from MainStorage that stock has been replenished.
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

    /**
     * Processes a request for two special requests.
     * Outputs available parts immediately or requests replenishment from the main warehouse if necessary.
     */

    private Behavior<Message> onFetchRequests(FetchRequests msg) {

        //Choose between two different special requests.
        List<String> shuffled = new ArrayList<>(TYPES);
        Collections.shuffle(shuffled);
        List<String> chosen = shuffled.subList(0, 2);

        //Check if one of the chosen parts is not available.
        List<String> missing = chosen.stream()
                .filter(req -> stock.get(req) <= 0)
                .collect(Collectors.toList());
        if (missing.isEmpty()) {

            //All requested parts are in the storage. -> Output to worker.
            chosen.forEach(req -> stock.put(req, stock.get(req) - 1));
            getContext().getLog().info("LocalStorage: supplied {} for order #{}", chosen, msg.orderId);

            //Inform worker that requested parts are available.
            msg.worker.tell(new Worker.SpecialRequestsFetched(msg.orderId, msg.productionLine, chosen));
        } else {

            //There is (min) one part missing in the storage -> request subsequent delivery.
            getContext().getLog().info("LocalStorage: {} missing, ordering from main storage", missing);

            //Put request on a queue until request subsequent delivery available.
            pending.add(msg);

            //Message to MainStorage that request subsequent delivery is necessary.
            mainStorage.tell(new MainStorage.OrderSupply(getContext().getSelf()));
        }
        return this;
    }

    /**
     * Edit message that request subsequent delivery is available.
     * Replenishes stock and serves all waiting requests,
     */

    private Behavior<Message> onRestock(Restock msg) {

        //Increase stock by 3 for all types (subsequent delivery).
        TYPES.forEach(t -> stock.put(t, stock.get(t) + 3));

        //Process all outstanding requests again now.
        getContext().getLog().info("LocalStorage: restocked all items");
        while (!pending.isEmpty()) {
            //Send each pending request to the same handler again.
            onFetchRequests(pending.poll());
        }
        return this;
    }
}
