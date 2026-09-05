package com.ollie.tierborne.combat;

import com.ollie.tierborne.config.RpgBalanceConfig;
import com.ollie.tierborne.data.PlayerProgress;
import com.ollie.tierborne.data.PlayerProgressSavedData;
import com.ollie.tierborne.playerclass.FighterPlayerClass;
import com.ollie.tierborne.playerclass.FighterStats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

import java.util.*;

public final class FighterCombat {
    private static final Map<UUID,State> STATES=new HashMap<>();
    private static final Set<UUID> REACTIVE_DAMAGE=new HashSet<>();
    private FighterCombat(){}
    private static State state(ServerPlayer p){return STATES.computeIfAbsent(p.getUUID(),u->new State());}
    private static PlayerProgress progress(ServerPlayer p){return PlayerProgressSavedData.get(p.getServer()).get(p.getUUID());}
    public static boolean reactive(ServerPlayer p){return REACTIVE_DAMAGE.contains(p.getUUID());}
    public static int comboBonus(ServerPlayer p){State s=state(p);return s.comboTarget!=null&&s.comboUntil>p.level.getGameTime()?Math.max(0,s.nextTotalPercent-100):0;}
    public static List<AbilityStatus> statuses(ServerPlayer p){State s=state(p);long now=p.level.getGameTime();PlayerProgress x=progress(p);List<AbilityStatus> out=new ArrayList<>();for(var e:s.cooldowns.entrySet()){long left=e.getValue()-now;if(left>0&&!("uppercut".equals(e.getKey())&&s.uppercutEndsAt>now))out.add(new AbilityStatus(name(e.getKey()),(int)left,s.cooldownDurations.getOrDefault(e.getKey(),(int)left),false,"COOLDOWN"));}if(s.pullUntil>now)out.add(new AbilityStatus("Pull",(int)(s.pullUntil-now),RpgBalanceConfig.ticks(RpgBalanceConfig.PULL_MAX_SECONDS),true,"ACTIVE"));if(s.uppercutEndsAt>now)out.add(new AbilityStatus("Uppercut",(int)(s.uppercutEndsAt-now),uppercutAnimationTicks(),true,s.uppercutHitLanded?"RECOVERY":"WINDUP"));if(s.chainReadyUntil>now)out.add(new AbilityStatus("Chain",(int)(s.chainReadyUntil-now),RpgBalanceConfig.ticks(RpgBalanceConfig.CHAIN_READY_SECONDS),true,"READY"));if(s.chainActive&&s.comboUntil>now)out.add(new AbilityStatus("Chain",(int)(s.comboUntil-now),chainWindowTicks(x),true,"ACTIVE"));else if(x.hasSkill(FighterPlayerClass.CHAMPION)&&s.comboTarget!=null&&s.comboUntil>now)out.add(new AbilityStatus("Combo",(int)(s.comboUntil-now),comboWindowTicks(x),true,"ACTIVE"));if(s.disarmUntil>now)out.add(new AbilityStatus("Disarm",(int)(s.disarmUntil-now),disarmDuration(x),true,"ACTIVE"));return out;}
    public static boolean input(ServerPlayer p,String id){if(CombatControl.offenseDisabled(p))return true;State s=state(p);long now=p.level.getGameTime();if(s.cooldowns.getOrDefault(id,0L)>now){if("uppercut".equals(id))p.displayClientMessage(net.minecraft.network.chat.Component.literal("Uppercut is on cooldown").withStyle(net.minecraft.ChatFormatting.RED),true);return true;}return switch(id){case "pull"->{startPull(p,s,now);yield true;}case "uppercut"->{startUppercut(p,s,now);yield true;}case "chain"->{s.chainReadyUntil=now+RpgBalanceConfig.ticks(RpgBalanceConfig.CHAIN_READY_SECONDS);s.chainActive=false;clearCombo(s);yield true;}case "disarm"->{startDisarm(p,s,now);yield true;}default->false;};}
    public static void tick(ServerPlayer p){State s=state(p);long now=p.level.getGameTime();if(s.pullUntil>0){LivingEntity target=entity(p,s.pullTarget);if(!FighterStats.isFist(p)||target==null||!target.isAlive()||now>=s.pullUntil)endPull(p,s,now);else if(target.distanceTo(p)<=2.5&&lineClear(p,target)){p.swing(InteractionHand.MAIN_HAND,true);dealFullAttack(p,target,false,0);endPull(p,s,now);}else{Vec3 toward=p.position().add(0,1,0).subtract(target.position()).normalize().scale(RpgBalanceConfig.PULL_STRENGTH.get());target.setDeltaMovement(toward);target.hurtMarked=true;}}tickUppercut(p,s,now);if(s.chainReadyUntil>0&&now>=s.chainReadyUntil){s.chainReadyUntil=0;cooldown(s,"chain",now,RpgBalanceConfig.ticks(RpgBalanceConfig.CHAIN_COOLDOWN_SECONDS));}if(s.chainActive&&now>=s.comboUntil)endChain(s,now);else if(!s.chainActive&&s.comboTarget!=null&&now>=s.comboUntil)clearCombo(s);if(s.disarmUntil>0&&now>=s.disarmUntil){s.disarmUntil=0;cooldown(s,"disarm",now,disarmCooldown(progress(p)));}}
    public static float modifyIntentionalHit(ServerPlayer p,LivingEntity target,float amount){PlayerProgress x=progress(p);double multiplier=1+(FighterStats.generalDamage(x)+(FighterStats.isFist(p)?FighterStats.fistDamage(x):0))/100.0;if(REACTIVE_DAMAGE.contains(p.getUUID()))return (float)(amount*multiplier);State s=state(p);long now=p.level.getGameTime();if(s.chainReadyUntil>now){s.chainReadyUntil=0;s.chainActive=true;s.comboTarget=target.getUUID();s.nextTotalPercent=(int)Math.round(100+chainOpening(x));multiplier*=s.nextTotalPercent/100.0;s.nextTotalPercent=compound(s.nextTotalPercent);s.comboUntil=now+chainWindowTicks(x);}else if(s.chainActive){if(!target.getUUID().equals(s.comboTarget)){endChain(s,now);}else{multiplier*=s.nextTotalPercent/100.0;s.nextTotalPercent=compound(s.nextTotalPercent);s.comboUntil=now+chainWindowTicks(x);}}else if(x.hasSkill(FighterPlayerClass.CHAMPION)){if(!target.getUUID().equals(s.comboTarget)||now>=s.comboUntil){s.comboTarget=target.getUUID();s.nextTotalPercent=compound(100);}else{multiplier*=s.nextTotalPercent/100.0;s.nextTotalPercent=compound(s.nextTotalPercent);}s.comboUntil=now+comboWindowTicks(x);}return (float)(amount*multiplier);}
    public static boolean tryCounter(ServerPlayer defender,LivingEntity attacker,float rawDamage){PlayerProgress x=progress(defender);if(!x.hasSkill(FighterPlayerClass.DUELIST)||REACTIVE_DAMAGE.contains(defender.getUUID())||attacker instanceof ServerPlayer player&&REACTIVE_DAMAGE.contains(player.getUUID())||attacker.distanceTo(defender)>defender.getAttributeValue(net.minecraftforge.common.ForgeMod.REACH_DISTANCE.get()))return false;double chance=x.hasSkill(FighterPlayerClass.COUNTER_CHANCE)?RpgBalanceConfig.DUELIST_UPGRADED_COUNTER_CHANCE.get():RpgBalanceConfig.DUELIST_COUNTER_CHANCE.get();if(defender.getRandom().nextDouble()*100>=chance)return false;defender.swing(InteractionHand.MAIN_HAND,true);dealFullAttack(defender,attacker,true,x.hasSkill(FighterPlayerClass.REFLECTIVE_COUNTER)?rawDamage:0);return x.hasSkill(FighterPlayerClass.PERFECT_COUNTER);}
    public static boolean offenseDisabled(Entity entity){return CombatControl.offenseDisabled(entity);}
    public static boolean uppercutActive(ServerPlayer p){return state(p).uppercutEndsAt>p.level.getGameTime();}
    public static void reset(ServerPlayer p){State s=STATES.remove(p.getUUID());if(s!=null&&s.disarmTarget!=null&&p.level instanceof ServerLevel level){Entity target=level.getEntity(s.disarmTarget);if(target!=null)CombatControl.clear(target);}CombatControl.clear(p);REACTIVE_DAMAGE.remove(p.getUUID());}
    private static void startPull(ServerPlayer p,State s,long now){if(!FighterStats.isFist(p)){p.displayClientMessage(net.minecraft.network.chat.Component.literal("Both hands must be empty to use Pull").withStyle(net.minecraft.ChatFormatting.RED),true);return;}double range=progress(p).hasSkill(FighterPlayerClass.EXTENDED_PULL)?RpgBalanceConfig.PULL_UPGRADED_RANGE.get():RpgBalanceConfig.PULL_RANGE.get();LivingEntity target=target(p,range);if(target==null)return;s.pullTarget=target.getUUID();s.pullUntil=now+RpgBalanceConfig.ticks(RpgBalanceConfig.PULL_MAX_SECONDS);}
    private static void endPull(ServerPlayer p,State s,long now){if(s.pullUntil<=0)return;s.pullUntil=0;s.pullTarget=null;int duration=RpgBalanceConfig.ticks(progress(p).hasSkill(FighterPlayerClass.PULL_RECOVERY)?RpgBalanceConfig.PULL_UPGRADED_COOLDOWN_SECONDS:RpgBalanceConfig.PULL_COOLDOWN_SECONDS);cooldown(s,"pull",now,duration);}
    private static void startUppercut(ServerPlayer p,State s,long now){
        if(!FighterStats.isFist(p)){p.displayClientMessage(net.minecraft.network.chat.Component.literal("Both hands must be empty to use Uppercut").withStyle(net.minecraft.ChatFormatting.RED),true);return;}
        LivingEntity victim=target(p,RpgBalanceConfig.UPPERCUT_RANGE.get());
        if(victim==null){p.displayClientMessage(net.minecraft.network.chat.Component.literal("Uppercut missed: no target in reach").withStyle(net.minecraft.ChatFormatting.RED),true);return;}
        int duration=uppercutAnimationTicks();
        s.uppercutTarget=victim.getUUID();
        s.uppercutHitAt=now+Math.max(1,RpgBalanceConfig.ticks(RpgBalanceConfig.UPPERCUT_WINDUP_SECONDS));
        s.uppercutEndsAt=now+duration;
        s.uppercutHitLanded=false;
        cooldown(s,"uppercut",now,RpgBalanceConfig.ticks(RpgBalanceConfig.UPPERCUT_COOLDOWN_SECONDS));
        com.ollie.tierborne.network.ModNetwork.syncUppercut(p,duration);
    }
    private static void tickUppercut(ServerPlayer p,State s,long now){
        if(s.uppercutEndsAt<=0)return;
        if(!s.uppercutHitLanded&&now>=s.uppercutHitAt){
            s.uppercutHitLanded=true;
            LivingEntity victim=entity(p,s.uppercutTarget);
            if(victim==null||!victim.isAlive()||victim.distanceTo(p)>RpgBalanceConfig.UPPERCUT_RANGE.get()+1.0||!lineClear(p,victim)){
                p.displayClientMessage(net.minecraft.network.chat.Component.literal("Uppercut missed").withStyle(net.minecraft.ChatFormatting.RED),true);
            }else{
                float damage=(float)(p.getAttributeValue(Attributes.ATTACK_DAMAGE)*(1.0+RpgBalanceConfig.UPPERCUT_DAMAGE_PERCENT.get()/100.0));
                victim.invulnerableTime=0;
                if(victim.hurt(DamageSource.playerAttack(p),damage)){
                    Vec3 away=victim.position().subtract(p.position());
                    away=new Vec3(away.x,0,away.z);
                    if(away.lengthSqr()<0.001)away=new Vec3(-net.minecraft.util.Mth.sin(p.getYRot()*(float)Math.PI/180.0F),0,net.minecraft.util.Mth.cos(p.getYRot()*(float)Math.PI/180.0F));
                    away=away.normalize().scale(RpgBalanceConfig.UPPERCUT_HORIZONTAL_KNOCKBACK.get());
                    victim.setDeltaMovement(victim.getDeltaMovement().x+away.x,Math.max(victim.getDeltaMovement().y,RpgBalanceConfig.UPPERCUT_VERTICAL_KNOCKBACK.get()),victim.getDeltaMovement().z+away.z);
                    victim.hurtMarked=true;
                    if(p.level instanceof ServerLevel level)level.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,victim.getX(),victim.getY()+victim.getBbHeight()*0.55,victim.getZ(),1,0,0,0,0);
                    p.level.playSound(null,p.getX(),p.getY(),p.getZ(),net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_STRONG,net.minecraft.sounds.SoundSource.PLAYERS,1.0F,0.82F);
                    p.displayClientMessage(net.minecraft.network.chat.Component.literal("Uppercut landed").withStyle(net.minecraft.ChatFormatting.GREEN),true);
                }else p.displayClientMessage(net.minecraft.network.chat.Component.literal("Uppercut was blocked").withStyle(net.minecraft.ChatFormatting.RED),true);
            }
        }
        if(now>=s.uppercutEndsAt){s.uppercutEndsAt=0;s.uppercutTarget=null;}
    }
    private static void startDisarm(ServerPlayer p,State s,long now){LivingEntity target=target(p,RpgBalanceConfig.DISARM_RANGE.get());if(target==null||(!(target instanceof net.minecraft.world.entity.player.Player)&&!(target instanceof net.minecraft.world.entity.monster.Enemy)))return;if(target.getHealth()>=p.getHealth()*RpgBalanceConfig.DISARM_HEALTH_RATIO.get()){p.displayClientMessage(net.minecraft.network.chat.Component.literal("Target has too much health to disarm").withStyle(net.minecraft.ChatFormatting.RED),true);return;}int duration=disarmDuration(progress(p));CombatControl.disableOffense(target,now+duration);s.disarmTarget=target.getUUID();s.disarmUntil=now+duration;p.displayClientMessage(net.minecraft.network.chat.Component.literal("Disarmed opponent").withStyle(net.minecraft.ChatFormatting.GREEN),true);}
    private static void dealFullAttack(ServerPlayer p,LivingEntity target,boolean reactive,float minimum){float damage=(float)Math.max(minimum,p.getAttributeValue(Attributes.ATTACK_DAMAGE));if(reactive)REACTIVE_DAMAGE.add(p.getUUID());try{target.invulnerableTime=0;target.hurt(DamageSource.playerAttack(p),damage);if(reactive&&!p.getMainHandItem().isEmpty())p.getMainHandItem().hurtAndBreak(1,p,u->u.broadcastBreakEvent(InteractionHand.MAIN_HAND));}finally{if(reactive)REACTIVE_DAMAGE.remove(p.getUUID());}}
    private static LivingEntity target(ServerPlayer p,double range){Vec3 start=p.getEyePosition(),end=start.add(p.getLookAngle().scale(range));HitResult block=p.level.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));if(block.getType()!=HitResult.Type.MISS)end=block.getLocation();Vec3 finalEnd=end;return p.level.getEntitiesOfClass(LivingEntity.class,new AABB(start,end).inflate(1),e->e!=p&&e.isAlive()&&e.getBoundingBox().inflate(.3).clip(start,finalEnd).isPresent()).stream().min(Comparator.comparingDouble(p::distanceToSqr)).orElse(null);}
    private static boolean lineClear(ServerPlayer p,LivingEntity target){return p.level.clip(new ClipContext(p.getEyePosition(),target.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getType()==HitResult.Type.MISS;}
    private static LivingEntity entity(ServerPlayer p,UUID id){return id!=null&&p.level instanceof ServerLevel level&&level.getEntity(id) instanceof LivingEntity living?living:null;}
    private static int compound(int total){return (int)Math.ceil(total*(1+RpgBalanceConfig.COMBO_COMPOUND_PERCENT.get()/100.0));}
    private static int comboWindowTicks(PlayerProgress x){return RpgBalanceConfig.ticks(x.hasSkill(FighterPlayerClass.COMBO_WINDOW_II)?RpgBalanceConfig.COMBO_WINDOW_II_SECONDS:x.hasSkill(FighterPlayerClass.COMBO_WINDOW_I)?RpgBalanceConfig.COMBO_WINDOW_I_SECONDS:RpgBalanceConfig.COMBO_WINDOW_SECONDS);}
    private static int chainWindowTicks(PlayerProgress x){return RpgBalanceConfig.ticks(x.hasSkill(FighterPlayerClass.CHAIN_WINDOW)?RpgBalanceConfig.CHAIN_UPGRADED_WINDOW_SECONDS:RpgBalanceConfig.CHAIN_WINDOW_SECONDS);}
    private static double chainOpening(PlayerProgress x){return x.hasSkill(FighterPlayerClass.CHAIN_BONUS)?RpgBalanceConfig.CHAIN_UPGRADED_OPENING_PERCENT.get():RpgBalanceConfig.CHAIN_OPENING_PERCENT.get();}
    private static int disarmDuration(PlayerProgress x){return RpgBalanceConfig.ticks(x.hasSkill(FighterPlayerClass.DISARM_DURATION)?RpgBalanceConfig.DISARM_UPGRADED_DURATION_SECONDS:RpgBalanceConfig.DISARM_DURATION_SECONDS);}
    private static int disarmCooldown(PlayerProgress x){return RpgBalanceConfig.ticks(x.hasSkill(FighterPlayerClass.DISARM_COOLDOWN)?RpgBalanceConfig.DISARM_UPGRADED_COOLDOWN_SECONDS:RpgBalanceConfig.DISARM_COOLDOWN_SECONDS);}
    private static void endChain(State s,long now){s.chainActive=false;clearCombo(s);cooldown(s,"chain",now,RpgBalanceConfig.ticks(RpgBalanceConfig.CHAIN_COOLDOWN_SECONDS));}
    private static void clearCombo(State s){s.comboTarget=null;s.comboUntil=0;s.nextTotalPercent=100;}
    private static void cooldown(State s,String id,long now,int ticks){s.cooldowns.put(id,now+ticks);s.cooldownDurations.put(id,ticks);}
    private static int uppercutAnimationTicks(){return Math.max(1,RpgBalanceConfig.ticks(RpgBalanceConfig.UPPERCUT_ANIMATION_SECONDS));}
    private static String name(String id){return switch(id){case "pull"->"Pull";case "uppercut"->"Uppercut";case "chain"->"Chain";case "disarm"->"Disarm";default->id;};}
    private static final class State{final Map<String,Long> cooldowns=new HashMap<>();final Map<String,Integer> cooldownDurations=new HashMap<>();UUID pullTarget,comboTarget,disarmTarget,uppercutTarget;long pullUntil,chainReadyUntil,comboUntil,disarmUntil,uppercutHitAt,uppercutEndsAt;int nextTotalPercent=100;boolean chainActive,uppercutHitLanded;}
}
