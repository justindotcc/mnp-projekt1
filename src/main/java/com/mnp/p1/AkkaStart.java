/**
 * Isabelle Bille 156252
 * Justin Gottwald 201237
 * Ilia Orlov 251287
 */

package com.mnp.p1;

import akka.actor.typed.ActorSystem;

/**
 * Class to start the system.
 * Class initialises the ActorSystem
 */

public class AkkaStart {
    public static void main(String[] args) {
        final ActorSystem<AkkaMainSystem.Message> system = ActorSystem.create(AkkaMainSystem.create(), "akkaMainSystem");
        System.out.println(">>> Press ENTER to exit <<<");
        try {
            System.in.read();
        } catch (Exception ignored) {
        }
        system.tell(new AkkaMainSystem.Terminate());
    }
}