package com.smd.scalinghealth.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public abstract class Message<REQ extends Message<REQ>> implements IMessage, IMessageHandler<REQ, IMessage> {
    @Override
    public final void fromBytes(ByteBuf buf) {
        read(buf);
    }

    @Override
    public final void toBytes(ByteBuf buf) {
        write(buf);
    }

    @Override
    public final IMessage onMessage(REQ message, MessageContext context) {
        return message.handleMessage(context);
    }

    protected abstract void read(ByteBuf buf);

    protected abstract void write(ByteBuf buf);

    protected abstract IMessage handleMessage(MessageContext context);
}
