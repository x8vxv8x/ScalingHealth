package com.smd.scalinghealth.network.message;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import com.smd.scalinghealth.ScalingHealth;
import com.smd.scalinghealth.capability.player.IPlayerState;
import com.smd.scalinghealth.capability.player.PlayerState;
import com.smd.scalinghealth.capability.player.PlayerStateAccess;
import com.smd.scalinghealth.network.Message;
import com.smd.scalinghealth.service.PlayerStateService;

import javax.annotation.Nullable;

@SuppressWarnings("WeakerAccess")
public class MessageDataSync extends Message<MessageDataSync> {
    private NBTTagCompound tags;
    private String playerName = "";

    @SuppressWarnings("unused")
    public MessageDataSync() {}

    public MessageDataSync(IPlayerState state, EntityPlayer player) {
        tags = new NBTTagCompound();
        state.writeToNBT(tags);
        this.playerName = player.getName();
    }

    @Override
    protected void read(io.netty.buffer.ByteBuf buf) {
        tags = ByteBufUtils.readTag(buf);
        playerName = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    protected void write(io.netty.buffer.ByteBuf buf) {
        ByteBufUtils.writeTag(buf, tags);
        ByteBufUtils.writeUTF8String(buf, playerName);
    }

    @Override
    @Nullable
    @SideOnly(Side.CLIENT)
    protected IMessage handleMessage(MessageContext context) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            EntityPlayer player = getPlayerByName(playerName);
            if (player != null) {
                IPlayerState state = PlayerStateAccess.get(player);
                if (state != null) {
                    state.readFromNBT(tags);
                    PlayerStateService.applySyncedState(player, state);
                } else {
                    PlayerState fallback = new PlayerState();
                    fallback.readFromNBT(tags);
                    PlayerStateService.applySyncedState(player, fallback);
                }
            }
        });

        return null;
    }

    @Nullable
    private static EntityPlayer getPlayerByName(String name) {
        EntityPlayer localPlayer = ScalingHealth.proxy.getClientPlayer();
        if (localPlayer != null) {
            World world = localPlayer.world;
            if (world != null) {
                return world.getPlayerEntityByName(name);
            }
        }
        return null;
    }
}
