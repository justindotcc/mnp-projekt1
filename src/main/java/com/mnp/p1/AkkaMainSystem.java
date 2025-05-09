package com.mnp.p1;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import com.mnp.p1.actors.ProductionLine;

public class AkkaMainSystem extends AbstractBehavior<AkkaMainSystem.Create> {

    public static class Create {
    }

    public static Behavior<Create> create() {
        return Behaviors.setup(AkkaMainSystem::new);
    }

    private AkkaMainSystem(ActorContext<Create> context) {
        super(context);
    }

    @Override
    public Receive<Create> createReceive() {
        System.out.println("AkkaMainSystem.createReceive()");
        return newReceiveBuilder()
                .onMessage(Create.class, this::onCreate)
                .build();
    }

    private Behavior<Create> onCreate(Create command) {

        // spawn ProductionLine actor
        ActorRef<ProductionLine.Command> productionLine = getContext().spawn(ProductionLine.create(), "productionLine");

        // start the production line
        productionLine.tell(new ProductionLine.StartProduction(1));


        return this;
    }
}
