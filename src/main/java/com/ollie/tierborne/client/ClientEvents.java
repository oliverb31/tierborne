package com.ollie.tierborne.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Vector3f;
import com.ollie.tierborne.Tierborne;
import com.ollie.tierborne.client.screen.SkillTreeScreen;
import com.ollie.tierborne.client.screen.PlayerMenuScreen;
import com.ollie.tierborne.client.screen.RpgUi;
import com.ollie.tierborne.playerclass.SkillBonusType;
import com.ollie.tierborne.playerclass.GeneralSkillRules;
import com.ollie.tierborne.network.ModNetwork;
import com.ollie.tierborne.network.AbilityActionPacket;
import com.ollie.tierborne.combat.AbilityAction;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.ollie.tierborne.playerclass.SwordsmanPlayerClass;
import com.ollie.tierborne.config.RpgBalanceConfig;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Tierborne.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    private static boolean utilityHeld;
    private static boolean alternateHeld;
    private static boolean offhandUseHeld;
    private static boolean homingFollowerHeld;
    private static final int COOLDOWN_BAR_WIDTH=92;
    private static final int COOLDOWN_ENTRY_HEIGHT=34;
    private static final int COOLDOWN_ENTRY_GAP=5;
    private static final KeyMapping OPEN_SKILLS = new KeyMapping(
            "key.tierborne.open_skills",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "key.categories.tierborne");
    private static final KeyMapping OPEN_PLAYER_MENU = new KeyMapping(
            "key.tierborne.open_player_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            "key.categories.tierborne");
    private static final KeyMapping ALTERNATE_ATTACK = new KeyMapping("key.tierborne.alternate_attack", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, "key.categories.tierborne");
    private static final KeyMapping SUBCLASS_UTILITY = new KeyMapping("key.tierborne.subclass_utility", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, "key.categories.tierborne");

    private ClientEvents() {}

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (ClientProgress.playerClassId().isEmpty()) return;
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        Minecraft minecraft = Minecraft.getInstance();
        boolean inRpgMenu = minecraft.screen instanceof PlayerMenuScreen
                || minecraft.screen instanceof SkillTreeScreen;

        if (OPEN_PLAYER_MENU.matches(event.getKey(), event.getScanCode())) {
            if (inRpgMenu) minecraft.setScreen(null);
            else if (minecraft.screen == null) minecraft.setScreen(new PlayerMenuScreen());
            return;
        }

        if (!OPEN_SKILLS.matches(event.getKey(), event.getScanCode())) return;
        if (minecraft.screen instanceof SkillTreeScreen skillTree && !skillTree.isGeneralTree()) {
            minecraft.setScreen(null);
        } else if (inRpgMenu || minecraft.screen == null) {
            minecraft.setScreen(new SkillTreeScreen(false));
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ClientProgress.tryOpenSelectionScreen();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {offhandUseHeld=false;alternateHeld=false;homingFollowerHeld=false;return;}
        if(minecraft.screen!=null){if(alternateHeld){alternateHeld=false;ModNetwork.CHANNEL.sendToServer(new AbilityActionPacket(AbilityAction.ALTERNATE_RELEASE));}offhandUseHeld=false;homingFollowerHeld=false;return;}
        boolean alternateDown=ALTERNATE_ATTACK.isDown();
        if(alternateDown!=alternateHeld){alternateHeld=alternateDown;ModNetwork.CHANNEL.sendToServer(new AbilityActionPacket(alternateDown?AbilityAction.ALTERNATE_ATTACK:AbilityAction.ALTERNATE_RELEASE));}
        boolean down = SUBCLASS_UTILITY.isDown();
        if (down != utilityHeld) {
            utilityHeld = down;
            ModNetwork.CHANNEL.sendToServer(new AbilityActionPacket(down ? AbilityAction.UTILITY_START : AbilityAction.UTILITY_STOP));
        }
        boolean dualWielding=ClientProgress.hasSkill(SwordsmanPlayerClass.DUAL)
                &&minecraft.player.getMainHandItem().getItem() instanceof SwordItem
                &&minecraft.player.getOffhandItem().getItem() instanceof SwordItem;
        boolean useDown=minecraft.options.keyUse.isDown();
        if(dualWielding&&useDown&&!offhandUseHeld)ModNetwork.CHANNEL.sendToServer(new AbilityActionPacket(AbilityAction.OFFHAND_ATTACK));
        offhandUseHeld=useDown;
        if(ClientAbilityState.isHoming()){
            boolean followerDown=minecraft.options.keyPickItem.isDown();
            if(followerDown&&!homingFollowerHeld)ModNetwork.CHANNEL.sendToServer(new AbilityActionPacket(AbilityAction.HOMING_ADD_FOLLOWER));
            homingFollowerHeld=followerDown;
        }else homingFollowerHeld=false;
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft=Minecraft.getInstance();
        if(minecraft.player!=null&&ClientAbilityState.isHoming()&&(event.isAttack()||event.isUseItem())){
            AbilityAction action=event.isAttack()?AbilityAction.HOMING_PUSH:AbilityAction.HOMING_PULL;
            ModNetwork.CHANNEL.sendToServer(new AbilityActionPacket(action));
            event.setSwingHand(false);event.setCanceled(true);return;
        }
        if(event.isAttack()&&minecraft.player!=null&&minecraft.player.getMainHandItem().getItem() instanceof SwordItem
                &&ClientAbilityState.blocksNormalAttack()){event.setSwingHand(false);event.setCanceled(true);return;}
        if(!event.isUseItem()||minecraft.player==null||!ClientProgress.hasSkill(SwordsmanPlayerClass.DUAL)
                ||!(minecraft.player.getMainHandItem().getItem() instanceof SwordItem)
                ||!(minecraft.player.getOffhandItem().getItem() instanceof SwordItem)) return;
        event.setCanceled(true);
        event.setSwingHand(false);
    }

    @SubscribeEvent
    public static void onRenderHud(RenderGuiOverlayEvent.Post event) {
        if(event.getOverlay()!=VanillaGuiOverlay.HOTBAR.type())return;
        Minecraft minecraft=Minecraft.getInstance();
        if(minecraft.player==null||minecraft.options.hideGui)return;
        renderTargetHealth(event,minecraft);
        java.util.List<com.ollie.tierborne.combat.AbilityStatus> statuses=ClientAbilityState.statuses();
        int width=COOLDOWN_BAR_WIDTH,x=event.getWindow().getGuiScaledWidth()-width-8;
        int y=event.getWindow().getGuiScaledHeight()-58-statuses.size()*(COOLDOWN_ENTRY_HEIGHT+COOLDOWN_ENTRY_GAP);
        for(com.ollie.tierborne.combat.AbilityStatus status:statuses){
            String title=status.name()+" - "+status.stateLabel();
            minecraft.font.drawShadow(event.getPoseStack(),title,x+(width-minecraft.font.width(title))/2.0F,y,0xFFE9E2D0);
            int barY=y+13;GuiComponent.fill(event.getPoseStack(),x,barY,x+width,barY+7,0xFF71572D);GuiComponent.fill(event.getPoseStack(),x+1,barY+1,x+width-1,barY+6,0xD0101218);
            int fill=(int)Math.round((width-2)*Math.min(1.0,status.remainingTicks()/(double)Math.max(1,status.totalTicks())));
            GuiComponent.fill(event.getPoseStack(),x+1,barY+1,x+1+fill,barY+6,status.active()?0xFF4EA56B:0xFFD7AD55);
            String timer=status.stateLabel().equals("CHARGING")?Math.round(100.0*status.remainingTicks()/Math.max(1,status.totalTicks()))+"%":String.format(java.util.Locale.ROOT,"%.1fs",status.remainingTicks()/20.0);minecraft.font.drawShadow(event.getPoseStack(),timer,x+(width-minecraft.font.width(timer))/2.0F,barY+10,0xFF9B968A);y+=COOLDOWN_ENTRY_HEIGHT+COOLDOWN_ENTRY_GAP;
        }
    }

    private static void renderTargetHealth(RenderGuiOverlayEvent.Post event,Minecraft minecraft){
        if(minecraft.screen!=null)return;
        LivingEntity target=crosshairTarget(minecraft,8.0);
        if(target==null||!target.isAlive())return;
        int centerX=event.getWindow().getGuiScaledWidth()/2;
        int width=Math.min(190,Math.max(120,event.getWindow().getGuiScaledWidth()-32));
        int left=centerX-width/2;
        int top=40;
        int right=left+width;
        RpgUi.panel(event.getPoseStack(),left-5,top-5,right+5,top+35);
        GuiComponent.fill(event.getPoseStack(),left,top+12,right,top+22,RpgUi.GOLD_DARK);
        GuiComponent.fill(event.getPoseStack(),left+1,top+13,right-1,top+21,0xFF35191C);
        float ratio=Mth.clamp(target.getHealth()/Math.max(0.001F,target.getMaxHealth()),0.0F,1.0F);
        int fillRight=left+1+Math.round((width-2)*ratio);
        GuiComponent.fill(event.getPoseStack(),left+1,top+13,fillRight,top+21,0xFF9E343C);
        if(ratio>0.55F)GuiComponent.fill(event.getPoseStack(),left+1,top+13,fillRight,top+14,0xFFD15A57);
        GuiComponent.drawCenteredString(event.getPoseStack(),minecraft.font,target.getDisplayName(),centerX,top,RpgUi.TEXT);
        String health=formatHealth(target.getHealth())+" / "+formatHealth(target.getMaxHealth());
        GuiComponent.drawCenteredString(event.getPoseStack(),minecraft.font,Component.literal(health),centerX,top+25,RpgUi.GOLD);
    }

    private static LivingEntity crosshairTarget(Minecraft minecraft,double range){
        Entity camera=minecraft.getCameraEntity();
        if(camera==null||minecraft.level==null)return null;
        Vec3 start=camera.getEyePosition(1.0F);
        Vec3 end=start.add(camera.getViewVector(1.0F).scale(range));
        HitResult blockHit=minecraft.level.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,camera));
        if(blockHit.getType()!=HitResult.Type.MISS)end=blockHit.getLocation();
        Vec3 ray=end.subtract(start);
        AABB search=camera.getBoundingBox().expandTowards(ray).inflate(1.0);
        EntityHitResult hit=ProjectileUtil.getEntityHitResult(camera,start,end,search,
                entity->entity instanceof LivingEntity living&&living.isAlive()&&!entity.isSpectator()&&entity.isPickable(),ray.lengthSqr());
        return hit!=null&&hit.getEntity() instanceof LivingEntity living?living:null;
    }

    private static String formatHealth(float value){
        float rounded=Math.round(value);
        if(Math.abs(value-rounded)<0.05F)return Integer.toString((int)rounded);
        return String.format(java.util.Locale.ROOT,"%.1f",value);
    }

    @SubscribeEvent
    public static void onRenderCrosshair(RenderGuiOverlayEvent.Pre event) {
        if(event.getOverlay()!=VanillaGuiOverlay.CROSSHAIR.type()
                ||(!ClientAbilityState.dualWielding()&&!ClientAbilityState.multislashActive()))return;
        Minecraft minecraft=Minecraft.getInstance();
        if(minecraft.player==null||minecraft.options.hideGui)return;
        event.setCanceled(true);
        int centerX=event.getWindow().getGuiScaledWidth()/2;
        int centerY=event.getWindow().getGuiScaledHeight()/2;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0,GuiComponent.GUI_ICONS_LOCATION);
        GuiComponent.blit(event.getPoseStack(),centerX-7,centerY-7,0,0,15,15,256,256);
        if(ClientAbilityState.dualWielding()&&!ClientAbilityState.multislashActive()){
            drawChargeBar(event,centerX-18,centerY-8,ClientAbilityState.offhandCharge(),0xFF74A7E8);
            drawChargeBar(event,centerX+14,centerY-8,ClientAbilityState.mainCharge(),0xFFE8B85E);
        }
        RenderSystem.disableBlend();
    }

    private static void drawChargeBar(RenderGuiOverlayEvent event,int x,int y,float charge,int color){
        int height=16;
        GuiComponent.fill(event.getPoseStack(),x,y,x+4,y+height,0xB0000000);
        GuiComponent.fill(event.getPoseStack(),x+1,y+1,x+3,y+height-1,0xFF343434);
        int fill=Math.round((height-2)*Mth.clamp(charge,0.0F,1.0F));
        GuiComponent.fill(event.getPoseStack(),x+1,y+height-1-fill,x+3,y+height-1,color);
    }

    @SubscribeEvent
    public static void onRenderCloakedPlayer(RenderPlayerEvent.Pre event) {
        if(ClientCloakState.isCloaked(event.getEntity().getId())){event.setCanceled(true);return;}
        if(ClientBlockState.isBlocking(event.getEntity().getId())){
            event.getRenderer().getModel().rightArmPose=event.getEntity().getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND).getItem() instanceof SwordItem&&event.getEntity().getMainArm()==net.minecraft.world.entity.HumanoidArm.RIGHT||event.getEntity().getItemBySlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND).getItem() instanceof SwordItem&&event.getEntity().getMainArm()!=net.minecraft.world.entity.HumanoidArm.RIGHT?HumanoidModel.ArmPose.BLOCK:HumanoidModel.ArmPose.EMPTY;
            event.getRenderer().getModel().leftArmPose=event.getEntity().getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND).getItem() instanceof SwordItem&&event.getEntity().getMainArm()==net.minecraft.world.entity.HumanoidArm.LEFT||event.getEntity().getItemBySlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND).getItem() instanceof SwordItem&&event.getEntity().getMainArm()!=net.minecraft.world.entity.HumanoidArm.LEFT?HumanoidModel.ArmPose.BLOCK:HumanoidModel.ArmPose.EMPTY;
        }
    }

    @SubscribeEvent
    public static void onRenderCloakedHand(RenderHandEvent event) {
        Minecraft minecraft=Minecraft.getInstance();
        if(minecraft.player!=null&&ClientCloakState.isCloaked(minecraft.player.getId())){event.setCanceled(true);return;}
        if(minecraft.player!=null&&ClientBlockState.isBlocking(minecraft.player.getId())&&event.getItemStack().getItem() instanceof SwordItem){
            boolean right=event.getHand()==InteractionHand.MAIN_HAND
                    ?minecraft.player.getMainArm()==net.minecraft.world.entity.HumanoidArm.RIGHT
                    :minecraft.player.getMainArm()!=net.minecraft.world.entity.HumanoidArm.RIGHT;
            if(event.getHand()==InteractionHand.MAIN_HAND){
                event.getPoseStack().translate(right?0.10:-0.10,0.08,-0.08);
                event.getPoseStack().mulPose(Vector3f.XP.rotationDegrees(-24.0F));
                event.getPoseStack().mulPose(Vector3f.YP.rotationDegrees(right?14.0F:-14.0F));
                event.getPoseStack().mulPose(Vector3f.ZP.rotationDegrees(right?-18.0F:18.0F));
            }else{
                event.getPoseStack().translate(right?-0.18:0.18,0.16,-0.2);
                event.getPoseStack().mulPose(Vector3f.XP.rotationDegrees(-42.0F));
                event.getPoseStack().mulPose(Vector3f.YP.rotationDegrees(right?-28.0F:28.0F));
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientCloakState.clear();
        ClientBlockState.clear();
        ClientAbilityState.clear();
        offhandUseHeld=false;
        alternateHeld=false;
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!event.getEntity().level.isClientSide()) return;
        int bonus = 0;
        if (GeneralSkillRules.isWoodBlock(event.getState())) {
            bonus = ClientProgress.totalBonus(SkillBonusType.WOODCUTTING_SPEED);
        } else if (event.getState().is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            bonus = ClientProgress.totalBonus(SkillBonusType.MINING_SPEED);
        }
        if (bonus > 0) event.setNewSpeed(event.getNewSpeed() * (1.0F + bonus / 100.0F));
    }

    /** Removes only Tierborne's intrinsic speed contribution from the vanilla FOV calculation. */
    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        double bonus = ClientProgress.totalBonus(SkillBonusType.MOVEMENT_SPEED);
        if(ClientProgress.hasSkill(SwordsmanPlayerClass.SWORDMASTER))bonus+=RpgBalanceConfig.SWORDMASTER_SPEED.get();
        if(ClientProgress.hasSkill(SwordsmanPlayerClass.SM_SPEED))bonus+=RpgBalanceConfig.SWORDMASTER_UPGRADE_SPEED.get();
        if(ClientProgress.hasSkill(SwordsmanPlayerClass.ROGUE))bonus+=RpgBalanceConfig.ROGUE_SPEED.get();
        double voluntaryLimit = ClientProgress.movementSpeedLimitPercent() / 100.0;
        if (bonus == 0 && voluntaryLimit == 1.0) return;
        float walkingSpeed = event.getPlayer().getAbilities().getWalkingSpeed();
        if (walkingSpeed == 0.0F) return;

        double speedWithBonus = event.getPlayer().getAttributeValue(Attributes.MOVEMENT_SPEED);
        double speedWithoutBonus = speedWithBonus / ((1.0 + bonus / 100.0) * voluntaryLimit);
        double currentSpeedFactor = (speedWithBonus / walkingSpeed + 1.0) / 2.0;
        double compensatedSpeedFactor = (speedWithoutBonus / walkingSpeed + 1.0) / 2.0;
        if (currentSpeedFactor == 0.0) return;

        float compensatedRawFov = (float) (event.getFovModifier()
                / currentSpeedFactor * compensatedSpeedFactor);
        double effectScale = Minecraft.getInstance().options.fovEffectScale().get();
        event.setNewFovModifier((float) Mth.lerp(effectScale, 1.0F, compensatedRawFov));
    }

    @Mod.EventBusSubscriber(modid = Tierborne.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBusEvents {
        private ModBusEvents() {}

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_SKILLS);
            event.register(OPEN_PLAYER_MENU);
            event.register(ALTERNATE_ATTACK);
            event.register(SUBCLASS_UTILITY);
        }
    }
}
