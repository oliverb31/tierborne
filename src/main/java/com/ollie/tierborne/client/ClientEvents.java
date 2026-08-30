package com.ollie.tierborne.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.ollie.tierborne.Tierborne;
import com.ollie.tierborne.client.screen.SkillTreeScreen;
import com.ollie.tierborne.client.screen.PlayerMenuScreen;
import com.ollie.tierborne.playerclass.SkillBonusType;
import com.ollie.tierborne.playerclass.GeneralSkillRules;
import com.ollie.tierborne.network.ModNetwork;
import com.ollie.tierborne.network.AbilityActionPacket;
import com.ollie.tierborne.combat.AbilityAction;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.SwordItem;
import com.ollie.tierborne.playerclass.SwordsmanPlayerClass;
import com.ollie.tierborne.config.RpgBalanceConfig;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Tierborne.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    private static boolean utilityHeld;
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
        if (minecraft.player == null || minecraft.screen != null) return;
        while (ALTERNATE_ATTACK.consumeClick()) ModNetwork.CHANNEL.sendToServer(new AbilityActionPacket(AbilityAction.ALTERNATE_ATTACK));
        boolean down = SUBCLASS_UTILITY.isDown();
        if (down != utilityHeld) {
            utilityHeld = down;
            ModNetwork.CHANNEL.sendToServer(new AbilityActionPacket(down ? AbilityAction.UTILITY_START : AbilityAction.UTILITY_STOP));
        }
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft=Minecraft.getInstance();
        if(!event.isUseItem()||minecraft.player==null||!ClientProgress.hasSkill(SwordsmanPlayerClass.DUAL)
                ||!(minecraft.player.getMainHandItem().getItem() instanceof SwordItem)
                ||!(minecraft.player.getOffhandItem().getItem() instanceof SwordItem)) return;
        event.setCanceled(true);
        if(event.getHand()==InteractionHand.MAIN_HAND) ModNetwork.CHANNEL.sendToServer(new AbilityActionPacket(AbilityAction.OFFHAND_ATTACK));
    }

    @SubscribeEvent
    public static void onRenderHud(RenderGuiOverlayEvent.Post event) {
        if(event.getOverlay()!=VanillaGuiOverlay.HOTBAR.type())return;
        Minecraft minecraft=Minecraft.getInstance();
        if(minecraft.player==null||minecraft.options.hideGui)return;
        java.util.List<com.ollie.tierborne.combat.AbilityStatus> statuses=ClientAbilityState.statuses();
        int width=92,x=event.getWindow().getGuiScaledWidth()-width-8;
        int y=event.getWindow().getGuiScaledHeight()-58-statuses.size()*24;
        for(com.ollie.tierborne.combat.AbilityStatus status:statuses){
            String title=status.active()?status.name()+" - ACTIVE":status.name();
            minecraft.font.drawShadow(event.getPoseStack(),title,x,y,0xFFE9E2D0);
            int barY=y+10;GuiComponent.fill(event.getPoseStack(),x,barY,x+width,barY+7,0xFF71572D);GuiComponent.fill(event.getPoseStack(),x+1,barY+1,x+width-1,barY+6,0xD0101218);
            int fill=(int)Math.round((width-2)*Math.min(1.0,status.remainingTicks()/(double)Math.max(1,status.totalTicks())));
            GuiComponent.fill(event.getPoseStack(),x+1,barY+1,x+1+fill,barY+6,status.active()?0xFF4EA56B:0xFFD7AD55);
            minecraft.font.drawShadow(event.getPoseStack(),String.format(java.util.Locale.ROOT,"%.1fs",status.remainingTicks()/20.0),x+width-28,y,0xFF9B968A);y+=24;
        }
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
        if (bonus == 0) return;
        float walkingSpeed = event.getPlayer().getAbilities().getWalkingSpeed();
        if (walkingSpeed == 0.0F) return;

        double speedWithBonus = event.getPlayer().getAttributeValue(Attributes.MOVEMENT_SPEED);
        double speedWithoutBonus = speedWithBonus / (1.0 + bonus / 100.0);
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
