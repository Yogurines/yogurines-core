package com.yogurines.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;

/**
 * Botón "vanilla oscuro" (estilo B de la pausa): la misma forma y textura de piedra de Minecraft,
 * más oscura y verdosa, con borde verde al pasar el ratón. Sustituye a un botón vanilla con su misma acción.
 */
public class DarkButton extends Button {
    public DarkButton(Button original) {
        super(original.getX(), original.getY(), original.getWidth(), original.getHeight(), original.getMessage(), b -> original.onPress(), DEFAULT_NARRATION);
        this.active = original.active;
        this.visible = original.visible;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean hov = isHoveredOrFocused() && active;
        g.fill(x, y, x + w, y + h, hov ? 0xFF4DFF88 : 0xFF000000);
        int base = active ? 0xFF4B524E : 0xFF2C302E;
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, base);
        if (active) {
            int n = w * h / 9;
            for (int k = 0; k < n; k++) {
                int xx = x + 1 + (int) Math.floor(MenuRenderer.rnd(k * 3 + x) * (w - 2));
                int yy = y + 1 + (int) Math.floor(MenuRenderer.rnd(k * 7 + y) * (h - 2));
                g.fill(xx, yy, xx + 1, yy + 1, MenuRenderer.rnd(k) > .5 ? 0xFF434A46 : 0xFF555D58);
            }
        }
        int hi = hov ? 0xFF4DFF88 : active ? 0xFF6B746F : 0xFF3A403D;
        int lo = hov ? 0xFF2FAE5A : 0xFF2F3532;
        g.fill(x + 1, y + 1, x + w - 1, y + 2, hi);
        g.fill(x + 1, y + 1, x + 2, y + h - 1, active ? (hov ? 0xFF6B746F : hi) : hi);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, lo);
        g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, 0xFF2F3532);
        int color = !active ? 0xFFA0A0A0 : hov ? 0xFFB9FFD0 : 0xFFE0E0E0;
        renderString(g, Minecraft.getInstance().font, color);
    }
}
