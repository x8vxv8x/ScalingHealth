package com.smd.scalinghealth.utils;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.monster.IMob;
import net.minecraft.util.ResourceLocation;
import com.smd.scalinghealth.config.Config;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class EntityDifficultyChangeList {

    private Map<String, DifficultyChanges> map = new HashMap<>();

    @Nonnull
    public DifficultyChanges get(Entity entity) {
        ResourceLocation resource = EntityList.getKey(entity);
        if (resource == null) {
            return defaultValues(entity);
        }
        String id = resource.toString();
        String idOld = EntityList.getEntityString(entity);

        for (Entry<String, DifficultyChanges> entry : map.entrySet()) {
            String key = entry.getKey();
            if (key.equalsIgnoreCase(id) || key.equalsIgnoreCase(idOld) || key.equalsIgnoreCase("minecraft:" + id)) {
                return entry.getValue();
            }
        }
        return defaultValues(entity);
    }

    public void put(String entityId, float value) {
        map.put(entityId, new DifficultyChanges(value));
    }

    public void clear() {
        map.clear();
    }

    public DifficultyChanges defaultValues(Entity entity) {
        boolean isBoss = !entity.isNonBoss();
        float standard;
        if (isBoss) {
            standard = Config.Difficulty.perBossKill;
        } else if (entity instanceof IMob) {
            standard = Config.Difficulty.perHostileKill;
        } else {
            standard = Config.Difficulty.perPassiveKill;
        }
        return new DifficultyChanges(standard);
    }

    public static class DifficultyChanges {
        public final float value;

        public DifficultyChanges(float value) {
            this.value = value;
        }
    }
}