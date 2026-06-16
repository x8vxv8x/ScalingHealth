package com.smd.scalinghealth.integration.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.player.IPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.annotations.ZenMethod;
import stanhebben.zenscript.annotations.ZenSetter;
import stanhebben.zenscript.annotations.ZenGetter;

import com.smd.scalinghealth.api.ScalingHealthAPI;

@ZenRegister
@ZenExpansion("crafttweaker.player.IPlayer")
public class PlayerExpansion {

    /**
     * 获取玩家个人难度
     */
    @ZenGetter("shDiff")
    public static double getShDiff(IPlayer player) {
        EntityPlayer mcPlayer = (EntityPlayer) player.getInternal();
        double diff = ScalingHealthAPI.getPlayerDifficulty(mcPlayer);
        return Double.isNaN(diff) ? 0.0 : diff;
    }

    /**
     * 设置玩家个人难度（通过差值实现，自动钳位）
     */
    @ZenSetter("shDiff")
    public static void setShDiff(IPlayer player, double value) {
        EntityPlayer mcPlayer = (EntityPlayer) player.getInternal();
        double current = ScalingHealthAPI.getPlayerDifficulty(mcPlayer);
        if (!Double.isNaN(current)) {
            ScalingHealthAPI.addPlayerDifficulty(mcPlayer, value - current);
        }
    }

    /**
     * 增加或减少玩家个人难度
     */
    @ZenMethod
    public static void addShDiff(IPlayer player, double amount) {
        EntityPlayer mcPlayer = (EntityPlayer) player.getInternal();
        ScalingHealthAPI.addPlayerDifficulty(mcPlayer, amount);
    }

    /**
     * 获取玩家当前位置的区域难度
     */
    @ZenGetter("areaShDiff")
    public static double getAreaShDiff(IPlayer player) {
        EntityPlayer mcPlayer = (EntityPlayer) player.getInternal();
        World world = mcPlayer.world;
        BlockPos pos = mcPlayer.getPosition();
        return ScalingHealthAPI.getAreaDifficulty(world, pos);
    }
}