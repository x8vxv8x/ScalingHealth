package com.smd.scalinghealth.gui;

import com.smd.scalinghealth.Tags;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.config.GuiConfig;
import com.smd.scalinghealth.config.Config;

public class GuiConfigScalingHealth extends GuiConfig {
    public GuiConfigScalingHealth(GuiScreen parent) {
        super(parent, Config.INSTANCE.getConfigElements(), Tags.MOD_ID,
                false, false, I18n.format("config.scalinghealth.title"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        super.actionPerformed(button);
    }
}
