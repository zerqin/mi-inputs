package com.tvsetup.mibutton;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Источники входа через штатный TvInputManager: список, типы, номера портов
 * и переключение. Ничего не требует от прошивки, кроме Android TV Input Framework.
 */
public final class Inputs {

    private static final String TAG = "MiInputs";

    /** Известные штатные списки входов (запасной вариант). */
    private static final String[][] VENDOR = {
            {"com.mitv.tvhome.atv", "com.mitv.tvhome.atv.app.tv.InputSourcePopupActivity",
                    "com.mitv.tvhome.atv.app.tv.INPUTSOURCE_POPUP"},
            {"com.mitv.tvhome", "com.mitv.tvhome.app.tv.InputSourcePopupActivity",
                    "com.mitv.tvhome.app.tv.INPUTSOURCE_POPUP"},
            {"com.mitv.tvhome.atv", "com.mitv.tvhome.atv.app.tv.InputSourceActivity", ""},
    };

    /** Пакеты, в которых имеет смысл искать штатный список входов. */
    private static final String[] VENDOR_PACKAGES = {
            "com.mitv.tvhome.atv",
            "com.mitv.tvhome",
            "com.mitv.tvhome.patchwall",
            "com.xiaomi.tvhome",
            "com.mitv.tv",
    };

    /** Типы входов (внутренние ключи, не для показа). */
    public static final String TYPE_HDMI = "hdmi";
    public static final String TYPE_AV = "av";
    public static final String TYPE_TV = "tv";
    public static final String TYPE_VGA = "vga";
    public static final String TYPE_SCART = "scart";
    public static final String TYPE_COMPONENT = "component";
    public static final String TYPE_SVIDEO = "svideo";
    public static final String TYPE_DP = "dp";
    public static final String TYPE_DVI = "dvi";
    public static final String TYPE_OTHER = "other";

    /** Откуда взят номер HDMI-порта. */
    public static final String SRC_NONE = "";
    public static final String SRC_LABEL = "label";
    public static final String SRC_ID = "id";
    public static final String SRC_ORDER = "order";

    public static final class Item {
        public final String id;
        public final int type;
        public final String typeKey;
        public final String systemLabel;
        public final String name;
        public final boolean hidden;
        public final boolean userNamed;
        public final int hdmiPort;
        public final String portSource;

        Item(String id, int type, String typeKey, String systemLabel, String name,
             boolean hidden, boolean userNamed, int hdmiPort, String portSource) {
            this.id = id;
            this.type = type;
            this.typeKey = typeKey;
            this.systemLabel = systemLabel;
            this.name = name;
            this.hidden = hidden;
            this.userNamed = userNamed;
            this.hdmiPort = hdmiPort;
            this.portSource = portSource;
        }

        public boolean isHdmi() {
            return TYPE_HDMI.equals(typeKey);
        }

        public String typeLabel(Context c) {
            return Inputs.typeLabel(c, typeKey);
        }

        public String portSourceLabel(Context c) {
            return Inputs.portSourceLabel(c, portSource);
        }
    }

    private Inputs() {
    }

    public static List<Item> load(Context c) {
        List<TvInputInfo> infos = collect(c);
        Map<String, Integer> ports = new HashMap<>();
        Map<String, String> sources = new HashMap<>();
        assignHdmiPorts(infos, c, ports, sources);

        List<Item> items = new ArrayList<>();
        for (TvInputInfo i : infos) {
            String system = label(i, c);
            String typeKey = typeKey(i.getType(), i.getId(), system);
            int port = ports.containsKey(i.getId()) ? ports.get(i.getId()) : 0;
            String source = sources.containsKey(i.getId()) ? sources.get(i.getId()) : SRC_NONE;
            String defaultName = defaultName(c, typeKey, system, port);
            String user = Prefs.getName(c, i.getId());
            items.add(new Item(
                    i.getId(),
                    i.getType(),
                    typeKey,
                    system,
                    user != null ? user : defaultName,
                    Prefs.isHidden(c, i.getId()),
                    user != null,
                    port,
                    source));
        }

        Collections.sort(items, new Comparator<Item>() {
            @Override
            public int compare(Item a, Item b) {
                int ra = rank(a.typeKey);
                int rb = rank(b.typeKey);
                if (ra != rb) {
                    return ra - rb;
                }
                return a.name.compareToIgnoreCase(b.name);
            }
        });
        return items;
    }

    /** Сколько входов каждого типа: "HDMI 3 · AV 1 · TV 1". */
    public static String summary(Context c, List<Item> items) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Item item : items) {
            Integer n = counts.get(item.typeKey);
            counts.put(item.typeKey, n == null ? 1 : n + 1);
        }
        String[] order = {TYPE_HDMI, TYPE_AV, TYPE_TV, TYPE_VGA, TYPE_SCART,
                TYPE_COMPONENT, TYPE_SVIDEO, TYPE_DP, TYPE_DVI};
        List<String> parts = new ArrayList<>();
        for (String key : order) {
            Integer n = counts.remove(key);
            if (n != null) {
                parts.add(typeLabel(c, key) + " " + n);
            }
        }
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            parts.add(typeLabel(c, e.getKey()) + " " + e.getValue());
        }
        if (parts.isEmpty()) {
            return c.getString(R.string.summary_empty);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                sb.append(" · ");
            }
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    private static List<TvInputInfo> collect(Context c) {
        List<TvInputInfo> infos = new ArrayList<>();
        try {
            TvInputManager tim = (TvInputManager) c.getSystemService(Context.TV_INPUT_SERVICE);
            if (tim == null) {
                return infos;
            }
            List<TvInputInfo> all = tim.getTvInputList();
            Set<String> parents = new HashSet<>();
            for (TvInputInfo i : all) {
                if (!TextUtils.isEmpty(i.getParentId())) {
                    parents.add(i.getParentId());
                }
            }
            boolean tunerAdded = false;
            for (TvInputInfo i : all) {
                boolean tuner = i.getType() == TvInputInfo.TYPE_TUNER;
                if (!i.isPassthroughInput() && !tuner) {
                    continue;
                }
                if (parents.contains(i.getId())) {
                    continue;
                }
                if (i.isHidden(c)) {
                    continue;
                }
                if (tuner) {
                    // В системе бывает два тюнера — показываем один, как и штатный список.
                    if (tunerAdded) {
                        continue;
                    }
                    tunerAdded = true;
                }
                infos.add(i);
            }
        } catch (Exception e) {
            Log.w(TAG, "cannot read TV inputs", e);
        }
        return infos;
    }

    /**
     * Номера HDMI-портов, по приоритету:
     * 1) цифра из системного названия ("HDMI 2(ARC)" -> 2);
     * 2) цифра из идентификатора входа ("HDMI2InputService" -> 2);
     * 3) свободный номер по порядку аппаратных портов.
     */
    private static void assignHdmiPorts(List<TvInputInfo> infos, Context c,
                                        Map<String, Integer> ports,
                                        Map<String, String> sources) {
        List<TvInputInfo> hdmi = new ArrayList<>();
        for (TvInputInfo i : infos) {
            if (TYPE_HDMI.equals(typeKey(i.getType(), i.getId(), label(i, c)))) {
                hdmi.add(i);
            }
        }
        Collections.sort(hdmi, new Comparator<TvInputInfo>() {
            @Override
            public int compare(TvInputInfo a, TvInputInfo b) {
                return hwIndex(a.getId()) - hwIndex(b.getId());
            }
        });

        Set<Integer> used = new HashSet<>();
        for (TvInputInfo i : hdmi) {
            int n = firstNumber(label(i, c));
            if (n > 0 && !used.contains(n)) {
                ports.put(i.getId(), n);
                sources.put(i.getId(), SRC_LABEL);
                used.add(n);
            }
        }
        for (TvInputInfo i : hdmi) {
            if (ports.containsKey(i.getId())) {
                continue;
            }
            int n = firstNumber(idHint(i.getId()));
            if (n > 0 && !used.contains(n)) {
                ports.put(i.getId(), n);
                sources.put(i.getId(), SRC_ID);
                used.add(n);
            }
        }
        int candidate = 1;
        for (TvInputInfo i : hdmi) {
            if (ports.containsKey(i.getId())) {
                continue;
            }
            while (used.contains(candidate)) {
                candidate++;
            }
            ports.put(i.getId(), candidate);
            sources.put(i.getId(), SRC_ORDER);
            used.add(candidate);
        }
    }

    /** Часть идентификатора до аппаратного суффикса: ищем в ней номер порта. */
    private static String idHint(String id) {
        if (id == null) {
            return "";
        }
        int hw = id.lastIndexOf("HW");
        return hw > 0 ? id.substring(0, hw) : id;
    }

    private static int firstNumber(String text) {
        if (text == null) {
            return -1;
        }
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch >= '0' && ch <= '9') {
                int value = ch - '0';
                int j = i + 1;
                while (j < text.length() && text.charAt(j) >= '0' && text.charAt(j) <= '9') {
                    value = value * 10 + (text.charAt(j) - '0');
                    j++;
                }
                return value;
            }
        }
        return -1;
    }

    private static int hwIndex(String id) {
        int i = id == null ? -1 : id.lastIndexOf("HW");
        if (i >= 0) {
            int value = firstNumber(id.substring(i + 2));
            if (value > 0) {
                return value;
            }
        }
        return Integer.MAX_VALUE;
    }

    private static int rank(String typeKey) {
        if (TYPE_HDMI.equals(typeKey)) {
            return 0;
        }
        if (TYPE_AV.equals(typeKey) || TYPE_SCART.equals(typeKey)
                || TYPE_COMPONENT.equals(typeKey) || TYPE_SVIDEO.equals(typeKey)) {
            return 1;
        }
        if (TYPE_TV.equals(typeKey)) {
            return 3;
        }
        return 2;
    }

    private static String label(TvInputInfo info, Context c) {
        try {
            CharSequence l = info.loadLabel(c);
            return l == null ? "" : l.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /** Внутренний тип входа. Для TYPE_OTHER и неизвестных — разбор по id. */
    private static String typeKey(int type, String id, String label) {
        switch (type) {
            case TvInputInfo.TYPE_TUNER:
                return TYPE_TV;
            case TvInputInfo.TYPE_COMPOSITE:
                return TYPE_AV;
            case TvInputInfo.TYPE_SVIDEO:
                return TYPE_SVIDEO;
            case TvInputInfo.TYPE_SCART:
                return TYPE_SCART;
            case TvInputInfo.TYPE_COMPONENT:
                return TYPE_COMPONENT;
            case TvInputInfo.TYPE_VGA:
                return TYPE_VGA;
            case TvInputInfo.TYPE_DVI:
                return TYPE_DVI;
            case TvInputInfo.TYPE_HDMI:
                return TYPE_HDMI;
            case TvInputInfo.TYPE_DISPLAY_PORT:
                return TYPE_DP;
            default:
                String s = (id + " " + label).toLowerCase();
                if (s.contains("hdmi")) {
                    return TYPE_HDMI;
                }
                if (s.contains("composite") || s.contains("avinput")
                        || s.contains(".av.") || s.contains("_av_")) {
                    return TYPE_AV;
                }
                if (s.contains("vga")) {
                    return TYPE_VGA;
                }
                if (s.contains("scart")) {
                    return TYPE_SCART;
                }
                if (s.contains("component")) {
                    return TYPE_COMPONENT;
                }
                if (s.contains("tuner")) {
                    return TYPE_TV;
                }
                return TYPE_OTHER;
        }
    }

    /** Тип входа по-человечески (с учётом языка интерфейса). */
    public static String typeLabel(Context c, String typeKey) {
        if (TYPE_HDMI.equals(typeKey)) {
            return "HDMI";
        }
        if (TYPE_AV.equals(typeKey)) {
            return "AV";
        }
        if (TYPE_TV.equals(typeKey)) {
            return c.getString(R.string.type_tv);
        }
        if (TYPE_VGA.equals(typeKey)) {
            return "VGA";
        }
        if (TYPE_SCART.equals(typeKey)) {
            return "SCART";
        }
        if (TYPE_COMPONENT.equals(typeKey)) {
            return c.getString(R.string.type_component);
        }
        if (TYPE_SVIDEO.equals(typeKey)) {
            return "S-Video";
        }
        if (TYPE_DP.equals(typeKey)) {
            return "DisplayPort";
        }
        if (TYPE_DVI.equals(typeKey)) {
            return "DVI";
        }
        return c.getString(R.string.type_input);
    }

    /** Подпись источника номера порта. */
    public static String portSourceLabel(Context c, String source) {
        if (SRC_LABEL.equals(source)) {
            return c.getString(R.string.port_source_label);
        }
        if (SRC_ID.equals(source)) {
            return c.getString(R.string.port_source_id);
        }
        if (SRC_ORDER.equals(source)) {
            return c.getString(R.string.port_source_order);
        }
        return "";
    }

    private static String defaultName(Context c, String typeKey, String systemLabel, int port) {
        if (TYPE_HDMI.equals(typeKey)) {
            String base = port > 0 ? "HDMI " + port : "HDMI";
            if (systemLabel != null && systemLabel.toUpperCase().contains("ARC")) {
                base += " (ARC)";
            }
            return base;
        }
        if (TYPE_AV.equals(typeKey)) {
            return "AV";
        }
        if (TYPE_TV.equals(typeKey)) {
            return c.getString(R.string.name_tv);
        }
        if (TYPE_VGA.equals(typeKey)) {
            return c.getString(R.string.name_pc_vga);
        }
        return TextUtils.isEmpty(systemLabel) ? typeLabel(c, typeKey) : systemLabel;
    }

    /** Переключение на вход. true — команда отправлена. */
    public static boolean switchTo(Context c, Item item) {
        try {
            Intent intent;
            if (item.type == TvInputInfo.TYPE_TUNER) {
                intent = new Intent(Intent.ACTION_VIEW, TvContract.Channels.CONTENT_URI);
            } else {
                intent = new Intent(Intent.ACTION_VIEW,
                        TvContract.buildChannelUriForPassthroughInput(item.id));
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "switch failed for " + item.id, e);
            return false;
        }
    }

    /** Штатный список входов прошивки (или null, если не найден). */
    public static Intent vendorPopup(Context c) {
        String stored = Prefs.getVendorComponent(c);
        if (stored != null) {
            String[] parts = stored.split("\\|", -1);
            Intent intent = new Intent();
            ComponentName cn = parts.length > 0 ? ComponentName.unflattenFromString(parts[0]) : null;
            if (cn != null) {
                intent.setComponent(cn);
            }
            if (parts.length > 1 && parts[1].length() > 0) {
                intent.setAction(parts[1]);
            }
            if (cn != null && resolves(c, intent)) {
                return intent;
            }
        }
        for (String[] k : VENDOR) {
            Intent intent = new Intent();
            if (k[2].length() > 0) {
                intent.setAction(k[2]);
            }
            intent.setComponent(new ComponentName(k[0], k[1]));
            if (resolves(c, intent)) {
                Prefs.setVendorComponent(c, k[0] + "/" + k[1] + "|" + k[2]);
                return intent;
            }
        }
        // Не нашли по списку — ищем активность с «InputSource» в названии
        // в известных пакетах Xiaomi (PatchWall / ATV / Google TV).
        Intent scanned = scanVendorPopup(c);
        if (scanned != null) {
            ComponentName cn = scanned.getComponent();
            Prefs.setVendorComponent(c, cn.flattenToString() + "|");
            return scanned;
        }
        return null;
    }

    private static Intent scanVendorPopup(Context c) {
        PackageManager pm = c.getPackageManager();
        for (String pkg : VENDOR_PACKAGES) {
            PackageInfo info;
            try {
                info = pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES);
            } catch (Exception e) {
                continue;
            }
            if (info == null || info.activities == null) {
                continue;
            }
            ComponentName best = null;
            for (ActivityInfo ai : info.activities) {
                if (!ai.exported) {
                    continue;
                }
                String name = ai.name.toLowerCase();
                if (!name.contains("inputsource") && !name.contains("input_source")) {
                    continue;
                }
                ComponentName cn = new ComponentName(pkg, ai.name);
                if (name.contains("popup")) {
                    best = cn;
                    break;
                }
                if (best == null) {
                    best = cn;
                }
            }
            if (best != null) {
                Intent intent = new Intent().setComponent(best);
                if (resolves(c, intent)) {
                    Log.i(TAG, "vendor popup found: " + best.flattenToString());
                    return intent;
                }
            }
        }
        return null;
    }

    private static boolean resolves(Context c, Intent intent) {
        try {
            return c.getPackageManager()
                    .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
