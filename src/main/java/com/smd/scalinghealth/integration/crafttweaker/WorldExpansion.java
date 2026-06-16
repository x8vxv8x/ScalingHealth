package com.smd.scalinghealth.integration.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.entity.IEntityLivingBase;
import crafttweaker.api.world.IWorld;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.annotations.ZenMethod;

import com.smd.scalinghealth.api.ScalingHealthAPI;

@ZenRegister
@ZenExpansion("crafttweaker.world.IWorld")
public class WorldExpansion {

    /**
     * 生成一个不受难度影响的实体
     */
    @ZenMethod
    public static void spawnWithoutDifficulty(IWorld world, IEntityLivingBase entity) {
        World mcWorld = (World) world.getInternal();
        EntityLivingBase mcEntity = (EntityLivingBase) entity.getInternal();
        ScalingHealthAPI.spawnWithoutDifficulty(mcWorld, mcEntity);
    }
}