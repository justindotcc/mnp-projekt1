package com.mnp.p1;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import com.mnp.p1.actors.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

    /*
    Aufgabe 1 (Die Autofabrik)
        (0 Punkte)
        Der Autobauer MeinAuto hat eine neue Fabrik eröffnet und möchte die Produktion entsprechend digitalisie-
        ren.

        MeinAuto besitzt eine Fabrik, diese hat
        + ein Auftragsbuch (OrderBook) für die zu bauenden Autos, wenn
        eine der
        + zwei Fertigungsstraßen (ProductionLine) gerade kein Auto baut, wird der erste Auftrag dieser Linie
        zugeordnet. Wenn der Bau beginnt, wird einer der
        + vier Angestellten (Worker) zufällig der Fertigungsstraße zugewiesen. Ein*e Arbeiter*in kann nur
        an einer Straße gleichzeitig arbeiten. Zuerst wird die
        + + Karosserie gefertigt und die Standardmodule eingebaut, was eine zufällige Zeit t ∈ [5,10]s dauert
        (Hinweis: TimerScheduler 1).
        ++ Anschließend geht die/der Bearbeiter*in ins lokale Lager (LocalStorage) um zufällig zwei von vier gelagerten
        Spezialanforderungen (SpecialRequests) zu holen, sollte allerdings eine Spezialforderung nicht vorrätig sein,
        wird eine Bestellung an das Hauptlager aufgegeben und für jede der vier Spezialforderungen werden drei Ein-
        heiten geliefert was t ∈ [10,15]s dauert. Im lokalen Lager sind zu Beginn 4 Einheiten jeder Spezialforderung
        gelagert. Danach wird das Auto fertiggestellt und ausgeliefert und die Fertigungsstraße freigegeben.

        Ein Auftrag für ein neues Auto kommt alle 15s herein und wird in das Auftragsbuch geschrieben.
        Dieses System läuft in einer Endlosschleife.

        Folgende Aktoren sollten sie wenigstens Implementieren, diese können über die angegebenen Nachrichten
        verfügen. Sollten Sie auf einen anderen Ansatz kommen, der die obige Aufgabe erfüllt, bestehen Sie das Projekt.
        • Auftragsbuch (OrderBook)
        – Neuer Auftrag: Ein neuer Auftrag wird in das Auftragsbuch übernommen, dies kann einfach eine
        Nummer sein.
        – Auftrag zuweisen: Der älteste nicht bearbeitete Auftrag wird der nächsten freien Produktionsstraße
        zugewiesen. Sollte keine vorliegen, wird 10 Sekunden gewartet und ein neuer Zuweisungsversuch
        unternommen.
        • Produktionsstraße (ProductionLine)
        – Produziere Karosserie: Die Karosserie wird durch den zugewiesenen Arbeiter in [5,10] Sekunden
        erstellt.
        – Verbaue Spezialwünsche: Nachdem die Karosserie erstellt wurde, werden die 2 Spezialwünsche
        aus dem Lager geholt und eingebaut.
        – Fertigstellung: Die Straße und die/ der Arbeiter*in werden wieder freigegeben.
        • Arbeiter*in (Worker)
        – Jeder Worker hat einen Namen.
        – Baue Karosserie: Baut die Karosserie.
        – Hole Spezialwünsche aus dem Lokalen Lager: Der Arbeiter geht in das lokale Lager und holt zwei
        zufällige Spezialwünsche aus dem Lager
        – Verbaue Spezialwünsche: Baut diese in das Auto ein.
        • Lokales Lager (LocalStorage)
        – Erhalte Spezialwünsche: Wenn die zufälligen Kisten nicht leer sind, erhält der Arbeiter die Spe-
        zialwünsche direkt. Sonst wird eine Bestellung ans Hauptlager aufgegeben, die zwischen [10,15]
        Sekunden braucht um geliefert zu werden. Die Entnahme senkt den Wert einer Kiste um 1.
        – Bestellung Hauptlager: Nach [10,15] Sekunden werden zu jeder Box drei Einheiten hinzuaddiert
        Während jedes Arbeitsschritts gibt das System eine Meldung aus, so dass ersichtlich ist welcher Arbeiter gerade
        an welchem Schritt arbeitet.
        1https://doc.akka.io/japi/akka/current/akka/actor/TimerScheduler.html

        Einige Hinweise:
        • Das lokale Lager hat keine Begrenzung in der Anzahl der gelagerten Stücke.
        • Beispielliste der Spezialforderungen: Ledersitze, Klimaautomatik, Elektrische Fensterheber, Automatik-
        getriebe.
        • Um den genauen Ablauf zu garantieren, müssen Sie eventuell weitere Nachrichten oder Aktoren dem
        System hinzufügen. Außerdem ist es möglicherweise Notwendig, dass die hier beschriebenen Nachrich-
        ten mehr Inhalt haben müssen, als hier angegeben.
        • Das System soll endlos laufen. Falls zu irgendeinen Zeitpunkt von den Aktoren keine Nachrichten mehr
        kommen, haben Sie wahrscheinlich einen Fehler
     */


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

        // Create WorkerActors
        List<ActorRef<Worker.Message>> workers = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            workers.add(context.spawn(Worker.create("Worker-" + i, localStorage), "worker-" + i));
        }

        // Create ProductionLines
        ActorRef<ProductionLine.Message> line1 = context.spawn(
                ProductionLine.create(workers, localStorage), "line-1");

        ActorRef<ProductionLine.Message> line2 = context.spawn(
                ProductionLine.create(workers, localStorage), "line-2");

        // Create OrderBook
        this.orderBook = context.spawn(
                OrderBook.create(List.of(line1, line2)), "order-book");

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
