package com.retro.eaglevision;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(EagleVisionMod.MOD_ID)
public class EagleVisionMod {
    public static final String MOD_ID = "eaglevision";

    public EagleVisionMod(FMLJavaModLoadingContext context) {
        context.getModEventBus().addListener(this::clientSetup);

        KeyBindings.register();
        MinecraftForge.EVENT_BUS.register(new EagleVisionEventHandler());

        System.out.println("Eagle Vision Mod charge !");
    }

    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            EntityColorConfig.load();
        });
    }
}