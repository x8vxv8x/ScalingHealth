package com.smd.scalinghealth.service;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import com.smd.scalinghealth.ScalingHealth;
import com.smd.scalinghealth.capability.player.IPlayerState;
import com.smd.scalinghealth.config.Config;
import com.smd.scalinghealth.scoreboard.SHScoreCriteria;
import com.smd.scalinghealth.utils.ModifierHandler;
import com.smd.scalinghealth.utils.SHUtils;

public final class PlayerStateService {
    private PlayerStateService() {}

    public static double getDifficulty(IPlayerState state) {
        return state.getDifficulty();
    }

    public static void setDifficulty(EntityPlayer player, IPlayerState state, double value) {
        double difficulty;
        if (Config.Difficulty.DIFFICULTY_EXEMPT_PLAYERS.contains(player)) {
            difficulty = 0;
        } else {
            difficulty = MathHelper.clamp(value, Config.Difficulty.minValue, Config.Difficulty.maxValue);
        }

        state.setDifficulty(difficulty);
        SHScoreCriteria.updateScore(player, (int) difficulty);
        SyncService.syncPlayer(player, state);
    }

    public static void incrementDifficulty(EntityPlayer player, IPlayerState state, double amount) {
        if (!player.world.getGameRules().getBoolean(ScalingHealth.GAME_RULE_DIFFICULTY)) {
            return;
        }

        Float dimensionMultiplier = Config.Difficulty.DIMENSION_INCREASE_MULTIPLIER.get(player.dimension);
        if (dimensionMultiplier != null) {
            amount *= dimensionMultiplier;
        }

        setDifficulty(player, state, state.getDifficulty() + amount);
    }

    public static int getHeartContainerUses(IPlayerState state) {
        return Math.max(0, state.getHeartContainerUses());
    }

    public static void setHeartContainerUses(IPlayerState state, int value) {
        int maxUses = getMaxHeartContainerUses();
        int clamped = maxUses <= 0 ? Math.max(0, value) : MathHelper.clamp(value, 0, maxUses);
        state.setHeartContainerUses(clamped);
    }

    public static void setHeartContainerUses(EntityPlayer player, IPlayerState state, int value) {
        setHeartContainerUses(state, value);
        applyDerivedHealth(player, state);
        SyncService.syncPlayer(player, state);
    }

    public static void incrementHeartContainerUses(EntityPlayer player, IPlayerState state, int amount) {
        setHeartContainerUses(player, state, getHeartContainerUses(state) + amount);
    }

    public static float getEffectiveMaxHealth(IPlayerState state) {
        return Config.Player.Health.startingHealth + getHeartContainerUses(state) * Config.Items.Heart.healthPerContainer;
    }

    public static int getMaxHeartContainerUses() {
        return Config.Player.Health.maxHeartContainers;
    }

    public static void applyDerivedHealth(EntityPlayer player, IPlayerState state) {
        if (Config.Player.Health.allowModify) {
            ModifierHandler.setMaxHealth(player, getEffectiveMaxHealth(state), 0);
        }
    }

    public static void applySyncedState(EntityPlayer player, IPlayerState state) {
        applyDerivedHealth(player, state);
        SHScoreCriteria.updateScore(player, (int) state.getDifficulty());
    }

    public static void healOnHeartUse(EntityPlayer player) {
        int current = (int) player.getHealth();
        SHUtils.heal(player, Config.Items.Heart.healthRestored, Config.Items.Heart.healingEvent);
        int newHealth = (int) player.getHealth();
        if (current + Config.Items.Heart.healthRestored != newHealth) {
            ScalingHealth.LOGGER.warn("Another mod seems to have canceled healing from a heart container (player {})", player.getName());
        }
    }
}
