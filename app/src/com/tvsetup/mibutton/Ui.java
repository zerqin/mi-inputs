package com.tvsetup.mibutton;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Простые элементы интерфейса, рассчитанные на управление пультом (D-pad).
 */
final class Ui {

    static final int TEXT = 0xFFF2F2F2;
    static final int TEXT_DIM = 0xFF9E9E9E;
    static final int CARD = 0xFF1E1E1E;
    static final int CARD_FOCUS = 0xFF2D6CDF;
    static final int BACKGROUND = 0xFF0D0D0D;
    static final int ACCENT = 0xFF4CAF50;

    private Ui() {
    }

    static int dp(Context c, int value) {
        return (int) (value * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    static StateListDrawable cardBackground(Context c) {
        GradientDrawable focused = new GradientDrawable();
        focused.setColor(CARD_FOCUS);
        focused.setCornerRadius(dp(c, 10));

        GradientDrawable normal = new GradientDrawable();
        normal.setColor(CARD);
        normal.setCornerRadius(dp(c, 10));

        StateListDrawable d = new StateListDrawable();
        d.addState(new int[]{android.R.attr.state_focused}, focused);
        d.addState(new int[]{android.R.attr.state_pressed}, focused);
        d.addState(new int[]{}, normal);
        return d;
    }

    static TextView text(Context c, String value, float size, int color) {
        TextView t = new TextView(c);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    static TextView section(Context c, String title) {
        TextView t = text(c, title, 12, TEXT_DIM);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, 16);
        lp.bottomMargin = dp(c, 6);
        lp.leftMargin = dp(c, 4);
        t.setLayoutParams(lp);
        return t;
    }

    static LinearLayout row(Context c, String title, String subtitle, View.OnClickListener click) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackground(cardBackground(c));
        row.setPadding(dp(c, 20), dp(c, 10), dp(c, 20), dp(c, 10));
        row.setFocusable(true);
        row.setFocusableInTouchMode(true);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(c, 6);
        row.setLayoutParams(lp);

        TextView t = text(c, title, 18, TEXT);
        row.addView(t);

        if (subtitle != null && subtitle.length() > 0) {
            TextView s = text(c, subtitle, 13, TEXT_DIM);
            s.setPadding(0, dp(c, 4), 0, 0);
            row.addView(s);
        }

        row.setOnClickListener(click);
        return row;
    }

    static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(0xFF262626);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(c, 1)));
        lp.topMargin = dp(c, 8);
        lp.bottomMargin = dp(c, 8);
        v.setLayoutParams(lp);
        return v;
    }

    static int color(String hex, int fallback) {
        try {
            return Color.parseColor(hex);
        } catch (Exception e) {
            return fallback;
        }
    }
}
