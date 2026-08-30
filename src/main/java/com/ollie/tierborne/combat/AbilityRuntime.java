package com.ollie.tierborne.combat;

import com.ollie.tierborne.config.RpgBalanceConfig;
import com.ollie.tierborne.data.*;
import com.ollie.tierborne.playerclass.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.*;
import java.util.*;

public final class AbilityRuntime {
    private static final String CLOAK_TAG="tierborne:cloak_active";
    private static final Map<UUID, State> STATES=new HashMap<>();
    private static final Set<UUID> INTERNAL_DAMAGE=new HashSet<>();
    private AbilityRuntime(){}
    public static State state(ServerPlayer p){return STATES.computeIfAbsent(p.getUUID(),u->new State());}
    public static boolean internalDamage(ServerPlayer p){return INTERNAL_DAMAGE.contains(p.getUUID());}
    public static List<AbilityStatus> statuses(ServerPlayer p){State s=state(p);long now=p.level.getGameTime();List<AbilityStatus> result=new ArrayList<>();for(Map.Entry<String,Long> entry:s.cooldowns.entrySet()){int remaining=(int)Math.max(0,entry.getValue()-now);if(remaining<=0)continue;int total=s.cooldownDurations.getOrDefault(entry.getKey(),remaining);result.add(new AbilityStatus(name(entry.getKey()),remaining,Math.max(1,total),false));}if(s.multislashActiveUntil>now)result.add(new AbilityStatus("Multislash",(int)(s.multislashActiveUntil-now),Math.max(1,s.multislashDuration),true));if(s.cloakedUntil>now)result.add(new AbilityStatus("Cloak",(int)(s.cloakedUntil-now),RpgBalanceConfig.ticks(RpgBalanceConfig.CLOAK_DURATION_SECONDS),true));if(s.utilityReady>now)result.add(new AbilityStatus("Dash",(int)(s.utilityReady-now),Math.max(1,s.utilityDuration),false));if(s.drawLockedUntil>now)result.add(new AbilityStatus("Draw Delay",(int)(s.drawLockedUntil-now),Math.max(1,s.drawLockDuration),true));if(s.normalAttackLockedUntil>now&&!progress(p).hasSkill(SwordsmanPlayerClass.HEAVY_RECOVERY))result.add(new AbilityStatus("Attack Recovery",(int)(s.normalAttackLockedUntil-now),Math.max(1,s.normalAttackLockDuration),true));result.sort(Comparator.comparing(AbilityStatus::name));return result;}
    public static void input(ServerPlayer p,AbilityAction action){
        PlayerProgress progress=progress(p); State s=state(p); long now=p.level.getGameTime();
        if(action==AbilityAction.UTILITY_START){setBlocking(p,s,progress.hasSkill(SwordsmanPlayerClass.DUAL)&&sword(p.getMainHandItem())&&sword(p.getOffhandItem()));if(progress.hasSkill(SwordsmanPlayerClass.SWORDMASTER))dash(p,progress,s,now);return;}
        if(action==AbilityAction.UTILITY_STOP){setBlocking(p,s,false);return;}
        if(action==AbilityAction.OFFHAND_ATTACK){offhand(p,progress,s,now);return;}
        AlternateAttackDefinition attack=AlternateAttackDefinition.find(progress.selectedAlternateAttack());
        if(attack==null||!progress.hasSkill(attack.skillId())||s.cooldowns.getOrDefault(attack.id(),0L)>now||s.multislashActiveUntil>now)return;
        switch(attack.id()){
            case "dash_strike"->dashStrike(p,progress,s,now);
            case "multislash"->multislash(p,progress,s,now);
            case "heavy_attack"->heavyAttack(p,progress,s,now);
            case "cloak"->cloak(p,s,now);
            case "leap_strike"->leap(p,s,now);
        }
    }
    public static void tick(ServerPlayer p){
        State s=state(p); long now=p.level.getGameTime(); PlayerProgress progress=progress(p);
        boolean holdingSword=sword(p.getMainHandItem());
        if(progress.hasSkill(SwordsmanPlayerClass.HEAVY)){
            if(holdingSword&&(!s.wasHoldingSword||p.getInventory().selected!=s.lastSelectedSlot||p.getMainHandItem().getItem()!=s.lastMainItem)){s.drawLockDuration=RpgBalanceConfig.ticks(RpgBalanceConfig.HEAVY_DRAW_DELAY_SECONDS);s.drawLockedUntil=now+s.drawLockDuration;}
            if(holdingSword)s.heavyMoveUntil=now+RpgBalanceConfig.ticks(RpgBalanceConfig.HEAVY_MOVE_LINGER_SECONDS);
        }
        s.wasHoldingSword=holdingSword;s.lastMainItem=p.getMainHandItem().getItem();s.lastSelectedSlot=p.getInventory().selected;
        if(s.multislashHitsRemaining>0&&now>=s.nextMultislashHit){boolean offhand=s.multislashUsesOffhand&&s.multislashHitsRemaining%2==1;ItemStack weapon=offhand?p.getOffhandItem():p.getMainHandItem();net.minecraft.world.InteractionHand hand=offhand?net.minecraft.world.InteractionHand.OFF_HAND:net.minecraft.world.InteractionHand.MAIN_HAND;p.swing(hand,true);strike(p,weapon,hand,1.0,s);s.multislashHitsRemaining--;s.nextMultislashHit+=s.multislashInterval;}
        if(s.multislashPendingCooldown&&now>=s.multislashActiveUntil){s.multislashPendingCooldown=false;s.multislashTarget=null;p.resetAttackStrengthTicker();if(s.multislashUsesOffhand)s.offLastAttack=now;cooldown(s,"multislash",now,RpgBalanceConfig.ticks(RpgBalanceConfig.MULTISLASH_COOLDOWN_SECONDS));}
        if(s.leaping&&p.isOnGround()&&now>s.leapStarted+3){s.leaping=false;double radius=RpgBalanceConfig.LEAP_RADIUS.get();for(LivingEntity target:p.level.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(radius),e->e!=p&&e.isAlive())){hurt(p,target,baseDamage(p));Vec3 away=target.position().subtract(p.position()).normalize().scale(RpgBalanceConfig.LEAP_KNOCKBACK.get());target.push(away.x,0.45,away.z);}}
        if(s.cloakedUntil>0&&now>=s.cloakedUntil){s.cloakedUntil=0;p.removeEffect(MobEffects.INVISIBILITY);p.getPersistentData().remove(CLOAK_TAG);com.ollie.tierborne.network.ModNetwork.syncCloak(p,false);}
        if(s.cloakedUntil>now)for(Mob mob:p.level.getEntitiesOfClass(Mob.class,p.getBoundingBox().inflate(32),m->m.getTarget()==p))mob.setTarget(null);
        if(progress.hasSkill(SwordsmanPlayerClass.ROGUE)&&now%20==0)preferOtherTargets(p);
        setBlocking(p,s,s.blocking&&progress.hasSkill(SwordsmanPlayerClass.DUAL)&&sword(p.getMainHandItem())&&sword(p.getOffhandItem()));
    }
    public static boolean normalAttackBlocked(ServerPlayer p){State s=state(p);long now=p.level.getGameTime();return now<s.multislashActiveUntil||now<s.drawLockedUntil||(now<s.normalAttackLockedUntil&&!progress(p).hasSkill(SwordsmanPlayerClass.HEAVY_RECOVERY));}
    public static boolean beginNormalSwordAttack(ServerPlayer p){return !normalAttackBlocked(p);}
    public static void offensiveAction(ServerPlayer p){State s=state(p);if(s.cloakedUntil>0){s.cloakedUntil=0;p.removeEffect(MobEffects.INVISIBILITY);p.getPersistentData().remove(CLOAK_TAG);com.ollie.tierborne.network.ModNetwork.syncCloak(p,false);}}
    public static boolean isCloaked(ServerPlayer p){return state(p).cloakedUntil>p.level.getGameTime();}
    public static boolean isBlocking(ServerPlayer p){return state(p).blocking;}
    public static boolean isDualWielding(ServerPlayer p){return progress(p).hasSkill(SwordsmanPlayerClass.DUAL)&&sword(p.getMainHandItem())&&sword(p.getOffhandItem());}
    public static boolean isMultislashActive(ServerPlayer p){return state(p).multislashActiveUntil>p.level.getGameTime();}
    public static float offhandCharge(ServerPlayer p){State s=state(p);long now=p.level.getGameTime();return s.offLastAttack==Long.MIN_VALUE?1.0F:(float)Math.min(1.0,(now-s.offLastAttack)/(double)recharge(progress(p)));}
    public static void resetTransient(ServerPlayer p){STATES.remove(p.getUUID());if(p.getPersistentData().getBoolean(CLOAK_TAG)){p.removeEffect(MobEffects.INVISIBILITY);p.getPersistentData().remove(CLOAK_TAG);}com.ollie.tierborne.network.ModNetwork.syncCloak(p,false);com.ollie.tierborne.network.ModNetwork.syncBlock(p,false);com.ollie.tierborne.network.ModNetwork.syncAbilities(p);}
    public static boolean heavyMovementPenaltyActive(ServerPlayer p){return p.level.getGameTime()<state(p).heavyMoveUntil;}
    public static void tryParry(ServerPlayer p,Entity attacker){PlayerProgress x=progress(p);State s=state(p);long now=p.level.getGameTime();if(!x.hasSkill(SwordsmanPlayerClass.PARRY)||now<s.parryReady||!(attacker instanceof LivingEntity target)||!sword(p.getMainHandItem())||!sword(p.getOffhandItem()))return;s.parryReady=now+RpgBalanceConfig.ticks(RpgBalanceConfig.PARRY_COOLDOWN_SECONDS);hurt(p,target,baseDamage(p));hurt(p,target,baseDamage(p));}
    public static double additionalSwordDamagePercent(ServerPlayer p,LivingEntity target){
        PlayerProgress x=progress(p);double bonus=SwordsmanStats.subclassSwordDamage(x.unlockedSkills());
        if(x.hasSkill(SwordsmanPlayerClass.ROGUE)){
            long now=p.level.getGameTime();State s=state(p);
            if(x.hasSkill(SwordsmanPlayerClass.BACKSTAB)){Vec3 toAttacker=p.position().subtract(target.position()).normalize();if(target.getLookAngle().dot(toAttacker)<-RpgBalanceConfig.BACKSTAB_DOT_THRESHOLD.get())bonus+=RpgBalanceConfig.BACKSTAB_DAMAGE.get();}
            Long lastHit=s.lastHit.get(target.getUUID());
            if(x.hasSkill(SwordsmanPlayerClass.FIRST_HIT)&&(lastHit==null||now-lastHit>RpgBalanceConfig.ticks(RpgBalanceConfig.FIRST_HIT_RESET_SECONDS)))bonus+=RpgBalanceConfig.FIRST_HIT_DAMAGE.get();
            if(x.hasSkill(SwordsmanPlayerClass.NON_AGGRO)&&target instanceof Mob mob&&mob.getTarget()!=p)bonus+=RpgBalanceConfig.NON_AGGRO_DAMAGE.get();
            s.lastHit.put(target.getUUID(),now);
        }
        return bonus;
    }
    private static void dashStrike(ServerPlayer p,PlayerProgress x,State s,long now){if(!sword(p.getMainHandItem()))return;offensiveAction(p);Vec3 look=horizontal(p.getLookAngle());p.setDeltaMovement(look.scale(1.5).add(0,0.1,0));p.hurtMarked=true;p.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);double bonus=x.hasSkill(SwordsmanPlayerClass.SM_DASH_STRIKE)?RpgBalanceConfig.DASH_STRIKE_UPGRADE_DAMAGE.get():RpgBalanceConfig.DASH_STRIKE_DAMAGE.get();target(p,RpgBalanceConfig.DASH_STRIKE_DISTANCE.get()).ifPresent(t->weaponHit(p,t,p.getMainHandItem(),net.minecraft.world.InteractionHand.MAIN_HAND,fullChargeDamage(p,p.getMainHandItem(),t)*(1+bonus/100),false));cooldown(s,"dash_strike",now,RpgBalanceConfig.ticks(RpgBalanceConfig.DASH_STRIKE_COOLDOWN_SECONDS));}
    private static void heavyAttack(ServerPlayer p,PlayerProgress x,State s,long now){if(!sword(p.getMainHandItem()))return;offensiveAction(p);p.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);double bonus=x.hasSkill(SwordsmanPlayerClass.HEAVY_ATTACK_DAMAGE)?RpgBalanceConfig.HEAVY_ATTACK_UPGRADE_DAMAGE.get():RpgBalanceConfig.HEAVY_ATTACK_DAMAGE.get();target(p,reach(x)).ifPresent(t->weaponHit(p,t,p.getMainHandItem(),net.minecraft.world.InteractionHand.MAIN_HAND,fullChargeDamage(p,p.getMainHandItem(),t)*(1+bonus/100),false));int cd=RpgBalanceConfig.ticks(x.hasSkill(SwordsmanPlayerClass.HEAVY_ATTACK_COOLDOWN)?RpgBalanceConfig.HEAVY_ATTACK_UPGRADE_COOLDOWN_SECONDS:RpgBalanceConfig.HEAVY_ATTACK_COOLDOWN_SECONDS);cooldown(s,"heavy_attack",now,cd);if(!x.hasSkill(SwordsmanPlayerClass.HEAVY_RECOVERY)){s.normalAttackLockDuration=cd;s.normalAttackLockedUntil=now+cd;}}
    private static void multislash(ServerPlayer p,PlayerProgress x,State s,long now){if(!sword(p.getMainHandItem())){p.displayClientMessage(Component.literal("Multislash requires a sword in the main hand."),true);return;}offensiveAction(p);s.multislashTarget=target(p,3.5).map(Entity::getUUID).orElse(null);s.multislashUsesOffhand=sword(p.getOffhandItem());s.multislashHitsRemaining=s.multislashUsesOffhand?4:2;s.multislashDuration=Math.max(1,RpgBalanceConfig.ticks(RpgBalanceConfig.MULTISLASH_DURATION_SECONDS));s.multislashInterval=Math.max(1,s.multislashDuration/s.multislashHitsRemaining);s.nextMultislashHit=now;s.multislashActiveUntil=now+s.multislashDuration;s.multislashPendingCooldown=true;}
    private static void cloak(ServerPlayer p,State s,long now){int duration=RpgBalanceConfig.ticks(RpgBalanceConfig.CLOAK_DURATION_SECONDS);p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,duration,0,false,false,true));p.getPersistentData().putBoolean(CLOAK_TAG,true);s.cloakedUntil=now+duration;com.ollie.tierborne.network.ModNetwork.syncCloak(p,true);cooldown(s,"cloak",now,RpgBalanceConfig.ticks(RpgBalanceConfig.CLOAK_COOLDOWN_SECONDS));for(Mob mob:p.level.getEntitiesOfClass(Mob.class,p.getBoundingBox().inflate(32),m->m.getTarget()==p))mob.setTarget(null);}
    private static void leap(ServerPlayer p,State s,long now){offensiveAction(p);p.setDeltaMovement(p.getDeltaMovement().x,RpgBalanceConfig.LEAP_LAUNCH.get(),p.getDeltaMovement().z);p.hurtMarked=true;s.leaping=true;s.leapStarted=now;cooldown(s,"leap_strike",now,RpgBalanceConfig.ticks(RpgBalanceConfig.LEAP_COOLDOWN_SECONDS));}
    private static void dash(ServerPlayer p,PlayerProgress x,State s,long now){if(now<s.utilityReady)return;boolean upgraded=x.hasSkill(SwordsmanPlayerClass.SM_DASH);Vec3 look=horizontal(p.getLookAngle());p.setDeltaMovement(look.scale(upgraded?RpgBalanceConfig.DASH_UPGRADE_VELOCITY.get():RpgBalanceConfig.DASH_VELOCITY.get()).add(0,0.08,0));p.hurtMarked=true;s.utilityDuration=RpgBalanceConfig.ticks(upgraded?RpgBalanceConfig.DASH_UPGRADE_COOLDOWN_SECONDS:RpgBalanceConfig.DASH_COOLDOWN_SECONDS);s.utilityReady=now+s.utilityDuration;}
    private static void offhand(ServerPlayer p,PlayerProgress x,State s,long now){if(!x.hasSkill(SwordsmanPlayerClass.DUAL)||now<s.multislashActiveUntil||!sword(p.getMainHandItem())||!sword(p.getOffhandItem()))return;offensiveAction(p);long recharge=recharge(x);double charge=s.offLastAttack==Long.MIN_VALUE?1.0:Math.min(1.0,(now-s.offLastAttack)/(double)recharge);double strength=0.2+charge*charge*0.8;target(p,3.2).ifPresent(t->weaponHit(p,t,p.getOffhandItem(),net.minecraft.world.InteractionHand.OFF_HAND,fullChargeDamage(p,p.getOffhandItem(),t)*strength,true));s.offLastAttack=now;p.swing(net.minecraft.world.InteractionHand.OFF_HAND,true);}
    private static void strike(ServerPlayer p,ItemStack sword,net.minecraft.world.InteractionHand hand,double multiplier,State state){if(!sword(sword))return;LivingEntity locked=null;if(state.multislashTarget!=null&&p.level instanceof net.minecraft.server.level.ServerLevel level&&level.getEntity(state.multislashTarget) instanceof LivingEntity living&&living.isAlive()&&living.distanceToSqr(p)<64.0)locked=living;Optional.ofNullable(locked).or(()->target(p,3.5)).ifPresent(t->weaponHit(p,t,sword,hand,fullChargeDamage(p,sword,t)*multiplier,true));}
    private static void setBlocking(ServerPlayer p,State s,boolean blocking){if(s.blocking==blocking)return;s.blocking=blocking;com.ollie.tierborne.network.ModNetwork.syncBlock(p,blocking);}
    private static boolean hurt(ServerPlayer p,LivingEntity target,double amount){INTERNAL_DAMAGE.add(p.getUUID());try{return target.hurt(DamageSource.playerAttack(p),(float)amount);}finally{INTERNAL_DAMAGE.remove(p.getUUID());}}
    private static void weaponHit(ServerPlayer p,LivingEntity target,ItemStack weapon,net.minecraft.world.InteractionHand hand,double amount,boolean bypassHurtFrames){if(bypassHurtFrames)target.invulnerableTime=0;if(!hurt(p,target,amount))return;int fire=net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT,weapon);if(fire>0)target.setSecondsOnFire(fire*4);int knockback=net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.KNOCKBACK,weapon)+(p.isSprinting()?1:0);if(knockback>0)target.knockback(knockback*0.5,net.minecraft.util.Mth.sin(p.getYRot()*(float)Math.PI/180.0F),-net.minecraft.util.Mth.cos(p.getYRot()*(float)Math.PI/180.0F));weapon.hurtAndBreak(1,p,owner->owner.broadcastBreakEvent(hand));}
    private static Optional<LivingEntity> target(ServerPlayer p,double distance){Vec3 start=p.getEyePosition(),end=start.add(p.getLookAngle().scale(distance));AABB box=p.getBoundingBox().expandTowards(p.getLookAngle().scale(distance)).inflate(1);return p.level.getEntitiesOfClass(LivingEntity.class,box,e->e!=p&&e.isAlive()).stream().filter(e->e.getBoundingBox().inflate(.3).clip(start,end).isPresent()).min(Comparator.comparingDouble(p::distanceToSqr));}
    private static double baseDamage(ServerPlayer p){return p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);}
    private static double fullChargeDamage(ServerPlayer p,ItemStack weapon,LivingEntity target){double damage=baseDamage(p);if(weapon!=p.getMainHandItem()&&weapon.getItem() instanceof SwordItem off&&p.getMainHandItem().getItem() instanceof SwordItem main)damage+=off.getDamage()-main.getDamage();return damage+net.minecraft.world.item.enchantment.EnchantmentHelper.getDamageBonus(weapon,target.getMobType());}
    private static double reach(PlayerProgress x){return 3.5+(x.hasSkill(SwordsmanPlayerClass.HEAVY_RANGE)?RpgBalanceConfig.HEAVY_RANGE.get():0);}
    private static long recharge(PlayerProgress x){double speed=x.hasSkill(SwordsmanPlayerClass.DUAL_SPEED)?RpgBalanceConfig.DUAL_SPEED_UPGRADE.get():RpgBalanceConfig.DUAL_ATTACK_SPEED.get();return Math.max(1,Math.round(12/(1+speed/100.0)));}
    private static boolean sword(ItemStack s){return s.getItem() instanceof SwordItem;}
    private static Vec3 horizontal(Vec3 v){Vec3 h=new Vec3(v.x,0,v.z);return h.lengthSqr()==0?Vec3.ZERO:h.normalize();}
    private static PlayerProgress progress(ServerPlayer p){return PlayerProgressSavedData.get(p.getServer()).get(p.getUUID());}
    private static void cooldown(State s,String id,long now,int duration){s.cooldowns.put(id,now+duration);s.cooldownDurations.put(id,duration);}
    private static String name(String id){AlternateAttackDefinition attack=AlternateAttackDefinition.find(id);return attack==null?id:attack.displayName();}
    private static void preferOtherTargets(ServerPlayer rogue){double radius=RpgBalanceConfig.ROGUE_RETARGET_RADIUS.get();for(Mob mob:rogue.level.getEntitiesOfClass(Mob.class,rogue.getBoundingBox().inflate(radius),m->m.getTarget()==rogue)){ServerPlayer alternative=rogue.getServer().getPlayerList().getPlayers().stream().filter(p->p!=rogue&&p.level==rogue.level&&p.distanceToSqr(mob)<radius*radius).min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);if(alternative!=null)mob.setTarget(alternative);}}
    public static final class State{private final Map<String,Long> cooldowns=new HashMap<>();private final Map<String,Integer> cooldownDurations=new HashMap<>();private final Map<UUID,Long> lastHit=new HashMap<>();private long utilityReady,normalAttackLockedUntil,drawLockedUntil,nextMultislashHit,cloakedUntil,leapStarted,heavyMoveUntil,multislashActiveUntil,offLastAttack=Long.MIN_VALUE;private int utilityDuration,normalAttackLockDuration,drawLockDuration,multislashDuration,multislashHitsRemaining,multislashInterval,lastSelectedSlot=-1;private boolean blocking,leaping,wasHoldingSword,multislashUsesOffhand,multislashPendingCooldown;private UUID multislashTarget;private net.minecraft.world.item.Item lastMainItem;public boolean blocking(){return blocking;}public long parryReady;}
}
