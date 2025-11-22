package com.retro.eaglevision;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.IronGolem;

import java.util.List;

@Mod.EventBusSubscriber(modid = EagleVisionMod.MOD_ID, value = Dist.CLIENT)
public class EagleVisionEventHandler {
    private static boolean isEagleVisionActive = false;
    private static int visionRadius = 50;
    private static final ResourceLocation DESATURATE_SHADER = ResourceLocation.fromNamespaceAndPath("eaglevision", "shaders/post/desaturate.json");
    private static CameraType lastCameraType = null;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getInstance();

            if (mc.player != null) {
                // Détecter le changement de caméra
                CameraType currentCamera = mc.options.getCameraType();
                if (isEagleVisionActive && lastCameraType != currentCamera) {
                    // Recharger le shader lors du changement de caméra
                    reloadShader(mc);
                    lastCameraType = currentCamera;
                }

                // Toggle Eagle Vision
                if (KeyBindings.EAGLE_VISION_KEY.consumeClick()) {
                    isEagleVisionActive = !isEagleVisionActive;

                    if (isEagleVisionActive) {
                        EntityColorConfig.load();
                        lastCameraType = mc.options.getCameraType();

                        try {
                            mc.gameRenderer.loadEffect(DESATURATE_SHADER);
                            System.out.println("[Eagle Vision] Shader activé");
                        } catch (Exception e) {
                            System.err.println("[Eagle Vision] Erreur lors du chargement du shader : " + e.getMessage());
                            e.printStackTrace();
                        }

                        mc.player.displayClientMessage(
                                net.minecraft.network.chat.Component.literal("§6Eagle Vision activee"),
                                true
                        );
                    } else {
                        lastCameraType = null;

                        try {
                            mc.gameRenderer.shutdownEffect();
                            System.out.println("[Eagle Vision] Shader désactivé");
                        } catch (Exception e) {
                            System.err.println("[Eagle Vision] Erreur lors de la désactivation du shader : " + e.getMessage());
                            e.printStackTrace();
                        }

                        mc.player.displayClientMessage(
                                net.minecraft.network.chat.Component.literal("§7Eagle Vision desactivee"),
                                true
                        );
                    }
                }
            }
        }
    }

    private static void reloadShader(Minecraft mc) {
        try {
            // Désactiver puis réactiver le shader
            mc.gameRenderer.shutdownEffect();
            mc.gameRenderer.loadEffect(DESATURATE_SHADER);
            System.out.println("[Eagle Vision] Shader rechargé après changement de caméra");
        } catch (Exception e) {
            System.err.println("[Eagle Vision] Erreur lors du rechargement du shader : " + e.getMessage());
        }
    }

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (!isEagleVisionActive) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            PoseStack poseStack = event.getPoseStack();

            AABB searchBox = mc.player.getBoundingBox().inflate(visionRadius);
            List<Entity> nearbyEntities = mc.level.getEntities(mc.player, searchBox);

            for (Entity entity : nearbyEntities) {
                // Exclure explicitement les items, projectiles, bateaux, minecarts, etc.
                if (entity instanceof net.minecraft.world.entity.item.ItemEntity ||
                        entity instanceof net.minecraft.world.entity.projectile.Projectile ||
                        entity instanceof net.minecraft.world.entity.vehicle.AbstractMinecart ||
                        entity instanceof net.minecraft.world.entity.vehicle.Boat ||
                        entity instanceof net.minecraft.world.entity.decoration.ArmorStand ||
                        entity instanceof net.minecraft.world.entity.decoration.ItemFrame ||
                        entity instanceof net.minecraft.world.entity.decoration.Painting) {
                    continue;
                }

                // Ne garder que les LivingEntity (créatures vivantes)
                if (!(entity instanceof LivingEntity)) {
                    continue;
                }

                // Exclure le joueur local en première personne
                boolean isLocalPlayerInFirstPerson = (entity instanceof Player &&
                        entity.equals(mc.player) &&
                        mc.options.getCameraType().isFirstPerson());

                if (!isLocalPlayerInFirstPerson) {
                    // Obtenir la couleur depuis la configuration
                    float[] color = EntityColorConfig.getColorForEntity(entity);

                    // Ne rendre que si une couleur est définie
                    if (color != null) {
                        renderGlowingEntity(entity, poseStack, color[0], color[1], color[2], event.getPartialTick());
                    }
                }
            }
        }
    }

    private static void renderGlowingEntity(Entity entity, PoseStack poseStack,
                                            float red, float green, float blue, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();

        poseStack.pushPose();

        double renderPosX = entity.xOld + (entity.getX() - entity.xOld) * partialTicks - mc.gameRenderer.getMainCamera().getPosition().x;
        double renderPosY = entity.yOld + (entity.getY() - entity.yOld) * partialTicks - mc.gameRenderer.getMainCamera().getPosition().y;
        double renderPosZ = entity.zOld + (entity.getZ() - entity.zOld) * partialTicks - mc.gameRenderer.getMainCamera().getPosition().z;

        poseStack.translate(renderPosX, renderPosY, renderPosZ);

        // Configurer le rendu avec glow
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515);
        RenderSystem.depthMask(false);

        // Forcer la couleur globale
        RenderSystem.setShaderColor(red * 3.0f, green * 3.0f, blue * 3.0f, 0.8f);

        var bufferSource = mc.renderBuffers().bufferSource();

        // Couche 1 : Silhouette principale
        poseStack.pushPose();
        try {
            mc.getEntityRenderDispatcher().render(entity, 0, 0, 0,
                    entity.getYRot(), partialTicks, poseStack, bufferSource, 15728880);
            bufferSource.endBatch();
        } catch (Exception e) {}
        poseStack.popPose();

        // Couche 2 : Glow moyen
        poseStack.pushPose();
        poseStack.scale(1.05f, 1.05f, 1.05f);
        poseStack.translate(0, -entity.getBbHeight() * 0.025f, 0);
        RenderSystem.setShaderColor(red * 2.5f, green * 2.5f, blue * 2.5f, 0.5f);
        try {
            mc.getEntityRenderDispatcher().render(entity, 0, 0, 0,
                    entity.getYRot(), partialTicks, poseStack, bufferSource, 15728880);
            bufferSource.endBatch();
        } catch (Exception e) {}
        poseStack.popPose();

        // Couche 3 : Glow externe
        poseStack.pushPose();
        poseStack.scale(1.1f, 1.1f, 1.1f);
        poseStack.translate(0, -entity.getBbHeight() * 0.05f, 0);
        RenderSystem.setShaderColor(red * 2.0f, green * 2.0f, blue * 2.0f, 0.3f);
        try {
            mc.getEntityRenderDispatcher().render(entity, 0, 0, 0,
                    entity.getYRot(), partialTicks, poseStack, bufferSource, 15728880);
            bufferSource.endBatch();
        } catch (Exception e) {}
        poseStack.popPose();

        // Restaurer
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.depthFunc(515);
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();

        poseStack.popPose();
    }
}