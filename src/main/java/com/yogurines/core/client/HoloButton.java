package com.yogurines.core.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Botón "holograma" (estilo B elegido): fondo translúcido, contorno verde con esquinas cortadas
 * y, al pasar el ratón, líneas de escáner. Se dibuja en coordenadas virtuales escaladas por k.
 */
public class HoloButton extends Button {
    public static final String[] GEAR = {"...###...", ".#.###.#.", "..#####..", "###...###", "###...###", "###...###", "..#####..", ".#.###.#.", "...###..."};
    public static final String[] PLUS = {"...##...", "...##...", "...##...", "########", "########", "...##...", "...##...", "...##..."};

    private final float ox, oy, k;
    private final int vx, vy, vw, vh;
    private final String[] icon;

    public HoloButton(float ox, float oy, float k, int vx, int vy, int vw, int vh, Component msg, String[] icon, OnPress onPress) {
        super(Math.round(ox + vx * k), Math.round(oy + vy * k), Math.round(vw * k), Math.round(vh * k), msg, onPress, DEFAULT_NARRATION);
        this.ox = ox;
        this.oy = oy;
        this.k = k;
        this.vx = vx;
        this.vy = vy;
        this.vw = vw;
        this.vh = vh;
        this.icon = icon;
    }

    /** Botón en coordenadas normales de la interfaz (para las pantallas del modo desarrollador). */
    public static HoloButton plain(int x, int y, int w, int h, Component msg, OnPress onPress) {
        return new HoloButton(0, 0, 1, x, y, w, h, msg, null, onPress);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
        boolean hov = isHoveredOrFocused() && active;
        g.pose().pushPose();
        g.pose().translate(ox, oy, 0);
        g.pose().scale(k, k, 1);
        draw(g, vx, vy, vw, vh, hov);
        int tc = !active ? 0xFF3F7A55 : hov ? 0xFFFFFFFF : 0xFF5DFF95;
        if (icon != null) {
            int iw = icon[0].length(), ih = icon.length;
            int ix = vx + (vw - iw) / 2, iy = vy + (vh - ih) / 2;
            for (int j = 0; j < ih; j++)
                for (int i = 0; i < iw; i++)
                    if (icon[j].charAt(i) == '#') g.fill(ix + i, iy + j, ix + i + 1, iy + j + 1, tc);
        } else {
            Font f = Minecraft.getInstance().font;
            String s = getMessage().getString();
            int maxW = vw - 6;
            if (f.width(s) > maxW) s = f.plainSubstrByWidth(s, maxW);
            int tx = vx + (vw - f.width(s)) / 2, ty = vy + (vh - 8) / 2 + 1;
            g.drawString(f, s, tx + 1, ty + 1, 0xFF03140A, false);
            g.drawString(f, s, tx, ty, tc, false);
        }
        g.pose().popPose();
    }

    static void draw(GuiGraphics g, int x, int y, int w, int h, boolean hov) {
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, hov ? 0x8C146E37 : 0x99041C0E);
        int oc = hov ? 0xFFA6FFC6 : 0xFF2FD06A;
        g.fill(x + 2, y, x + w - 2, y + 1, oc);
        g.fill(x + 2, y + h - 1, x + w - 2, y + h, oc);
        g.fill(x, y + 2, x + 1, y + h - 2, oc);
        g.fill(x + w - 1, y + 2, x + w, y + h - 2, oc);
        g.fill(x + 1, y + 1, x + 2, y + 2, oc);
        g.fill(x + w - 2, y + 1, x + w - 1, y + 2, oc);
        g.fill(x + 1, y + h - 2, x + 2, y + h - 1, oc);
        g.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, oc);
        if (hov) {
            for (int yy = y + 2; yy < y + h - 2; yy += 2) g.fill(x + 2, yy, x + w - 2, yy + 1, 0x1F78FFAA);
            double t = Util.getMillis() / 1000.0;
            int sy = y + 2 + (int) Math.floor((t * 30) % Math.max(1, h - 4));
            g.fill(x + 2, sy, x + w - 2, sy + 1, 0x59C8FFDC);
        }
    }
}
