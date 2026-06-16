package com.smd.scalinghealth.init;

import com.smd.scalinghealth.Tags;
import net.minecraft.item.Item;
import net.minecraftforge.registries.IForgeRegistry;
import com.smd.scalinghealth.ScalingHealth;
import com.smd.scalinghealth.item.ItemDifficultyChanger;
import com.smd.scalinghealth.item.ItemHeartContainer;

public class ModItems {
    public static final ItemHeartContainer heart = new ItemHeartContainer();
    public static final Item crystalShard = new Item();
    public static final Item heartDust = new Item();
    public static final ItemDifficultyChanger difficultyChanger = new ItemDifficultyChanger();

    public static void registerAll(IForgeRegistry<Item> registry) {
        register(registry, heart, "heartcontainer");
        register(registry, crystalShard, "crystalshard");
        register(registry, heartDust, "heartdust");
        register(registry, difficultyChanger, "difficultychanger");
    }

    private static void register(IForgeRegistry<Item> registry, Item item, String name) {
        item.setRegistryName(Tags.MOD_ID, name);
        item.setTranslationKey(Tags.MOD_ID + "." + name);
        item.setCreativeTab(ScalingHealth.creativeTab);
        registry.register(item);
    }
}
