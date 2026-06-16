package com.smd.scalinghealth.capability.player;

import net.minecraft.nbt.NBTTagCompound;

public interface IPlayerState {
    double getDifficulty();

    void setDifficulty(double value);

    int getHeartContainerUses();

    void setHeartContainerUses(int value);

    void writeToNBT(NBTTagCompound tags);

    void readFromNBT(NBTTagCompound tags);
}
