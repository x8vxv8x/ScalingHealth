package com.smd.scalinghealth;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import com.smd.scalinghealth.command.CommandRecalculate;
import com.smd.scalinghealth.command.CommandScalingHealth;
import com.smd.scalinghealth.init.ModItems;
import com.smd.scalinghealth.proxy.ScalingHealthCommonProxy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

@Mod(modid = Tags.MOD_ID,
        name = Tags.MOD_NAME,
        version = Tags.VERSION,
        guiFactory = "com.smd.scalinghealth.gui.GuiFactoryScalingHealth")

public class ScalingHealth {

    public static final String GAME_RULE_DIFFICULTY = "ScalingHealthDifficulty";

    public static final Random random = new Random();

    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    public static final CreativeTabs creativeTab = new CreativeTabs(Tags.MOD_ID) {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(ModItems.heart);
        }
    };

    @SidedProxy(clientSide = "com.smd.scalinghealth.proxy.ScalingHealthClientProxy",
                serverSide = "com.smd.scalinghealth.proxy.ScalingHealthCommonProxy")

    public static ScalingHealthCommonProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @EventHandler
    public void onServerLoad(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandScalingHealth());
        event.registerServerCommand(new CommandRecalculate());
    }

    @EventHandler
    public void onServerStarted(FMLServerStartedEvent event) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null) {
            server.worlds[0].getGameRules().setOrCreateGameRule(GAME_RULE_DIFFICULTY, "true");
        }
    }

}
