package com.smd.scalinghealth.capability.player;

import com.smd.scalinghealth.Tags;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

public final class PlayerStateCapability {
    public static final ResourceLocation ID = new ResourceLocation(Tags.MOD_ID, "player_state");

    @CapabilityInject(IPlayerState.class)
    public static final Capability<IPlayerState> CAPABILITY = null;

    private PlayerStateCapability() {}

    public static void register() {
        CapabilityManager.INSTANCE.register(IPlayerState.class, new Storage(), PlayerState::new);
    }

    private static final class Storage implements Capability.IStorage<IPlayerState> {
        @Override
        public NBTBase writeNBT(Capability<IPlayerState> capability, IPlayerState instance, EnumFacing side) {
            NBTTagCompound tags = new NBTTagCompound();
            instance.writeToNBT(tags);
            return tags;
        }

        @Override
        public void readNBT(Capability<IPlayerState> capability, IPlayerState instance, EnumFacing side, NBTBase nbt) {
            if (nbt instanceof NBTTagCompound) {
                instance.readFromNBT((NBTTagCompound) nbt);
            }
        }
    }
}
