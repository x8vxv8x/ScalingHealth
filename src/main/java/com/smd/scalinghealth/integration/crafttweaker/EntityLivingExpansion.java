package com.smd.scalinghealth.integration.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.entity.IEntityLivingBase;
import net.minecraft.entity.EntityLivingBase;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.annotations.ZenGetter;

import com.smd.scalinghealth.api.ScalingHealthAPI;

@ZenRegister
@ZenExpansion("crafttweaker.entity.IEntityLivingBase")
public class EntityLivingExpansion {

    /**
     * 获取实体的难度：玩家返回其个人难度，非玩家返回其生成时的难度
     */
    @ZenGetter("shDiff")
    public static double getShDiff(IEntityLivingBase entity) {
        EntityLivingBase mcEntity = (EntityLivingBase) entity.getInternal();
        return ScalingHealthAPI.getEntityDifficulty(mcEntity);
    }
}