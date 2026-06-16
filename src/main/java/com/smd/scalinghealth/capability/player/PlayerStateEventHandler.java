package com.smd.scalinghealth.capability.player;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.StartTracking;
import net.minecraftforge.event.entity.player.PlayerEvent.Clone;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import com.smd.scalinghealth.service.PlayerStateService;
import com.smd.scalinghealth.service.SyncService;

public class PlayerStateEventHandler {
    @SubscribeEvent
    public void attachPlayerCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof EntityPlayer) {
            event.addCapability(PlayerStateCapability.ID, new PlayerStateProvider());
        }
    }

    @SubscribeEvent
    public void clonePlayer(Clone event) {
        IPlayerState oldState = PlayerStateAccess.get(event.getOriginal());
        IPlayerState newState = PlayerStateAccess.get(event.getEntityPlayer());
        if (oldState != null && newState != null) {
            NBTTagCompound tags = new NBTTagCompound();
            oldState.writeToNBT(tags);
            newState.readFromNBT(tags);
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            IPlayerState state = PlayerStateAccess.get(event.player);
            if (state != null) {
                migrateLegacyDifficulty(event.player, state);
                PlayerStateService.applyDerivedHealth(event.player, state);
                SyncService.syncPlayer(event.player, state);
            }
        }
    }

    @SubscribeEvent
    public void onStartTracking(StartTracking event) {
        if (!(event.getTarget() instanceof EntityPlayer) || !(event.getEntityPlayer() instanceof EntityPlayerMP)) {
            return;
        }

        EntityPlayer trackedPlayer = (EntityPlayer) event.getTarget();
        IPlayerState state = PlayerStateAccess.get(trackedPlayer);
        if (state != null) {
            SyncService.syncPlayerTo((EntityPlayerMP) event.getEntityPlayer(), trackedPlayer, state);
        }
    }

    private static void migrateLegacyDifficulty(EntityPlayer player, IPlayerState state) {
        NBTTagCompound persisted = player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        if (!persisted.hasKey(ScalingHealthLegacyData.NBT_ROOT)) {
            return;
        }

        NBTTagCompound legacy = persisted.getCompoundTag(ScalingHealthLegacyData.NBT_ROOT);
        if (legacy.hasKey(ScalingHealthLegacyData.NBT_DIFFICULTY)) {
            PlayerStateService.setDifficulty(player, state, legacy.getDouble(ScalingHealthLegacyData.NBT_DIFFICULTY));
        }

        legacy.removeTag(ScalingHealthLegacyData.NBT_DIFFICULTY);
        legacy.removeTag(ScalingHealthLegacyData.NBT_HEALTH);
        legacy.removeTag(ScalingHealthLegacyData.NBT_MAX_HEALTH);
        legacy.removeTag(ScalingHealthLegacyData.NBT_LAST_LOGIN);
        if (legacy.getSize() == 0) {
            persisted.removeTag(ScalingHealthLegacyData.NBT_ROOT);
        } else {
            persisted.setTag(ScalingHealthLegacyData.NBT_ROOT, legacy);
        }
    }
}
