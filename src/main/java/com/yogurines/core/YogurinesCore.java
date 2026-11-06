package com.yogurines.core;

import com.yogurines.core.client.CoreConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** Los Yogurines VI: Overclock. Mod solo de cliente con el estilo del server. */
@Mod(YogurinesCore.MOD_ID)
public class YogurinesCore {
    public static final String MOD_ID = "yogurines_core";

    public YogurinesCore() {
        if (FMLEnvironment.dist.isClient()) {
            CoreConfig.load();
            CoreConfig.fetchOnline();
        }
    }
}
