package com.ollie.tierborne.playerclass;

import java.util.List;

public record AlternateAttackDefinition(String id, String skillId, String displayName, String description) {
    public static final List<AlternateAttackDefinition> ALL = List.of(
            new AlternateAttackDefinition("dash_strike", SwordsmanPlayerClass.DASH_STRIKE, "Dash Strike", "Dash forward and strike for increased damage."),
            new AlternateAttackDefinition("multislash", SwordsmanPlayerClass.MULTISLASH, "Multislash", "Deal one combined hit equal to two full-power strikes from each equipped sword."),
            new AlternateAttackDefinition("heavy_attack", SwordsmanPlayerClass.HEAVY_ATTACK, "Heavy Attack", "Deliver a powerful sword strike."),
            new AlternateAttackDefinition("cloak", SwordsmanPlayerClass.CLOAK, "Cloak", "Become invisible and shed hostile attention."),
            new AlternateAttackDefinition("leap_strike", SwordsmanPlayerClass.LEAP_STRIKE, "Leap Strike", "Leap upward and strike nearby enemies on landing."),
            new AlternateAttackDefinition("fireball", SwordsmanPlayerClass.FIREBALL, "Fireball", "Cast a server-authoritative fire projectile."),
            new AlternateAttackDefinition("flame_slash", SwordsmanPlayerClass.FLAME_SLASH, "Flame Slash", "Launch a travelling crescent of elemental flame."),
            new AlternateAttackDefinition("multishot_bow", ArcherPlayerClass.MULTISHOT, "Multishot", "Fire two full-power crossbow arrows."),
            new AlternateAttackDefinition("fully_charged", ArcherPlayerClass.FULLY_CHARGED, "Fully Charged", "Empower one deliberately slow bow shot."),
            new AlternateAttackDefinition("elemental_shot", ArcherPlayerClass.ELEMENTAL_SHOT, "Elemental Shot", "Instantly fire a burning, freezing arrow."),
            new AlternateAttackDefinition("backstep", ArcherPlayerClass.BACKSTEP, "Backstep", "Fire while dashing away."),
            new AlternateAttackDefinition("natures_roots", ArcherPlayerClass.NATURES_ROOTS, "Nature's Roots", "Channel roots on the target under the crosshair."),
            new AlternateAttackDefinition("pull", FighterPlayerClass.PULL, "Pull", "Pull a target into a full-power fist strike."),
            new AlternateAttackDefinition("uppercut", FighterPlayerClass.UPPERCUT, "Uppercut", "Strike upward with both hands empty and launch a nearby target."),
            new AlternateAttackDefinition("chain", FighterPlayerClass.CHAIN, "Chain", "Temporarily arm a stronger Combo."),
            new AlternateAttackDefinition("disarm", FighterPlayerClass.DISARM, "Disarm", "Temporarily disable a target's offensive actions."),
            new AlternateAttackDefinition("berserk", BarbarianPlayerClass.BERSERKER, "Berserk", "Toggle blood-fuelled axe damage, speed, lifesteal and Bleed."),
            new AlternateAttackDefinition("execute", BarbarianPlayerClass.EXECUTIONER, "Execute", "Charge an axe strike that grows stronger as the target weakens."),
            new AlternateAttackDefinition("fireball_volley", MagePlayerClass.FIREBALL_VOLLEY, "Blazing Barrage", "Conjure seven fire circles and launch a spreading volley of animated fireballs."),
            new AlternateAttackDefinition("flame_ring", MagePlayerClass.FLAME_RING, "Meteor Ring", "Call down a meteor eruption of flame, rubble, and ruptured ground around yourself."),
            new AlternateAttackDefinition("ice_lance", MagePlayerClass.ICE_LANCE, "Hailpiercer", "Raise a line of animated ice lances that pierces and freezes enemies."),
            new AlternateAttackDefinition("frost_nova", MagePlayerClass.FROST_NOVA, "Cryo Prison", "Encase nearby enemies in an erupting animated ice prison."),
            new AlternateAttackDefinition("venom_bolt", MagePlayerClass.VENOM_BOLT, "Venom Bolt", "Strike a distant enemy with concentrated venom."),
            new AlternateAttackDefinition("toxic_cloud", MagePlayerClass.TOXIC_CLOUD, "Toxic Cloud", "Create a lingering poisonous cloud."),
            new AlternateAttackDefinition("chain_lightning", MagePlayerClass.CHAIN_LIGHTNING, "Chain Lightning", "Electrocute several nearby enemies in sequence."),
            new AlternateAttackDefinition("thunderstep", MagePlayerClass.THUNDERSTEP, "Thunderstep", "Teleport and shock enemies at both ends."),
            new AlternateAttackDefinition("healing_pulse", MagePlayerClass.HEALING_PULSE, "Healing Pulse", "Heal yourself and nearby players."),
            new AlternateAttackDefinition("purge", MagePlayerClass.PURGE, "Purge", "Cleanse allies and damage undead."),
            new AlternateAttackDefinition("adrenaline", MagePlayerClass.ADRENALINE, "Adrenaline", "Empower nearby players with combat-enhancing effects."));

    public static AlternateAttackDefinition find(String id) { return ALL.stream().filter(a -> a.id.equals(id)).findFirst().orElse(null); }
    public static AlternateAttackDefinition forSkill(String skillId) { return ALL.stream().filter(a -> a.skillId.equals(skillId)).findFirst().orElse(null); }
}
