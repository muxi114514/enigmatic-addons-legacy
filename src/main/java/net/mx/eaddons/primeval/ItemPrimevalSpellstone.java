package net.mx.eaddons.primeval;

import baubles.api.BaublesApi;
import keletu.enigmaticlegacy.api.cap.IForbiddenConsumed;
import keletu.enigmaticlegacy.item.EAddonsSpellstoneBauble;
import keletu.enigmaticlegacy.util.interfaces.IFortuneBonus;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.mx.eaddons.EAddonsMod;
import net.mx.eaddons.primeval.network.PrimevalOpenRecordMessage;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 原初立方（术石）：术石台用集满的原初立方重铸而来，也是合成非欧立方的核心。
 *
 * <p>被动：幸运、时运、免疫一切负面效果、免疫挤压 / 撞墙动能 / 摔落（含末影珍珠落地）/ 荆棘 / 火焰 / 熔岩伤害。
 * 伤害免疫走 EL 的 immunityList——EL 在 LivingAttackEvent（HIGH）里消费它，早于 First Aid 接管玩家伤害。
 * <p>主动：打开记录界面，最多记下身上三个正面效果，佩戴期间持续获得「记录等级 +1」（见 {@link PrimevalRecord}）。
 * <p>注册名随 EL 咒石基类落在 enigmaticlegacy 域，模型由 EAddonsMod 单独指到 eaddons。
 */
public class ItemPrimevalSpellstone extends EAddonsSpellstoneBauble implements IFortuneBonus {
    public static final ItemPrimevalSpellstone INSTANCE = new ItemPrimevalSpellstone();

    private static final UUID LUCK_UUID = UUID.fromString("6b3f6a8e-0e1a-4c2b-9d3e-000000000070");

    public ItemPrimevalSpellstone() {
        super("primeval_spellstone", EnumRarity.EPIC);
        DamageSource[] immune = {DamageSource.CRAMMING, DamageSource.FLY_INTO_WALL, DamageSource.FALL,
                DamageSource.IN_FIRE, DamageSource.ON_FIRE, DamageSource.LAVA, DamageSource.HOT_FLOOR};
        for (DamageSource source : immune) {
            this.immunityList.add(source.damageType);
        }
        this.immunityList.add("thorns");
    }

    public static boolean isWorn(EntityLivingBase entity) {
        return entity instanceof EntityPlayer
                && BaublesApi.isBaubleEquipped((EntityPlayer) entity, INSTANCE) != -1;
    }

    public int getCooldown(EntityPlayer player) {
        return PrimevalConfig.recordCooldown;
    }

    @Override
    public int bonusLevelFortune() {
        return PrimevalConfig.spellstoneFortune;
    }

    // ============================ 主动：打开记录界面 ============================

    /** 冷却只在真正记下新效果时才计（见 PrimevalRecord#applySelection），打开界面本身不耗。 */
    @Override
    public void triggerActiveAbility(World world, EntityPlayerMP player, ItemStack stack) {
        if (IForbiddenConsumed.get(player).getSpellstoneCooldown() > 0) {
            return;
        }
        EAddonsMod.PACKET_HANDLER.sendTo(new PrimevalOpenRecordMessage(PrimevalRecord.read(stack)), player);
    }

    // ============================ 佩戴被动 ============================

    @Override
    public void onWornTick(ItemStack stack, EntityLivingBase living) {
        super.onWornTick(stack, living);
        if (!(living instanceof EntityPlayer) || living.world.isRemote) {
            return;
        }
        EntityPlayer player = (EntityPlayer) living;

        // 幸运修饰符不随饰品存盘，登录、跨维度时 Baubles 也不一定走 onEquipped，每 tick 校一次
        IAttributeInstance luck = player.getEntityAttribute(SharedMonsterAttributes.LUCK);
        if (luck != null && luck.getModifier(LUCK_UUID) == null) {
            luck.applyModifier(new AttributeModifier(LUCK_UUID, "Primeval luck",
                    PrimevalConfig.spellstoneLuck, 0).setSaved(false));
        }
        // 火焰伤害已免疫，顺手熄掉身上的火，省得一直冒火
        if (player.isBurning()) {
            player.extinguish();
        }
        // 清理层：戴上之前就有的负面效果（阻断层在 PrimevalSpellstoneEvents）
        List<Potion> bad = null;
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getPotion().isBadEffect()) {
                if (bad == null) {
                    bad = new ArrayList<>();
                }
                bad.add(effect.getPotion());
            }
        }
        if (bad != null) {
            for (Potion potion : bad) {
                player.removePotionEffect(potion);
            }
        }

        PrimevalRecord.supply(player, stack);
    }

    @Override
    public void onUnequipped(ItemStack stack, EntityLivingBase living) {
        super.onUnequipped(stack, living);
        IAttributeInstance luck = living.getEntityAttribute(SharedMonsterAttributes.LUCK);
        if (luck != null && luck.getModifier(LUCK_UUID) != null) {
            luck.removeModifier(LUCK_UUID);
        }
    }

    // ============================ Tooltip ============================

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> list, ITooltipFlag flag) {
        list.add(I18n.format("tooltip.eaddons.primeval_spellstone.brief"));
        if (GuiScreen.isShiftKeyDown()) {
            list.add("");
            list.add(I18n.format("tooltip.eaddons.primeval_spellstone.active"));
            list.add(I18n.format("tooltip.eaddons.primeval_spellstone.active1", PrimevalConfig.recordSlots));
            list.add(I18n.format("tooltip.eaddons.primeval_spellstone.active2", PrimevalConfig.recordCooldown / 20));
            list.add("");
            list.add(I18n.format("tooltip.eaddons.primeval_spellstone.passive"));
            list.add(I18n.format("tooltip.eaddons.primeval_spellstone.passive1", PrimevalConfig.spellstoneLuck));
            list.add(I18n.format("tooltip.eaddons.primeval_spellstone.passive2", PrimevalConfig.spellstoneFortune));
            list.add(I18n.format("tooltip.eaddons.primeval_spellstone.passive3"));
            list.add(I18n.format("tooltip.eaddons.primeval_spellstone.passive4"));
        } else {
            list.add(I18n.format("tooltip.eaddons.primeval_spellstone.hold_shift"));
        }
        list.add("");
        List<PrimevalRecord.Entry> record = PrimevalRecord.read(stack);
        if (record.isEmpty()) {
            list.add(I18n.format("tooltip.eaddons.primeval_spellstone.record_empty"));
        } else {
            list.add(I18n.format("tooltip.eaddons.primeval_spellstone.record_header"));
            for (PrimevalRecord.Entry entry : record) {
                list.add(I18n.format("tooltip.eaddons.primeval_spellstone.record_entry",
                        I18n.format(entry.potion.getName()), levelText(PrimevalRecord.givenAmplifier(entry.base))));
            }
        }
    }

    /** amplifier 转罗马数字（原版语言文件只有 I~X，更高的用阿拉伯数字）。 */
    @SideOnly(Side.CLIENT)
    public static String levelText(int amplifier) {
        String key = "enchantment.level." + (amplifier + 1);
        return I18n.hasKey(key) ? I18n.format(key) : String.valueOf(amplifier + 1);
    }
}
