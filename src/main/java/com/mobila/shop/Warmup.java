package com.mobila.shop;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** După pornire: încălzește PBKDF2 (JIT) în fundal, ca prima autentificare să nu dureze secunde. */
@Component
public class Warmup {
    @EventListener(ApplicationReadyEvent.class)
    public void warm() {
        Thread t = new Thread(() -> { try { Util.burn("warmup"); Util.burn("warmup2"); } catch (Exception ignored) { } }, "warmup");
        t.setDaemon(true);
        t.start();
    }
}
