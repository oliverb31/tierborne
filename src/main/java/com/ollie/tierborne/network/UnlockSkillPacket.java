package com.ollie.tierborne.network;
import com.ollie.tierborne.*; import com.ollie.tierborne.data.*; import net.minecraft.network.FriendlyByteBuf; import net.minecraft.server.level.ServerPlayer; import net.minecraftforge.network.NetworkEvent; import java.util.function.Supplier;
public record UnlockSkillPacket(String skillId) {
 public static void encode(UnlockSkillPacket p,FriendlyByteBuf b){b.writeUtf(p.skillId,64);} public static UnlockSkillPacket decode(FriendlyByteBuf b){return new UnlockSkillPacket(b.readUtf(64));}
 public static void handle(UnlockSkillPacket p,Supplier<NetworkEvent.Context> supplier){NetworkEvent.Context c=supplier.get();c.enqueueWork(()->{ServerPlayer player=c.getSender();if(player==null)return;PlayerProgressSavedData d=PlayerProgressSavedData.get(player.getServer());if(d.get(player.getUUID()).unlock(p.skillId)){d.changed();TierborneEvents.applySkillEffects(player,d.get(player.getUUID()));}ModNetwork.sync(player);});c.setPacketHandled(true);}
}
