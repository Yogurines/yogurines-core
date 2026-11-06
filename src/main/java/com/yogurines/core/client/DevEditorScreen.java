package com.yogurines.core.client;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

/**
 * Editor del modo desarrollador: IP del server del botón Jugar, textos de los botones,
 * mostrar u ocultar el +, y cambiar la contraseña.
 * "Guardar" lo aplica en este PC; "Publicar para todos" además copia el server.json y abre GitHub
 * para pegarlo, y así les llega a todos los jugadores al abrir el juego.
 */
public class DevEditorScreen extends DevScreen {
    private EditBox ip, play, quit, pass;
    private boolean showPlus;
    private HoloButton plusBtn;
    private String status = "";
    private long statusT;

    public DevEditorScreen(Screen parent) {
        super(parent, "Modo desarrollador · Editor", 300, 214);
    }

    @Override
    protected void init() {
        super.init();
        CoreConfig c = CoreConfig.get();
        int x = px + 10, w = pw - 20, half = (w - 10) / 2;
        ip = box(x, py + 32, w, c.ip, 100);
        play = box(x, py + 66, half, c.playText, 24);
        quit = box(x + half + 10, py + 66, half, c.quitText, 24);
        showPlus = c.showPlus;
        plusBtn = addRenderableWidget(HoloButton.plain(x, py + 100, half, 16, plusLabel(), b -> {
            showPlus = !showPlus;
            b.setMessage(plusLabel());
        }));
        pass = box(x + half + 10, py + 100, half, "", 64);
        pass.setFormatter((s, i) -> FormattedCharSequence.forward("*".repeat(s.length()), Style.EMPTY));
        int bw = (w - 10) / 2;
        addRenderableWidget(HoloButton.plain(x, py + ph - 50, bw, 18, Component.literal("Guardar en este PC"), b -> save(false)));
        addRenderableWidget(HoloButton.plain(x + bw + 10, py + ph - 50, bw, 18, Component.literal("Publicar para todos"), b -> save(true)));
        addRenderableWidget(HoloButton.plain(x, py + ph - 26, w, 18, Component.literal("Salir del modo desarrollador"), b -> onClose()));
        setInitialFocus(ip);
    }

    private Component plusLabel() {
        return Component.literal(showPlus ? "Visible" : "Oculto");
    }

    private EditBox box(int x, int y, int w, String value, int max) {
        EditBox b = new EditBox(font, x, y, w, 16, Component.empty());
        b.setMaxLength(max);
        b.setValue(value == null ? "" : value);
        addRenderableWidget(b);
        return b;
    }

    private void save(boolean publish) {
        CoreConfig c = CoreConfig.get();
        c.ip = ip.getValue().trim();
        c.playText = play.getValue().isBlank() ? "Jugar" : play.getValue();
        c.quitText = quit.getValue().isBlank() ? "Salir" : quit.getValue();
        c.showPlus = showPlus;
        if (!pass.getValue().isEmpty()) {
            c.passHash = CoreConfig.sha256(pass.getValue());
            pass.setValue("");
        }
        CoreConfig.save();
        if (publish) {
            minecraft.keyboardHandler.setClipboard(c.serverJson());
            Util.getPlatform().openUri(CoreConfig.editUrl());
            status = "Copiado. En GitHub: pega, y pulsa Commit changes";
        } else {
            status = "Guardado en este PC";
        }
        statusT = Util.getMillis();
    }

    @Override
    protected void drawPanel(GuiGraphics g, int mx, int my, float pt) {
        int x = px + 10, half = (pw - 30) / 2;
        label(g, "IP del servidor (botón Jugar)", x, py + 22);
        label(g, "Texto de Jugar", x, py + 56);
        label(g, "Texto de Salir", x + half + 10, py + 56);
        label(g, "Botón + (solitario)", x, py + 90);
        label(g, "Nueva contraseña", x + half + 10, py + 90);
        g.drawString(font, "Vacío = sin IP: Jugar abre la lista de servers.", x, py + 124, 0xFF6F8A79, false);
        g.drawString(font, "Publicar: copia la config y abre GitHub.", x, py + 136, 0xFF6F8A79, false);
        if (!status.isEmpty() && Util.getMillis() - statusT < 6000)
            g.drawString(font, status, x, py + ph - 62, 0xFF4DFF88, false);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(new YogurinesTitleScreen());
    }
}
