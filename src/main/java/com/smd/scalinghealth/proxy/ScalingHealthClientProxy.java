package com.smd.scalinghealth.proxy;

import com.smd.scalinghealth.Tags;
import com.smd.scalinghealth.config.Config;
import com.smd.scalinghealth.event.ScalingHealthClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import com.smd.scalinghealth.client.DifficultyDisplayHandler;
import com.smd.scalinghealth.client.key.KeyTrackerSH;
import com.smd.scalinghealth.init.ModItems;
import com.smd.scalinghealth.item.ItemDifficultyChanger;

public class ScalingHealthClientProxy extends ScalingHealthCommonProxy {
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        if(Config.Debug.debugMode){
            MinecraftForge.EVENT_BUS.register(ScalingHealthClientEvents.INSTANCE);
        }
        MinecraftForge.EVENT_BUS.register(DifficultyDisplayHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(KeyTrackerSH.INSTANCE);
        ClientRegistry.registerKeyBinding(KeyTrackerSH.INSTANCE.getKeyToggleDifficultyBar());
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
    }

    @Override
    public EntityPlayer getClientPlayer() {
        return Minecraft.getMinecraft().player;
    }

    @Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
    public static final class ModelEvents {
        private ModelEvents() {
        }

        @SubscribeEvent
        public static void onModelRegistry(ModelRegistryEvent event) {
            registerModel(ModItems.heart, 0, "heartcontainer");
            registerModel(ModItems.crystalShard, 0, "crystalshard");
            registerModel(ModItems.heartDust, 0, "heartdust");
            registerModel(ModItems.difficultyChanger, ItemDifficultyChanger.Type.ENCHANTED.getItemDamage(), "enchanted_heart");
            registerModel(ModItems.difficultyChanger, ItemDifficultyChanger.Type.CURSED.getItemDamage(), "cursed_heart");
        }

        private static void registerModel(Item item, int meta, String name) {
            ModelLoader.setCustomModelResourceLocation(item, meta,
                    new ModelResourceLocation(Tags.MOD_ID + ":" + name, "inventory"));
        }
    }
}
