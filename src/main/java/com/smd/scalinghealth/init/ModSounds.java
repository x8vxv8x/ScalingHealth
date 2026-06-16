package com.smd.scalinghealth.init;

import com.smd.scalinghealth.Tags;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModSounds {
    private static final Map<String, SoundEvent> SOUND_EVENTS = new LinkedHashMap<>();

    public static final SoundEvent CURSED_HEART_USE = create("cursed_heart_use");
    public static final SoundEvent ENCHANTED_HEART_USE = create("enchanted_heart_use");
    public static final SoundEvent HEART_CONTAINER_USE = create("heart_container_use");

    private ModSounds() {
    }

    public static void registerAll(IForgeRegistry<SoundEvent> registry) {
        SOUND_EVENTS.values().forEach(registry::register);
    }

    private static SoundEvent create(String soundId) {
        ResourceLocation name = new ResourceLocation(Tags.MOD_ID, soundId);
        SoundEvent sound = new SoundEvent(name).setRegistryName(name);
        SOUND_EVENTS.put(soundId, sound);
        return sound;
    }
}
