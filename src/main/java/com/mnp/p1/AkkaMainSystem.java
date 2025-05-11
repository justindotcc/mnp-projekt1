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

    public static class Create implements Message {
    }

    public static class GenerateOrder implements Message {
    }

    public static class Terminate implements Message {
    }

    public static Behavior<Message> create() {
        return Behaviors.setup(ctx ->
                Behaviors.withTimers(timers ->
                        new AkkaMainSystem(ctx, timers)
                )
        );
    }

    private final TimerScheduler<Message> timers;
    private final ActorRef<OrderBook.Message> orderBook;
    private int orderCounter = 0;

    private AkkaMainSystem(ActorContext<Message> context, TimerScheduler<Message> timers) {
        super(context);
        this.timers = timers;

        // Create MainStorage
        ActorRef<MainStorage.Message> mainStorage =
                context.spawn(MainStorage.create(), "main-storage");

        // Create LocalStorage
        ActorRef<LocalStorage.Message> localStorage =
                context.spawn(LocalStorage.create(mainStorage), "local-storage");

        // Create Workers
        List<ActorRef<Worker.Message>> workers = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            workers.add(context.spawn(Worker.create("Worker-" + i, localStorage), "worker-" + i));
        }

        // Create ProductionLines with null OrderBook (init later)
        ActorRef<ProductionLine.Message> line1 = context.spawn(
                ProductionLine.create(null, workers, localStorage), "line-1");
        ActorRef<ProductionLine.Message> line2 = context.spawn(
                ProductionLine.create(null, workers, localStorage), "line-2");

        List<ActorRef<ProductionLine.Message>> lines = List.of(line1, line2);

        // Now create OrderBook
        this.orderBook = context.spawn(OrderBook.create(lines), "order-book");

        // Inform lines of OrderBook
        line1.tell(new ProductionLine.InitOrderBook(orderBook));
        line2.tell(new ProductionLine.InitOrderBook(orderBook));

        // Start 15s repeating order generation
        timers.startTimerAtFixedRate("order-generator", new GenerateOrder(), Duration.ofSeconds(15));
    }

    @Override
    public Receive<Message> createReceive() {
        return newReceiveBuilder()
                .onMessage(Create.class, this::onCreate)
                .onMessage(GenerateOrder.class, this::onGenerateOrder)
                .onMessage(Terminate.class, this::onTerminate)
                .build();
    }

    private Behavior<Message> onCreate(Create command) {
        return this;
    }

    private Behavior<Message> onGenerateOrder(GenerateOrder msg) {
        orderCounter++;
        getContext().getLog().info("Factory: Generating new order #{}", orderCounter);
        orderBook.tell(new OrderBook.NewOrder(orderCounter));
        return this;
    }

    private Behavior<Message> onTerminate(Terminate command) {
        getContext().getLog().info("Terminate");
        return Behaviors.stopped();
    }
}
