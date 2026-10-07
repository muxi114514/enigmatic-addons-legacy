package net.mx.eaddons.client;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.mx.eaddons.spellstone.CubeResonance;
import net.mx.eaddons.spellstone.SpellstoneData;
import net.mx.eaddons.spellstone.SpellstoneForm;
import net.mx.eaddons.spellstone.SwordShieldUse;

import java.util.List;

/** 术质共鸣者的 tooltip（从物品类拆出，只在客户端加载）。 */
@SideOnly(Side.CLIENT)
public final class SpellstoneSwordTooltip {

    private SpellstoneSwordTooltip() {
    }

    public static void addInformation(ItemStack stack, List<String> list) {
        SpellstoneForm form = SpellstoneData.getForm(stack);
        SpellstoneForm link = SpellstoneData.getLinkForm(stack);
        boolean linkActive = link != SpellstoneForm.NONE && SpellstoneData.isLinkActive(stack);
        int level = SpellstoneData.getLevel(stack);
        list.add("");
        if (GuiScreen.isShiftKeyDown()) {
            if (link != SpellstoneForm.NONE) {
                list.add(TextFormatting.GOLD + I18n.format("tooltip.eaddons.spellstone_sword.effect_header"));
                addEffectLines(list, link.path(), true);
                if (link == SpellstoneForm.PRIMEVAL_CUBE) {
                    list.add(TextFormatting.GRAY + I18n.format("tooltip.eaddons.spellstone_sword.switch_hint",
                            keyName(SpellstoneFormKeyHandler.switchFormKey)));
                    if (form != SpellstoneForm.PRIMEVAL_CUBE && form != SpellstoneForm.NONE) {
                        list.add("");
                        list.add(TextFormatting.GOLD + I18n.format("tooltip.eaddons.spellstone_sword.sub_header",
                                I18n.format("item." + form.path() + ".name")));
                        addEffectLines(list, form.path(), true);
                    }
                }
            } else if (form == SpellstoneForm.NONE) {
                list.add(TextFormatting.GRAY + I18n.format("tooltip.eaddons.spellstone_sword.absent1"));
                list.add(TextFormatting.GRAY + I18n.format("tooltip.eaddons.spellstone_sword.absent2"));
                list.add(TextFormatting.GRAY + I18n.format("tooltip.eaddons.spellstone_sword.absent3"));
            } else {
                list.add(TextFormatting.GOLD + I18n.format("tooltip.eaddons.spellstone_sword.effect_header"));
                addEffectLines(list, form.path(), level > 4);
            }
            if (SwordShieldUse.hasShield(stack)) {
                list.add(TextFormatting.GRAY + I18n.format("tooltip.eaddons.spellstone_sword.shield_hint",
                        keyName(SpellstoneSkillKeyHandler.skillKey)));
            }
        } else {
            list.add(I18n.format("tooltip.eaddons.spellstone_sword.hold_shift"));
        }
        list.add("");
        if (link != SpellstoneForm.NONE) {
            list.add(TextFormatting.GRAY + I18n.format("tooltip.eaddons.spellstone_sword.resonance",
                    TextFormatting.GOLD + I18n.format("item." + link.path() + ".name") + TextFormatting.GRAY));
            if (!linkActive) {
                list.add(TextFormatting.RED + I18n.format("tooltip.eaddons.spellstone_sword.dormant",
                        I18n.format("item." + link.path() + ".name")));
            }
            int swap = SpellstoneData.getTimer(stack, CubeResonance.SWAP_TIMER);
            if (swap > 0) {
                list.add(TextFormatting.DARK_GRAY + I18n.format("tooltip.eaddons.spellstone_sword.swap_cooldown",
                        String.format("%.1f", swap / 20.0F)));
            }
        } else if (form == SpellstoneForm.NONE) {
            list.add(TextFormatting.DARK_GRAY + I18n.format("tooltip.eaddons.spellstone_sword.resonance_absent"));
        } else {
            list.add(TextFormatting.GRAY + I18n.format("tooltip.eaddons.spellstone_sword.resonance",
                    TextFormatting.GOLD + I18n.format("item." + form.path() + ".name") + TextFormatting.GRAY));
        }
        int max = SpellstoneData.getMaxEnergy(stack);
        if (max > 0 && form != SpellstoneForm.NONE) {
            list.add(TextFormatting.GRAY + I18n.format("tooltip.eaddons.spellstone_sword.energy",
                    SpellstoneData.getEnergy(stack), max));
        }
        list.add(TextFormatting.GRAY + I18n.format("tooltip.eaddons.spellstone_sword.level", level));
        list.add(TextFormatting.DARK_GRAY + I18n.format("tooltip.eaddons.spellstone_sword.durability",
                stack.getMaxDamage() - stack.getItemDamage(), stack.getMaxDamage()));
    }

    /** 形态效果行：普通条目按 .1 .2 .3… 排下去，满级那条单独用 .max。 */
    private static void addEffectLines(List<String> list, String path, boolean max) {
        String base = "tooltip.eaddons.spellstone_sword.effect." + path;
        for (int i = 1; I18n.hasKey(base + "." + i); i++) {
            list.add(TextFormatting.LIGHT_PURPLE + I18n.format(base + "." + i));
        }
        if (max && I18n.hasKey(base + ".max")) {
            list.add(TextFormatting.AQUA + I18n.format(base + ".max"));
        }
    }

    private static String keyName(net.minecraft.client.settings.KeyBinding key) {
        return key == null ? "?" : key.getDisplayName();
    }
}
