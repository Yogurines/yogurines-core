package com.yogurines.core.client;

import com.yogurines.core.YogurinesCore;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

/**
 * Menú principal de Los Yogurines VI: Overclock.
 * Diseño en un lienzo virtual de 480x270: el fondo cubre toda la ventana y el logo y los botones
 * se ajustan para verse siempre enteros.
 */
public class YogurinesTitleScreen extends Screen {
    private static final ResourceLocation LOGO = new ResourceLocation(YogurinesCore.MOD_ID, "textures/menu/logo.png");
    private static final int LOGO_W = 301, LOGO_H = 80;
    private static final long START = Util.getMillis();
    private float k = 1, ux, uy;

    public YogurinesTitleScreen() {
        super(Component.literal("Los Yogurines VI"));
    }

    @Override
    protected void init() {
        k = Math.min(width / (float) MenuRenderer.W, height / (float) MenuRenderer.H);
        ux = (width - MenuRenderer.W * k) / 2f;
        uy = (height - MenuRenderer.H * k) / 2f;
        CoreConfig c = CoreConfig.get();
        addRenderableWidget(new HoloButton(ux, uy, k, 140, 204, 200, 20, Component.literal(c.playText), null, b -> play()));
        addRenderableWidget(new HoloButton(ux, uy, k, 140, 228, 20, 20, Component.translatable("menu.options"), HoloButton.GEAR,
                b -> minecraft.setScreen(new OptionsScreen(this, minecraft.options))));
        if (c.showPlus) {
            addRenderableWidget(new HoloButton(ux, uy, k, 164, 228, 152, 20, Component.literal(c.quitText), null, b -> minecraft.stop()));
            addRenderableWidget(new HoloButton(ux, uy, k, 320, 228, 20, 20, Component.translatable("menu.singleplayer"), HoloButton.PLUS,
                    b -> minecraft.setScreen(new SelectWorldScreen(this))));
        } else {
            addRenderableWidget(new HoloButton(ux, uy, k, 164, 228, 176, 20, Component.literal(c.quitText), null, b -> minecraft.stop()));
        }
    }

    private void play() {
        String ip = CoreConfig.get().ip;
        if (ip == null || ip.isBlank()) {
            // Sin IP configurada: abre la lista de servidores normal.
            minecraft.setScreen(new JoinMultiplayerScreen(this));
            return;
        }
        ServerData data = new ServerData("Los Yogurines VI", ip.trim(), false);
        ConnectScreen.startConnecting(this, minecraft, ServerAddress.parseString(ip.trim()), data, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        double t = (Util.getMillis() - START) / 1000.0;
        MenuRenderer r = MenuRenderer.get();
        g.fill(0, 0, width, height, 0xFF020604);
        if (r.ready()) {
            r.render(t);
            float kc = Math.max(width / (float) MenuRenderer.W, height / (float) MenuRenderer.H);
            float bx = (width - MenuRenderer.W * kc) / 2f, by = (height - MenuRenderer.H * kc) / 2f;
            g.pose().pushPose();
            g.pose().translate(bx, by, 0);
            g.pose().scale(kc, kc, 1);
            g.blit(MenuRenderer.TEX, 0, 0, 0, 0, MenuRenderer.W, MenuRenderer.H, MenuRenderer.W, MenuRenderer.H);
            g.pose().popPose();
        }
        g.pose().pushPose();
        g.pose().translate(ux, uy, 0);
        g.pose().scale(k, k, 1);
        drawLogo(g, 240 - (LOGO_W + 1) / 2, 0, t);
        g.drawString(font, "Los Yogurines VI · Overclock", 4, 260, 0xFF6F8A79, false);
        String v = "v1.0";
        g.drawString(font, v, MenuRenderer.W - font.width(v) - 4, 260, 0xFF6F8A79, false);
        g.pose().popPose();
        super.render(g, mx, my, pt);
    }

    /** Logo neón con un pequeño fallo de señal cada pocos segundos (franjas desplazadas). */
    private void drawLogo(GuiGraphics g, int x, int y, double t) {
        boolean glitch = (t % 3.1) < .14;
        if (!glitch) {
            g.blit(LOGO, x, y, 0, 0, LOGO_W, LOGO_H, LOGO_W, LOGO_H);
            return;
        }
        long seed = (long) Math.floor(t / 3.1);
        int[] band = new int[LOGO_H];
        for (int i = 0; i < 3; i++) {
            int yy = 6 + (int) Math.floor(MenuRenderer.rnd(seed * 5 + i) * 60);
            int dx = MenuRenderer.rnd(seed + i) > .5 ? 4 : -4;
            for (int j = yy; j < Math.min(LOGO_H, yy + 3); j++) band[j] = dx;
        }
        int start = 0;
        for (int j = 1; j <= LOGO_H; j++) {
            if (j == LOGO_H || band[j] != band[start]) {
                g.blit(LOGO, x + band[start], y + start, 0, start, LOGO_W, j - start, LOGO_W, LOGO_H);
                start = j;
            }
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_D && hasControlDown() && hasShiftDown()) {
            minecraft.setScreen(new DevPasswordScreen(this));
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
