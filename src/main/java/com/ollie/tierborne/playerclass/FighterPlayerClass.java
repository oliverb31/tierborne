package com.ollie.tierborne.playerclass;

import com.ollie.tierborne.Tierborne;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Set;

public final class FighterPlayerClass extends PlayerClass {
    public static final String ID="fighter",ROOT="fighter_root";
    public static final String MONK_MOVE="fighter_monk_move",MONK_FIST="fighter_monk_fist",PULL="fighter_pull",MONK="fighter_subclass_monk",PULL_RECOVERY="monk_pull_recovery",EXTENDED_PULL="monk_extended_pull",MONK_FIST_UPGRADE="monk_fist_upgrade";
    public static final String CHAMPION_DAMAGE="fighter_champion_damage",CHAMPION_SPEED="fighter_champion_speed",CHAIN="fighter_chain",CHAMPION="fighter_subclass_champion",COMBO_WINDOW_I="champion_combo_window_1",COMBO_WINDOW_II="champion_combo_window_2",CHAIN_BONUS="champion_chain_bonus",CHAIN_WINDOW="champion_chain_window";
    public static final String DUELIST_DAMAGE="fighter_duelist_damage",DUELIST_SPEED="fighter_duelist_speed",DISARM="fighter_disarm",DUELIST="fighter_subclass_duelist",COUNTER_CHANCE="duelist_counter_chance",PERFECT_COUNTER="duelist_perfect_counter",REFLECTIVE_COUNTER="duelist_reflective_counter",DUELIST_DAMAGE_UPGRADE="duelist_damage_upgrade",DISARM_DURATION="duelist_disarm_duration",DISARM_COOLDOWN="duelist_disarm_cooldown";
    private static final List<String> SUBCLASSES=List.of(MONK,CHAMPION,DUELIST);
    private static final List<Skill> SKILLS=List.of(
        skill(ROOT,"Fighter","The foundation of the versatile Fighter path.",0,Items.IRON_AXE,0,0,List.of(),List.of(),true,null,SkillEffect.NONE),
        upgrade(MONK_MOVE,"Movement Speed",SkillBonusType.MOVEMENT_SPEED,10,Items.LEATHER_BOOTS,-2,-2,ROOT),upgrade(MONK_FIST,"Fist Damage",SkillBonusType.FIST_DAMAGE,50,Items.RABBIT_HIDE,-3,-3,MONK_MOVE),alt(PULL,"Pull","Pull a crosshair target in and strike with a full-power fist.",Items.LEAD,-4,-4,MONK_FIST),subclass(MONK,"Monk",Items.RABBIT_FOOT,-5,-5,PULL),node(PULL_RECOVERY,"Pull Recovery","Reduces Pull cooldown.",Items.CLOCK,-6,-4,MONK),node(EXTENDED_PULL,"Extended Pull","Increases Pull range.",Items.LEAD,-6,-5,MONK),node(MONK_FIST_UPGRADE,"Fist Damage","Further increases Monk fist damage.",Items.RABBIT_HIDE,-6,-6,MONK),
        upgrade(CHAMPION_DAMAGE,"General Damage",SkillBonusType.GENERAL_DAMAGE,5,Items.IRON_SWORD,0,-2,ROOT),upgrade(CHAMPION_SPEED,"Melee Charge Speed",SkillBonusType.MELEE_CHARGE_SPEED,10,Items.SUGAR,0,-3,CHAMPION_DAMAGE),alt(CHAIN,"Chain","Arm a temporary compounding same-target Combo.",Items.CHAIN,0,-4,CHAMPION_SPEED),subclass(CHAMPION,"Champion",Items.GOLDEN_SWORD,0,-5,CHAIN),node(COMBO_WINDOW_I,"Combo Window I","Extends the normal Champion Combo window.",Items.CLOCK,-1,-6,CHAMPION),node(COMBO_WINDOW_II,"Combo Window II","Further extends the normal Combo window.",Items.CLOCK,-1,-7,COMBO_WINDOW_I),node(CHAIN_BONUS,"Chain Opening","Increases Chain's opening damage.",Items.GOLDEN_SWORD,1,-6,CHAMPION),node(CHAIN_WINDOW,"Chain Window","Extends Chain's combo window.",Items.CLOCK,1,-7,CHAIN_BONUS),
        upgrade(DUELIST_DAMAGE,"General Damage",SkillBonusType.GENERAL_DAMAGE,5,Items.IRON_AXE,2,2,ROOT),upgrade(DUELIST_SPEED,"Melee Charge Speed",SkillBonusType.MELEE_CHARGE_SPEED,10,Items.SUGAR,3,3,DUELIST_DAMAGE),alt(DISARM,"Disarm","Disable a qualifying crosshair target's offensive actions.",Items.SHEARS,4,4,DUELIST_SPEED),subclass(DUELIST,"Duelist",Items.IRON_SWORD,5,5,DISARM),node(COUNTER_CHANCE,"Counter Chance","Raises automatic Counterattack chance.",Items.SHIELD,6,4,DUELIST),node(PERFECT_COUNTER,"Perfect Counter","Successful counters negate their triggering hit.",Items.SHIELD,7,4,COUNTER_CHANCE),node(REFLECTIVE_COUNTER,"Reflective Counter","Counters reflect at least the incoming raw damage.",Items.AMETHYST_SHARD,8,4,PERFECT_COUNTER),node(DUELIST_DAMAGE_UPGRADE,"Versatile Damage","Further increases broad weapon and fist damage.",Items.DIAMOND_SWORD,6,5,DUELIST),node(DISARM_DURATION,"Disarm Duration","Extends Disarm's offensive lock.",Items.CLOCK,6,6,DUELIST),node(DISARM_COOLDOWN,"Disarm Recovery","Reduces Disarm cooldown.",Items.CLOCK,7,6,DISARM_DURATION)
    );
    public FighterPlayerClass(){super(ID,"Fighter","A versatile combatant specialising in martial arts, relentless combos, or reactive duelling.",texture("fighter"),Items.IRON_AXE,List.of("Monk","Champion","Duelist"));}
    @Override public List<Skill> skills(){return SKILLS;}
    public Skill selectedSubclass(Set<String> skills){return SKILLS.stream().filter(s->SUBCLASSES.contains(s.id())&&skills.contains(s.id())).findFirst().orElse(null);}
    private static Skill subclass(String id,String name,Item icon,int x,int y,String pre){return skill(id,name,"Choose the "+name+" subclass.",1,icon,x,y,List.of(pre),SUBCLASSES.stream().filter(s->!s.equals(id)).toList(),false,null,SkillEffect.CLASS_SUBCLASS);}
    private static Skill alt(String id,String name,String desc,Item icon,int x,int y,String pre){return skill(id,name,"Alternate Attack: "+desc,1,icon,x,y,List.of(pre),List.of(),false,null,SkillEffect.ALTERNATE_ATTACK);}
    private static Skill node(String id,String name,String desc,Item icon,int x,int y,String pre){return skill(id,name,desc,1,icon,x,y,List.of(pre),List.of(),false,null,SkillEffect.NONE);}
    private static Skill upgrade(String id,String name,SkillBonusType type,int amount,Item icon,int x,int y,String pre){return skill(id,name,"Adds "+amount+"% "+type.displayName()+".",1,icon,x,y,List.of(pre),List.of(),false,new SkillUpgrade(type,amount),SkillEffect.NONE);}
    private static Skill skill(String id,String name,String desc,int cost,Item item,int x,int y,List<String> pre,List<String> exclusions,boolean automatic,SkillUpgrade upgrade,SkillEffect effect){return new Skill(id,name,name.substring(0,1),desc,cost,texture(id),SkillIcon.item(item),x,y,pre,exclusions,automatic,upgrade,effect);}
    private static ResourceLocation texture(String name){return new ResourceLocation(Tierborne.MOD_ID,"textures/gui/icons/fighter_"+name+".png");}
}
