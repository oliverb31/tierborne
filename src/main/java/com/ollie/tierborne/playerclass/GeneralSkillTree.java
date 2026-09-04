package com.ollie.tierborne.playerclass;

import com.ollie.tierborne.Tierborne;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Set;

public final class GeneralSkillTree implements SkillTreeDefinition {
    public static final GeneralSkillTree INSTANCE = new GeneralSkillTree();
    public static final String ROOT = "general_player_root";
    public static final String WORK_ETHIC = "general_work_ethic";
    public static final String AXE_HANDLING_I = "general_axe_handling_1";
    public static final String AXE_HANDLING_II = "general_axe_handling_2";
    public static final String CAREFUL_FELLING = "general_careful_felling";
    public static final String PICKAXE_HANDLING_I = "general_pickaxe_handling_1";
    public static final String PICKAXE_HANDLING_II = "general_pickaxe_handling_2";
    public static final String CAREFUL_EXTRACTION = "general_careful_extraction";
    public static final String MATERIAL_STUDY = "general_material_study";
    public static final String EFFICIENT_SMITHING = "general_efficient_smithing";
    public static final String ENCHANTMENT_THEORY = "general_enchantment_theory";
    public static final String LUMBERJACK = "general_subclass_lumberjack";
    public static final String MINER = "general_subclass_miner";
    public static final String BLACKSMITH = "general_subclass_blacksmith";
    public static final String WOODCUTTING_SPEED = "general_woodcutting_speed";
    public static final String WOOD_DROPS = "general_wood_drops";
    public static final String MINING_SPEED = "general_mining_speed";
    public static final String ORE_DROPS = "general_ore_drops";
    public static final String MATERIAL_RECOVERY = "general_material_recovery";
    public static final String ENCHANTED_CRAFTING = "general_enchanted_crafting";

    private static final List<Skill> SKILLS = List.of(
            skill(ROOT, "Player", "P", "The root of general progression.", 0, Items.PLAYER_HEAD, 0, 0,
                    List.of(), List.of(), true, null, SkillEffect.NONE),
            upgrade(WORK_ETHIC, "Work Ethic", "Move " + GeneralSkillBalance.MINOR_MOVEMENT_SPEED_PERCENT
                            + "% faster while travelling between jobs.", Items.LEATHER_BOOTS, 0, 1, ROOT,
                    SkillBonusType.MOVEMENT_SPEED, GeneralSkillBalance.MINOR_MOVEMENT_SPEED_PERCENT),

            upgrade(AXE_HANDLING_I, "Axe Handling I", "Break eligible wood "
                            + GeneralSkillBalance.MINOR_BREAK_SPEED_PERCENT + "% faster.",
                    Items.WOODEN_AXE, -3, 2, WORK_ETHIC, SkillBonusType.WOODCUTTING_SPEED,
                    GeneralSkillBalance.MINOR_BREAK_SPEED_PERCENT),
            upgrade(AXE_HANDLING_II, "Axe Handling II", "Break eligible wood another "
                            + GeneralSkillBalance.MINOR_BREAK_SPEED_PERCENT + "% faster.",
                    Items.STONE_AXE, -4, 3, AXE_HANDLING_I, SkillBonusType.WOODCUTTING_SPEED,
                    GeneralSkillBalance.MINOR_BREAK_SPEED_PERCENT),
            upgrade(CAREFUL_FELLING, "Careful Felling", "Gain a "
                            + GeneralSkillBalance.MINOR_DROP_CHANCE_PERCENT
                            + "% chance to receive one additional eligible wood block.",
                    Items.OAK_LOG, -5, 4, AXE_HANDLING_II, SkillBonusType.WOOD_DROP_CHANCE,
                    GeneralSkillBalance.MINOR_DROP_CHANCE_PERCENT),
            skill(LUMBERJACK, "Lumberjack", "AXE", "Specialise in harvesting wood and trees.", 1,
                    Items.IRON_AXE, -6, 5, List.of(CAREFUL_FELLING), List.of(MINER, BLACKSMITH),
                    false, null, SkillEffect.GENERAL_SUBCLASS),

            upgrade(PICKAXE_HANDLING_I, "Pickaxe Handling I", "Break pickaxe-mineable blocks "
                            + GeneralSkillBalance.MINOR_BREAK_SPEED_PERCENT + "% faster.",
                    Items.WOODEN_PICKAXE, 0, 2, WORK_ETHIC, SkillBonusType.MINING_SPEED,
                    GeneralSkillBalance.MINOR_BREAK_SPEED_PERCENT),
            upgrade(PICKAXE_HANDLING_II, "Pickaxe Handling II", "Break pickaxe-mineable blocks another "
                            + GeneralSkillBalance.MINOR_BREAK_SPEED_PERCENT + "% faster.",
                    Items.STONE_PICKAXE, 0, 3, PICKAXE_HANDLING_I, SkillBonusType.MINING_SPEED,
                    GeneralSkillBalance.MINOR_BREAK_SPEED_PERCENT),
            upgrade(CAREFUL_EXTRACTION, "Careful Extraction", "Gain a "
                            + GeneralSkillBalance.MINOR_DROP_CHANCE_PERCENT
                            + "% chance to receive one additional ore drop. Silk Touch is excluded.",
                    Items.RAW_IRON, 0, 4, PICKAXE_HANDLING_II, SkillBonusType.ORE_DROP_CHANCE,
                    GeneralSkillBalance.MINOR_DROP_CHANCE_PERCENT),
            skill(MINER, "Miner", "PICK", "Specialise in extracting stone and valuable ores.", 1,
                    Items.IRON_PICKAXE, 0, 5, List.of(CAREFUL_EXTRACTION), List.of(LUMBERJACK, BLACKSMITH),
                    false, null, SkillEffect.GENERAL_SUBCLASS),

            upgrade(MATERIAL_STUDY, "Material Study", "Gain a "
                            + GeneralSkillBalance.MINOR_CRAFTING_CHANCE_PERCENT
                            + "% chance to recover one ingredient when crafting equipment.",
                    Items.IRON_INGOT, 3, 2, WORK_ETHIC, SkillBonusType.MATERIAL_RECOVERY_CHANCE,
                    GeneralSkillBalance.MINOR_CRAFTING_CHANCE_PERCENT),
            upgrade(EFFICIENT_SMITHING, "Efficient Smithing", "Gain another "
                            + GeneralSkillBalance.MINOR_CRAFTING_CHANCE_PERCENT
                            + "% chance to recover one ingredient when crafting equipment.",
                    Items.SMITHING_TABLE, 4, 3, MATERIAL_STUDY, SkillBonusType.MATERIAL_RECOVERY_CHANCE,
                    GeneralSkillBalance.MINOR_CRAFTING_CHANCE_PERCENT),
            upgrade(ENCHANTMENT_THEORY, "Enchantment Theory", "Gain a "
                            + GeneralSkillBalance.MINOR_CRAFTING_CHANCE_PERCENT
                            + "% chance for crafted equipment to receive compatible enchantments.",
                    Items.LAPIS_LAZULI, 5, 4, EFFICIENT_SMITHING, SkillBonusType.ENCHANTED_CRAFTING_CHANCE,
                    GeneralSkillBalance.MINOR_CRAFTING_CHANCE_PERCENT),
            skill(BLACKSMITH, "Blacksmith", "ANVIL", "Specialise in crafting weapons and armour.", 1,
                    Items.ANVIL, 6, 5, List.of(ENCHANTMENT_THEORY), List.of(LUMBERJACK, MINER),
                    false, null, SkillEffect.GENERAL_SUBCLASS),

            skill(WOODCUTTING_SPEED, "Woodcutting Speed", "+50%",
                    "Break eligible axe-mineable wood " + GeneralSkillBalance.TARGETED_BREAK_SPEED_PERCENT + "% faster.",
                    1, Items.DIAMOND_AXE, -7, 6,
                    List.of(LUMBERJACK), List.of(), false,
                    new SkillUpgrade(SkillBonusType.WOODCUTTING_SPEED,
                            GeneralSkillBalance.TARGETED_BREAK_SPEED_PERCENT), SkillEffect.NONE),
            skill(WOOD_DROPS, "Increased Wood Drops", "+50%",
                    GeneralSkillBalance.EXTRA_DROP_CHANCE_PERCENT
                            + "% chance to gain 1 additional eligible wood block. Block entities are excluded.",
                    1, Items.OAK_LOG, -5, 6,
                    List.of(LUMBERJACK), List.of(), false,
                    new SkillUpgrade(SkillBonusType.WOOD_DROP_CHANCE,
                            GeneralSkillBalance.EXTRA_DROP_CHANCE_PERCENT), SkillEffect.WOOD_DROPS),
            skill(MINING_SPEED, "Mining Speed", "+50%",
                    "Break pickaxe-mineable blocks " + GeneralSkillBalance.TARGETED_BREAK_SPEED_PERCENT + "% faster.",
                    1, Items.DIAMOND_PICKAXE, -1, 6,
                    List.of(MINER), List.of(), false,
                    new SkillUpgrade(SkillBonusType.MINING_SPEED,
                            GeneralSkillBalance.TARGETED_BREAK_SPEED_PERCENT), SkillEffect.NONE),
            skill(ORE_DROPS, "Increased Ore Drops", "+50%",
                    GeneralSkillBalance.EXTRA_DROP_CHANCE_PERCENT
                            + "% chance to gain 1 additional ore drop. Does not trigger with Silk Touch.",
                    1, Items.DIAMOND_ORE, 1, 6,
                    List.of(MINER), List.of(), false,
                    new SkillUpgrade(SkillBonusType.ORE_DROP_CHANCE,
                            GeneralSkillBalance.EXTRA_DROP_CHANCE_PERCENT), SkillEffect.ORE_DROPS),
            skill(MATERIAL_RECOVERY, "Material Recovery", "MR",
                    GeneralSkillBalance.MATERIAL_REFUND_CHANCE_PERCENT
                            + "% chance to return 1 actual ingredient when crafting eligible equipment.",
                    1, Items.GOLD_INGOT, 5, 6,
                    List.of(BLACKSMITH), List.of(ENCHANTED_CRAFTING), false,
                    new SkillUpgrade(SkillBonusType.MATERIAL_RECOVERY_CHANCE,
                            GeneralSkillBalance.MATERIAL_REFUND_CHANCE_PERCENT), SkillEffect.MATERIAL_RECOVERY),
            skill(ENCHANTED_CRAFTING, "Enchanted Crafting", "EC",
                    GeneralSkillBalance.ENCHANTED_CRAFTING_CHANCE_PERCENT
                            + "% chance for eligible equipment to gain compatible random enchantments at power "
                            + GeneralSkillBalance.ENCHANTMENT_MIN_LEVEL + "-"
                            + GeneralSkillBalance.ENCHANTMENT_MAX_LEVEL + ".",
                    1, Items.ENCHANTED_BOOK, 7, 6,
                    List.of(BLACKSMITH), List.of(MATERIAL_RECOVERY), false,
                    new SkillUpgrade(SkillBonusType.ENCHANTED_CRAFTING_CHANCE,
                            GeneralSkillBalance.ENCHANTED_CRAFTING_CHANCE_PERCENT), SkillEffect.ENCHANTED_CRAFTING)
    );

    private GeneralSkillTree() {}

    @Override public String id() { return "general"; }
    @Override public String displayName() { return "General"; }
    @Override public ItemStack iconStack() { return new ItemStack(Items.COMPASS); }
    @Override public List<Skill> skills() { return SKILLS; }

    public Skill selectedSubclass(Set<String> unlockedSkills) {
        return SKILLS.stream().filter(skill -> skill.effect() == SkillEffect.GENERAL_SUBCLASS)
                .filter(skill -> unlockedSkills.contains(skill.id())).findFirst().orElse(null);
    }

    private static Skill skill(String id, String name, String label, String description, int cost,
                               net.minecraft.world.item.Item iconItem,
                               int column, int row, List<String> prerequisites, List<String> exclusions,
                               boolean automatic, SkillUpgrade upgrade, SkillEffect effect) {
        return new Skill(id, name, label, description, cost,
                new ResourceLocation(Tierborne.MOD_ID, "textures/gui/icons/" + id + ".png"),
                SkillIcon.item(iconItem), column, row, prerequisites, exclusions, automatic, upgrade, effect);
    }

    private static Skill upgrade(String id, String name, String description,
                                 net.minecraft.world.item.Item iconItem, int column, int row,
                                 String prerequisite, SkillBonusType type, int amount) {
        return skill(id, name, "+" + amount + "%", description, 1, iconItem, column, row,
                List.of(prerequisite), List.of(), false, new SkillUpgrade(type, amount), SkillEffect.NONE);
    }
}
