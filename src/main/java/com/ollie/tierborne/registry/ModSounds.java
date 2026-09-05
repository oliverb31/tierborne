package com.ollie.tierborne.registry;

import com.ollie.tierborne.Tierborne;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Tierborne.MOD_ID);

    public static final RegistryObject<SoundEvent> ICE_KNIGHT_DEATH = register("ice_knight.death");
    public static final RegistryObject<SoundEvent> ICE_KNIGHT_HURT_1 = register("ice_knight.hurt1");
    public static final RegistryObject<SoundEvent> ICE_KNIGHT_HURT_2 = register("ice_knight.hurt2");
    public static final RegistryObject<SoundEvent> ICE_KNIGHT_MOUNT_HURT_1 = register("ice_knight.mount_hurt1");
    public static final RegistryObject<SoundEvent> ICE_KNIGHT_MOUNT_HURT_2 = register("ice_knight.mount_hurt2");
    public static final RegistryObject<SoundEvent> ICE_KNIGHT_MINION_DEATH = register("ice_knight.minion_death");
    public static final RegistryObject<SoundEvent> ICE_KNIGHT_MINION_HURT_1 = register("ice_knight.minion_hurt1");
    public static final RegistryObject<SoundEvent> ICE_KNIGHT_MINION_HURT_2 = register("ice_knight.minion_hurt2");
    public static final RegistryObject<SoundEvent> ICE_KNIGHT_RAISE_SPEAR = register("ice_knight.raise_spear");
    public static final RegistryObject<SoundEvent> ICE_KNIGHT_SHOCKWAVE = register("ice_knight.shockwave");
    public static final RegistryObject<SoundEvent> ICE_KNIGHT_SLASH = register("ice_knight.slash");
    public static final RegistryObject<SoundEvent> ICE_KNIGHT_SPAWN_MOUNT = register("ice_knight.spawn_mount");
    public static final RegistryObject<SoundEvent> ICE_KNIGHT_STAB_FORWARD = register("ice_knight.stab_forward");
    public static final RegistryObject<SoundEvent> MAGE_FIRE_BALL = register("mage.fire_ball");
    public static final RegistryObject<SoundEvent> MAGE_FIRE_BLAST = register("mage.fire_blast");
    public static final RegistryObject<SoundEvent> MAGE_FIRE_CIRCLE = register("mage.fire_circle");
    public static final RegistryObject<SoundEvent> MAGE_FIRE_EXPLODE = register("mage.fire_explode");
    public static final RegistryObject<SoundEvent> MAGE_GLACIAL_SPIKES = register("mage.glacial_spikes_shoot");
    public static final RegistryObject<SoundEvent> MAGE_ICE_BREAK = register("mage.ice_break");
    public static final RegistryObject<SoundEvent> MAGE_ICE_CHARGE = register("mage.ice_charge");
    public static final RegistryObject<SoundEvent> MAGE_ICE_SPIKE = register("mage.ice_spike_creation");
    public static final RegistryObject<SoundEvent> MAGE_BARRIER_BREAK = register("mage.mana_barrier_break");
    public static final RegistryObject<SoundEvent> MAGE_METEOR_CREATE = register("mage.meteor_creation");
    public static final RegistryObject<SoundEvent> MAGE_METEOR_EXPLODE = register("mage.meteor_explosion");
    public static final RegistryObject<SoundEvent> MAGE_METEOR_SHOOT = register("mage.meteor_shoot");
    public static final RegistryObject<SoundEvent> MAGE_THUNDER_STRIKE = register("mage.thunder_strike");
    public static final RegistryObject<SoundEvent> MAGE_THUNDER_TELEPORT = register("mage.thunder_teleport");

    private ModSounds() {
    }

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> new SoundEvent(
                new ResourceLocation(Tierborne.MOD_ID, name)));
    }
}
