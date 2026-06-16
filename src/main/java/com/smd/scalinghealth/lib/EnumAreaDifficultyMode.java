package com.smd.scalinghealth.lib;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import com.smd.scalinghealth.ScalingHealth;
import com.smd.scalinghealth.capability.player.IPlayerState;
import com.smd.scalinghealth.capability.player.PlayerStateAccess;
import com.smd.scalinghealth.config.Config;
import com.smd.scalinghealth.service.PlayerStateService;

import java.util.List;

public enum EnumAreaDifficultyMode {
    SINGLE_PLAYER,
    LOCAL_PLAYERS;

    public double getAreaDifficulty(World world, BlockPos pos) {
        return getAreaDifficulty(world, pos, true);
    }

    public double getAreaDifficulty(World world, BlockPos pos, boolean addGroupBonus) {
        return getAreaDifficulty(world, pos, addGroupBonus, true);
    }

    public double getAreaDifficulty(World world, BlockPos pos, boolean addGroupBonus, boolean clampValue) {
        if (!world.isRemote && !world.getGameRules().getBoolean(ScalingHealth.GAME_RULE_DIFFICULTY)) {
            // Difficulty is disabled via game rule.
            return 0.0;
        }

        int radius = Config.Difficulty.searchRadius;
        boolean unlimitedRadius = radius <= 0;
        final long radiusSquared = radius <= 0 ? Long.MAX_VALUE : (long) radius * radius;
        radius = radius <= 0 ? Integer.MAX_VALUE : radius;

        double total = 0;
        double ret = 0;
        int playerCountForGroupBonus = 0;

        switch (this) {
            case SINGLE_PLAYER:
                EntityPlayer closestPlayer = world.getClosestPlayer(pos.getX(), pos.getY(), pos.getZ(),
                        unlimitedRadius ? -1 : radius, false);
                if (closestPlayer != null) {
                    IPlayerState state = PlayerStateAccess.get(closestPlayer);
                    if (state != null) {
                        ret = PlayerStateService.getDifficulty(state);
                    }
                }
                break;

            case LOCAL_PLAYERS:
                List<EntityPlayer> players = world.getPlayers(EntityPlayer.class,
                        p -> p.getDistanceSq(pos) <= radiusSquared);
                if (players.isEmpty()) {
                    break;
                }
                playerCountForGroupBonus = players.size();
                int totalWeight = 0;
                for (EntityPlayer player : players) {
                    IPlayerState state = PlayerStateAccess.get(player);
                    if (state != null) {
                        int distance = (int) pos.getDistance((int) player.posX, pos.getY(), (int) player.posZ);
                        int weight = (radius - distance) / 16 + 1;

                        total += weight * PlayerStateService.getDifficulty(state);
                        totalWeight += weight;
                    }
                }
                ret = totalWeight <= 0 ? 0 : total / totalWeight;
                break;

        }

        // Clamp to difficulty range (intentionally done before group bonus)
        if (clampValue)
            ret = MathHelper.clamp(ret, Config.Difficulty.minValue, Config.Difficulty.maxValue);

        // Group bonus?
        if (addGroupBonus && playerCountForGroupBonus > 1)
            ret *= 1 + Config.Difficulty.groupAreaBonus * (playerCountForGroupBonus - 1);

        // Dimension value factor
        SimpleExpression dimensionFactor = Config.Difficulty.DIMENSION_VALUE_FACTOR.get(world.provider.getDimension());
        if (dimensionFactor != null)
            ret = dimensionFactor.apply(ret);

        return ret;
    }
}
