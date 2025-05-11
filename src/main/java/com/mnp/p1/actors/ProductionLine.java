package com.mnp.p1.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import java.time.Duration;
import java.util.List;

public class ProductionLine extends AbstractBehavior<ProductionLine.Message> {

    public interface Message {
    }

    public static final class StartProduction implements Message {
        public final int orderId;

        public StartProduction(int orderId) {
            this.orderId = orderId;
        }
    }

    public static final class StopProduction implements Message {
        public final int orderId;

        public StopProduction(int orderId) {
            this.orderId = orderId;
        }
    }

    public static final class InitOrderBook implements Message {
        public final ActorRef<OrderBook.Message> orderBook;

        public InitOrderBook(ActorRef<OrderBook.Message> orderBook) {
            this.orderBook = orderBook;
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
    private ActorRef<OrderBook.Message> orderBook; // now assignable
    private final List<ActorRef<Worker.Message>> workers;
    private final ActorRef<LocalStorage.Message> localStorage;
    private boolean isBusy = false;

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
                .onMessage(StopProduction.class, this::onStopProduction)
                .onMessage(InitOrderBook.class, this::onInitOrderBook)
                .build();
    }

    private Behavior<Message> onInitOrderBook(InitOrderBook msg) {
        this.orderBook = msg.orderBook;
        getContext().getLog().info("ProductionLine received OrderBook reference");
        orderBook.tell(new OrderBook.ProductionLineAvailable(getContext().getSelf()));
        return this;
    }

    private Behavior<Message> onStartProduction(StartProduction msg) {
        if (isBusy) return this;
        isBusy = true;
        getContext().getLog().info("Starting production for order #{}", msg.orderId);
        int delay = 5 + (int) (Math.random() * 6);
        timers.startSingleTimer(new StopProduction(msg.orderId), Duration.ofSeconds(delay));
        return this;
    }

    private Behavior<Message> onStopProduction(StopProduction msg) {
        isBusy = false;
        getContext().getLog().info("Finished production for order #{}", msg.orderId);
        orderBook.tell(new OrderBook.ProductionLineAvailable(getContext().getSelf()));
        return this;
    }
}
