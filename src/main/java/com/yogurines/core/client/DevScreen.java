package com.yogurines.core.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Base de las pantallas del modo desarrollador: el menú detrás oscurecido y un panel verde. */
abstract class DevScreen extends Screen {
    protected final Screen parent;
    protected int px, py, pw, ph;

    protected DevScreen(Screen parent, String title, int pw, int ph) {
        super(Component.literal(title));
        this.parent = parent;
        this.pw = pw;
        this.ph = ph;
    }

    @Override
    protected void init() {
        parent.init(minecraft, width, height);
        pw = Math.min(pw, width - 8);
        ph = Math.min(ph, height - 8);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        parent.render(g, -1, -1, pt);
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        g.fill(0, 0, width, height, 0x99000000);
        g.fill(px, py, px + pw, py + ph, 0xFF4DFF88);
        g.fill(px + 1, py + 1, px + pw - 1, py + ph - 1, 0xFF08110C);
        g.fill(px + 1, py + 1, px + pw - 1, py + 15, 0xFF103A20);
        g.drawString(font, title, px + 6, py + 4, 0xFFD9FFE6, false);
        drawPanel(g, mx, my, pt);
        g.pose().popPose();
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        super.render(g, mx, my, pt);
        g.pose().popPose();
    }

    protected void drawPanel(GuiGraphics g, int mx, int my, float pt) {}

    protected void label(GuiGraphics g, String s, int x, int y) {
        g.drawString(font, s, x, y, 0xFF93C9A6, false);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
