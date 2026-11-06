package com.yogurines.core.client;

import com.yogurines.core.YogurinesCore;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = YogurinesCore.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {

    /** Cambia el menú principal de Minecraft por el de Los Yogurines VI y pone la rejilla animada. */
    @SubscribeEvent
    public static void onOpen(ScreenEvent.Opening e) {
        GridTexture.ensure();
        if (e.getNewScreen() instanceof TitleScreen) e.setNewScreen(new YogurinesTitleScreen());
    }

    /** Pausa y ajustes: cambia los botones normales por los de estilo "vanilla oscuro". */
    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post e) {
        Screen s = e.getScreen();
        if (!(s instanceof PauseScreen || s instanceof OptionsScreen || s instanceof OptionsSubScreen)) return;
        List<GuiEventListener> list = new ArrayList<>(e.getListenersList());
        for (GuiEventListener l : list) {
            if (l.getClass() == Button.class) {
                Button b = (Button) l;
                e.removeListener(b);
                e.addListener(new DarkButton(b));
            }
        }
    }
}
