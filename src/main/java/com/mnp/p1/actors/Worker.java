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
import java.util.List;
import java.util.Random;

import com.mnp.p1.actors.ProductionLine;

/**
 * Class for a worker who carries out orders.
 * A worker can only work on one job at a time.
 * Carry out the following steps: Build body, get special requests, install special requests
 */

public class Worker extends AbstractBehavior<Worker.Message> {

    public interface Message {
    }

    public static record IsAvailable(ActorRef<ProductionLine.Message> replyTo) implements Message {
    }


    //Order: Build new car body for orderId. Contains reference to the ProductionLine for the answers.
    public static record BuildBody(int orderId, ActorRef<ProductionLine.Message> replyTo) implements Message {
    }

    //Internal message that body construction is complete.
    public static record BodyBuilt(int orderId, ActorRef<ProductionLine.Message> replyTo) implements Message {
    }

    //Order: Get two special requests from the LocalStorage.
    public static record FetchSpecialRequests(int orderId,
                                              ActorRef<ProductionLine.Message> replyTo) implements Message {
    }

    //Message from the LocalStorage: Get special requests have been retrieved and provided.
    public static record SpecialRequestsFetched(int orderId, ActorRef<ProductionLine.Message> replyTo,
                                                List<String> requests) implements Message {
    }

    public static record InstallSpecialRequests(int orderId, ActorRef<ProductionLine.Message> replyTo,
                                                List<String> requests) implements Message {
    }

    public static record SpecialRequestsInstalled(int orderId,
                                                  ActorRef<ProductionLine.Message> replyTo) implements Message {
    }

    private final TimerScheduler<Message> timers;
    private final String name;
    private final ActorRef<LocalStorage.Message> localStorage;
    private final Random random = new Random();

    public boolean isBusy = false;

    //Create a new worker-actor with name and reference to the LocalStorage.
    private Worker(
            ActorContext<Message> context,
            TimerScheduler<Message> timers,
            String name,
            ActorRef<LocalStorage.Message> localStorage
    ) {
        super(context);
        this.timers = timers;
        this.name = name;
        this.localStorage = localStorage;
    }

    public static Behavior<Message> create(String name, ActorRef<LocalStorage.Message> localStorage) {
        return Behaviors.setup(context ->
                Behaviors.withTimers(timers -> new Worker(context, timers, name, localStorage))
        );
    }

    @Override
    public Receive<Message> createReceive() {
        return newReceiveBuilder()
                .onMessage(IsAvailable.class, this::onIsAvailable)
                .onMessage(BuildBody.class, this::onBuildBody)
                .onMessage(BodyBuilt.class, this::onBodyBuilt)
                .onMessage(FetchSpecialRequests.class, this::onFetchSpecialRequests)
                .onMessage(SpecialRequestsFetched.class, this::onSpecialRequestsFetched)
                .build();
    }

    private Behavior<Message> onIsAvailable(IsAvailable msg) {
        getContext().getLog().info("Worker {}: is available", name);
        msg.replyTo.tell(new ProductionLine.Availability(!isBusy, getContext().getSelf()));
        return this;
    }

    /**
     * Edit the order for the car body.
     * Set timer to show the generated car part after a random time.
     */

    private Behavior<Message> onBuildBody(BuildBody msg) {
        if (isBusy) {
            getContext().getLog().info("Worker {}: busy, order #{} is skipped", name, msg.orderId);
            return this;
        }
        isBusy = true;
        getContext().getLog().info("Worker {}: building body for order #{}", name, msg.orderId);
        int delay = 5 + random.nextInt(6);
        timers.startSingleTimer(msg, new BodyBuilt(msg.orderId, msg.replyTo), Duration.ofSeconds(delay));
        return this;
    }

    /**
     * Edit the incoming BodyBuilt-message and inform that the production of the car body is finished.
     * Send information to the ProductionLine.
     */

    private Behavior<Message> onBodyBuilt(BodyBuilt msg) {
        getContext().getLog().info("Worker {}: finished body #{}", name, msg.orderId);
        msg.replyTo.tell(new ProductionLine.BodyBuilt(msg.orderId, getContext().getSelf()));
        return this;
    }

    /**
     * Edit the order for the two special wishes which comes from the local storage.
     * Requests the necessary parts from the LocalStorage.
     */

    private Behavior<Message> onFetchSpecialRequests(FetchSpecialRequests msg) {
        getContext().getLog().info("Worker {}: fetching special requests for order #{}", name, msg.orderId);
        localStorage.tell(new LocalStorage.FetchRequests(msg.orderId, getContext().getSelf(), msg.replyTo));
        return this;
    }

    /**
     * Processing the message that wished parts fetched from the storage.
     * Installation of the special wishes.
     * Message that the ProductionLine is now available again.
     */

    private Behavior<Message> onSpecialRequestsFetched(SpecialRequestsFetched msg) {
        getContext().getLog().info("Worker {}: fetched {} for order #{}", name, msg.requests, msg.orderId);
        getContext().getLog().info("Worker {}: installing special requests for order #{}", name, msg.orderId);
        msg.replyTo.tell(new ProductionLine.SpecialRequestsInstalled(msg.orderId));
        isBusy = false;
        return this;
    }
}
