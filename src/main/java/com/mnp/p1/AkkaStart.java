package com.mnp.p1;

import akka.actor.typed.ActorSystem;

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