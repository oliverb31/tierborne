package com.ollie.tierborne.data;
import net.minecraft.nbt.CompoundTag; import net.minecraft.server.MinecraftServer; import net.minecraft.world.level.saveddata.SavedData; import java.util.*;
public final class PlayerProgressSavedData extends SavedData {
 private static final String NAME="tierborne_player_progress"; private final Map<UUID,PlayerProgress> players=new HashMap<>();
 public static PlayerProgressSavedData get(MinecraftServer s){return s.overworld().getDataStorage().computeIfAbsent(PlayerProgressSavedData::load,PlayerProgressSavedData::new,NAME);}
 public PlayerProgress get(UUID id){return players.computeIfAbsent(id,key->{setDirty();return new PlayerProgress();});} public void changed(){setDirty();}
 @Override public CompoundTag save(CompoundTag t){CompoundTag all=new CompoundTag();players.forEach((id,p)->all.put(id.toString(),p.save()));t.put("Players",all);return t;}
 private static PlayerProgressSavedData load(CompoundTag t){PlayerProgressSavedData d=new PlayerProgressSavedData();CompoundTag all=t.getCompound("Players");for(String key:all.getAllKeys())try{d.players.put(UUID.fromString(key),PlayerProgress.load(all.getCompound(key)));}catch(IllegalArgumentException ignored){}return d;}
}
