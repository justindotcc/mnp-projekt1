package com.mnp.p1;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import com.mnp.p1.actors.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class AkkaMainSystem extends AbstractBehavior<AkkaMainSystem.Message> {

    public interface Message {
    }

    public static class GenerateOrder implements Message {
    }

    public static class Terminate implements Message {
    }

    public static Behavior<Message> create() {
        return Behaviors.setup(ctx ->
                Behaviors.withTimers(timers -> new AkkaMainSystem(ctx, timers))
        );
    }

    private final TimerScheduler<Message> timers;
    private final ActorRef<OrderBook.Message> orderBook;
    private int orderCounter = 0;

    private AkkaMainSystem(ActorContext<Message> context, TimerScheduler<Message> timers) {
        super(context);
        this.timers = timers;

        ActorRef<MainStorage.Message> mainStorage = context.spawn(MainStorage.create(), "main-storage");
        ActorRef<LocalStorage.Message> localStorage = context.spawn(LocalStorage.create(mainStorage), "local-storage");

        List<ActorRef<Worker.Message>> workers = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            workers.add(context.spawn(Worker.create("Worker-" + i, localStorage), "worker-" + i));
        }

        this.orderBook = context.spawn(OrderBook.create(), "order-book");

        ActorRef<ProductionLine.Message> line1 = context.spawn(
                ProductionLine.create(orderBook, workers, localStorage), "line-1"
        );
        ActorRef<ProductionLine.Message> line2 = context.spawn(
                ProductionLine.create(orderBook, workers, localStorage), "line-2"
        );

        // initial availability
        orderBook.tell(new OrderBook.ProductionLineAvailable(line1));
        orderBook.tell(new OrderBook.ProductionLineAvailable(line2));

        timers.startTimerAtFixedRate("order-generator", new GenerateOrder(), Duration.ofSeconds(15));
    }

    @Override
    public Receive<Message> createReceive() {
        return newReceiveBuilder()
                .onMessage(GenerateOrder.class, this::onGenerateOrder)
                .onMessage(Terminate.class, this::onTerminate)
                .build();
    }

    private Behavior<Message> onGenerateOrder(GenerateOrder msg) {
        orderCounter++;
        getContext().getLog().info("Factory: Generating new order #{}", orderCounter);
        orderBook.tell(new OrderBook.NewOrder(orderCounter));
        return this;
    }

    private Behavior<Message> onTerminate(Terminate msg) {
        getContext().getLog().info("Factory: terminate");
        return Behaviors.stopped();
    }
}
