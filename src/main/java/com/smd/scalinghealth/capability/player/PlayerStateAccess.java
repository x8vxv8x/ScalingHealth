package com.smd.scalinghealth.capability.player;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.FakePlayer;
import com.smd.scalinghealth.config.Config;

import javax.annotation.Nullable;

public final class PlayerStateAccess {
    private PlayerStateAccess() {}

    @Nullable
    public static IPlayerState get(EntityPlayer player) {
        if (player instanceof FakePlayer && !Config.FakePlayer.haveDifficulty) {
            return null;
        }
        return PlayerStateCapability.CAPABILITY != null ? player.getCapability(PlayerStateCapability.CAPABILITY, null) : null;
    }
}
