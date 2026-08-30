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
    private static final Map<UUID, State> STATES=new HashMap<>();
    private static final Set<UUID> INTERNAL_DAMAGE=new HashSet<>();
    private AbilityRuntime(){}
    public static State state(ServerPlayer p){return STATES.computeIfAbsent(p.getUUID(),u->new State());}
    public static boolean internalDamage(ServerPlayer p){return INTERNAL_DAMAGE.contains(p.getUUID());}
    public static List<AbilityStatus> statuses(ServerPlayer p){State s=state(p);long now=p.level.getGameTime();List<AbilityStatus> result=new ArrayList<>();for(Map.Entry<String,Long> entry:s.cooldowns.entrySet()){int remaining=(int)Math.max(0,entry.getValue()-now);if(remaining<=0)continue;boolean active=false;int shownRemaining=remaining,total=s.cooldownDurations.getOrDefault(entry.getKey(),remaining);if(entry.getKey().equals("cloak")&&s.cloakedUntil>now){active=true;shownRemaining=(int)(s.cloakedUntil-now);total=RpgBalanceConfig.ticks(RpgBalanceConfig.CLOAK_DURATION_SECONDS);}else if(entry.getKey().equals("multislash")&&s.multislashHitsRemaining>0){active=true;shownRemaining=(int)Math.max(1,s.multislashActiveUntil-now);total=RpgBalanceConfig.ticks(RpgBalanceConfig.MULTISLASH_DURATION_SECONDS);}result.add(new AbilityStatus(name(entry.getKey()),shownRemaining,Math.max(1,total),active));}if(s.utilityReady>now)result.add(new AbilityStatus("Dash",(int)(s.utilityReady-now),Math.max(1,s.utilityDuration),false));return result;}
    public static void input(ServerPlayer p,AbilityAction action){
        PlayerProgress progress=progress(p); State s=state(p); long now=p.level.getGameTime();
        if(action==AbilityAction.UTILITY_START){s.blocking=progress.hasSkill(SwordsmanPlayerClass.DUAL);if(progress.hasSkill(SwordsmanPlayerClass.SWORDMASTER))dash(p,progress,s,now);return;}
        if(action==AbilityAction.UTILITY_STOP){s.blocking=false;return;}
        if(action==AbilityAction.OFFHAND_ATTACK){offhand(p,progress,s,now);return;}
        AlternateAttackDefinition attack=AlternateAttackDefinition.find(progress.selectedAlternateAttack());
        if(attack==null||!progress.hasSkill(attack.skillId())||s.cooldowns.getOrDefault(attack.id(),0L)>now)return;
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
            if(holdingSword&&(!s.wasHoldingSword||p.getMainHandItem().getItem()!=s.lastMainItem))s.drawLockedUntil=now+RpgBalanceConfig.ticks(RpgBalanceConfig.HEAVY_DRAW_DELAY_SECONDS);
            if(holdingSword)s.heavyMoveUntil=now+RpgBalanceConfig.ticks(RpgBalanceConfig.HEAVY_MOVE_LINGER_SECONDS);
        }
        s.wasHoldingSword=holdingSword;s.lastMainItem=p.getMainHandItem().getItem();
        if(s.multislashHitsRemaining>0&&now>=s.nextMultislashHit){strike(p,s.multislashHitsRemaining%2==0?p.getMainHandItem():p.getOffhandItem(),1.0);s.multislashHitsRemaining--;s.nextMultislashHit=now+Math.max(1,RpgBalanceConfig.ticks(RpgBalanceConfig.MULTISLASH_DURATION_SECONDS)/4);if(s.multislashHitsRemaining==0){s.mainReady=now+recharge(progress);s.offReady=now+recharge(progress);}}
        if(s.leaping&&p.isOnGround()&&now>s.leapStarted+3){s.leaping=false;double radius=RpgBalanceConfig.LEAP_RADIUS.get();for(LivingEntity target:p.level.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(radius),e->e!=p&&e.isAlive())){hurt(p,target,baseDamage(p));Vec3 away=target.position().subtract(p.position()).normalize().scale(RpgBalanceConfig.LEAP_KNOCKBACK.get());target.push(away.x,0.45,away.z);}}
        if(s.cloakedUntil>0&&now>=s.cloakedUntil){s.cloakedUntil=0;p.removeEffect(MobEffects.INVISIBILITY);}
        if(s.cloakedUntil>now)for(Mob mob:p.level.getEntitiesOfClass(Mob.class,p.getBoundingBox().inflate(32),m->m.getTarget()==p))mob.setTarget(null);
        if(progress.hasSkill(SwordsmanPlayerClass.ROGUE)&&now%20==0)preferOtherTargets(p);
        s.blocking=s.blocking&&progress.hasSkill(SwordsmanPlayerClass.DUAL);
    }
    public static boolean normalAttackBlocked(ServerPlayer p){State s=state(p);long now=p.level.getGameTime();return s.multislashHitsRemaining>0||now<s.drawLockedUntil||(now<s.normalAttackLockedUntil&&!progress(p).hasSkill(SwordsmanPlayerClass.HEAVY_RECOVERY));}
    public static boolean beginNormalSwordAttack(ServerPlayer p){State s=state(p);long now=p.level.getGameTime();PlayerProgress x=progress(p);if(normalAttackBlocked(p)||now<s.mainReady)return false;double modifier=x.hasSkill(SwordsmanPlayerClass.HEAVY)?RpgBalanceConfig.HEAVY_ATTACK_SPEED.get():x.hasSkill(SwordsmanPlayerClass.DUAL_SPEED)?RpgBalanceConfig.DUAL_SPEED_UPGRADE.get():RpgBalanceConfig.DUAL_ATTACK_SPEED.get();s.mainReady=now+Math.max(1,Math.round(12/(1+modifier/100.0)));return true;}
    public static void offensiveAction(ServerPlayer p){State s=state(p);if(s.cloakedUntil>0){s.cloakedUntil=0;p.removeEffect(MobEffects.INVISIBILITY);}}
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
    private static void dashStrike(ServerPlayer p,PlayerProgress x,State s,long now){if(!sword(p.getMainHandItem()))return;offensiveAction(p);Vec3 look=horizontal(p.getLookAngle());p.setDeltaMovement(look.scale(1.5).add(0,0.1,0));p.hurtMarked=true;double bonus=x.hasSkill(SwordsmanPlayerClass.SM_DASH_STRIKE)?RpgBalanceConfig.DASH_STRIKE_UPGRADE_DAMAGE.get():RpgBalanceConfig.DASH_STRIKE_DAMAGE.get();target(p,RpgBalanceConfig.DASH_STRIKE_DISTANCE.get()).ifPresent(t->hurt(p,t,baseDamage(p)*(1+bonus/100)));cooldown(s,"dash_strike",now,RpgBalanceConfig.ticks(RpgBalanceConfig.DASH_STRIKE_COOLDOWN_SECONDS));}
    private static void heavyAttack(ServerPlayer p,PlayerProgress x,State s,long now){if(!sword(p.getMainHandItem()))return;offensiveAction(p);double bonus=x.hasSkill(SwordsmanPlayerClass.HEAVY_ATTACK_DAMAGE)?RpgBalanceConfig.HEAVY_ATTACK_UPGRADE_DAMAGE.get():RpgBalanceConfig.HEAVY_ATTACK_DAMAGE.get();target(p,reach(x)).ifPresent(t->hurt(p,t,baseDamage(p)*(1+bonus/100)));int cd=RpgBalanceConfig.ticks(x.hasSkill(SwordsmanPlayerClass.HEAVY_ATTACK_COOLDOWN)?RpgBalanceConfig.HEAVY_ATTACK_UPGRADE_COOLDOWN_SECONDS:RpgBalanceConfig.HEAVY_ATTACK_COOLDOWN_SECONDS);cooldown(s,"heavy_attack",now,cd);s.normalAttackLockedUntil=now+cd;}
    private static void multislash(ServerPlayer p,PlayerProgress x,State s,long now){if(!sword(p.getMainHandItem())||!sword(p.getOffhandItem())){p.displayClientMessage(Component.literal("Multislash requires a sword in both hands."),true);return;}offensiveAction(p);s.multislashHitsRemaining=4;s.nextMultislashHit=now;s.multislashActiveUntil=now+RpgBalanceConfig.ticks(RpgBalanceConfig.MULTISLASH_DURATION_SECONDS);cooldown(s,"multislash",now,RpgBalanceConfig.ticks(RpgBalanceConfig.MULTISLASH_COOLDOWN_SECONDS));}
    private static void cloak(ServerPlayer p,State s,long now){int duration=RpgBalanceConfig.ticks(RpgBalanceConfig.CLOAK_DURATION_SECONDS);p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,duration,0,false,false,true));s.cloakedUntil=now+duration;cooldown(s,"cloak",now,RpgBalanceConfig.ticks(RpgBalanceConfig.CLOAK_COOLDOWN_SECONDS));for(Mob mob:p.level.getEntitiesOfClass(Mob.class,p.getBoundingBox().inflate(32),m->m.getTarget()==p))mob.setTarget(null);}
    private static void leap(ServerPlayer p,State s,long now){offensiveAction(p);p.setDeltaMovement(p.getDeltaMovement().x,RpgBalanceConfig.LEAP_LAUNCH.get(),p.getDeltaMovement().z);p.hurtMarked=true;s.leaping=true;s.leapStarted=now;cooldown(s,"leap_strike",now,RpgBalanceConfig.ticks(RpgBalanceConfig.LEAP_COOLDOWN_SECONDS));}
    private static void dash(ServerPlayer p,PlayerProgress x,State s,long now){if(now<s.utilityReady)return;boolean upgraded=x.hasSkill(SwordsmanPlayerClass.SM_DASH);Vec3 look=horizontal(p.getLookAngle());p.setDeltaMovement(look.scale(upgraded?RpgBalanceConfig.DASH_UPGRADE_VELOCITY.get():RpgBalanceConfig.DASH_VELOCITY.get()).add(0,0.08,0));p.hurtMarked=true;s.utilityDuration=RpgBalanceConfig.ticks(upgraded?RpgBalanceConfig.DASH_UPGRADE_COOLDOWN_SECONDS:RpgBalanceConfig.DASH_COOLDOWN_SECONDS);s.utilityReady=now+s.utilityDuration;}
    private static void offhand(ServerPlayer p,PlayerProgress x,State s,long now){if(!x.hasSkill(SwordsmanPlayerClass.DUAL)||s.multislashHitsRemaining>0||!sword(p.getMainHandItem())||!sword(p.getOffhandItem())||now<s.offReady)return;offensiveAction(p);target(p,3.2).ifPresent(t->hurt(p,t,baseDamage(p)));s.offReady=now+recharge(x);}
    private static void strike(ServerPlayer p,ItemStack sword,double multiplier){if(!sword(sword))return;target(p,3.5).ifPresent(t->hurt(p,t,baseDamage(p)*multiplier));}
    private static void hurt(ServerPlayer p,LivingEntity target,double amount){INTERNAL_DAMAGE.add(p.getUUID());try{target.hurt(DamageSource.playerAttack(p),(float)amount);}finally{INTERNAL_DAMAGE.remove(p.getUUID());}}
    private static Optional<LivingEntity> target(ServerPlayer p,double distance){Vec3 start=p.getEyePosition(),end=start.add(p.getLookAngle().scale(distance));AABB box=p.getBoundingBox().expandTowards(p.getLookAngle().scale(distance)).inflate(1);return p.level.getEntitiesOfClass(LivingEntity.class,box,e->e!=p&&e.isAlive()).stream().filter(e->e.getBoundingBox().inflate(.3).clip(start,end).isPresent()).min(Comparator.comparingDouble(p::distanceToSqr));}
    private static double baseDamage(ServerPlayer p){return p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);}
    private static double reach(PlayerProgress x){return 3.5+(x.hasSkill(SwordsmanPlayerClass.HEAVY_RANGE)?RpgBalanceConfig.HEAVY_RANGE.get():0);}
    private static long recharge(PlayerProgress x){double speed=x.hasSkill(SwordsmanPlayerClass.DUAL_SPEED)?RpgBalanceConfig.DUAL_SPEED_UPGRADE.get():RpgBalanceConfig.DUAL_ATTACK_SPEED.get();return Math.max(1,Math.round(12/(1+speed/100.0)));}
    private static boolean sword(ItemStack s){return s.getItem() instanceof SwordItem;}
    private static Vec3 horizontal(Vec3 v){Vec3 h=new Vec3(v.x,0,v.z);return h.lengthSqr()==0?Vec3.ZERO:h.normalize();}
    private static PlayerProgress progress(ServerPlayer p){return PlayerProgressSavedData.get(p.getServer()).get(p.getUUID());}
    private static void cooldown(State s,String id,long now,int duration){s.cooldowns.put(id,now+duration);s.cooldownDurations.put(id,duration);}
    private static String name(String id){AlternateAttackDefinition attack=AlternateAttackDefinition.find(id);return attack==null?id:attack.displayName();}
    private static void preferOtherTargets(ServerPlayer rogue){double radius=RpgBalanceConfig.ROGUE_RETARGET_RADIUS.get();for(Mob mob:rogue.level.getEntitiesOfClass(Mob.class,rogue.getBoundingBox().inflate(radius),m->m.getTarget()==rogue)){ServerPlayer alternative=rogue.getServer().getPlayerList().getPlayers().stream().filter(p->p!=rogue&&p.level==rogue.level&&p.distanceToSqr(mob)<radius*radius).min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);if(alternative!=null)mob.setTarget(alternative);}}
    public static final class State{private final Map<String,Long> cooldowns=new HashMap<>();private final Map<String,Integer> cooldownDurations=new HashMap<>();private final Map<UUID,Long> lastHit=new HashMap<>();private long utilityReady,normalAttackLockedUntil,drawLockedUntil,mainReady,offReady,nextMultislashHit,cloakedUntil,leapStarted,heavyMoveUntil,multislashActiveUntil;private int utilityDuration,multislashHitsRemaining;private boolean blocking,leaping,wasHoldingSword;private net.minecraft.world.item.Item lastMainItem;public boolean blocking(){return blocking;}public long parryReady;}
}
