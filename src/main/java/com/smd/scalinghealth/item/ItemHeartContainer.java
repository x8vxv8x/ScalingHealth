package com.smd.scalinghealth.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import com.smd.scalinghealth.capability.player.IPlayerState;
import com.smd.scalinghealth.capability.player.PlayerStateAccess;
import com.smd.scalinghealth.ScalingHealth;
import com.smd.scalinghealth.config.Config;
import com.smd.scalinghealth.init.ModSounds;
import com.smd.scalinghealth.service.PlayerStateService;
import com.smd.scalinghealth.utils.SHI18n;
import com.smd.scalinghealth.utils.SHUtils;
import net.minecraftforge.common.IRarity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ItemHeartContainer extends Item {

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> list, ITooltipFlag flag) {
        list.add(SHI18n.itemText(this, "desc"));
    }

    @Override
    public IRarity getForgeRarity(ItemStack stack) {
        return EnumRarity.RARE;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (!world.isRemote) {
            IPlayerState state = PlayerStateAccess.get(player);
            if (state == null) return ActionResult.newResult(EnumActionResult.PASS, stack);

            final boolean healthIncreaseAllowed = isHealthIncreaseAllowed(state);
            final int levelRequirement = getLevelsRequiredToUse(player, stack, healthIncreaseAllowed);

            if (player.experienceLevel < levelRequirement) {
                SHUtils.translateStatus(player, SHI18n.itemKey(this, "notEnoughXP"), true, levelRequirement);
                return ActionResult.newResult(EnumActionResult.PASS, stack);
            }

            final boolean consumed = Config.Items.Heart.healthRestored > 0 && player.getHealth() < player.getMaxHealth();
            if (consumed) {
                PlayerStateService.healOnHeartUse(player);
            }

            if (!healthIncreaseAllowed) {
                return useAsHealingItem(world, player, stack, levelRequirement, consumed);
            }

            useForHealthIncrease(world, player, stack, state, levelRequirement);
        }
        return ActionResult.newResult(EnumActionResult.SUCCESS, stack);
    }

    private void incrementUseStat(EntityPlayer player) {
        StatBase useStat = StatList.getObjectUseStats(this);
        if (useStat != null) player.addStat(useStat);
    }

    @Nonnull
    private ActionResult<ItemStack> useAsHealingItem(World world, EntityPlayer player, ItemStack stack, int levelRequirement, boolean consumed) {
        if (consumed) {
            world.playSound(null, player.getPosition(), SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS,
                    0.5f, 1.0f + 0.1f * (float) ScalingHealth.random.nextGaussian());
            stack.shrink(1);
            consumeLevels(player, levelRequirement);
            incrementUseStat(player);
            return ActionResult.newResult(EnumActionResult.SUCCESS, stack);
        } else {
            return ActionResult.newResult(EnumActionResult.PASS, stack);
        }
    }

    private void useForHealthIncrease(World world, EntityPlayer player, ItemStack stack, IPlayerState state, int levelRequirement) {
        PlayerStateService.incrementHeartContainerUses(player, state, 1);
        PlayerStateService.healOnHeartUse(player);
        stack.shrink(1);
        spawnParticlesAndPlaySound(world, player);
        consumeLevels(player, levelRequirement);
        incrementUseStat(player);
    }

    private static int getLevelsRequiredToUse(EntityPlayer player, ItemStack stack, boolean healthIncreaseAllowed) {
        return player.capabilities.isCreativeMode ? 0 : Config.Items.Heart.xpCost;
    }

    private static void consumeLevels(EntityPlayer player, int amount) {
        player.experienceLevel -= amount;
    }

    private static boolean isHealthIncreaseAllowed(IPlayerState state) {
        int maxUses = PlayerStateService.getMaxHeartContainerUses();
        return maxUses <= 0 || PlayerStateService.getHeartContainerUses(state) < maxUses;
    }

    private static void spawnParticlesAndPlaySound(World world, EntityPlayer player) {
        ScalingHealth.proxy.playSoundOnClient(player, ModSounds.HEART_CONTAINER_USE,
                0.5f, 1.0f + 0.1f * (float) ScalingHealth.random.nextGaussian());
    }
}
