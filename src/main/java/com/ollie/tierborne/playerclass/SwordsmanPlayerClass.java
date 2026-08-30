package com.ollie.tierborne.playerclass;

import com.ollie.tierborne.Tierborne;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import java.util.List;
import java.util.Set;

public final class SwordsmanPlayerClass extends PlayerClass {
    public static final String ID="swordsman", ROOT="swordsman_root", SWORD_DAMAGE_1="sword_damage", SWORD_DAMAGE_2="sword_damage_2", MOVE_SPEED_1="move_speed", MOVE_SPEED_2="move_speed_2";
    public static final String DASH_STRIKE="swordsman_dash_strike", MULTISLASH="swordsman_multislash", HEAVY_ATTACK="swordsman_heavy_attack", CLOAK="swordsman_cloak";
    public static final String SWORDMASTER="swordsman_subclass_swordmaster", DUAL="swordsman_subclass_dual", HEAVY="swordsman_subclass_heavy", ROGUE="swordsman_subclass_rogue";
    public static final String SM_DAMAGE="swordmaster_damage", SM_SPEED="swordmaster_speed", SM_DASH_STRIKE="swordmaster_dash_strike_upgrade", SM_DASH="swordmaster_dash_upgrade";
    public static final String DUAL_SPEED="dual_attack_speed", DUAL_DAMAGE="dual_damage", PARRY="dual_parry", IMPROVED_BLOCK="dual_improved_block";
    public static final String HEAVY_DAMAGE="heavy_damage", HEAVY_RANGE="heavy_range", HEAVY_RECOVERY="heavy_attack_recovery", HEAVY_ATTACK_DAMAGE="heavy_attack_damage", HEAVY_ATTACK_COOLDOWN="heavy_attack_cooldown", LEAP_STRIKE="heavy_leap_strike";
    public static final String BACKSTAB="rogue_backstab", FIRST_HIT="rogue_first_hit", NON_AGGRO="rogue_non_aggro";
    private static final List<String> SUBCLASSES=List.of(SWORDMASTER,DUAL,HEAVY,ROGUE);
    private static final List<Skill> SKILLS=List.of(
        skill(ROOT,"Swordsman","The foundation of the Swordsman path.",0,Items.DIAMOND_SWORD,0,0,List.of(),List.of(),true,null,SkillEffect.NONE),
        skill(SWORD_DAMAGE_1,"Sword Damage I","Adds 50% sword damage.",1,Items.IRON_SWORD,1,0,List.of(ROOT),List.of(),false,new SkillUpgrade(SkillBonusType.SWORD_DAMAGE,50),SkillEffect.NONE),
        skill(SWORD_DAMAGE_2,"Sword Damage II","Adds another 100% sword damage.",1,Items.DIAMOND_SWORD,2,0,List.of(SWORD_DAMAGE_1),List.of(),false,new SkillUpgrade(SkillBonusType.SWORD_DAMAGE,100),SkillEffect.NONE),
        speed(MOVE_SPEED_1,"Movement Speed I",50,-1,0,ROOT), speed(MOVE_SPEED_2,"Movement Speed II",100,-2,0,MOVE_SPEED_1),
        alt(DASH_STRIKE,"Dash Strike","Dash forward and strike using its configured damage modifier.",Items.GOLDEN_SWORD,3,1,SWORD_DAMAGE_2),
        alt(MULTISLASH,"Multislash","Perform two fully charged strikes with one sword or four alternating strikes with two swords.",Items.IRON_SWORD,-3,1,MOVE_SPEED_2),
        alt(HEAVY_ATTACK,"Heavy Attack","A powerful configured strike with a shared attack recovery.",Items.NETHERITE_SWORD,1,1,SWORD_DAMAGE_2),
        alt(CLOAK,"Cloak","Become invisible and shed hostile attention for the configured duration.",Items.ENDER_EYE,-1,1,MOVE_SPEED_2),
        subclass(SWORDMASTER,"Swordmaster",Items.DIAMOND_SWORD,3,2,DASH_STRIKE), subclass(DUAL,"Dual Swordsman",Items.IRON_SWORD,-3,2,MULTISLASH),
        subclass(HEAVY,"Heavy Swordsman",Items.NETHERITE_SWORD,1,2,HEAVY_ATTACK), subclass(ROGUE,"Rogue",Items.LEATHER_BOOTS,-1,2,CLOAK),
        node(SM_DAMAGE,"Increased Sword Damage","Further increases Swordmaster sword damage.",Items.DIAMOND_SWORD,3,3,SWORDMASTER), node(SM_SPEED,"Increased Movement Speed","Further increases intrinsic speed.",Items.SUGAR,4,3,SWORDMASTER),
        node(SM_DASH_STRIKE,"Dash Strike Upgrade","Increases Dash Strike damage.",Items.GOLDEN_SWORD,3,4,SM_DAMAGE), node(SM_DASH,"Dash Upgrade","Improves Dash velocity and cooldown.",Items.FEATHER,4,4,SM_SPEED),
        node(DUAL_SPEED,"Increased Attack Speed","Both independent sword timers recharge faster.",Items.SUGAR,-4,3,DUAL), node(DUAL_DAMAGE,"Increased Damage Per Sword","Reduces the per-sword damage penalty.",Items.DIAMOND_SWORD,-3,3,DUAL),
        node(PARRY,"Parry","Blocking can counterattack with both swords.",Items.SHIELD,-4,4,DUAL_SPEED), node(IMPROVED_BLOCK,"Improved Block","Increases blocked incoming damage.",Items.SHIELD,-3,4,DUAL_DAMAGE),
        node(HEAVY_DAMAGE,"Further Increased Sword Damage","Further increases Heavy Swordsman damage.",Items.NETHERITE_SWORD,0,3,HEAVY), node(HEAVY_RANGE,"Increased Range","Increases Heavy Swordsman melee reach.",Items.STICK,1,3,HEAVY),
        node(HEAVY_RECOVERY,"Heavy Attack Recovery","Heavy Attack no longer locks normal attacks.",Items.CLOCK,2,3,HEAVY), node(HEAVY_ATTACK_DAMAGE,"Increased Heavy Attack Damage","Raises Heavy Attack damage.",Items.NETHERITE_SWORD,0,4,HEAVY_DAMAGE),
        node(HEAVY_ATTACK_COOLDOWN,"Reduced Heavy Attack Cooldown","Reduces Heavy Attack cooldown.",Items.CLOCK,2,4,HEAVY_RECOVERY), alt(LEAP_STRIKE,"Leap Strike","Leap and strike nearby enemies when landing.",Items.IRON_BOOTS,1,5,HEAVY_RANGE),
        node(BACKSTAB,"Backstab Damage","Increases damage when attacking from behind.",Items.IRON_SWORD,-2,3,ROGUE), node(FIRST_HIT,"First Hit Damage","Increases the first hit after disengaging.",Items.CLOCK,-1,3,ROGUE), node(NON_AGGRO,"Non-Aggro Damage","Increases damage against mobs not targeting you.",Items.ENDER_EYE,0,3,ROGUE)
    );
    public SwordsmanPlayerClass(){super(ID,"Swordsman","A relentless blade master built for fast, close combat.",texture("swordsman"),Items.IRON_SWORD);}
    @Override public List<Skill> skills(){return SKILLS;}
    public Skill selectedSubclass(Set<String> skills){return SKILLS.stream().filter(s->s.effect()==SkillEffect.CLASS_SUBCLASS&&skills.contains(s.id())).findFirst().orElse(null);}
    private static Skill subclass(String id,String name,Item icon,int x,int y,String pre){return skill(id,name,"Choose the "+name+" class subclass. Excludes the other class subclasses.",1,icon,x,y,List.of(pre),SUBCLASSES.stream().filter(s->!s.equals(id)).toList(),false,null,SkillEffect.CLASS_SUBCLASS);}
    private static Skill alt(String id,String name,String desc,Item icon,int x,int y,String pre){return skill(id,name,"Alternate Attack: "+desc,1,icon,x,y,List.of(pre),List.of(),false,null,SkillEffect.ALTERNATE_ATTACK);}
    private static Skill node(String id,String name,String desc,Item icon,int x,int y,String pre){return skill(id,name,desc,1,icon,x,y,List.of(pre),List.of(),false,null,SkillEffect.NONE);}
    private static Skill speed(String id,String name,int amount,int x,int y,String pre){return new Skill(id,name,"+"+amount+"%","Adds "+amount+"% intrinsic movement speed.",1,texture(id),SkillIcon.effect(MobEffects.MOVEMENT_SPEED),x,y,List.of(pre),List.of(),false,new SkillUpgrade(SkillBonusType.MOVEMENT_SPEED,amount),SkillEffect.NONE);}
    private static Skill skill(String id,String name,String desc,int cost,Item item,int x,int y,List<String> pre,List<String> exclusions,boolean automatic,SkillUpgrade upgrade,SkillEffect effect){return new Skill(id,name,name.substring(0,1),desc,cost,texture(id),SkillIcon.item(item),x,y,pre,exclusions,automatic,upgrade,effect);}
    private static ResourceLocation texture(String name){return new ResourceLocation(Tierborne.MOD_ID,"textures/gui/icons/"+name+".png");}
}
