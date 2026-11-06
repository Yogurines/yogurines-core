package com.yogurines.core.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

/** Pide la contraseña del modo desarrollador (Ctrl+Shift+D en el menú). */
public class DevPasswordScreen extends DevScreen {
    private EditBox box;
    private boolean error;

    public DevPasswordScreen(Screen parent) {
        super(parent, "Modo desarrollador", 200, 100);
    }

    @Override
    protected void init() {
        super.init();
        box = new EditBox(font, px + 10, py + 36, pw - 20, 16, Component.literal("Contraseña"));
        box.setMaxLength(64);
        box.setFormatter((s, i) -> FormattedCharSequence.forward("*".repeat(s.length()), Style.EMPTY));
        box.setResponder(s -> error = false);
        addRenderableWidget(box);
        addRenderableWidget(HoloButton.plain(px + 10, py + ph - 26, (pw - 28) / 2, 18, Component.literal("Entrar"), b -> submit()));
        addRenderableWidget(HoloButton.plain(px + 18 + (pw - 28) / 2, py + ph - 26, (pw - 28) / 2, 18, Component.literal("Cancelar"), b -> onClose()));
        setInitialFocus(box);
    }

    private void submit() {
        if (CoreConfig.get().checkPassword(box.getValue())) {
            minecraft.setScreen(new DevEditorScreen(parent));
        } else {
            error = true;
            box.setValue("");
        }
    }

    @Override
    protected void drawPanel(GuiGraphics g, int mx, int my, float pt) {
        label(g, "Contraseña:", px + 10, py + 24);
        if (error) g.drawString(font, "Contraseña incorrecta", px + 10, py + 56, 0xFFFF8A8A, false);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            submit();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }
}
