package com.tvsetup.mibutton;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Экран настроек: состояние службы, кнопка Mi, названия входов, язык.
 */
public class MainActivity extends Activity {

    private static final int DIALOG_THEME = android.R.style.Theme_DeviceDefault_Dialog_Alert;

    private static final String SERVICE =
            "com.tvsetup.mibutton/com.tvsetup.mibutton.MiKeyService";

    private LinearLayout content;
    private AlertDialog captureDialog;
    private boolean firstRender = true;
    private boolean focusIntercept;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(AppLocale.wrap(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.BACKGROUND);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(Ui.dp(this, 32), Ui.dp(this, 12),
                Ui.dp(this, 32), Ui.dp(this, 24));
        scroll.addView(content);

        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    @Override
    protected void onPause() {
        super.onPause();
        KeyBus.listener = null;
    }

    private void render() {
        content.removeAllViews();

        TextView title = Ui.text(this, getString(R.string.app_title), 24, Ui.TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(title, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        final TextView language = Ui.text(this,
                AppLocale.shortName(Prefs.getLanguage(this)), 14, Ui.TEXT);
        int padH = Ui.dp(this, 14);
        int padV = Ui.dp(this, 8);
        language.setPadding(padH, padV, padH, padV);
        language.setBackground(Ui.cardBackground(this));
        language.setFocusable(true);
        language.setFocusableInTouchMode(true);
        language.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseLanguage();
            }
        });
        header.addView(language);
        content.addView(header);

        TextView subtitle = Ui.text(this, getString(R.string.app_subtitle), 14, Ui.TEXT_DIM);
        subtitle.setPadding(0, Ui.dp(this, 4), 0, 0);
        content.addView(subtitle);

        boolean enabled = isServiceEnabled();
        final boolean intercept = Prefs.isInterceptEnabled(this);

        String statusHint;
        if (enabled) {
            statusHint = getString(intercept
                    ? R.string.service_on_hint : R.string.service_paused_hint);
        } else {
            statusHint = getString(R.string.service_off_hint);
        }

        content.addView(Ui.section(this, getString(R.string.section_status)));
        content.addView(Ui.row(this,
                enabled ? getString(R.string.service_on) : getString(R.string.service_off),
                statusHint,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openAccessibilitySettings();
                    }
                }));

        View interceptRow = Ui.row(this,
                getString(intercept ? R.string.intercept_on : R.string.intercept_off),
                getString(intercept
                        ? R.string.intercept_on_hint : R.string.intercept_off_hint),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean next = !Prefs.isInterceptEnabled(MainActivity.this);
                        Prefs.setInterceptEnabled(MainActivity.this, next);
                        Toast.makeText(MainActivity.this,
                                getString(next
                                        ? R.string.interception_enabled
                                        : R.string.interception_paused),
                                Toast.LENGTH_SHORT).show();
                        focusIntercept = true;
                        render();
                    }
                });
        content.addView(interceptRow);

        content.addView(Ui.section(this, getString(R.string.section_button)));

        final int keyCode = Prefs.getKeyCode(this);
        content.addView(Ui.row(this,
                getString(R.string.mi_key, keyCode),
                getString(R.string.mi_key_hint),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        captureKey();
                    }
                }));

        final boolean systemPopup = Prefs.useSystemPopup(this);
        content.addView(Ui.row(this,
                getString(R.string.input_list, getString(systemPopup
                        ? R.string.input_list_system : R.string.input_list_custom)),
                getString(systemPopup
                        ? R.string.input_list_system_hint : R.string.input_list_custom_hint),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        choosePopupMode();
                    }
                }));

        List<Inputs.Item> items = Inputs.load(this);
        content.addView(Ui.section(this, getString(R.string.section_inputs, items.size())));

        if (items.isEmpty()) {
            content.addView(Ui.row(this, getString(R.string.no_inputs),
                    getString(R.string.no_inputs_hint), null));
        } else {
            for (final Inputs.Item item : items) {
                String subtitleText = item.systemLabel;
                if (subtitleText == null || subtitleText.length() == 0) {
                    subtitleText = getString(R.string.no_system_name);
                }
                if (item.hidden) {
                    subtitleText = getString(R.string.hidden_prefix, subtitleText);
                }
                content.addView(Ui.row(this, item.name, subtitleText,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                editInput(item);
                            }
                        }));
            }
        }

        content.addView(Ui.row(this, getString(R.string.refresh_list), null,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        render();
                        Toast.makeText(MainActivity.this, getString(R.string.list_refreshed),
                                Toast.LENGTH_SHORT).show();
                    }
                }));

        content.addView(Ui.section(this, getString(R.string.section_check)));

        content.addView(Ui.row(this, getString(R.string.ports_and_inputs),
                getString(R.string.ports_and_inputs_hint),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(MainActivity.this, PortsActivity.class));
                    }
                }));

        content.addView(Ui.row(this, getString(R.string.open_input_list),
                getString(R.string.open_input_list_hint),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(MainActivity.this, InputPopupActivity.class));
                    }
                }));

        content.addView(Ui.row(this, getString(R.string.how_to_enable),
                getString(R.string.how_to_enable_hint),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showEnableHelp();
                    }
                }));

        TextView footer = Ui.text(this, getString(R.string.footer, versionName()),
                12, 0xFF6E6E6E);
        footer.setPadding(0, Ui.dp(this, 28), 0, 0);
        content.addView(footer);

        View focusTarget = null;
        if (focusIntercept) {
            focusIntercept = false;
            focusTarget = interceptRow;
        } else if (firstRender) {
            firstRender = false;
            focusTarget = language;
        }
        if (focusTarget != null) {
            final View target = focusTarget;
            target.post(new Runnable() {
                @Override
                public void run() {
                    target.requestFocus();
                }
            });
        }
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "";
        }
    }

    private boolean isServiceEnabled() {
        try {
            AccessibilityManager am =
                    (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
            if (am == null) {
                return false;
            }
            List<AccessibilityServiceInfo> list = am.getEnabledAccessibilityServiceList(
                    AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
            for (AccessibilityServiceInfo info : list) {
                if (info.getResolveInfo() != null
                        && info.getResolveInfo().serviceInfo != null
                        && getPackageName().equals(
                        info.getResolveInfo().serviceInfo.packageName)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private void openAccessibilitySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.settings_unavailable),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void captureKey() {
        if (!isServiceEnabled()) {
            Toast.makeText(this, getString(R.string.enable_service_first),
                    Toast.LENGTH_LONG).show();
            return;
        }

        captureDialog = new AlertDialog.Builder(this, DIALOG_THEME)
                .setTitle(getString(R.string.capture_title))
                .setMessage(getString(R.string.capture_message))
                .setNegativeButton(getString(R.string.cancel), null)
                .create();

        KeyBus.listener = new KeyBus.Listener() {
            @Override
            public void onKey(final int code) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (captureDialog != null && captureDialog.isShowing()) {
                            captureDialog.dismiss();
                        }
                        KeyBus.listener = null;
                        Prefs.setKeyCode(MainActivity.this, code);
                        Toast.makeText(MainActivity.this,
                                getString(R.string.capture_saved, code),
                                Toast.LENGTH_LONG).show();
                        render();
                    }
                });
            }
        };

        captureDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                KeyBus.listener = null;
                captureDialog = null;
            }
        });
        captureDialog.show();
    }

    private void choosePopupMode() {
        new AlertDialog.Builder(this, DIALOG_THEME)
                .setTitle(getString(R.string.popup_title))
                .setItems(new String[]{
                        getString(R.string.popup_custom),
                        getString(R.string.popup_system)
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Prefs.setUseSystemPopup(MainActivity.this, which == 1);
                        render();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void chooseLanguage() {
        final String[] codes = {AppLocale.EN, AppLocale.RU};
        String[] labels = {
                "English",
                "Русский"
        };
        String current = Prefs.getLanguage(this);
        int checked = 0;
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(current)) {
                checked = i;
            }
        }
        new AlertDialog.Builder(this, DIALOG_THEME)
                .setTitle(getString(R.string.language_title))
                .setSingleChoiceItems(labels, checked, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Prefs.setLanguage(MainActivity.this, codes[which]);
                        dialog.dismiss();
                        recreate();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private String[] presets() {
        return new String[]{
                "HDMI 1", "HDMI 2", "HDMI 2 (ARC)", "HDMI 3", "HDMI 4",
                "AV", getString(R.string.preset_tv), getString(R.string.preset_pc),
                "USB", getString(R.string.preset_media), getString(R.string.preset_console)
        };
    }

    private void editInput(final Inputs.Item item) {
        final String[] presets = presets();
        final String[] actions = new String[presets.length + 2];
        System.arraycopy(presets, 0, actions, 0, presets.length);
        actions[presets.length] = getString(R.string.restore_system_name);
        actions[presets.length + 1] = getString(item.hidden
                ? R.string.show_in_list : R.string.hide_from_list);

        String systemLabel = item.systemLabel;
        if (systemLabel == null || systemLabel.length() == 0) {
            systemLabel = "—";
        }

        new AlertDialog.Builder(this, DIALOG_THEME)
                .setTitle(getString(R.string.rename_title, item.name))
                .setMessage(getString(R.string.rename_system_name, systemLabel))
                .setItems(actions, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which < presets.length) {
                            Prefs.setName(MainActivity.this, item.id, presets[which]);
                        } else if (which == presets.length) {
                            Prefs.setName(MainActivity.this, item.id, null);
                        } else {
                            Prefs.setHidden(MainActivity.this, item.id, !item.hidden);
                        }
                        render();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showEnableHelp() {
        String current = "";
        try {
            current = Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        } catch (Exception ignored) {
        }
        if (current == null) {
            current = "";
        }

        String list = current;
        if (!list.contains(SERVICE)) {
            list = list.length() == 0 ? SERVICE : list + ":" + SERVICE;
        }

        new AlertDialog.Builder(this, DIALOG_THEME)
                .setTitle(getString(R.string.help_title))
                .setMessage(getString(R.string.help_text, list))
                .setPositiveButton(getString(R.string.ok), null)
                .show();
    }
}
