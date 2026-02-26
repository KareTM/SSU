package kare.ssu.client;

import com.mojang.blaze3d.platform.InputConstants;
import kare.ssu.RecipeQuery;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class RecipeQueryClient implements ClientModInitializer {
    private static final Logger log = LoggerFactory.getLogger(RecipeQueryClient.class);
    private static Minecraft client;

    public static KeyMapping queryKey;
    public static KeyMapping enchantedKey;

    private static final Map<String, String> replacements = Map.ofEntries(
            Map.entry("Raw Porkchop", "Pork"),
            Map.entry("White Wool", "Wool"),
            Map.entry("Rabbit's Foot", "Rabbit Foot"),
            Map.entry("Wheat", "Hay"),
            Map.entry("Iron Ingot", "Iron"),
            Map.entry("Gold Ingot", "Gold"),
            Map.entry("Nether Quartz", "Quartz")
    );

    public static void onRecipeQueryKeyPressed(Slot slot) {
        if (slot == null)
            return;
        var item = slot.getItem();

        var name = item.getDisplayName().getString().replace("[", "").replace("]", "").toLowerCase();
        log.info(name);
        Pattern tradePattern = Pattern.compile(" x\\d+$");
        if (tradePattern.matcher(name).find()) {
            name = name.replaceAll(tradePattern.pattern(), "");
            log.info(name);
        }

        var customData = item.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            var reforge = customData.copyTag().get("modifier");
            if (reforge != null) {
                var reforge_str = reforge.asString();
                if (reforge_str.isPresent()) {
                    name = name.replace(reforge_str.get(), "");
                }
            }
            var thick = customData.copyTag().get("wood_singularity_count");
            if (thick != null) {
                var thickVal = thick.asInt();
                if (thickVal.isPresent() && thickVal.get() != 0) {
                    name = name.replace("thick", "");
                }
            }
        }

        String itemString = doSubstitution(name);
        assert client.player != null;
        client.player.connection.sendCommand("recipe " + itemString);
    }

    public static void onViewEnchanted(Slot slot) {
        if (slot == null)
            return;
        assert client.player != null;
        var customData = slot.getItem().get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            client.getChatListener().handleSystemMessage(Component.literal("This item has no data and cannot be checked.").withStyle(ChatFormatting.BOLD, ChatFormatting.RED, ChatFormatting.ITALIC), false);
            return;
        }
        var ID = Objects.requireNonNull(customData).copyTag().get("id");
        if (ID == null) {
            client.getChatListener().handleSystemMessage(Component.literal("This item has no ID and cannot be checked.").withStyle(ChatFormatting.BOLD, ChatFormatting.RED, ChatFormatting.ITALIC), false);
            return;
        }
        String id_string = ID.toString().replace("\"", "");
        var chains = RecipeQuery.INSTANCE.getChains();
        if (chains == null) {
            client.getChatListener().handleSystemMessage(Component.literal("Crafting chains data missing, try reloading your client (F3 + T). If it persists, report this issue.").withStyle(ChatFormatting.BOLD, ChatFormatting.RED, ChatFormatting.ITALIC), false);
            return;
        }

        if (id_string.contains("GEM")) {
            var split = id_string.split("_", 2);
            var gem_string = split.length > 1 ? split[1] : "";
            var chain_string = split[0];

            for (var chain : chains) {
                if (chain.isInChain(chain_string) && chain.getNext(chain_string) != null) {
                    client.player.connection.sendCommand("viewrecipe " + chain.getNext(chain_string) + "_" + gem_string);
                    return;
                }
            }
        } else {
            for (var chain : chains) {
                if (chain.isInChain(id_string) && chain.getNext(id_string) != null) {
                    client.player.connection.sendCommand("viewrecipe " + chain.getNext(id_string));
                    return;
                }
            }
        }

        client.getChatListener().handleSystemMessage(Component.literal("This item has no enchanted/upgraded version.").withStyle(ChatFormatting.BOLD, ChatFormatting.RED, ChatFormatting.ITALIC), false);
    }



    public static String doSubstitution(String input) {
        if (input.contains("Gemstone")) {
            var splitString = input.split("\\s", 3);
            if (splitString.length == 3) {
                return splitString[2];
            } else {
                return input;
            }
        }

        return replacements.getOrDefault(input, input);
    }


    @Override
    public void onInitializeClient() {
        client = Minecraft.getInstance();
        KeyMapping.Category recipequery = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("recipequery", "keys"));

        // The translation key of the keybinding's name
        // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
        // The keycode of the key
        // The translation key of the keybinding's category.
         queryKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.recipequery.query", // The translation key of the keybinding's name
                InputConstants.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_R, // The keycode of the key
                recipequery // The translation key of the keybinding's category.
        ));

        enchantedKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.recipequery.viewenchanted", // The translation key of the keybinding's name
                InputConstants.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_Y, // The keycode of the key
                recipequery // The translation key of the keybinding's category.
        ));

    }
}

