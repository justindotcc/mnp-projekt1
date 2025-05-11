package com.mnp.p1.actors;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

public class MainStorage extends AbstractBehavior<MainStorage.Message> {

    public static Behavior<Message> create() {
        return Behaviors.setup(MainStorage::new);
    }

    public interface Message {
    }

    public static class Create implements Message {
    }

    private MainStorage(ActorContext<Message> context) {
        super(context);
    }

    @Override
    public Receive<Message> createReceive() {
        return newReceiveBuilder()
                .onMessage(Create.class, this::onCreate)
                .build();
    }

    private Behavior<Message> onCreate(Create message) {
        return this;
    }
}