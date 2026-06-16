package com.smd.scalinghealth.wealth;

import com.smd.scalinghealth.ScalingHealth;
import com.smd.scalinghealth.api.ScalingHealthAPI;
import com.smd.scalinghealth.config.Config;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class WealthDropHandler {
    public static final WealthDropHandler INSTANCE = new WealthDropHandler();

    private WealthDropHandler() {
    }

    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        if (!Config.Wealth.enabled || event.getEntityLiving().world.isRemote) {
            return;
        }
        if (Config.Wealth.requireRecentlyHit && !event.isRecentlyHit()) {
            return;
        }
        if (Config.Wealth.respectMobLootGameRule
                && !event.getEntityLiving().world.getGameRules().getBoolean("doMobLoot")) {
            return;
        }

        EntityPlayer player = getPlayerThatCausedDeath(event.getSource());
        if (player instanceof FakePlayer && !Config.Wealth.allowFakePlayers) {
            return;
        }

        double difficulty = getDifficulty(event.getEntityLiving(), player);
        if (Double.isNaN(difficulty)) {
            return;
        }

        ResourceLocation entityId = EntityList.getKey(event.getEntityLiving());
        if (entityId == null) {
            return;
        }

        List<WealthDropTable.DropPool> pools = WealthDropTable.INSTANCE.getPools(entityId);
        if (pools.isEmpty()) {
            return;
        }

        List<ItemStack> stacks = new ArrayList<>();
        for (WealthDropTable.DropPool pool : pools) {
            if (pool.matches(difficulty)) {
                pool.addDrops(stacks, ScalingHealth.random);
            }
        }

        for (ItemStack stack : stacks) {
            event.getEntityLiving().entityDropItem(stack, 0.5F);
        }
    }

    private static double getDifficulty(EntityLivingBase killedEntity, @Nullable EntityPlayer player) {
        if (Config.Wealth.difficultyBasedOnPlayer) {
            if (player == null) {
                return Double.NaN;
            }
            return ScalingHealthAPI.getPlayerDifficulty(player);
        }
        return ScalingHealthAPI.getEntityDifficulty(killedEntity);
    }

    @Nullable
    private static EntityPlayer getPlayerThatCausedDeath(DamageSource source) {
        if (source == null) {
            return null;
        }

        Entity entitySource = source.getTrueSource();
        if (entitySource instanceof EntityPlayer) {
            return (EntityPlayer) entitySource;
        }

        if (entitySource instanceof EntityTameable) {
            EntityTameable tamed = (EntityTameable) entitySource;
            if (tamed.isTamed() && tamed.getOwner() instanceof EntityPlayer) {
                return (EntityPlayer) tamed.getOwner();
            }
        }

        return null;
    }
}
