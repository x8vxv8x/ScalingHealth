package com.smd.scalinghealth.capability.player;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PlayerStateProvider implements ICapabilitySerializable<NBTTagCompound> {
    private final IPlayerState state = new PlayerState();

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == PlayerStateCapability.CAPABILITY;
    }

    @Override
    @Nullable
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        return capability == PlayerStateCapability.CAPABILITY && PlayerStateCapability.CAPABILITY != null
                ? PlayerStateCapability.CAPABILITY.cast(state)
                : null;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tags = new NBTTagCompound();
        state.writeToNBT(tags);
        return tags;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        state.readFromNBT(nbt);
    }
}
