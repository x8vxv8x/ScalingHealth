package com.smd.scalinghealth.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import com.smd.scalinghealth.capability.player.IPlayerState;
import com.smd.scalinghealth.capability.player.PlayerStateAccess;
import com.smd.scalinghealth.ScalingHealth;
import com.smd.scalinghealth.config.Config;
import com.smd.scalinghealth.init.ModSounds;
import com.smd.scalinghealth.service.PlayerStateService;
import com.smd.scalinghealth.utils.SHI18n;
import net.minecraftforge.common.IRarity;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

public class ItemDifficultyChanger extends Item {
    public enum Type {
        ENCHANTED, CURSED; // down (0), up (1)

        public static Type getByMeta(int meta) {
            return values()[meta & 1];
        }

        public String getItemName() {
            return name().toLowerCase(Locale.ROOT) + "_heart";
        }

        public int getItemDamage() {
            return ordinal();
        }
    }

    public ItemDifficultyChanger() {
        setHasSubtypes(true);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> list, ITooltipFlag flag) {
        if (stack.getItemDamage() > 1) return;

        String amountStr = String.format("%d", stack.getItemDamage() == Type.ENCHANTED.ordinal()
                ? (int) Config.Items.enchantedHeartChange
                : (int) Config.Items.cursedHeartChange);
        if (amountStr.matches("^\\d+"))
            amountStr = "+" + amountStr;

        String line = SHI18n.itemText("difficultychanger", "effectDesc", amountStr);
        list.add(TextFormatting.WHITE + line);
    }

    @Override
    public IRarity getForgeRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> list) {
        if (!isInCreativeTab(tab)) return;
        for (Type type : Type.values())
            list.add(new ItemStack(this, 1, type.getItemDamage()));
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        IPlayerState state = PlayerStateAccess.get(player);

        if (state == null) {
            return new ActionResult<>(EnumActionResult.PASS, stack);
        }

        double particleX = player.posX;
        double particleY = player.posY + 0.65f * player.height;
        double particleZ = player.posZ;

        switch (Type.getByMeta(stack.getItemDamage())) {
            // Enchanted Heart
            case ENCHANTED:
                // Lower difficulty, consume 1 from stack.
                if (!world.isRemote) {
                    PlayerStateService.incrementDifficulty(player, state, Config.Items.enchantedHeartChange);
                    stack.shrink(1);
                }

                world.playSound(null, player.getPosition(), ModSounds.ENCHANTED_HEART_USE,
                        SoundCategory.PLAYERS, 0.4f, 1.7f);

                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            // Cursed Heart
            case CURSED:
                // Raise difficulty, consume 1 from stack.
                if (!world.isRemote) {
                    PlayerStateService.incrementDifficulty(player, state, Config.Items.cursedHeartChange);
                    stack.shrink(1);
                }

                world.playSound(null, player.getPosition(), ModSounds.CURSED_HEART_USE,
                        SoundCategory.PLAYERS, 0.3f,
                        (float) (0.7f + 0.05f * ScalingHealth.random.nextGaussian()));

                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            default:
                ScalingHealth.LOGGER.warn("DifficultyChanger invalid meta: {}", stack.getItemDamage());
                return new ActionResult<>(EnumActionResult.PASS, stack);
        }
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        return SHI18n.itemKey(Type.getByMeta(stack.getItemDamage()).getItemName());
    }
}
