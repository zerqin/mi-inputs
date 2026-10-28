package com.tvsetup.mibutton;

/**
 * Канал передачи нажатой клавиши из службы в экран настроек
 * (используется только во время определения кнопки Mi).
 */
public final class KeyBus {

    public interface Listener {
        void onKey(int keyCode);
    }

    public static volatile Listener listener;

    private KeyBus() {
    }
}
