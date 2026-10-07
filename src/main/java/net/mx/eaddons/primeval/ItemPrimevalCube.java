package net.mx.eaddons.primeval;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.mx.eaddons.spellstone.SpellstoneForm;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 原初立方（进度物品）：主手持它、副手持术石右键，消耗术石并铭刻进来；十种都铭刻后放进术石台核心位重铸成原初立方术石。
 * <p>每个玩家只在第一个原版战利品箱里拿到一次（{@link PrimevalCubeLoot}），所以死亡不掉落（{@link PrimevalCubeSoulbound}），
 * 掉在地上也毁不掉、不会消失。
 */
public class ItemPrimevalCube extends Item {
    public static final ItemPrimevalCube INSTANCE = new ItemPrimevalCube();

    public ItemPrimevalCube() {
        setUnlocalizedName("primeval_cube");
        setRegistryName("primeval_cube");
        setCreativeTab(CreativeTabs.MISC);
        setMaxStackSize(1);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }

    /** 集满后发光，一眼能看出可以去重铸了。 */
    @Override
    public boolean hasEffect(ItemStack stack) {
        return PrimevalCubeData.isComplete(stack);
    }

    // ============================ 铭刻：主手立方 + 副手术石右键 ============================

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack cube = player.getHeldItem(hand);
        if (hand != EnumHand.MAIN_HAND) {
            return new ActionResult<>(EnumActionResult.PASS, cube);
        }
        ItemStack offhand = player.getHeldItemOffhand();
        if (!SpellstoneForm.isResonatable(offhand)) {
            return new ActionResult<>(EnumActionResult.PASS, cube);
        }
        SpellstoneForm form = SpellstoneForm.byItem(offhand.getItem());
        if (!PrimevalCubeData.required().contains(form)) {
            return new ActionResult<>(EnumActionResult.PASS, cube);
        }
        // 重复的术石不收、也不消耗
        if (PrimevalCubeData.has(cube, form)) {
            if (!world.isRemote) {
                player.sendStatusMessage(new TextComponentTranslation("message.eaddons.primeval_cube.duplicate",
                        offhand.getDisplayName()), true);
            }
            return new ActionResult<>(EnumActionResult.FAIL, cube);
        }
        if (!world.isRemote) {
            String name = offhand.getDisplayName();
            PrimevalCubeData.add(cube, form);
            if (!player.capabilities.isCreativeMode) {
                offhand.shrink(1);
            }
            int done = PrimevalCubeData.count(cube);
            int total = PrimevalCubeData.required().size();
            if (done >= total) {
                world.playSound(null, player.getPosition(), SoundEvents.BLOCK_END_PORTAL_SPAWN,
                        SoundCategory.PLAYERS, 0.8F, 1.2F);
                player.sendStatusMessage(new TextComponentTranslation("message.eaddons.primeval_cube.complete"), true);
            } else {
                world.playSound(null, player.getPosition(), SoundEvents.BLOCK_END_PORTAL_FRAME_FILL,
                        SoundCategory.PLAYERS, 1.0F, 0.8F + 0.05F * done);
                player.sendStatusMessage(new TextComponentTranslation("message.eaddons.primeval_cube.recorded",
                        name, done, total), true);
            }
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, cube);
    }

    // ============================ 掉落物：毁不掉、不消失 ============================

    @Override
    public int getEntityLifespan(ItemStack stack, World world) {
        return Integer.MAX_VALUE;
    }

    /** 无敌的掉落物挡住火、岩浆、爆炸、仙人掌；掉进虚空（OUT_OF_WORLD）仍会消失。 */
    @Override
    public boolean onEntityItemUpdate(EntityItem entityItem) {
        if (!entityItem.getIsInvulnerable()) {
            entityItem.setEntityInvulnerable(true);
        }
        return false;
    }

    // ============================ Tooltip ============================

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> list, ITooltipFlag flag) {
        List<SpellstoneForm> required = PrimevalCubeData.required();
        int done = PrimevalCubeData.count(stack);
        list.add(I18n.format("tooltip.eaddons.primeval_cube.progress", done, required.size()));
        if (done >= required.size()) {
            list.add(I18n.format("tooltip.eaddons.primeval_cube.complete"));
        } else {
            list.add(I18n.format("tooltip.eaddons.primeval_cube.hint"));
        }
        if (GuiScreen.isShiftKeyDown()) {
            StringBuilder recorded = new StringBuilder();
            StringBuilder missing = new StringBuilder();
            for (SpellstoneForm form : required) {
                Item stone = PrimevalCubeData.stoneOf(form);
                if (stone == null) {
                    continue;
                }
                StringBuilder target = PrimevalCubeData.has(stack, form) ? recorded : missing;
                if (target.length() > 0) {
                    target.append(TextFormatting.DARK_GRAY).append("、");
                }
                target.append(TextFormatting.GRAY).append(TextFormatting.getTextWithoutFormattingCodes(
                        new ItemStack(stone).getDisplayName()));
            }
            if (recorded.length() > 0) {
                list.add(I18n.format("tooltip.eaddons.primeval_cube.recorded") + recorded);
            }
            if (missing.length() > 0) {
                list.add(I18n.format("tooltip.eaddons.primeval_cube.missing") + missing);
            }
        } else {
            list.add(I18n.format("tooltip.eaddons.primeval_cube.hold_shift"));
        }
        list.add(I18n.format("tooltip.eaddons.primeval_cube.soulbound"));
    }
}
