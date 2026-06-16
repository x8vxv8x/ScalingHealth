package com.smd.scalinghealth.loot.properties;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.smd.scalinghealth.Tags;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.properties.EntityProperty;
import com.smd.scalinghealth.api.ScalingHealthAPI;

import java.util.Random;

public class PropertyDifficulty implements EntityProperty {
    private final int min;
    private final int max;

    private PropertyDifficulty(int min, int max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean testProperty(Random random, Entity entityIn) {
        if ( entityIn instanceof EntityLivingBase) {
            double difficulty = ScalingHealthAPI.getEntityDifficulty((EntityLivingBase) entityIn);
            return difficulty >= this.min && difficulty <= this.max;
        }
        return false;
    }

    public static class Serializer extends EntityProperty.Serializer<PropertyDifficulty> {
        public Serializer() {
            super(new ResourceLocation(Tags.MOD_ID, "difficulty"), PropertyDifficulty.class);
        }

        @Override
        public JsonElement serialize(PropertyDifficulty property, JsonSerializationContext serializationContext) {
            JsonObject json = new JsonObject();
            json.addProperty("min", property.min);
            json.addProperty("max", property.max);
            return json;
        }

        @Override
        public PropertyDifficulty deserialize(JsonElement element, JsonDeserializationContext deserializationContext) {
            JsonObject json = element.getAsJsonObject();
            int min = JsonUtils.getInt(json, "min", 0);
            int max = JsonUtils.getInt(json, "max", Integer.MAX_VALUE);
            return new PropertyDifficulty(min, max);
        }
    }
}
