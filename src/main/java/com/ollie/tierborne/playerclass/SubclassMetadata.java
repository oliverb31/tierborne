package com.ollie.tierborne.playerclass;
import java.util.Map;
public record SubclassMetadata(String skillId,String description){
 private static final Map<String,SubclassMetadata> VALUES=java.util.stream.Stream.of(
  new SubclassMetadata(SwordsmanPlayerClass.SWORDMASTER,"A mobile duelist focused on speed, precision and aggressive swordplay."),
  new SubclassMetadata(SwordsmanPlayerClass.DUAL,"A relentless fighter trading individual hit strength for rapid attacks with two swords."),
  new SubclassMetadata(SwordsmanPlayerClass.HEAVY,"A slow but devastating fighter built around powerful attacks and heavy commitments."),
  new SubclassMetadata(SwordsmanPlayerClass.ROGUE,"An elusive fighter specialising in surprise attacks and avoiding enemy attention."),
  new SubclassMetadata(SwordsmanPlayerClass.MAGIC,"A spellblade who infuses swordplay with fire and launches destructive magic at range."),
  new SubclassMetadata(ArcherPlayerClass.CROSSBOWMAN,"A rapid-loading specialist whose crossbow attacks hit harder."),
  new SubclassMetadata(ArcherPlayerClass.LONGBOWMAN,"A deliberate marksman trading draw speed for exceptional bow damage."),
  new SubclassMetadata(ArcherPlayerClass.ELEMENTAL_ARCHER,"An archer who permanently specialises in either Fire or Ice arrows."),
  new SubclassMetadata(ArcherPlayerClass.RANGER,"A mobile wilderness fighter specialising in Backstep and binding roots."),
  new SubclassMetadata(FighterPlayerClass.MONK,"A swift martial artist whose empty-main-hand attacks become devastating."),
  new SubclassMetadata(FighterPlayerClass.CHAMPION,"A relentless combatant whose same-target hits compound through Combo."),
  new SubclassMetadata(FighterPlayerClass.DUELIST,"A versatile weapon user who reacts to nearby attackers with counters."),
  new SubclassMetadata(BarbarianPlayerClass.BERSERKER,"A blood-fuelled axe fighter who trades safety for speed, damage and lifesteal."),
  new SubclassMetadata(BarbarianPlayerClass.EXECUTIONER,"A deliberate headsman who charges devastating attacks against wounded enemies."),
  new SubclassMetadata(GeneralSkillTree.LUMBERJACK,"A specialist in efficiently harvesting wood and trees."),
  new SubclassMetadata(GeneralSkillTree.MINER,"A specialist in mining stone and valuable ores."),
  new SubclassMetadata(GeneralSkillTree.BLACKSMITH,"A specialist in crafting and recovering equipment materials.")
 ).collect(java.util.stream.Collectors.toUnmodifiableMap(SubclassMetadata::skillId,v->v));
 public static SubclassMetadata get(String id){return VALUES.get(id);}
}
