package com.ollie.tierborne.network;
import com.ollie.tierborne.client.ClientProgress; import com.ollie.tierborne.data.*; import net.minecraft.network.FriendlyByteBuf; import net.minecraft.server.level.ServerPlayer; import net.minecraftforge.network.NetworkEvent; import java.util.*; import java.util.function.Supplier;
public record SyncProgressPacket(String playerClassId,int skillPoints,Set<String> unlockedSkills,String selectedAlternateAttack) {
 public static SyncProgressPacket from(ServerPlayer p){PlayerProgress x=PlayerProgressSavedData.get(p.getServer()).get(p.getUUID());return new SyncProgressPacket(x.playerClassId(),x.skillPoints(),x.unlockedSkills(),x.selectedAlternateAttack());}
 public static void encode(SyncProgressPacket p,FriendlyByteBuf b){b.writeUtf(p.playerClassId,64);b.writeVarInt(p.skillPoints);b.writeCollection(p.unlockedSkills,(buf,s)->buf.writeUtf(s,64));b.writeUtf(p.selectedAlternateAttack,64);}
 public static SyncProgressPacket decode(FriendlyByteBuf b){return new SyncProgressPacket(b.readUtf(64),b.readVarInt(),b.readCollection(HashSet::new,buf->buf.readUtf(64)),b.readUtf(64));}
 public static void handle(SyncProgressPacket p,Supplier<NetworkEvent.Context> supplier){NetworkEvent.Context c=supplier.get();c.enqueueWork(()->ClientProgress.receive(p));c.setPacketHandled(true);}
}
