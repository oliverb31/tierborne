package com.ollie.tierborne.network;
import com.ollie.tierborne.data.*; import net.minecraft.network.FriendlyByteBuf; import net.minecraft.server.level.ServerPlayer; import net.minecraftforge.network.NetworkEvent; import java.util.function.Supplier;
public record SelectAlternateAttackPacket(String attackId){
 public static void encode(SelectAlternateAttackPacket p,FriendlyByteBuf b){b.writeUtf(p.attackId,64);} public static SelectAlternateAttackPacket decode(FriendlyByteBuf b){return new SelectAlternateAttackPacket(b.readUtf(64));}
 public static void handle(SelectAlternateAttackPacket p,Supplier<NetworkEvent.Context> s){NetworkEvent.Context c=s.get();c.enqueueWork(()->{ServerPlayer player=c.getSender();if(player==null)return;PlayerProgressSavedData d=PlayerProgressSavedData.get(player.getServer());if(d.get(player.getUUID()).selectAlternateAttack(p.attackId)){d.changed();ModNetwork.sync(player);}});c.setPacketHandled(true);}
}
