package com.ollie.tierborne.playerclass;

import com.ollie.tierborne.Tierborne;
import com.ollie.tierborne.item.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Set;

public final class MagePlayerClass extends PlayerClass {
    public static final String ID = "mage";
    public static final String ROOT = "mage_root";
    public static final String ARCANE_POWER = "mage_arcane_power";
    public static final String QUICK_FOCUS = "mage_quick_focus";

    public static final String FIRE_MAGE = "mage_subclass_fire";
    public static final String FIREBALL_VOLLEY = "fire_mage_fireball_volley";
    public static final String VOLLEY_MASTERY = "fire_mage_volley_mastery";
    public static final String FLAME_RING = "fire_mage_flame_ring";
    public static final String INFERNO_CORE = "fire_mage_inferno_core";

    public static final String ICE_MAGE = "mage_subclass_ice";
    public static final String ICE_LANCE = "ice_mage_ice_lance";
    public static final String PIERCING_LANCE = "ice_mage_piercing_lance";
    public static final String FROST_NOVA = "ice_mage_frost_nova";
    public static final String SHATTER = "ice_mage_shatter";

    public static final String POISON_MAGE = "mage_subclass_poison";
    public static final String VENOM_BOLT = "poison_mage_venom_bolt";
    public static final String VIRULENT_VENOM = "poison_mage_virulent_venom";
    public static final String TOXIC_CLOUD = "poison_mage_toxic_cloud";
    public static final String PLAGUE_CLOUD = "poison_mage_plague_cloud";

    public static final String LIGHTNING_MAGE = "mage_subclass_lightning";
    public static final String CHAIN_LIGHTNING = "lightning_mage_chain_lightning";
    public static final String CONDUCTIVITY = "lightning_mage_conductivity";
    public static final String THUNDERSTEP = "lightning_mage_thunderstep";
    public static final String OVERCHARGE = "lightning_mage_overcharge";

    public static final String DOCTOR = "mage_subclass_doctor";
    public static final String HEALING_PULSE = "doctor_healing_pulse";
    public static final String TRIAGE = "doctor_triage";
    public static final String PURGE = "doctor_purge";
    public static final String ADRENALINE = "doctor_adrenaline";
    public static final String FIELD_MEDIC = "doctor_field_medic";

    private static final List<String> SUBCLASSES = List.of(
            FIRE_MAGE, ICE_MAGE, POISON_MAGE, LIGHTNING_MAGE, DOCTOR);

    private static final List<Skill> SKILLS = List.of(
            skill(ROOT, "Mage", "Wield a Mage Staff and cast a ranged Arcane Bolt with right click.", 0,
                    Items.BLAZE_ROD, 0, 0, List.of(), List.of(), true, null, SkillEffect.NONE),
            upgrade(ARCANE_POWER, "Arcane Power", SkillBonusType.MAGIC_DAMAGE, 15,
                    Items.AMETHYST_SHARD, -1, 0, ROOT),
            node(QUICK_FOCUS, "Quick Focus", "Reduces the Mage Staff's Arcane Bolt cooldown.",
                    Items.CLOCK, 1, 0, ROOT),

            subclass(FIRE_MAGE, "Fire Mage", "Gain 15% magic damage and ignite targets struck by Arcane Bolt.",
                    Items.FIRE_CHARGE, 0, -3, ARCANE_POWER),
            alt(FIREBALL_VOLLEY, "Blazing Barrage", "Conjure fire circles and launch animated explosive fireballs.",
                    Items.FIRE_CHARGE, -1, -4, FIRE_MAGE),
            node(VOLLEY_MASTERY, "Volley Mastery", "Adds two fireballs and increases Blazing Barrage damage.",
                    Items.BLAZE_POWDER, -2, -5, FIREBALL_VOLLEY),
            alt(FLAME_RING, "Meteor Ring", "Erupt a meteor, rubble, and burning ruptures around yourself.",
                    Items.MAGMA_CREAM, 1, -4, FIRE_MAGE),
            node(INFERNO_CORE, "Inferno Core", "Increases Meteor Ring radius and burn duration.",
                    Items.LAVA_BUCKET, 2, -5, FLAME_RING),

            subclass(ICE_MAGE, "Ice Mage", "Gain 10% magic damage; Arcane Bolt slows enemies and you resist freezing.",
                    Items.PACKED_ICE, 3, -1, ARCANE_POWER),
            alt(ICE_LANCE, "Hailpiercer", "Raise a long line of ice lances that damages and heavily slows targets.",
                    Items.BLUE_ICE, 4, -2, ICE_MAGE),
            node(PIERCING_LANCE, "Piercing Lance", "Hailpiercer strikes every enemy along its path.",
                    Items.SPECTRAL_ARROW, 5, -3, ICE_LANCE),
            alt(FROST_NOVA, "Cryo Prison", "Erupt an ice prison that freezes and cages every nearby enemy.",
                    Items.SNOWBALL, 4, 0, ICE_MAGE),
            node(SHATTER, "Shatter", "Frost spells deal bonus damage to already slowed targets.",
                    Items.PRISMARINE_CRYSTALS, 5, 1, FROST_NOVA),

            subclass(POISON_MAGE, "Poison Mage", "Arcane Bolt poisons enemies struck by it.",
                    Items.SPIDER_EYE, 3, 2, QUICK_FOCUS),
            alt(VENOM_BOLT, "Venom Bolt", "Strike a distant target with concentrated venom.",
                    Items.FERMENTED_SPIDER_EYE, 4, 3, POISON_MAGE),
            node(VIRULENT_VENOM, "Virulent Venom", "Raises Venom Bolt's poison potency and duration.",
                    Items.POISONOUS_POTATO, 5, 4, VENOM_BOLT),
            alt(TOXIC_CLOUD, "Toxic Cloud", "Create a lingering poisonous cloud at the targeted location.",
                    Items.SLIME_BALL, 2, 4, POISON_MAGE),
            node(PLAGUE_CLOUD, "Plague Cloud", "Increases Toxic Cloud radius, duration, and damage.",
                    Items.MOSS_BLOCK, 1, 5, TOXIC_CLOUD),

            subclass(LIGHTNING_MAGE, "Lightning Mage", "Gain 20% magic damage and Arcane Bolt jumps to a nearby enemy.",
                    Items.LIGHTNING_ROD, 0, 3, QUICK_FOCUS),
            alt(CHAIN_LIGHTNING, "Chain Lightning", "Electrocute a target, then leap between nearby enemies.",
                    Items.COPPER_INGOT, -1, 4, LIGHTNING_MAGE),
            node(CONDUCTIVITY, "Conductivity", "Adds two Chain Lightning jumps and increases its search range.",
                    Items.COPPER_BLOCK, -2, 5, CHAIN_LIGHTNING),
            alt(THUNDERSTEP, "Thunderstep", "Teleport toward the crosshair and shock enemies at both ends.",
                    Items.ENDER_PEARL, 1, 4, LIGHTNING_MAGE),
            node(OVERCHARGE, "Overcharge", "Increases Thunderstep range and impact damage.",
                    Items.GLOWSTONE_DUST, 2, 5, THUNDERSTEP),

            subclass(DOCTOR, "Doctor", "Arcane Bolt becomes a healing ray; Doctor healing spells restore 25% more health.",
                    Items.GOLDEN_CARROT, -3, -1, QUICK_FOCUS),
            alt(HEALING_PULSE, "Healing Pulse", "Instantly heal yourself and nearby players.",
                    Items.GLISTERING_MELON_SLICE, -4, -2, DOCTOR),
            node(TRIAGE, "Triage", "Healing Pulse heals more and prioritises badly wounded allies.",
                    Items.GOLDEN_APPLE, -5, -3, HEALING_PULSE),
            alt(PURGE, "Purge", "Cleanse harmful effects from nearby players and damage nearby undead.",
                    Items.MILK_BUCKET, -4, 0, DOCTOR),
            alt(ADRENALINE, "Adrenaline", "Grant nearby players regeneration, speed, and damage resistance.",
                    Items.SUGAR, -3, 1, DOCTOR),
            node(FIELD_MEDIC, "Field Medic", "Increases Doctor spell radius and reduces all Doctor cooldowns.",
                    Items.TOTEM_OF_UNDYING, -4, 2, List.of(PURGE, ADRENALINE))
    );

    public MagePlayerClass() {
        super(ID, "Mage", "A spellcaster who specialises in fire, ice, poison, lightning, or medicine.",
                texture("mage"), ModItems.MAGE_STAFF.get(),
                List.of("Fire Mage", "Ice Mage", "Poison Mage", "Lightning Mage", "Doctor"));
    }

    @Override
    public List<Skill> skills() {
        return SKILLS;
    }

    public Skill selectedSubclass(Set<String> skills) {
        return SKILLS.stream().filter(skill -> skill.effect() == SkillEffect.CLASS_SUBCLASS
                && SUBCLASSES.contains(skill.id()) && skills.contains(skill.id())).findFirst().orElse(null);
    }

    private static Skill subclass(String id, String name, String description, Item icon,
                                  int x, int y, String prerequisite) {
        return skill(id, name, description + " Choosing it permanently excludes the other Mage subclasses.",
                1, icon, x, y, List.of(prerequisite),
                SUBCLASSES.stream().filter(other -> !other.equals(id)).toList(), false, null,
                SkillEffect.CLASS_SUBCLASS);
    }

    private static Skill alt(String id, String name, String description, Item icon,
                             int x, int y, String prerequisite) {
        return skill(id, name, "Alternate Attack: " + description, 1, icon, x, y,
                List.of(prerequisite), List.of(), false, null, SkillEffect.ALTERNATE_ATTACK);
    }

    private static Skill node(String id, String name, String description, Item icon,
                              int x, int y, String prerequisite) {
        return node(id, name, description, icon, x, y, List.of(prerequisite));
    }

    private static Skill node(String id, String name, String description, Item icon,
                              int x, int y, List<String> prerequisites) {
        return skill(id, name, description, 1, icon, x, y, prerequisites, List.of(),
                false, null, SkillEffect.NONE);
    }

    private static Skill upgrade(String id, String name, SkillBonusType type, int amount,
                                 Item icon, int x, int y, String prerequisite) {
        return skill(id, name, "Adds " + amount + "% " + type.displayName() + ".", 1,
                icon, x, y, List.of(prerequisite), List.of(), false,
                new SkillUpgrade(type, amount), SkillEffect.NONE);
    }

    private static Skill skill(String id, String name, String description, int cost, Item item,
                               int x, int y, List<String> prerequisites, List<String> exclusions,
                               boolean automatic, SkillUpgrade upgrade, SkillEffect effect) {
        return new Skill(id, name, name.substring(0, 1), description, cost, texture(id),
                SkillIcon.item(item), x, y, prerequisites, exclusions, automatic, upgrade, effect);
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(Tierborne.MOD_ID, "textures/gui/icons/mage_" + name + ".png");
    }
}
