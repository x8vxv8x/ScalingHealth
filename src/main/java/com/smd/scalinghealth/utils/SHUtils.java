package com.smd.scalinghealth.utils;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.UUID;

public final class SHUtils {
    private SHUtils() {}

    public static void heal(EntityLivingBase entity, float amount, boolean fireEvent) {
        if (amount <= 0) {
            return;
        }

        float healAmount = amount;
        if (fireEvent) {
            LivingHealEvent event = new LivingHealEvent(entity, amount);
            if (MinecraftForge.EVENT_BUS.post(event)) {
                return;
            }
            healAmount = event.getAmount();
        }

        entity.setHealth(Math.min(entity.getHealth() + healAmount, entity.getMaxHealth()));
    }

    public static void removeModifier(EntityLivingBase entity, IAttribute attribute, UUID modifierId) {
        IAttributeInstance instance = entity.getEntityAttribute(attribute);
        if (instance == null) {
            return;
        }

        net.minecraft.entity.ai.attributes.AttributeModifier modifier = instance.getModifier(modifierId);
        if (modifier != null) {
            instance.removeModifier(modifier);
        }
    }

    public static void translateStatus(EntityPlayer player, String key, boolean actionBar, Object... args) {
        player.sendStatusMessage(new TextComponentTranslation(key, args), actionBar);
    }
}
