package com.yogurines.core.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.yogurines.core.YogurinesCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Dibuja el fondo animado del menú (480x270 píxeles) por software, igual que en la propuesta,
 * y lo sube a una textura dinámica cada fotograma.
 * Fondos: A = la forja, B = núcleo hackeado, C = sala de servidores. Se elige uno al azar al abrir el juego.
 */
public final class MenuRenderer {
    private static final Logger LOG = LoggerFactory.getLogger("yogurines_core");
    public static final int W = 480, H = 270;
    public static final ResourceLocation TEX = new ResourceLocation(YogurinesCore.MOD_ID, "dyn/menu_bg");
    private static MenuRenderer instance;

    /** Fondo elegido para esta partida (se sortea una vez cada vez que se abre el juego). */
    public static final char BG = "ABC".charAt(ThreadLocalRandom.current().nextInt(3));

    private record Img(int w, int h, int[] px) {}

    private final int[] buf = new int[W * H];
    private Img bgStatic, glow, holo;
    private final Img[] statues = new Img[3];
    private int[][][] cables, trays;
    private float[][] leds;
    private int forgeCx, forgeCy, hackHz, serverCx, serverVy, serverBw;
    private final int feet = 186;
    private final int[] xs = {150, 240, 330};
    private DynamicTexture tex;
    private boolean ok;

    public static MenuRenderer get() {
        if (instance == null) {
            instance = new MenuRenderer();
            instance.init();
        }
        return instance;
    }

    public boolean ready() {
        return ok;
    }

    private static ResourceLocation rl(String path) {
        return new ResourceLocation(YogurinesCore.MOD_ID, path);
    }

    private static Img load(String name) throws Exception {
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(rl("textures/menu/" + name + ".png"));
             NativeImage ni = NativeImage.read(in)) {
            int w = ni.getWidth(), h = ni.getHeight();
            int[] px = new int[w * h];
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++) {
                    int p = ni.getPixelRGBA(x, y); // ABGR
                    int a = (p >>> 24) & 255, b = (p >> 16) & 255, g = (p >> 8) & 255, r = p & 255;
                    px[y * w + x] = (a << 24) | (r << 16) | (g << 8) | b;
                }
            return new Img(w, h, px);
        }
    }

    private static int[][][] paths(JsonArray arr) {
        int[][][] out = new int[arr.size()][][];
        for (int i = 0; i < arr.size(); i++) {
            JsonArray p = arr.get(i).getAsJsonArray();
            out[i] = new int[p.size()][2];
            for (int j = 0; j < p.size(); j++) {
                JsonArray q = p.get(j).getAsJsonArray();
                out[i][j][0] = q.get(0).getAsInt();
                out[i][j][1] = q.get(1).getAsInt();
            }
        }
        return out;
    }

    private void init() {
        try {
            bgStatic = load("bg_" + BG);
            glow = load("glow");
            holo = load("holo");
            statues[0] = load("statue_samu");
            statues[1] = load("statue_yotu");
            statues[2] = load("statue_metta");
            JsonObject d;
            try (InputStream in = Minecraft.getInstance().getResourceManager().open(rl("data/menu_data.json"))) {
                d = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            }
            cables = paths(d.getAsJsonArray("cables"));
            trays = paths(d.getAsJsonArray("trays"));
            JsonArray l = d.getAsJsonArray("leds");
            leds = new float[l.size()][];
            for (int i = 0; i < l.size(); i++) {
                JsonArray a = l.get(i).getAsJsonArray();
                leds[i] = new float[a.size()];
                int k = 0;
                for (JsonElement e : a) leds[i][k++] = e.getAsFloat();
            }
            forgeCx = d.getAsJsonObject("forge").get("cx").getAsInt();
            forgeCy = d.getAsJsonObject("forge").get("cy").getAsInt();
            hackHz = d.getAsJsonObject("hack").get("hz").getAsInt();
            serverCx = d.getAsJsonObject("server").get("cx").getAsInt();
            serverVy = d.getAsJsonObject("server").get("vy").getAsInt();
            serverBw = d.getAsJsonObject("server").get("bw").getAsInt();
            tex = new DynamicTexture(W, H, false);
            Minecraft.getInstance().getTextureManager().register(TEX, tex);
            ok = true;
        } catch (Exception e) {
            LOG.error("No se pudo cargar el fondo del menú", e);
        }
    }

    // ---------------- utilidades de píxel ----------------

    static double rnd(double s) {
        double x = Math.sin(s * 12.9898 + 78.233) * 43758.5453;
        return x - Math.floor(x);
    }

    private void blend(int x, int y, int rgb, double a) {
        if (x < 0 || y < 0 || x >= W || y >= H || a <= 0) return;
        if (a > 1) a = 1;
        int i = y * W + x, d = buf[i];
        int dr = (d >> 16) & 255, dg = (d >> 8) & 255, db = d & 255;
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        dr += (int) Math.round((r - dr) * a);
        dg += (int) Math.round((g - dg) * a);
        db += (int) Math.round((b - db) * a);
        buf[i] = 0xFF000000 | (dr << 16) | (dg << 8) | db;
    }

    private void rect(double x, double y, double w, double h, int rgb, double a) {
        int x0 = (int) Math.round(x), y0 = (int) Math.round(y), ww = (int) Math.round(w), hh = (int) Math.round(h);
        for (int j = y0; j < y0 + hh; j++)
            for (int i = x0; i < x0 + ww; i++) blend(i, j, rgb, a);
    }

    private void rect(double x, double y, double w, double h, int rgb) {
        rect(x, y, w, h, rgb, 1);
    }

    private void sprite(Img im, int ox, int oy, double alpha) {
        for (int y = 0; y < im.h; y++)
            for (int x = 0; x < im.w; x++) {
                int p = im.px[y * im.w + x];
                int a = (p >>> 24) & 255;
                if (a == 0) continue;
                blend(ox + x, oy + y, p & 0xFFFFFF, a / 255.0 * alpha);
            }
    }

    private static int mix(int a, int b, double t) {
        int r = (int) (((a >> 16) & 255) + ((((b >> 16) & 255) - ((a >> 16) & 255)) * t));
        int g = (int) (((a >> 8) & 255) + ((((b >> 8) & 255) - ((a >> 8) & 255)) * t));
        int bl = (int) ((a & 255) + (((b & 255) - (a & 255)) * t));
        return (r << 16) | (g << 8) | bl;
    }

    private static double mod(double a, double m) {
        double r = a % m;
        return r < 0 ? r + m : r;
    }

    // ---------------- fotograma ----------------

    public void render(double t) {
        if (!ok) return;
        System.arraycopy(bgStatic.px, 0, buf, 0, buf.length);
        for (int i = 0; i < buf.length; i++) buf[i] |= 0xFF000000;
        switch (BG) {
            case 'A' -> forge(t);
            case 'B' -> hack(t);
            default -> servers(t);
        }
        for (int i = 0; i < 3; i++) {
            if (BG == 'B') pedestal(xs[i], t, i);
            Img s = statues[i];
            sprite(s, Math.round(xs[i] - s.w / 2f), feet - s.h + 6, 1);
            if (BG != 'B') pedestal(xs[i], t, i);
        }
        if (BG == 'B' && mod(t, 4.3) < .12) {
            int y = (int) Math.floor(rnd(Math.floor(t / 4.3)) * 200);
            for (int j = y; j < Math.min(H, y + 6); j++) {
                int row = j * W;
                System.arraycopy(buf, row, buf, row + 3, W - 3);
            }
            rect(0, y, W, 1, 0x4dff88, .18);
        }
        for (int y = 196; y < H; y++) rect(0, y, W, 1, 0x020604, (y - 196) / 74.0 * .7);

        NativeImage ni = tex.getPixels();
        if (ni == null) return;
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++) {
                int p = buf[y * W + x];
                int r = (p >> 16) & 255, g = (p >> 8) & 255, b = p & 255;
                ni.setPixelRGBA(x, y, 0xFF000000 | (b << 16) | (g << 8) | r);
            }
        tex.upload();
    }

    private void forge(double t) {
        sprite(glow, 0, 0, .55 + .45 * (0.5 + 0.5 * Math.sin(t * 1.6)));
        for (int i = 0; i < cables.length; i++) {
            int[][] p = cables[i];
            for (int k = 0; k < 2; k++) {
                double u = mod(t * .35 + i * .17 + k * .5, 1);
                int[] q = p[(int) Math.floor(u * (p.length - 1))];
                rect(q[0] - 1, q[1], 3, 3, 0x4dff88, .9);
                rect(q[0], q[1] + 1, 1, 1, 0xeaffef);
            }
        }
        for (int k = 0; k < 26; k++) {
            double life = mod(t * .4 + rnd(k), 1);
            double x = forgeCx - 60 + rnd(k * 3) * 120 + Math.sin(t * 2 + k) * 4;
            double y = feet + 6 - life * 150;
            rect(x, y, 1, 1, life < .7 ? 0xb8ffcf : 0x2fae5a);
        }
    }

    private void hack(double t) {
        for (int i = 0; i < 80; i++) {
            int x = i * 6;
            double sp = 18 + rnd(i) * 40, ph = rnd(i + 3) * 400;
            int len = 6 + (int) Math.floor(rnd(i + 7) * 12);
            double head = mod(t * sp + ph, hackHz + 120) - 20;
            for (int j = 0; j < len; j++) {
                int y = (int) Math.floor(head - j * 6);
                if (y < 0 || y > hackHz - 5) continue;
                double a = 1 - (double) j / len;
                double gseed = Math.floor(t * 3 + x + j);
                for (int r = 0; r < 5; r++)
                    for (int q = 0; q < 3; q++)
                        if (rnd(gseed * 13 + r * 3 + q) > .45) {
                            if (j == 0) blend(x + q, y + r, 0xdcffe6, .9);
                            else blend(x + q, y + r, 0x3ce678, a * .55);
                        }
            }
        }
        double fl = .75 + .25 * Math.sin(t * 7) * Math.sin(t * 2.3);
        sprite(holo, 0, 0, fl);
        for (int k = 0; k < 8; k++) {
            double z = mod(k + t * .6, 8) / 8;
            int y = (int) Math.round(hackHz + (H - hackHz) * z * z);
            if (y > hackHz) rect(0, y, W, 1, 0x1c9a4a, .25 + z * .6);
        }
    }

    private void servers(double t) {
        for (float[] l : leds) {
            double ph = l[3];
            boolean on = Math.sin(t * (2 + ph * 6) + ph * 30) > -.2;
            if (on) rect(l[0], l[1], l[2], l[2], l[4] > 0 ? 0xffc24a : 0x5dff95);
        }
        for (int i = 0; i < trays.length; i++) {
            int[][] p = trays[i];
            double u = mod(t * .25 + i * .3, 1);
            int[] q = p[(int) Math.floor(u * (p.length - 1))];
            rect(q[0], q[1], 2, 2, 0x78ffaa, .9);
        }
        double a = .10 + .06 * Math.sin(t * 1.3);
        rect(serverCx - serverBw + 6, serverVy - 40, serverBw * 2 - 12, 80, 0x9dffc0, a);
    }

    private void pedestal(int cx, double t, int i) {
        if (BG == 'A') {
            rect(cx - 24, feet, 48, 16, 0x050806);
            rect(cx - 23, feet + 1, 46, 3, 0x3a4d42);
            rect(cx - 23, feet + 4, 46, 11, 0x1a2620);
            rect(cx - 21, feet + 8, 42, 1, 0x0a120d);
            double a = .5 + .5 * Math.sin(t * 2 + i);
            for (int k = 0; k < 6; k++) rect(cx - 15 + k * 6, feet + 10, 3, 2, mix(0x14532b, 0x7dffaa, a));
        } else if (BG == 'B') {
            double a = .55 + .25 * Math.sin(t * 3 + i * 2);
            for (int y = 0; y < 120; y++) rect(cx - 18, feet - y, 36, 1, 0x4dff88, (1 - y / 120.0) * .16 * a);
            for (int k = 0; k < 64; k++) {
                double an = k / 64.0 * Math.PI * 2;
                rect(cx + Math.cos(an) * 24, feet + 2 + Math.sin(an) * 5, 1, 1, 0x4dff88, .5 + a * .5);
            }
            for (int k = 0; k < 48; k++) {
                double an = k / 48.0 * Math.PI * 2;
                rect(cx + Math.cos(an) * 16, feet + 2 + Math.sin(an) * 3, 1, 1, 0xb4ffd2, .7);
            }
        } else {
            rect(cx - 22, feet, 44, 15, 0x030504);
            rect(cx - 21, feet + 1, 42, 13, 0x141c18);
            rect(cx - 21, feet + 1, 42, 1, 0x2c3d34);
            for (int k = 0; k < 5; k++) {
                boolean on = Math.sin(t * 5 + k * 1.7 + i) > 0;
                rect(cx - 16 + k * 4, feet + 6, 2, 2, on ? 0x6dffa0 : 0x163a24);
            }
            rect(cx + 6, feet + 5, 11, 4, 0x08100b);
        }
    }
}
