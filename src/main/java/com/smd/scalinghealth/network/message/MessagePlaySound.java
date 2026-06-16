package com.smd.scalinghealth.network.message;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import com.smd.scalinghealth.ScalingHealth;
import com.smd.scalinghealth.network.Message;

import javax.annotation.Nullable;
import java.util.Objects;

@SuppressWarnings("WeakerAccess")
public class MessagePlaySound extends Message<MessagePlaySound> {
    private String soundId = "";
    private float volume;
    private float pitch;

    @SuppressWarnings("unused")
    public MessagePlaySound() {}

    public MessagePlaySound(SoundEvent sound, float volume, float pitch) {
        this.soundId = Objects.requireNonNull(sound.getRegistryName()).toString();
        this.volume = volume;
        this.pitch = pitch;
    }

    @Override
    protected void read(io.netty.buffer.ByteBuf buf) {
        soundId = ByteBufUtils.readUTF8String(buf);
        volume = buf.readFloat();
        pitch = buf.readFloat();
    }

    @Override
    protected void write(io.netty.buffer.ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, soundId);
        buf.writeFloat(volume);
        buf.writeFloat(pitch);
    }

    @Override
    @Nullable
    @SideOnly(Side.CLIENT)
    protected IMessage handleMessage(MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            EntityPlayer player = ScalingHealth.proxy.getClientPlayer();
            if (player != null) {
                SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(soundId));
                if (sound != null) {
                    player.playSound(sound, volume, pitch);
                }
            }
        });

        return null;
    }

}
