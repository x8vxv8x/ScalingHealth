package com.smd.scalinghealth.service;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import com.smd.scalinghealth.capability.player.IPlayerState;
import com.smd.scalinghealth.network.NetworkHandler;
import com.smd.scalinghealth.network.message.MessageDataSync;

public final class SyncService {
    private SyncService() {}

    public static void syncPlayer(EntityPlayer player, IPlayerState state) {
        if (player instanceof EntityPlayerMP) {
            EntityPlayerMP playerMP = (EntityPlayerMP) player;
            IMessage playerMessage = new MessageDataSync(state, player);
            NetworkHandler.INSTANCE.sendTo(playerMessage, playerMP);
        }
    }

    public static void syncPlayerTo(EntityPlayerMP observer, EntityPlayer target, IPlayerState state) {
        IMessage message = new MessageDataSync(state, target);
        NetworkHandler.INSTANCE.sendTo(message, observer);
    }
}
