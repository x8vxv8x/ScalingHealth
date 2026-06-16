package com.smd.scalinghealth.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import com.smd.scalinghealth.capability.player.IPlayerState;
import com.smd.scalinghealth.capability.player.PlayerStateAccess;
import com.smd.scalinghealth.config.Config;
import com.smd.scalinghealth.lib.EnumAreaDifficultyMode;
import com.smd.scalinghealth.service.PlayerStateService;
import org.lwjgl.opengl.GL11;

public final class ScalingHealthClientEvents {

    public static final ScalingHealthClientEvents INSTANCE = new ScalingHealthClientEvents();

    private static final float DEBUG_TEXT_SCALE = 0.6f;

    @SubscribeEvent
    public void renderTick(RenderGameOverlayEvent.Post event) {
        if (Config.Debug.debugOverlay && Minecraft.getMinecraft().world != null && event.getType() == ElementType.ALL) {
            FontRenderer fontRender = Minecraft.getMinecraft().fontRenderer;

            GL11.glPushMatrix();
            GlStateManager.scale(DEBUG_TEXT_SCALE, DEBUG_TEXT_SCALE, 1.0f);

            String text = getDebugText();
            int y = 3;
            for (String line : text.split("\n")) {
                String[] array = line.split("=");
                if (array.length == 2) {
                    fontRender.drawString(array[0].trim(), 3, y, 0xFFFFFF);
                    fontRender.drawString(array[1].trim(), 100, y, 0xFFFFFF);
                } else {
                    fontRender.drawString(line, 3, y, 0xFFFFFF);
                }
                y += 10;
            }

            GL11.glPopMatrix();
        }
    }

    private String getDebugText() {
        World world = Minecraft.getMinecraft().world;
        EntityPlayer player = Minecraft.getMinecraft().player;
        IPlayerState state = PlayerStateAccess.get(player);
        EnumAreaDifficultyMode areaMode = Config.Difficulty.AREA_DIFFICULTY_MODE;
        if (state == null)
            return "Player data is null!";

        StringBuilder ret = new StringBuilder();

        ret.append(String.format("Area Difficulty = %.4f (%s)\n",
                areaMode.getAreaDifficulty(world, player.getPosition()), areaMode.name()));
        ret.append(String.format("Player Difficulty = %.4f\n", PlayerStateService.getDifficulty(state)));
        ret.append("Player Health = ").append(player.getHealth()).append(" / ").append(player.getMaxHealth()).append("\n");

        // Display all health attribute modifiers.
        IAttributeInstance attr = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (!attr.getModifiers().isEmpty()) {
            for (AttributeModifier mod : attr.getModifiers()) {
                ret.append("         ").append(mod).append("\n");
            }
        } else {
            ret.append("        No modifiers! That should not happen.\n");
        }

        // Mob process count
        int mobsProcessed = DifficultyHandler.debugGetMobsProcessed();
        float mobsProcessedRate = DifficultyHandler.debugGetMobsProcessedRate();
        ret.append(String.format("Mobs processed = %d (%.1f/s avg)", mobsProcessed, mobsProcessedRate)).append("\n");

        return ret.toString();
    }
}
