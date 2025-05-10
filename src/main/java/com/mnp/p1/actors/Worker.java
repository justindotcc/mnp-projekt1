package com.mnp.p1.actors;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class Worker extends AbstractBehavior<Worker.Message> {

    /*
     * • Arbeiter*in (Worker)
     * – Jeder Worker hat einen Namen.
     * – Baue Karosserie: Baut die Karosserie.
     * – Hole Spezialwünsche aus dem Lokalen Lager: Der Arbeiter geht in das lokale Lager und
     *   holt zwei zufällige Spezialwünsche aus dem Lager
     * – Verbaue Spezialwünsche: Baut diese in das Auto ein.
     */


    interface Message {
    }

    public static class BuildBody implements Message {
    }

    public record FetchSpecialRequests() implements Message {
    }

    public record InstallSpecialRequests() implements Message {
    }

    public String name;

    private Worker(ActorContext<Message> context, String name) {
        super(context);
        this.name = name;
    }

    public static Behavior<Message> create(String name) {
        return Behaviors.setup(context -> new Worker(context, name));
    }

    @Override
    public Receive<Message> createReceive() {
        return newReceiveBuilder()
                .onMessage(BuildBody.class, this::onBuildBody)
                .onMessage(FetchSpecialRequests.class, this::onFetchSpecialRequests)
                .onMessage(InstallSpecialRequests.class, this::onInstallSpecialRequests)
                .build();
    }

    private Behavior<Message> onBuildBody(BuildBody command) {
        getContext().getLog().info("Worker {} is building the body", name);

        return this;
    }

    private Behavior<Message> onFetchSpecialRequests(FetchSpecialRequests command) {
        getContext().getLog().info("Worker {} is fetching special requests", name);

        return this;
    }

    private Behavior<Message> onInstallSpecialRequests(InstallSpecialRequests command) {
        getContext().getLog().info("Worker {} is installing special requests", name);

        return this;
    }
}
