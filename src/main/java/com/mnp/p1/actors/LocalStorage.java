package com.mnp.p1.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

/*
 * • Lokales Lager (LocalStorage)
 * – Erhalte Spezialwünsche: Wenn die zufälligen Kisten nicht leer sind,
 *   erhält der Arbeiter die Spezialwünsche direkt. Sonst wird eine Bestellung
 *   ans Hauptlager aufgegeben, die zwischen [10,15] Sekunden braucht um geliefert zu werden.
 *   Die Entnahme senkt den Wert einer Kiste um 1.
 * – Bestellung Hauptlager: Nach [10,15] Sekunden werden zu jeder Box drei Einheiten hinzuaddiert
 */


public class LocalStorage extends AbstractBehavior<LocalStorage.Message> {

    public interface Message {
    }

    public record receiveSpecialRequests() implements Message {
    }

    public record orderMainStorage() implements Message {
    }

    public record receiveMainStorage() implements Message {
    }

    private LocalStorage(ActorContext<Message> context) {
        super(context);
    }

    public static Behavior<Message> create(ActorRef<MainStorage.Message> mainStorage) {
        return Behaviors.setup(LocalStorage::new);
    }

    @Override
    public Receive<Message> createReceive() {
        return newReceiveBuilder()
                .onMessage(receiveSpecialRequests.class, this::onReceiveSpecialRequests)
                .onMessage(orderMainStorage.class, this::onOrderMainStorage)
                .onMessage(receiveMainStorage.class, this::onReceiveMainStorage)
                .build();
    }

    private Behavior<Message> onReceiveSpecialRequests(receiveSpecialRequests command) {
        return this;
    }

    private Behavior<Message> onOrderMainStorage(orderMainStorage command) {
        return this;
    }

    private Behavior<Message> onReceiveMainStorage(receiveMainStorage command) {
        return this;
    }
}
