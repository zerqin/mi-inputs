package com.tvsetup.mibutton;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Служба перехвата кнопки Mi. Поглощает нажатие и открывает список входов,
 * поэтому штатное действие (Live TV) не запускается.
 */
public class MiKeyService extends AccessibilityService {

    private static final String TAG = "MiKeyService";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // События не нужны: работаем только с клавишами.
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();

        KeyBus.Listener listener = KeyBus.listener;
        if (listener != null && !isNavigationKey(code)) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                Log.i(TAG, "captured key=" + code);
                listener.onKey(code);
            }
            return true;
        }

        if (code == Prefs.getKeyCode(this)) {
            if (!Prefs.isInterceptEnabled(this)) {
                return false;
            }
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                openInputs();
            }
            return true;
        }
        return false;
    }

    private void openInputs() {
        try {
            if (Prefs.useSystemPopup(this)) {
                Intent system = Inputs.vendorPopup(this);
                if (system != null) {
                    system.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(system);
                    Log.i(TAG, "system input popup started");
                    return;
                }
            }
            Intent intent = new Intent(this, InputPopupActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            Log.i(TAG, "input popup started");
        } catch (Exception e) {
            Log.e(TAG, "cannot open input popup", e);
        }
    }

    /** Клавиши, которые нельзя перехватывать во время определения кнопки Mi. */
    private static boolean isNavigationKey(int code) {
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                return true;
            default:
                return false;
        }
    }
}
