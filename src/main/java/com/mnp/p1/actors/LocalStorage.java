package com.mnp.p1.actors;

import akka.actor.AbstractActor;

public class LocalStorage extends AbstractActor<LocalStorage.Command> {

    interface Command {
    }

    public record receiveSpecialRequests() implements Command {
    }

    /*
     * • Lokales Lager (LocalStorage)
     * – Erhalte Spezialwünsche: Wenn die zufälligen Kisten nicht leer sind,
     *   erhält der Arbeiter die Spezialwünsche direkt. Sonst wird eine Bestellung
     *   ans Hauptlager aufgegeben, die zwischen [10,15] Sekunden braucht um geliefert zu werden.
     *   Die Entnahme senkt den Wert einer Kiste um 1.
     * – Bestellung Hauptlager: Nach [10,15] Sekunden werden zu jeder Box drei Einheiten hinzuaddiert
     */

}
