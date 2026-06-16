package com.smd.scalinghealth.client;

import com.smd.scalinghealth.Tags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import com.smd.scalinghealth.capability.player.IPlayerState;
import com.smd.scalinghealth.capability.player.PlayerStateAccess;
import com.smd.scalinghealth.config.Config;
import com.smd.scalinghealth.service.PlayerStateService;
import com.smd.scalinghealth.utils.SHI18n;

public class DifficultyDisplayHandler extends Gui {

    public static final DifficultyDisplayHandler INSTANCE = new DifficultyDisplayHandler();

    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MOD_ID, "textures/gui/hud.png");

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != ElementType.TEXT || Config.Difficulty.maxValue <= 0
                || !Config.Client.Difficulty.renderMeter || !Config.Client.Difficulty.renderMeterAlways)
            return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;

        int width = event.getResolution().getScaledWidth();
        int height = event.getResolution().getScaledHeight();

        IPlayerState state = player != null ? PlayerStateAccess.get(player) : null;
        if (state == null)
            return;

        int difficulty = (int) PlayerStateService.getDifficulty(state);
        int areaDifficultyUnclamped = (int) Config.Difficulty.AREA_DIFFICULTY_MODE.getAreaDifficulty(player.world, player.getPosition());
        int areaDifficulty = MathHelper.clamp(areaDifficultyUnclamped, 0, (int) Config.Difficulty.maxValue);

        GlStateManager.enableBlend();

        mc.renderEngine.bindTexture(TEXTURE);

        GlStateManager.pushMatrix();
        // GlStateManager.scale(1f, 0.5f, 1f);

        int posX = Config.Client.Difficulty.meterPosX; // 5;
        if (posX < 0)
            posX = posX + width - 64;
        int posY = Config.Client.Difficulty.meterPosY; // height - 30;
        if (posY < 0)
            posY = posY + height - 12;

        // Frame
        drawTexturedModalRect(posX, posY, 190, 0, 66, 14, 0xFFFFFF);

        // Area Difficulty
        int barLength = (int) (60.0 * areaDifficulty / Config.Difficulty.maxValue);
        drawTexturedModalRect(posX + 3, posY + 5, 193, 19, barLength, 6, 0xFFFFFF);

        // Player Difficulty
        barLength = (int) (60.0 * difficulty / Config.Difficulty.maxValue);
        drawTexturedModalRect(posX + 3, posY + 3, 193, 17, barLength, 2, 0xFFFFFF);

        // Text
        GlStateManager.pushMatrix();
        float textScale = 0.6f;
        GlStateManager.scale(textScale, textScale, 1.0f);
        String localizedString = SHI18n.miscText("difficultyMeterText");
        mc.fontRenderer.drawStringWithShadow(localizedString, posX / textScale + 4, posY / textScale - 9, 0xFFFFFF);
        // Text Difficulty
        String str = String.format("%d", areaDifficultyUnclamped);
        int strWidth = mc.fontRenderer.getStringWidth(str);
        mc.fontRenderer.drawStringWithShadow(str, posX / textScale + 104 - strWidth, posY / textScale - 9, 0xAAAAAA);
        GlStateManager.popMatrix();

        GlStateManager.popMatrix();
        GlStateManager.disableBlend();
    }

    private void drawTexturedModalRect(int x, int y, int textureX, int textureY, int width, int height, int color) {
        float r = ((color >> 16) & 255) / 255f;
        float g = ((color >> 8) & 255) / 255f;
        float b = (color & 255) / 255f;
        GlStateManager.color(r, g, b);
        drawTexturedModalRect(x, y, textureX, textureY, width, height);
        GlStateManager.color(1f, 1f, 1f);
    }
}
