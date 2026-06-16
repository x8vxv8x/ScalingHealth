package com.smd.scalinghealth.network;

import com.smd.scalinghealth.Tags;
import com.smd.scalinghealth.network.message.MessageDataSync;
import com.smd.scalinghealth.network.message.MessagePlaySound;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class NetworkHandler {
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MOD_ID);

    private static int nextId = 0;

    private NetworkHandler() {}

    public static void init() {
        register(MessageDataSync.class, Side.CLIENT);
        register(MessagePlaySound.class, Side.CLIENT);
    }

    private static <T extends Message<T>> void register(Class<T> messageClass, Side handlerSide) {
        INSTANCE.registerMessage(messageClass, messageClass, nextId++, handlerSide);
    }
}
