package com.smd.scalinghealth.capability.player;

import net.minecraft.nbt.NBTTagCompound;

public class PlayerState implements IPlayerState {
    public static final String NBT_DIFFICULTY = "difficulty";
    public static final String NBT_HEART_CONTAINER_USES = "heart_container_uses";

    private double difficulty;
    private int heartContainerUses;

    @Override
    public double getDifficulty() {
        return difficulty;
    }

    @Override
    public void setDifficulty(double value) {
        difficulty = value;
    }

    @Override
    public int getHeartContainerUses() {
        return heartContainerUses;
    }

    @Override
    public void setHeartContainerUses(int value) {
        heartContainerUses = value;
    }

    @Override
    public void writeToNBT(NBTTagCompound tags) {
        tags.setDouble(NBT_DIFFICULTY, difficulty);
        tags.setInteger(NBT_HEART_CONTAINER_USES, heartContainerUses);
    }

    @Override
    public void readFromNBT(NBTTagCompound tags) {
        difficulty = tags.getDouble(NBT_DIFFICULTY);
        heartContainerUses = Math.max(0, tags.getInteger(NBT_HEART_CONTAINER_USES));
    }
}
