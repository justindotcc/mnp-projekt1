package com.mnp.p1;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import com.mnp.p1.actors.ProductionLine;

public class AkkaMainSystem extends AbstractBehavior<AkkaMainSystem.Message> {

    public interface Message {
    }

    public static class Create implements Message {
    }

    public static class Terminate implements Message {
    }

    public static Behavior<Message> create() {
        return Behaviors.setup(AkkaMainSystem::new);
    }

    private AkkaMainSystem(ActorContext<Message> context) {
        super(context);
    }

    @Override
    public Receive<Message> createReceive() {
        return newReceiveBuilder()
                .onMessage(Create.class, this::onCreate)
                .onMessage(Terminate.class, this::onTerminate)
                .build();
    }

    private Behavior<Message> onCreate(Create command) {

        // spawn ProductionLine actor
        ActorRef<ProductionLine.Command> productionLine = getContext().spawn(ProductionLine.create(), "productionLine");

        // start the production line
        productionLine.tell(new ProductionLine.StartProduction(1));


        return this;
    }

    private Behavior<Message> onTerminate(Terminate command) {

        // TODO: Implement termination logic

        return this;
    }
}
