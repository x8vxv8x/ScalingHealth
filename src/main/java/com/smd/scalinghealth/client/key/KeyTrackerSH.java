package com.smd.scalinghealth.client.key;

import com.smd.scalinghealth.Tags;
import com.smd.scalinghealth.config.Config;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;

public class KeyTrackerSH {
    public static final KeyTrackerSH INSTANCE = new KeyTrackerSH();

    private final KeyBinding keyToggleDifficultyBar = new KeyBinding(
            "key." + Tags.MOD_ID + ".difficulty_meter_toggle",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            Keyboard.KEY_N,
            "key.categories." + Tags.MOD_ID);

    private KeyTrackerSH() {
    }

    public KeyBinding getKeyToggleDifficultyBar() {
        return keyToggleDifficultyBar;
    }

    @SubscribeEvent
    public void onKeyInput(KeyInputEvent event) {
        if (keyToggleDifficultyBar.isPressed()) {
            Config.Client.Difficulty.renderMeterAlways = !Config.Client.Difficulty.renderMeterAlways;
            Config.INSTANCE.save();
        }
    }

}
