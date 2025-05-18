package com.mnp.p1.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ProductionLine extends AbstractBehavior<ProductionLine.Message> {

    public interface Message {
    }

    public record StartProduction(int orderId) implements Message {
    }

    public record BodyBuilt(int orderId, ActorRef<Worker.Message> worker) implements Message {
    }

    public record SpecialRequestsInstalled(int orderId) implements Message {
    }

    public static final class Availability implements Message {
        public final boolean available;
        public final ActorRef<Worker.Message> worker;

        public Availability(boolean available, ActorRef<Worker.Message> worker) {
            this.available = available;
            this.worker = worker;
        }
    }

    public static Behavior<Message> create(
            ActorRef<OrderBook.Message> orderBook,
            List<ActorRef<Worker.Message>> workers,
            ActorRef<LocalStorage.Message> localStorage
    ) {
        return Behaviors.setup(ctx ->
                Behaviors.withTimers(timers ->
                        new ProductionLine(ctx, timers, orderBook, workers, localStorage)
                )
        );
    }

    private final TimerScheduler<Message> timers;
    private final ActorRef<OrderBook.Message> orderBook;
    private final List<ActorRef<Worker.Message>> workers;
    private final ActorRef<LocalStorage.Message> localStorage;
    private final Random random = new Random();

    private boolean isBusy = false;
    private int currentOrder;
    private ActorRef<Worker.Message> currentWorker;

    // New state for availability checks
    private int pendingChecks = 0;
    private final List<ActorRef<Worker.Message>> availableWorkers = new ArrayList<>();

    private ProductionLine(
            ActorContext<Message> context,
            TimerScheduler<Message> timers,
            ActorRef<OrderBook.Message> orderBook,
            List<ActorRef<Worker.Message>> workers,
            ActorRef<LocalStorage.Message> localStorage
    ) {
        super(context);
        this.timers = timers;
        this.orderBook = orderBook;
        this.workers = workers;
        this.localStorage = localStorage;
    }

    @Override
    public Receive<Message> createReceive() {
        return newReceiveBuilder()
                .onMessage(StartProduction.class, this::onStartProduction)
                .onMessage(Availability.class, this::onAvailability)
                .onMessage(BodyBuilt.class, this::onBodyBuilt)
                .onMessage(SpecialRequestsInstalled.class, this::onSpecialRequestsInstalled)
                .build();
    }

    private Behavior<Message> onStartProduction(StartProduction msg) {
        if (isBusy) return this;

        isBusy = true;
        currentOrder = msg.orderId;
        availableWorkers.clear();
        pendingChecks = workers.size();

        // Query all workers
        for (var worker : workers) {
            worker.tell(new Worker.IsAvailable(getContext().getSelf()));
        }
        return this;
    }

    private Behavior<Message> onAvailability(Availability msg) {
        pendingChecks--;
        if (msg.available) {
            availableWorkers.add(msg.worker);
        }

        // Once all replies are in, pick one or skip
        if (pendingChecks == 0) {
            if (availableWorkers.isEmpty()) {
                getContext().getLog().info(
                        "ProductionLine {}: no available workers for order #{}",
                        getContext().getSelf().path().name(), currentOrder
                );
                isBusy = false;
            } else {
                currentWorker = availableWorkers.size() == 1
                        ? availableWorkers.get(0)
                        : availableWorkers.get(random.nextInt(availableWorkers.size()));
                currentWorker.tell(new Worker.BuildBody(currentOrder, getContext().getSelf()));
            }
        }
        return this;
    }

    private Behavior<Message> onBodyBuilt(BodyBuilt msg) {
        getContext().getLog().info(
                "ProductionLine {}: body built for order #{} by {}",
                getContext().getSelf().path().name(), msg.orderId, msg.worker.path().name()
        );
        msg.worker.tell(new Worker.FetchSpecialRequests(currentOrder, getContext().getSelf()));
        return this;
    }

    private Behavior<Message> onSpecialRequestsInstalled(SpecialRequestsInstalled msg) {
        getContext().getLog().info(
                "ProductionLine {}: finished order #{}",
                getContext().getSelf().path().name(), msg.orderId
        );
        isBusy = false;
        orderBook.tell(new OrderBook.ProductionLineAvailable(getContext().getSelf()));
        return this;
    }
}