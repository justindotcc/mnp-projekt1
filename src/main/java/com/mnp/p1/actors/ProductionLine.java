package com.mnp.p1.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import java.util.List;
import java.util.Random;

public class ProductionLine extends AbstractBehavior<ProductionLine.Message> {

    public interface Message {
    }

    public static final class StartProduction implements Message {
        public final int orderId;

        public StartProduction(int orderId) {
            this.orderId = orderId;
        }
    }

    public static final class BodyBuilt implements Message {
        public final int orderId;
        public final ActorRef<Worker.Message> worker;

        public BodyBuilt(int orderId, ActorRef<Worker.Message> worker) {
            this.orderId = orderId;
            this.worker = worker;
        }
    }

    public static final class SpecialRequestsInstalled implements Message {
        public final int orderId;

        public SpecialRequestsInstalled(int orderId) {
            this.orderId = orderId;
        }
    }

    public static Behavior<Message> create(
            ActorRef<OrderBook.Message> orderBook,
            List<ActorRef<Worker.Message>> workers,
            ActorRef<LocalStorage.Message> localStorage
    ) {
        return Behaviors.setup(ctx ->
                Behaviors.withTimers(timers -> new ProductionLine(ctx, timers, orderBook, workers, localStorage))
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
                .onMessage(BodyBuilt.class, this::onBodyBuilt)
                .onMessage(SpecialRequestsInstalled.class, this::onSpecialRequestsInstalled)
                .build();
    }

    private Behavior<Message> onStartProduction(StartProduction msg) {
        if (isBusy) return this;
        isBusy = true;
        this.currentOrder = msg.orderId;
        this.currentWorker = workers.get(random.nextInt(workers.size()));
        getContext().getLog().info(
                "ProductionLine {}: starting order #{} with {}",
                getContext().getSelf().path().name(), currentOrder, currentWorker.path().name()
        );
        currentWorker.tell(new Worker.BuildBody(currentOrder, getContext().getSelf()));
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
