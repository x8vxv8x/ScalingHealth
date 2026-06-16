package com.smd.scalinghealth.wealth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.smd.scalinghealth.ScalingHealth;
import com.smd.scalinghealth.config.Config;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class WealthDropTable {
    public static final WealthDropTable INSTANCE = new WealthDropTable();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "wealth_drops.json";

    private File file;
    private Map<ResourceLocation, List<DropPool>> dropsByEntity = Collections.emptyMap();
    private Stats stats = Stats.empty();

    private WealthDropTable() {
    }

    public void init(File modConfigDirectory) {
        File scalingHealthDirectory = new File(modConfigDirectory, "scalinghealth");
        if (!scalingHealthDirectory.exists() && !scalingHealthDirectory.mkdirs()) {
            ScalingHealth.LOGGER.warn("Could not create Scaling Health config directory: {}", scalingHealthDirectory);
        }
        file = new File(scalingHealthDirectory, FILE_NAME);
        reload();
    }

    public Stats reload() {
        ensureFile();

        Builder builder = new Builder();
        try (FileReader reader = new FileReader(file)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) {
                warn("Wealth drop table is empty: {}", file);
            } else {
                readRoot(root, builder);
            }
        } catch (IOException | JsonParseException | IllegalStateException ex) {
            builder.invalidEntries++;
            ScalingHealth.LOGGER.error("Could not load wealth drop table: {}", file, ex);
        }

        dropsByEntity = builder.build();
        stats = builder.toStats();
        ScalingHealth.LOGGER.info("Loaded wealth drops: {} entities, {} pools, {} definitions, {} invalid entries",
                stats.entityCount, stats.poolCount, stats.definitionCount, stats.invalidEntries);
        return stats;
    }

    public List<DropPool> getPools(ResourceLocation entityId) {
        List<DropPool> pools = dropsByEntity.get(entityId);
        return pools == null ? Collections.emptyList() : pools;
    }

    public Stats getStats() {
        return stats;
    }

    private void ensureFile() {
        if (file == null) {
            throw new IllegalStateException("WealthDropTable has not been initialized");
        }
        if (file.exists()) {
            return;
        }

        try (FileWriter writer = new FileWriter(file)) {
            JsonObject root = new JsonObject();
            root.add("entities", new JsonObject());
            GSON.toJson(root, writer);
            ScalingHealth.LOGGER.info("Created empty wealth drop table: {}", file);
        } catch (IOException ex) {
            ScalingHealth.LOGGER.error("Could not create wealth drop table: {}", file, ex);
        }
    }

    private void readRoot(JsonObject root, Builder builder) {
        JsonObject entities = getObject(root, "entities");
        if (entities == null) {
            warn("Wealth drop table has no \"entities\" object: {}", file);
            return;
        }

        for (Map.Entry<String, JsonElement> entityEntry : entities.entrySet()) {
            ResourceLocation entityId = new ResourceLocation(entityEntry.getKey());
            if (!ForgeRegistries.ENTITIES.containsKey(entityId)) {
                builder.invalidEntries++;
                warn("Wealth drop entity does not exist: {}", entityId);
                continue;
            }

            JsonObject pools = asObject(entityEntry.getValue(), "entity " + entityId);
            if (pools == null) {
                builder.invalidEntries++;
                continue;
            }

            for (Map.Entry<String, JsonElement> poolEntry : pools.entrySet()) {
                DifficultyRange range = DifficultyRange.parse(poolEntry.getKey());
                if (range == null) {
                    builder.invalidEntries++;
                    warn("Invalid wealth drop difficulty range for {}: {}", entityId, poolEntry.getKey());
                    continue;
                }

                if (!poolEntry.getValue().isJsonArray()) {
                    builder.invalidEntries++;
                    warn("Wealth drop pool for {} {} is not an array", entityId, poolEntry.getKey());
                    continue;
                }

                List<DropDefinition> definitions = new ArrayList<>();
                for (JsonElement definitionElement : poolEntry.getValue().getAsJsonArray()) {
                    JsonObject definitionObject = asObject(definitionElement, "drop definition for " + entityId);
                    if (definitionObject == null) {
                        builder.invalidEntries++;
                        continue;
                    }

                    DropDefinition definition = DropDefinition.parse(entityId, range, definitionObject);
                    if (definition == null) {
                        builder.invalidEntries++;
                        continue;
                    }
                    definitions.add(definition);
                    builder.definitionCount++;
                }

                if (!definitions.isEmpty()) {
                    builder.add(entityId, new DropPool(range, definitions));
                    builder.poolCount++;
                }
            }
        }
    }

    @Nullable
    private static JsonObject getObject(JsonObject parent, String key) {
        JsonElement element = parent.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    @Nullable
    private static JsonObject asObject(JsonElement element, String context) {
        if (element != null && element.isJsonObject()) {
            return element.getAsJsonObject();
        }
        warn("Invalid wealth drop JSON object: {}", context);
        return null;
    }

    private static void warn(String message, Object... args) {
        if (Config.Wealth.warnInvalidDrops) {
            ScalingHealth.LOGGER.warn(message, args);
        }
    }

    public static final class DropPool {
        private final DifficultyRange range;
        private final List<DropDefinition> definitions;

        private DropPool(DifficultyRange range, List<DropDefinition> definitions) {
            this.range = range;
            this.definitions = Collections.unmodifiableList(definitions);
        }

        public boolean matches(double difficulty) {
            return range.contains(difficulty);
        }

        public void addDrops(List<ItemStack> drops, Random random) {
            for (DropDefinition definition : definitions) {
                ItemStack stack = definition.createStack(random);
                if (stack != null) {
                    drops.add(stack);
                }
            }
        }
    }

    private static final class DropDefinition {
        private final Item item;
        private final int meta;
        private final int amountMin;
        private final int amountMax;
        private final double chance;
        private final NBTTagCompound nbt;

        private DropDefinition(Item item, int meta, int amountMin, int amountMax, double chance,
                               @Nullable NBTTagCompound nbt) {
            this.item = item;
            this.meta = meta;
            this.amountMin = amountMin;
            this.amountMax = amountMax;
            this.chance = chance;
            this.nbt = nbt;
        }

        @Nullable
        private static DropDefinition parse(ResourceLocation entityId, DifficultyRange range, JsonObject json) {
            String itemName = getString(json, "item", null);
            if (itemName == null || itemName.trim().isEmpty()) {
                warn("Wealth drop for {} {} has no item", entityId, range);
                return null;
            }

            ParsedItem parsedItem = ParsedItem.parse(itemName);
            if (parsedItem == null) {
                warn("Invalid wealth drop item id for {} {}: {}", entityId, range, itemName);
                return null;
            }

            Item item = ForgeRegistries.ITEMS.getValue(parsedItem.id);
            if (item == null) {
                warn("Wealth drop item does not exist for {} {}: {}", entityId, range, itemName);
                return null;
            }

            DifficultyRange amount = DifficultyRange.parse(getString(json, "amount", "[1,1]"));
            if (amount == null) {
                warn("Invalid wealth drop amount for {} {}: {}", entityId, range, json.get("amount"));
                return null;
            }

            double chance = getDouble(json, "chance", 1D);
            if (chance <= 0D) {
                return null;
            }

            NBTTagCompound nbt = null;
            if (json.has("nbt") && !json.get("nbt").isJsonNull()) {
                try {
                    nbt = JsonToNBT.getTagFromJson(json.get("nbt").toString());
                } catch (NBTException ex) {
                    warn("Invalid wealth drop NBT for {} {}: {}", entityId, range, json.get("nbt"));
                    return null;
                }
            }

            return new DropDefinition(item, parsedItem.meta, amount.min, amount.max, Math.min(chance, 1D), nbt);
        }

        @Nullable
        private ItemStack createStack(Random random) {
            if (random.nextDouble() > chance) {
                return null;
            }

            int amount = amountMin >= amountMax ? amountMin : amountMin + random.nextInt(amountMax - amountMin + 1);
            ItemStack stack = new ItemStack(item, amount, meta);
            if (nbt != null) {
                stack.setTagCompound(nbt.copy());
            }
            return stack;
        }
    }

    private static final class ParsedItem {
        private final ResourceLocation id;
        private final int meta;

        private ParsedItem(ResourceLocation id, int meta) {
            this.id = id;
            this.meta = meta;
        }

        @Nullable
        private static ParsedItem parse(String value) {
            String[] parts = value.split(":");
            if (parts.length < 2 || parts.length > 3) {
                return null;
            }

            int meta = 0;
            if (parts.length == 3) {
                if ("*".equals(parts[2])) {
                    meta = OreDictionary.WILDCARD_VALUE;
                } else {
                    try {
                        meta = Integer.parseInt(parts[2]);
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                }
            }
            return new ParsedItem(new ResourceLocation(parts[0], parts[1]), meta);
        }
    }

    private static final class DifficultyRange {
        private final int min;
        private final int max;
        private final boolean minInclusive;
        private final boolean maxInclusive;

        private DifficultyRange(int min, int max, boolean minInclusive, boolean maxInclusive) {
            this.min = min;
            this.max = max;
            this.minInclusive = minInclusive;
            this.maxInclusive = maxInclusive;
        }

        @Nullable
        private static DifficultyRange parse(String value) {
            if (value == null || value.length() < 5) {
                return null;
            }

            char left = value.charAt(0);
            char right = value.charAt(value.length() - 1);
            boolean minInclusive = left == '[' || left == ']';
            boolean maxInclusive = right == ']' || right == '[';
            if (left != '[' && left != '(' && left != ']' && left != ')') {
                return null;
            }
            if (right != ']' && right != ')' && right != '[' && right != '(') {
                return null;
            }

            String[] parts = value.substring(1, value.length() - 1).split(",");
            if (parts.length != 2) {
                return null;
            }

            try {
                return new DifficultyRange(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()),
                        minInclusive, maxInclusive);
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        private boolean contains(double value) {
            boolean lower = minInclusive ? min <= value : min < value;
            boolean upper = maxInclusive ? value <= max : value < max;
            return min <= max ? lower && upper : lower || upper;
        }

        @Override
        public String toString() {
            return (minInclusive ? "[" : "(") + min + "," + max + (maxInclusive ? "]" : ")");
        }
    }

    public static final class Stats {
        public final int entityCount;
        public final int poolCount;
        public final int definitionCount;
        public final int invalidEntries;

        private Stats(int entityCount, int poolCount, int definitionCount, int invalidEntries) {
            this.entityCount = entityCount;
            this.poolCount = poolCount;
            this.definitionCount = definitionCount;
            this.invalidEntries = invalidEntries;
        }

        private static Stats empty() {
            return new Stats(0, 0, 0, 0);
        }
    }

    private static final class Builder {
        private final Map<ResourceLocation, List<DropPool>> drops = new HashMap<>();
        private int poolCount;
        private int definitionCount;
        private int invalidEntries;

        private void add(ResourceLocation entityId, DropPool pool) {
            drops.computeIfAbsent(entityId, ignored -> new ArrayList<>()).add(pool);
        }

        private Map<ResourceLocation, List<DropPool>> build() {
            Map<ResourceLocation, List<DropPool>> built = new HashMap<>();
            for (Map.Entry<ResourceLocation, List<DropPool>> entry : drops.entrySet()) {
                built.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
            }
            return Collections.unmodifiableMap(built);
        }

        private Stats toStats() {
            return new Stats(drops.size(), poolCount, definitionCount, invalidEntries);
        }
    }

    @Nullable
    private static String getString(JsonObject json, String key, @Nullable String defaultValue) {
        JsonElement element = json.get(key);
        return element == null || element.isJsonNull() ? defaultValue : element.getAsString();
    }

    private static double getDouble(JsonObject json, String key, double defaultValue) {
        JsonElement element = json.get(key);
        return element == null || element.isJsonNull() ? defaultValue : element.getAsDouble();
    }
}
