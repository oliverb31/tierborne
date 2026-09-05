package com.ollie.tierborne.network;
import com.ollie.tierborne.*; import com.ollie.tierborne.data.*; import com.ollie.tierborne.playerclass.ClassStarterEquipment; import net.minecraft.network.FriendlyByteBuf; import net.minecraft.server.level.ServerPlayer; import net.minecraftforge.network.NetworkEvent; import java.util.function.Supplier;
public record ChoosePlayerClassPacket(String playerClassId) {
 public static void encode(ChoosePlayerClassPacket p,FriendlyByteBuf b){b.writeUtf(p.playerClassId,64);} public static ChoosePlayerClassPacket decode(FriendlyByteBuf b){return new ChoosePlayerClassPacket(b.readUtf(64));}
 public static void handle(ChoosePlayerClassPacket p,Supplier<NetworkEvent.Context> supplier){NetworkEvent.Context c=supplier.get();c.enqueueWork(()->{ServerPlayer player=c.getSender();if(player==null)return;PlayerProgressSavedData d=PlayerProgressSavedData.get(player.getServer());if(d.get(player.getUUID()).choosePlayerClass(p.playerClassId)){d.changed();TierborneEvents.applySkillEffects(player,d.get(player.getUUID()));ClassStarterEquipment.grant(player,p.playerClassId);}ModNetwork.sync(player);});c.setPacketHandled(true);}
}
