package com.yogurines.core.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.Tickable;
import net.minecraft.resources.ResourceLocation;

/**
 * Fondo de las pantallas de ajustes, crear mundo, etc. (el "de tierra" de Minecraft) cambiado por
 * una rejilla verde con movimiento: pulsos de luz que recorren las líneas y un brillo que respira.
 * Sustituye a la textura minecraft:textures/gui/options_background.png, así se anima en todas partes.
 * Minecraft oscurece esta textura al dibujarla, por eso los colores de aquí son claros.
 */
public class GridTexture extends DynamicTexture implements Tickable {
    public static final ResourceLocation BG = new ResourceLocation("minecraft", "textures/gui/options_background.png");
    private static final int N = 16;
    private static GridTexture instance;
    private int tick;

    private GridTexture() {
        super(N, N, false);
        draw();
        upload();
    }

    /** Se asegura de que la rejilla animada está puesta (por si una recarga de recursos la quitó). */
    public static void ensure() {
        TextureManager tm = Minecraft.getInstance().getTextureManager();
        if (instance == null) instance = new GridTexture();
        AbstractTexture cur = tm.getTexture(BG, null);
        if (cur != instance) tm.register(BG, instance);
    }

    @Override
    public void tick() {
        tick++;
        draw();
        upload();
    }

    private static int abgr(int r, int g, int b) {
        return 0xFF000000 | (b << 16) | (g << 8) | r;
    }

    private void draw() {
        NativeImage im = getPixels();
        if (im == null) return;
        double t = tick / 20.0;
        double breathe = 0.5 + 0.5 * Math.sin(t * 1.4);
        int pos = (tick / 2) % (N * 2); // recorrido del pulso por las dos líneas
        for (int y = 0; y < N; y++)
            for (int x = 0; x < N; x++) {
                int r = 0x2a, g = 0x40, b = 0x33;
                boolean line = x == 0 || y == 0;
                if (line) {
                    r = (int) (0x2e + 0x18 * breathe);
                    g = (int) (0xc8 + 0x28 * breathe);
                    b = (int) (0x62 + 0x18 * breathe);
                    // distancia del píxel al pulso que recorre la línea de arriba y luego la de la izquierda
                    int p = y == 0 ? x : N + y;
                    int d = Math.floorMod(pos - p, N * 2);
                    if (d < 4) {
                        double k = 1 - d / 4.0;
                        r += (int) ((255 - r) * k);
                        g += (int) ((255 - g) * k);
                        b += (int) ((255 - b) * k);
                    }
                }
                if (x == 0 && y == 0) { r = 0xa0; g = 0xff; b = 0xc8; }
                im.setPixelRGBA(x, y, abgr(r, g, b));
            }
    }
}
