package kare.ssu.utils;

import kare.ssu.RecipeQuery;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public class RecipeQueryReloadListener implements ResourceManagerReloadListener {
    public static void register() {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloader(getResourceLocation(), new RecipeQueryReloadListener());
    }

    public static Identifier getResourceLocation() {
        return Identifier.fromNamespaceAndPath("ssu", "data/recipe_chains");
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        resourceManager.getResource(getResourceLocation()).ifPresent(resource -> RecipeQuery.INSTANCE.reloadChains(resource));
    }
}
