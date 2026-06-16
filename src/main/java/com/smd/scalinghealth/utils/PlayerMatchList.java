package com.smd.scalinghealth.utils;

import net.minecraft.entity.player.EntityPlayer;
import java.util.HashSet;
import java.util.Set;

public class PlayerMatchList {
    private final Set<String> list = new HashSet<>();

    public void add(String name) {
        if (name != null) {
            list.add(name.toLowerCase());
        }
    }

    public void clear() {
        list.clear();
    }

    public boolean contains(EntityPlayer player) {
        if (player == null) return false;
        return list.contains(player.getName().toLowerCase());
    }
}