package com.ollie.tierborne.playerclass;
import java.util.Map;
public record SubclassMetadata(String skillId,String description){
 private static final Map<String,SubclassMetadata> VALUES=java.util.stream.Stream.of(
  new SubclassMetadata(SwordsmanPlayerClass.SWORDMASTER,"A mobile duelist focused on speed, precision and aggressive swordplay."),
  new SubclassMetadata(SwordsmanPlayerClass.DUAL,"A relentless fighter trading individual hit strength for rapid attacks with two swords."),
  new SubclassMetadata(SwordsmanPlayerClass.HEAVY,"A slow but devastating fighter built around powerful attacks and heavy commitments."),
  new SubclassMetadata(SwordsmanPlayerClass.ROGUE,"An elusive fighter specialising in surprise attacks and avoiding enemy attention."),
  new SubclassMetadata(GeneralSkillTree.LUMBERJACK,"A specialist in efficiently harvesting wood and trees."),
  new SubclassMetadata(GeneralSkillTree.MINER,"A specialist in mining stone and valuable ores."),
  new SubclassMetadata(GeneralSkillTree.BLACKSMITH,"A specialist in crafting and recovering equipment materials.")
 ).collect(java.util.stream.Collectors.toUnmodifiableMap(SubclassMetadata::skillId,v->v));
 public static SubclassMetadata get(String id){return VALUES.get(id);}
}
