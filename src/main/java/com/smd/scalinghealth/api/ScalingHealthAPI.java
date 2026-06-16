package com.smd.scalinghealth.api;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import com.smd.scalinghealth.capability.player.IPlayerState;
import com.smd.scalinghealth.capability.player.PlayerStateAccess;
import com.smd.scalinghealth.config.Config;
import com.smd.scalinghealth.event.DifficultyHandler;
import com.smd.scalinghealth.service.PlayerStateService;

@SuppressWarnings("unused")
public final class ScalingHealthAPI {
    private ScalingHealthAPI() {
        throw new IllegalAccessError("Utility class");
    }

    public static void spawnWithoutDifficulty(World world, EntityLivingBase entity) {
        entity.getEntityData().setInteger(DifficultyHandler.NBT_ENTITY_DIFFICULTY, -1);
        world.spawnEntity(entity);
    }

    // **************************************************************************
    // Difficulty
    // **************************************************************************

    /**
     * Gets the area difficulty for the given position.
     *
     * @return The area difficulty.
     */
    public static double getAreaDifficulty(World world, BlockPos pos) {
        return Config.Difficulty.AREA_DIFFICULTY_MODE.getAreaDifficulty(world, pos);
    }

    /**
     * Gets the player difficulty for the given player.
     *
     * @return The player's difficulty, or Double.NaN if the data can't be obtained for some reason.
     */
    public static double getPlayerDifficulty(EntityPlayer player) {
        IPlayerState state = PlayerStateAccess.get(player);
        if (state == null) {
            return Double.NaN;
        }

        return PlayerStateService.getDifficulty(state);
    }

    /**
     * Adds difficulty to the player. The player's difficulty will be clamped to valid values.
     */
    public static void addPlayerDifficulty(EntityPlayer player, double amount) {
        IPlayerState state = PlayerStateAccess.get(player);
        if (state != null) {
            PlayerStateService.incrementDifficulty(player, state, amount);
        }
    }

    /**
     * For players, gets the player's difficulty. For other entities, it gets the difficulty they
     * spawned with. Non-player difficulty is stored as an int, so it will always be a whole
     * number.
     */
    public static double getEntityDifficulty(EntityLivingBase entity) {
        if (entity instanceof EntityPlayer)
            return getPlayerDifficulty((EntityPlayer) entity);

        return entity.getEntityData().getInteger(DifficultyHandler.NBT_ENTITY_DIFFICULTY);
    }
}
