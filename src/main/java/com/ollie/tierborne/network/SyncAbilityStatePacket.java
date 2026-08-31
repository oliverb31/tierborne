package com.ollie.tierborne.network;
import com.ollie.tierborne.client.ClientAbilityState; import com.ollie.tierborne.combat.AbilityStatus; import net.minecraft.network.FriendlyByteBuf; import net.minecraftforge.network.NetworkEvent; import java.util.*; import java.util.function.Supplier;
public record SyncAbilityStatePacket(List<AbilityStatus> statuses,float mainCharge,float offhandCharge,boolean dualWielding,boolean multislashActive){
 public static void encode(SyncAbilityStatePacket p,FriendlyByteBuf b){b.writeCollection(p.statuses,(buf,s)->{buf.writeUtf(s.name(),64);buf.writeVarInt(s.remainingTicks());buf.writeVarInt(s.totalTicks());buf.writeBoolean(s.active());buf.writeUtf(s.stateLabel(),24);});b.writeFloat(p.mainCharge);b.writeFloat(p.offhandCharge);b.writeBoolean(p.dualWielding);b.writeBoolean(p.multislashActive);}
 public static SyncAbilityStatePacket decode(FriendlyByteBuf b){return new SyncAbilityStatePacket(b.readList(buf->new AbilityStatus(buf.readUtf(64),buf.readVarInt(),buf.readVarInt(),buf.readBoolean(),buf.readUtf(24))),b.readFloat(),b.readFloat(),b.readBoolean(),b.readBoolean());}
 public static void handle(SyncAbilityStatePacket p,Supplier<NetworkEvent.Context> s){NetworkEvent.Context c=s.get();c.enqueueWork(()->ClientAbilityState.receive(p.statuses,p.mainCharge,p.offhandCharge,p.dualWielding,p.multislashActive));c.setPacketHandled(true);}
}
