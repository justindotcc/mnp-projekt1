/**
 * Isabelle Bille 156252
 * Justin Gottwald 201237
 * Ilia Orlov
 */

package com.mnp.p1;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import com.mnp.p1.actors.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to start and control the car-fabric-system.
 * Creates the actors (OrderBook, ProductionLines, Workers, LocalStorage, mainStorage)
 * Creates new orders in specific intervals.
 */

public class AkkaMainSystem extends AbstractBehavior<AkkaMainSystem.Message> {

    public interface Message {
    }

    //Message to generate a new order.
    public static class GenerateOrder implements Message {
    }

    //Message to terminate the system.
    public static class Terminate implements Message {
    }

    //Creates the AkkaMainSystem-actor.
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

        //Create the MainStorage-actor.
        ActorRef<MainStorage.Message> mainStorage = context.spawn(MainStorage.create(), "main-storage");

        //Create the local storage-actor with a reference to the MainStorage.
        ActorRef<LocalStorage.Message> localStorage = context.spawn(LocalStorage.create(mainStorage), "local-storage");

        //Create the worker-actors.
        List<ActorRef<Worker.Message>> workers = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            workers.add(context.spawn(Worker.create("Worker-" + i, localStorage), "worker-" + i));
        }

        //Create the OrderBook-actor.
        this.orderBook = context.spawn(OrderBook.create(), "order-book");

        //Create two ProductLine-actors with reference to orderBook, WorkerList and LocalStorage.
        ActorRef<ProductionLine.Message> line1 = context.spawn(
                ProductionLine.create(orderBook, workers, localStorage), "line-1"
        );
        ActorRef<ProductionLine.Message> line2 = context.spawn(
                ProductionLine.create(orderBook, workers, localStorage), "line-2"
        );

        //Message to confirm that both productLines are initial available.
        orderBook.tell(new OrderBook.ProductionLineAvailable(line1));
        orderBook.tell(new OrderBook.ProductionLineAvailable(line2));

        //Start timer to generate a new order every 15 seconds.
        timers.startTimerAtFixedRate("order-generator", new GenerateOrder(), Duration.ofSeconds(15));
    }

    @Override
    public Receive<Message> createReceive() {
        return newReceiveBuilder()
                .onMessage(GenerateOrder.class, this::onGenerateOrder)
                .onMessage(Terminate.class, this::onTerminate)
                .build();
    }

    //Message-Handler for messages that generates a new order.
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
