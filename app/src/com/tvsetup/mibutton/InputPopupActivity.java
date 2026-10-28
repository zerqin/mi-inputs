package com.tvsetup.mibutton;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Свой список источников входа с понятными названиями.
 */
public class InputPopupActivity extends Activity {

    private boolean gotFocus;
    private boolean closing;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(AppLocale.wrap(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.dimAmount = 0.6f;
        getWindow().setAttributes(attrs);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xB3000000);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(Ui.dp(this, 24), Ui.dp(this, 20),
                Ui.dp(this, 24), Ui.dp(this, 20));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF5121212);
        bg.setCornerRadius(Ui.dp(this, 16));
        panel.setBackground(bg);

        TextView title = Ui.text(this, getString(R.string.popup_inputs_title), 13, Ui.TEXT_DIM);
        title.setPadding(Ui.dp(this, 4), 0, 0, Ui.dp(this, 12));
        panel.addView(title);

        List<Inputs.Item> items = visibleInputs();
        View first = null;

        if (items.isEmpty()) {
            panel.addView(Ui.text(this, getString(R.string.popup_empty), 16, Ui.TEXT));
            View fallback = Ui.row(this, getString(R.string.popup_open_system), null,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            openSystemPopup();
                        }
                    });
            panel.addView(fallback);
            first = fallback;
        } else {
            for (Inputs.Item item : items) {
                final Inputs.Item target = item;
                LinearLayout row = Ui.row(this, item.name, null,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                select(target);
                            }
                        });
                panel.addView(row);
                if (first == null) {
                    first = row;
                }
            }
        }

        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.62f);
        width = Math.max(width, Ui.dp(this, 360));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                width, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        root.addView(panel, lp);

        setContentView(root);

        if (first != null) {
            final View target = first;
            target.post(new Runnable() {
                @Override
                public void run() {
                    target.requestFocus();
                }
            });
        }
    }

    private void close() {
        if (!closing) {
            closing = true;
            finish();
        }
    }

    private List<Inputs.Item> visibleInputs() {
        List<Inputs.Item> result = new ArrayList<>();
        for (Inputs.Item item : Inputs.load(this)) {
            if (!item.hidden) {
                result.add(item);
            }
        }
        return result;
    }

    private void select(Inputs.Item item) {
        boolean ok = Inputs.switchTo(this, item);
        if (!ok) {
            Toast.makeText(this, getString(R.string.popup_switch_failed, item.name),
                    Toast.LENGTH_SHORT).show();
        }
        close();
    }

    private void openSystemPopup() {
        Intent intent = Inputs.vendorPopup(this);
        if (intent == null) {
            Toast.makeText(this, getString(R.string.popup_system_missing),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.popup_system_failed),
                    Toast.LENGTH_SHORT).show();
        }
        close();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            gotFocus = true;
        } else if (gotFocus) {
            // Попап закрывается, если пользователь ушёл с него (как системный).
            close();
        }
    }
}
