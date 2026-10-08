package dev.shadowsoffire.fastsuite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeType;

public class FastSuite implements ModInitializer {
    public static final String MODID = "fastsuite";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
    public static final boolean DEBUG_MATCHING = "on".equalsIgnoreCase(System.getenv("FASTSUITE_DEBUG_MATCHING"));
    public static final Set<RecipeType<?>> indexedTypes = new HashSet<>();

    @Override
    public void onInitialize() {
        Path config = Path.of("config", MODID + ".properties");
        try {
            Files.createDirectories(config.getParent());
            if (Files.notExists(config)) Files.writeString(config, "# Recipe types to index, comma separated\nindexedRecipeTypes=minecraft:crafting,minecraft:smelting,minecraft:blasting,minecraft:smoking\n");
        } catch (IOException e) { LOGGER.error("Could not load FastSuite configuration", e); }

        // Mod recipe types are registered by other mods during initialization. Resolve configured IDs
        // after all registrations and datapack recipes are ready, before the server begins ticking.
        ServerLifecycleEvents.SERVER_STARTED.register(server -> loadIndexedTypes());
    }

    private static void loadIndexedTypes() {
        indexedTypes.clear();
        Path config = Path.of("config", MODID + ".properties");
        try {
            String line = Files.readAllLines(config).stream()
                .map(String::trim)
                .filter(s -> s.startsWith("indexedRecipeTypes="))
                .findFirst()
                .orElse("indexedRecipeTypes=minecraft:crafting,minecraft:smelting,minecraft:blasting,minecraft:smoking");
            for (String value : line.substring(line.indexOf('=') + 1).split(",")) {
                String id = value.trim();
                try {
                    RecipeType<?> type = BuiltInRegistries.RECIPE_TYPE.getValue(Identifier.parse(id));
                    if (type != null) indexedTypes.add(type);
                    else LOGGER.error("Unknown recipe type {} in FastSuite config; it will be ignored.", id);
                } catch (RuntimeException ex) {
                    LOGGER.error("Invalid recipe type {} in FastSuite config; it will be ignored.", id);
                }
            }
            LOGGER.info("FastSuite will index {} recipe types.", indexedTypes.size());
        } catch (IOException e) {
            LOGGER.error("Could not load FastSuite configuration", e);
        }
    }

    public static void registerSafeRecipeClass(Class<?> clazz) { CachedRecipeList.parallelRecipeClassCache.put(clazz, true); }
    public static void registerSafeIngredientClass(Class<?> clazz) { CachedRecipeList.ingredientClassCache.put(clazz, true); }
}
