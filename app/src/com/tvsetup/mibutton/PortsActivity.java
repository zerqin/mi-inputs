package com.tvsetup.mibutton;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Диагностика: какие порты и входы нашлись, как определён номер порта,
 * и проверка переключения на каждый вход.
 */
public class PortsActivity extends Activity {

    private LinearLayout content;

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

    private void render() {
        content.removeAllViews();

        TextView title = Ui.text(this, getString(R.string.ports_title), 24, Ui.TEXT);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        content.addView(title);

        final List<Inputs.Item> items = Inputs.load(this);

        TextView summary = Ui.text(this,
                getString(R.string.ports_found, items.size(), Inputs.summary(this, items)),
                14, Ui.TEXT_DIM);
        summary.setPadding(0, Ui.dp(this, 4), 0, 0);
        content.addView(summary);

        content.addView(Ui.section(this, getString(R.string.section_inputs_plain)));

        if (items.isEmpty()) {
            content.addView(Ui.row(this, getString(R.string.no_inputs),
                    getString(R.string.ports_empty_hint), null));
        } else {
            for (final Inputs.Item item : items) {
                content.addView(Ui.row(this, item.name, describe(item),
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                test(item);
                            }
                        }));
            }
        }

        content.addView(Ui.row(this, getString(R.string.ports_refresh),
                getString(R.string.ports_refresh_hint),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        render();
                        Toast.makeText(PortsActivity.this,
                                getString(R.string.ports_refreshed),
                                Toast.LENGTH_SHORT).show();
                    }
                }));

        content.addView(Ui.section(this, getString(R.string.section_how)));
        content.addView(Ui.row(this, getString(R.string.how_list_title),
                getString(R.string.how_list_text), null));
        content.addView(Ui.row(this, getString(R.string.how_port_title),
                getString(R.string.how_port_text), null));
        content.addView(Ui.row(this, getString(R.string.how_signal_title),
                getString(R.string.how_signal_text), null));

        TextView footer = Ui.text(this, getString(R.string.ports_footer),
                12, 0xFF6E6E6E);
        footer.setPadding(0, Ui.dp(this, 20), 0, 0);
        content.addView(footer);
    }

    private String describe(Inputs.Item item) {
        StringBuilder sb = new StringBuilder();
        sb.append(item.typeLabel(this));
        if (item.isHdmi() && item.hdmiPort > 0) {
            sb.append(" · ").append(getString(R.string.port_hdmi, item.hdmiPort));
            String source = item.portSourceLabel(this);
            if (source.length() > 0) {
                sb.append(" (").append(source).append(")");
            }
        }
        if (item.hidden) {
            sb.append(" · ").append(getString(R.string.hidden_in_list));
        }
        String systemLabel = item.systemLabel;
        if (systemLabel == null || systemLabel.length() == 0) {
            systemLabel = "—";
        }
        sb.append('\n').append(getString(R.string.system_name, systemLabel));
        sb.append('\n').append(item.id);
        return sb.toString();
    }

    private void test(Inputs.Item item) {
        boolean ok = Inputs.switchTo(this, item);
        Toast.makeText(this,
                ok ? getString(R.string.switch_to, item.name)
                        : getString(R.string.switch_failed, item.name),
                Toast.LENGTH_SHORT).show();
    }
}
