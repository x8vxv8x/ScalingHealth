package com.smd.scalinghealth.event;

import com.smd.scalinghealth.Tags;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import com.smd.scalinghealth.ScalingHealth;
import com.smd.scalinghealth.capability.player.IPlayerState;
import com.smd.scalinghealth.capability.player.PlayerStateAccess;
import com.smd.scalinghealth.config.Config;
import com.smd.scalinghealth.init.ModItems;
import com.smd.scalinghealth.service.PlayerStateService;

import javax.annotation.Nullable;
import java.util.Random;

public class ScalingHealthCommonEvents {
    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        // Handle heart drops.
        // Was a player responsible for the death?
        EntityPlayer player = getPlayerThatCausedDeath(event.getSource());
        if (player == null || (player instanceof FakePlayer
                && !Config.FakePlayer.generateHearts)) {
            return;
        }

        // Mob loot disabled?
        if (!player.world.getGameRules().getBoolean("doMobLoot")) return;

        EntityLivingBase killedEntity = event.getEntityLiving();
        if (!killedEntity.world.isRemote) {
            Random rand = ScalingHealth.random;
            int stackSize = 0;

            // Different drop rates for hostiles and passives.
            float dropRate = killedEntity instanceof IMob ? Config.Items.Heart.chanceHostile : Config.Items.Heart.chancePassive;
            if (killedEntity instanceof EntitySlime) {
                dropRate /= 6f;
            }

            ScalingHealth.LOGGER.debug("heart drop rate for {} is {}", killedEntity.getName(), dropRate);

            // Basic heart drops for all mobs.
            if (event.isRecentlyHit() && rand.nextFloat() <= dropRate) {
                stackSize += 1;
            }

            // Heart drops for bosses.
            if (!killedEntity.isNonBoss()) {
                int min = Config.Items.Heart.bossMin;
                int max = Config.Items.Heart.bossMax;
                stackSize += min + rand.nextInt(max - min + 1);
            }

            if (stackSize > 0) {
                Item itemToDrop = Config.Items.Heart.dropShardsInstead ? ModItems.crystalShard
                        : ModItems.heart;
                killedEntity.dropItem(itemToDrop, stackSize);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onXPDropped(LivingExperienceDropEvent event) {
        EntityLivingBase entityLiving = event.getEntityLiving();

        // Additional XP from all mobs.
        int difficulty = entityLiving.getEntityData()
                .getInteger(DifficultyHandler.NBT_ENTITY_DIFFICULTY);
        if (difficulty < 0) {
            difficulty = 0;
        }
        float multi = 1.0f + Config.Mob.xpBoost * difficulty;

        float amount = event.getDroppedExperience();
        amount *= multi;

        event.setDroppedExperience(Math.round(amount));
    }

    /**
     * Get the player that caused a mob's death. Could be a FakePlayer or null.
     *
     * @return The player that caused the damage, or the owner of the tamed animal that caused the
     * damage.
     */
    private @Nullable
    EntityPlayer getPlayerThatCausedDeath(DamageSource source) {
        if (source == null) {
            return null;
        }

        // Player is true source.
        Entity entitySource = source.getTrueSource();
        if (entitySource instanceof EntityPlayer) {
            return (EntityPlayer) entitySource;
        }

        // Player's pet is true source.
        boolean isTamedAnimal = entitySource instanceof EntityTameable
                && ((EntityTameable) entitySource).isTamed();
        if (entitySource instanceof EntityTameable) {
            EntityTameable tamed = (EntityTameable) entitySource;
            if (tamed.isTamed() && tamed.getOwner() instanceof EntityPlayer) {
                return (EntityPlayer) tamed.getOwner();
            }
        }

        // No player responsible.
        return null;
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.player;
            IPlayerState state = PlayerStateAccess.get(player);
            if (state == null) return;

            // Lose difficulty on death?
            if (!event.isEndConquered()) {
                double currentDifficulty = PlayerStateService.getDifficulty(state);
                double newDifficulty = MathHelper.clamp(
                        currentDifficulty - Config.Difficulty.lostOnDeath,
                        Config.Difficulty.minValue, Config.Difficulty.maxValue);
                PlayerStateService.setDifficulty(player, state, newDifficulty);
            }

            PlayerStateService.applyDerivedHealth(player, state);
            player.setHealth(player.getMaxHealth());
        }
    }

    @SubscribeEvent
    public void onPlayerJoinedServer(PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.player;
            IPlayerState state = PlayerStateAccess.get(player);
            if (state == null) return;
            PlayerStateService.applyDerivedHealth(player, state);
        }
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(Tags.MOD_ID)) {
            Config.INSTANCE.load();
            Config.INSTANCE.save();
        }
    }
}
