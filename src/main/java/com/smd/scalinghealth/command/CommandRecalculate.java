package com.smd.scalinghealth.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import com.smd.scalinghealth.ScalingHealth;
import com.smd.scalinghealth.api.ScalingHealthAPI;
import com.smd.scalinghealth.event.DifficultyHandler;
import com.smd.scalinghealth.utils.ModifierHandler;

public class CommandRecalculate extends CommandBase {
    @Override
    public String getName() {
        return "sh_recalculate";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return TextFormatting.RED + "Usage: /" + getName();
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        // no arguments
        int processed = recalculateAllEntities(sender.getEntityWorld());
        ScalingHealth.LOGGER.info("Recalculated difficulty for {} entities, see details above", processed);
        ITextComponent message = new TextComponentTranslation("command.scalinghealth.recalculate.result", processed);
        sender.sendMessage(message);
    }

    private static int recalculateAllEntities(World world) {
        int processed = 0;

        for (EntityLivingBase entity : world.getEntities(EntityLivingBase.class, e -> !(e instanceof EntityPlayer))) {
            // Old entity properties, mostly for logging
            double oldDifficulty = ScalingHealthAPI.getEntityDifficulty(entity);
            double oldMaxHealth = ModifierHandler.getHealthModifier(entity);
            double oldAttackDamage = ModifierHandler.getDamageModifier(entity);

            entity.clearActivePotions();

            if (DifficultyHandler.INSTANCE.recalculate(entity)) {
                ++processed;

                double newDifficulty = ScalingHealthAPI.getEntityDifficulty(entity);
                double newMaxHealth = ModifierHandler.getHealthModifier(entity);
                double newAttackDamage = ModifierHandler.getDamageModifier(entity);

                ScalingHealth.LOGGER.info("Recalculate {}: difficulty {} -> {}; max health {} -> {}; attack damage {} -> {}",
                        entity.getName(), oldDifficulty, newDifficulty, oldMaxHealth, newMaxHealth, oldAttackDamage, newAttackDamage);
            }
        }
        return processed;
    }
}
