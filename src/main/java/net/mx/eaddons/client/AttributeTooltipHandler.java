package net.mx.eaddons.client;

import java.util.List;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.mx.eaddons.attribute.EnigmaticAttributes;

/**
 * 物品提示里，比例型自定义属性的「加值」修饰符改显示为百分比。
 * <p>原版对加值修饰符直接打印小数（「+0.1 吸血」），这里改成「+10% 吸血」，
 * 只改写与原版格式完全吻合的行，其它模组的提示不受影响。
 */
public class AttributeTooltipHandler {

    private static final IAttribute[] PERCENT_ATTRIBUTES = {
            EnigmaticAttributes.LIFESTEAL, EnigmaticAttributes.PROJECTILE_DEFLECT,
            EnigmaticAttributes.ETHERIUM_SHIELD, EnigmaticAttributes.MINING_SPEED, EnigmaticAttributes.CRIT_DAMAGE
    };
    private static final String MARK = "\u0001";

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onTooltip(ItemTooltipEvent event) {
        List<String> lines = event.getToolTip();
        for (int i = 0; i < lines.size(); i++) {
            String converted = toPercent(lines.get(i));
            if (converted != null) {
                lines.set(i, converted);
            }
        }
    }

    private static String toPercent(String line) {
        String plain = TextFormatting.getTextWithoutFormattingCodes(line);
        if (plain == null) {
            return null;
        }
        for (IAttribute attribute : PERCENT_ATTRIBUTES) {
            String name = I18n.format("attribute.name." + attribute.getName());
            if (!plain.endsWith(name)) {
                continue;
            }
            for (String sign : new String[]{"plus", "take"}) {
                String template = I18n.format("attribute.modifier." + sign + ".0", MARK, name);
                int mark = template.indexOf(MARK);
                if (mark < 0) {
                    continue;
                }
                String prefix = template.substring(0, mark);
                String suffix = template.substring(mark + MARK.length());
                if (!plain.startsWith(prefix) || !plain.endsWith(suffix)
                        || plain.length() <= prefix.length() + suffix.length()) {
                    continue;
                }
                String number = plain.substring(prefix.length(), plain.length() - suffix.length());
                double value;
                try {
                    value = Double.parseDouble(number.replace(",", ".").trim());
                } catch (NumberFormatException e) {
                    continue;
                }
                String color = line.substring(0, line.length() - stripLeadingCodes(line).length());
                return color + I18n.format("attribute.modifier." + sign + ".1",
                        ItemStack.DECIMALFORMAT.format(value * 100), name);
            }
        }
        return null;
    }

    /** 去掉行首的格式代码（颜色前缀），用于保留原来的颜色 */
    private static String stripLeadingCodes(String line) {
        int i = 0;
        while (i + 1 < line.length() && line.charAt(i) == '§') {
            i += 2;
        }
        return line.substring(i);
    }
}
