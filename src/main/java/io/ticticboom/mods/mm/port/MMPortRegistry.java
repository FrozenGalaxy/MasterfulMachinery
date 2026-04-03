package io.ticticboom.mods.mm.port;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.botania.mana.BotaniaManaPortType;
import io.ticticboom.mods.mm.port.energy.EnergyPortType;
import io.ticticboom.mods.mm.port.fluid.FluidPortType;
import io.ticticboom.mods.mm.port.item.ItemPortType;
import io.ticticboom.mods.mm.port.kinetic.CreateKineticPortType;
import io.ticticboom.mods.mm.port.mekanism.gas.MekanismGasPortType;
import io.ticticboom.mods.mm.port.mekanism.infuse.MekanismInfusePortType;
import io.ticticboom.mods.mm.port.mekanism.pigment.MekanismPigmentPortType;
import io.ticticboom.mods.mm.port.mekanism.slurry.MekanismSlurryPortType;
import io.ticticboom.mods.mm.port.pneumaticcraft.air.PneumaticAirPortType;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import io.ticticboom.mods.mm.util.ParserUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;

import java.util.*;

public class MMPortRegistry {
    private static final Map<ResourceLocation, PortType> PORT_TYPES = new HashMap<>();
    public static List<RegistryGroupHolder> PORTS = new ArrayList<>();
    public static final Map<ResourceLocation, List<io.ticticboom.mods.mm.model.PortModel>> PORT_MODELS_BY_CONTROLLER = new HashMap<>();
    public static final Map<ResourceLocation, java.util.Set<ResourceLocation>> PORT_TYPES_BY_CONTROLLER = new HashMap<>();

    public static void init() {
        register(Ref.Ports.ITEM, new ItemPortType());
        register(Ref.Ports.FLUID, new FluidPortType());
        register(Ref.Ports.ENERGY, new EnergyPortType());

        if (ModList.get().isLoaded("mekanism")) {
            register(Ref.Ports.MEK_GAS, new MekanismGasPortType());
            register(Ref.Ports.MEK_SLURRY, new MekanismSlurryPortType());
            register(Ref.Ports.MEK_PIGMENT, new MekanismPigmentPortType());
            register(Ref.Ports.MEK_INFUSE, new MekanismInfusePortType());
        }
        if (ModList.get().isLoaded("create")) {
            register(Ref.Ports.CREATE_KINETIC, new CreateKineticPortType());
        }

        if (ModList.get().isLoaded("pneumaticcraft")) {
            register(Ref.Ports.PNEUMATIC_AIR, new PneumaticAirPortType());
        }

        if (ModList.get().isLoaded("botania")) {
            register(Ref.Ports.BOTANIA_MANA, new BotaniaManaPortType());
        }
    }

    public static PortType get(ResourceLocation id) {
        return PORT_TYPES.get(id);
    }

    public static void register(ResourceLocation id, PortType type) {
        PORT_TYPES.put(id, type);
    }

    public static void rebuildPortCache() {
        PORT_MODELS_BY_CONTROLLER.clear();
        PORT_TYPES_BY_CONTROLLER.clear();
        for (RegistryGroupHolder holder : PORTS) {
            try {
                if (holder.getBlock().get() instanceof IPortBlock bp) {
                    var model = bp.getModel();
                    for (ResourceLocation controllerId : model.controllerIds().getIds()) {
                        PORT_MODELS_BY_CONTROLLER.computeIfAbsent(controllerId, x -> new ArrayList<>()).add(model);
                        PORT_TYPES_BY_CONTROLLER.computeIfAbsent(controllerId, x -> new java.util.HashSet<>()).add(model.type());
                    }
                }
            } catch (Exception ignored) { }
        }
    }

    // New flexible parser: accepts a JsonElement which can be a JsonObject with 'type' (existing),
    // or a KubeJS Item object (with 'id' or 'item' and possibly 'nbt'), or a primitive string like 'mod:item{...}'.
    public static IPortIngredient parseIngredient(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            throw new RuntimeException("Ingredient element is null");
        }
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            // If it already has a 'type' field, use normal dispatch
            if (obj.has("type")) {
                var type = ParserUtils.parseId(obj, "type");
                return PORT_TYPES.get(type).getParser().parseRecipeIngredient(obj);
            }
            // If it's a KubeJS Item object or similar, try to normalize to item-ingredient form
            JsonObject normalized = new JsonObject();
            normalized.addProperty("type", Ref.Ports.ITEM.toString());
            if (obj.has("item")) {
                normalized.add("item", obj.get("item"));
            } else if (obj.has("id")) {
                normalized.add("item", obj.get("id"));
            }
            if (obj.has("count")) normalized.add("count", obj.get("count"));
            if (obj.has("nbt")) normalized.add("nbt", obj.get("nbt"));
            if (obj.has("nbt_snbt")) normalized.add("nbt_snbt", obj.get("nbt_snbt"));
            if (obj.has("nbt_match")) normalized.add("nbt_match", obj.get("nbt_match"));
            return PORT_TYPES.get(Ref.Ports.ITEM).getParser().parseRecipeIngredient(normalized);
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            // support strings like "mod:item" or "mod:item{...snbt...}"
            String s = element.getAsString();
            String itemId = s;
            String snbt = null;
            int idx = s.indexOf('{');
            if (idx >= 0) {
                itemId = s.substring(0, idx);
                snbt = s.substring(idx);
            }
            JsonObject normalized = new JsonObject();
            normalized.addProperty("type", Ref.Ports.ITEM.toString());
            var itemEl = new com.google.gson.JsonPrimitive(itemId);
            normalized.add("item", itemEl);
            normalized.addProperty("count", 1);
            if (snbt != null) {
                normalized.addProperty("nbt_snbt", snbt);
            }
            return PORT_TYPES.get(Ref.Ports.ITEM).getParser().parseRecipeIngredient(normalized);
        }
        throw new RuntimeException("Unsupported ingredient format: " + element);
    }

    public static List<PortModel> getPortModelsForControllerId(ResourceLocation id) {
        if (id == null) return List.of();
        var list = PORT_MODELS_BY_CONTROLLER.get(id);
        if (list == null) return List.of();
        return List.copyOf(list);
    }
}
