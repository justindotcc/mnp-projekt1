package com.mnp.p1.actors;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class Worker extends AbstractBehavior<Worker.Command> {

    /*
     * • Arbeiter*in (Worker)
     * – Jeder Worker hat einen Namen.
     * – Baue Karosserie: Baut die Karosserie.
     * – Hole Spezialwünsche aus dem Lokalen Lager: Der Arbeiter geht in das lokale Lager und
     *   holt zwei zufällige Spezialwünsche aus dem Lager
     * – Verbaue Spezialwünsche: Baut diese in das Auto ein.
     */


    interface Command {
    }

    public record BuildBody() implements Command {
    }

    ;

    public record FetchSpecialRequests() implements Command {
    }

    ;

    public record InstallSpecialRequests() implements Command {
    }

    ;

    public String name;

    private Worker(ActorContext<Command> context, String name) {
        super(context);
        this.name = name;
    }

    public static Behavior<Command> create(String name) {
        return Behaviors.setup(context -> new Worker(context, name));
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(BuildBody.class, this::onBuildBody)
                .onMessage(FetchSpecialRequests.class, this::onFetchSpecialRequests)
                .onMessage(InstallSpecialRequests.class, this::onInstallSpecialRequests)
                .build();
    }

    private Behavior<Command> onBuildBody(BuildBody command) {
        getContext().getLog().info("Worker {} is building the body", name);

        return this;
    }

    private Behavior<Command> onFetchSpecialRequests(FetchSpecialRequests command) {
        getContext().getLog().info("Worker {} is fetching special requests", name);

        return this;
    }

    private Behavior<Command> onInstallSpecialRequests(InstallSpecialRequests command) {
        getContext().getLog().info("Worker {} is installing special requests", name);

        return this;
    }
}
