package com.retro.eaglevision;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.registries.Registries;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;

import java.io.InputStreamReader;
import java.io.IOException;
import java.util.*;

public class EntityColorConfig {
    private static final Gson GSON = new Gson();
    private static final Map<String, ColorCategory> categories = new HashMap<>();
    private static ColorCategory defaultColor = null;
    private static boolean isLoaded = false;

    public static void load() {
        categories.clear();
        isLoaded = false;

        try {
            ResourceLocation configLocation = ResourceLocation.fromNamespaceAndPath("eaglevision", "entity_colors.json");

            Resource resource = Minecraft.getInstance().getResourceManager().getResourceOrThrow(configLocation);
            loadFromResource(resource);
            isLoaded = true;

        } catch (Exception e) {
            System.err.println("[Eagle Vision] Erreur lors du chargement de la configuration : " + e.getMessage());
            e.printStackTrace();
            loadDefaults();
            isLoaded = true;
        }
    }

    private static void ensureLoaded() {
        if (!isLoaded) {
            load();
        }
    }

    private static void loadFromResource(Resource resource) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(resource.open())) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);

            for (String categoryName : root.keySet()) {
                JsonObject categoryObj = root.getAsJsonObject(categoryName);
                ColorCategory category = parseCategory(categoryObj);
                categories.put(categoryName.toLowerCase(), category);

                if ("passive".equals(categoryName.toLowerCase())) {
                    defaultColor = category;
                }
            }

            System.out.println("[Eagle Vision] Configuration chargée : " + categories.size() + " catégories");
        }
    }

    private static ColorCategory parseCategory(JsonObject categoryObj) {
        ColorCategory category = new ColorCategory();

        // Parse couleur
        if (categoryObj.has("color")) {
            JsonObject colorObj = categoryObj.getAsJsonObject("color");
            category.red = colorObj.get("red").getAsFloat();
            category.green = colorObj.get("green").getAsFloat();
            category.blue = colorObj.get("blue").getAsFloat();
        }

        // Parse entités
        if (categoryObj.has("entities")) {
            categoryObj.getAsJsonArray("entities").forEach(element ->
                    category.entities.add(element.getAsString())
            );
        }

        // Parse tags
        if (categoryObj.has("tags")) {
            categoryObj.getAsJsonArray("tags").forEach(element ->
                    category.tags.add(element.getAsString())
            );
        }

        return category;
    }

    private static void loadDefaults() {
        // Hostile (rouge)
        ColorCategory hostile = new ColorCategory();
        hostile.red = 1.0f;
        hostile.green = 0.0f;
        hostile.blue = 0.0f;
        hostile.entities.add("minecraft:zombie");
        hostile.entities.add("minecraft:skeleton");
        hostile.entities.add("minecraft:creeper");
        categories.put("hostile", hostile);

        // Passive (jaune)
        ColorCategory passive = new ColorCategory();
        passive.red = 1.0f;
        passive.green = 0.85f;
        passive.blue = 0.0f;
        passive.entities.add("minecraft:pig");
        passive.entities.add("minecraft:cow");
        passive.entities.add("minecraft:sheep");
        categories.put("passive", passive);
        defaultColor = passive;

        // Player (bleu)
        ColorCategory player = new ColorCategory();
        player.red = 0.0f;
        player.green = 0.7f;
        player.blue = 1.0f;
        categories.put("player", player);
    }

    public static float[] getColorForEntity(Entity entity) {
        // S'assurer que la config est chargée
        ensureLoaded();

        Minecraft mc = Minecraft.getInstance();

        // Joueur
        if (entity instanceof net.minecraft.world.entity.player.Player) {
            ColorCategory playerCat = categories.get("player");
            if (playerCat != null) {
                return new float[]{playerCat.red, playerCat.green, playerCat.blue};
            }
            return new float[]{0.0f, 0.5f, 1.0f}; // Bleu par défaut
        }

        EntityType<?> entityType = entity.getType();
        ResourceLocation entityId = EntityType.getKey(entityType);
        String entityIdString = entityId.toString();

        // Vérifier si c'est une créature apprivoisée/alliée
        if (entity instanceof net.minecraft.world.entity.TamableAnimal) {
            net.minecraft.world.entity.TamableAnimal tamable = (net.minecraft.world.entity.TamableAnimal) entity;

            // Seulement si apprivoisé ET propriétaire = joueur
            if (tamable.isTame() && tamable.getOwner() != null && tamable.getOwner().equals(mc.player)) {
                ColorCategory allyCat = categories.get("ally");
                if (allyCat != null) {
                    return new float[]{allyCat.red, allyCat.green, allyCat.blue};
                }
                return new float[]{0.0f, 0.5f, 1.0f}; // Bleu par défaut
            }

            // Si apprivoisé par quelqu'un d'autre ET hostile envers nous
            if (tamable.isTame() && tamable.getOwner() != null && !tamable.getOwner().equals(mc.player)) {
                // Vérifier si l'entité nous attaque
                if (entity instanceof net.minecraft.world.entity.Mob) {
                    net.minecraft.world.entity.Mob mob = (net.minecraft.world.entity.Mob) entity;
                    if (mob.getTarget() != null && mob.getTarget().equals(mc.player)) {
                        ColorCategory hostileCat = categories.get("hostile");
                        if (hostileCat != null) {
                            return new float[]{hostileCat.red, hostileCat.green, hostileCat.blue};
                        }
                        return new float[]{1.0f, 0.0f, 0.0f}; // Rouge
                    }
                }
                // Sinon, ne pas colorer (neutre)
                return null;
            }

            // Loup sauvage non apprivoisé
            if (!tamable.isTame()) {
                // Vérifier s'il est hostile (nous attaque)
                if (entity instanceof net.minecraft.world.entity.Mob) {
                    net.minecraft.world.entity.Mob mob = (net.minecraft.world.entity.Mob) entity;
                    if (mob.getTarget() != null && mob.getTarget().equals(mc.player)) {
                        ColorCategory hostileCat = categories.get("hostile");
                        if (hostileCat != null) {
                            return new float[]{hostileCat.red, hostileCat.green, hostileCat.blue};
                        }
                        return new float[]{1.0f, 0.0f, 0.0f}; // Rouge
                    }
                }
                // Sinon neutre (non coloré)
                return null;
            }
        }

        // Golems de fer
        if (entity instanceof net.minecraft.world.entity.animal.IronGolem) {
            net.minecraft.world.entity.animal.IronGolem golem = (net.minecraft.world.entity.animal.IronGolem) entity;

            // Si créé par le joueur
            if (golem.isPlayerCreated()) {
                ColorCategory allyCat = categories.get("ally");
                if (allyCat != null) {
                    return new float[]{allyCat.red, allyCat.green, allyCat.blue};
                }
                return new float[]{0.0f, 0.5f, 1.0f}; // Bleu par défaut
            }

            // Golem de village : vérifier s'il nous attaque
            if (golem.getTarget() != null && golem.getTarget().equals(mc.player)) {
                ColorCategory hostileCat = categories.get("hostile");
                if (hostileCat != null) {
                    return new float[]{hostileCat.red, hostileCat.green, hostileCat.blue};
                }
                return new float[]{1.0f, 0.0f, 0.0f}; // Rouge
            }

            // Sinon neutre (non coloré)
            return null;
        }

        // Golems de neige (toujours alliés si créés par le joueur)
        if (entity instanceof net.minecraft.world.entity.animal.SnowGolem) {
            ColorCategory allyCat = categories.get("ally");
            if (allyCat != null) {
                return new float[]{allyCat.red, allyCat.green, allyCat.blue};
            }
            return new float[]{0.0f, 0.5f, 1.0f}; // Bleu par défaut
        }

        // Vérifier si c'est un mob hostile qui nous cible
        if (entity instanceof net.minecraft.world.entity.Mob) {
            net.minecraft.world.entity.Mob mob = (net.minecraft.world.entity.Mob) entity;
            if (mob.getTarget() != null && mob.getTarget().equals(mc.player)) {
                ColorCategory hostileCat = categories.get("hostile");
                if (hostileCat != null) {
                    return new float[]{hostileCat.red, hostileCat.green, hostileCat.blue};
                }
                return new float[]{1.0f, 0.0f, 0.0f}; // Rouge
            }
        }

        // Ordre de priorité des catégories (du plus spécifique au plus général)
        String[] priorityOrder = {"ally", "hostile", "green", "purple", "orange", "gray", "yellow"};

        for (String categoryName : priorityOrder) {
            ColorCategory category = categories.get(categoryName);
            if (category == null) continue;

            // Vérifier les entités spécifiques
            if (category.entities.contains(entityIdString)) {
                return new float[]{category.red, category.green, category.blue};
            }

            // Vérifier les tags
            for (String tagString : category.tags) {
                try {
                    String[] parts = tagString.split(":");
                    if (parts.length == 2) {
                        ResourceLocation tagLocation = ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
                        TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, tagLocation);
                        if (entityType.is(tag)) {
                            return new float[]{category.red, category.green, category.blue};
                        }
                    }
                } catch (Exception e) {
                    // Tag invalide, ignorer
                }
            }
        }

        // Pas de couleur par défaut - les créatures passives/neutres ne sont pas colorisées
        return null;
    }

    private static class ColorCategory {
        float red = 1.0f;
        float green = 1.0f;
        float blue = 1.0f;
        List<String> entities = new ArrayList<>();
        List<String> tags = new ArrayList<>();
    }
}