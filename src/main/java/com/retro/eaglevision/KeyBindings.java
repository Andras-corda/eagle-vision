package com.retro.eaglevision;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = EagleVisionMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class KeyBindings {
    public static final KeyMapping EAGLE_VISION_KEY = new KeyMapping(
            "key.eaglevision.activate",
            GLFW.GLFW_KEY_V,
            "key.categories.gameplay"
    );

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(EAGLE_VISION_KEY);
    }

    public static void register() {
    }
}