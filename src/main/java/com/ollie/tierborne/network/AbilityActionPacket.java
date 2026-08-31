package com.ollie.tierborne.network;
import com.ollie.tierborne.combat.*; import net.minecraft.network.FriendlyByteBuf; import net.minecraft.server.level.ServerPlayer; import net.minecraftforge.network.NetworkEvent; import java.util.function.Supplier;
public record AbilityActionPacket(AbilityAction action){
 public static void encode(AbilityActionPacket p,FriendlyByteBuf b){b.writeEnum(p.action);} public static AbilityActionPacket decode(FriendlyByteBuf b){return new AbilityActionPacket(b.readEnum(AbilityAction.class));}
 public static void handle(AbilityActionPacket p,Supplier<NetworkEvent.Context> s){NetworkEvent.Context c=s.get();c.enqueueWork(()->{ServerPlayer player=c.getSender();if(player!=null)AbilityRuntime.input(player,p.action);});c.setPacketHandled(true);}
}
