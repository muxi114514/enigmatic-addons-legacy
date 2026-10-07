package net.mx.eaddons.item;

import keletu.enigmaticlegacy.api.cap.IForbiddenConsumed;
import keletu.enigmaticlegacy.event.SuperpositionHandler;
import keletu.enigmaticlegacy.item.EAddonsSpellstoneBauble;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.EnchantmentFrostWalker;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.mx.eaddons.compat.IceAndFireCompat;
import net.mx.eaddons.compat.ModCompat;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 忘却冰晶：移植自 1.20 神遗拓展 ForgottenIce，按 1.12.2 实际可用的能力重新设计。
 *
 * <p>与 1.20 的差异及原因：原版整套机制建立在 1.17 的细雪冻结（{@code ticksFrozen} /
 * {@code isFullyFrozen} / {@code DamageTypes.FREEZE}）之上，1.12.2 一样都没有，
 * 故冻结改为本模组自建（见 {@link FrostHelper}），并去掉了依赖「累计冻结秒数」的主动技能增伤
 * 与 1.19 才有的音爆抗性。
 *
 * <p>注册名随 EL 咒石基类落在 enigmaticlegacy 域，模型由 EAddonsMod 单独指到 eaddons。
 */
public class ItemForgottenIce extends EAddonsSpellstoneBauble {
    public static final ItemForgottenIce INSTANCE = new ItemForgottenIce();

    /**
     * 主动技能的冻结伤害：无视护甲，对应 1.20 的 FREEZE。
     * <p>用 {@link EntityDamageSource} 而非裸 {@link DamageSource}：后者不带攻击者，
     * 怪物不会仇恨佩戴者、击杀也不算玩家击杀（不掉经验、不走抢夺）。
     */
    public static DamageSource frostDamage(EntityPlayer attacker) {
        return new EntityDamageSource("eaddons.frost", attacker).setDamageBypassesArmor().setMagicDamage();
    }

    public ItemForgottenIce() {
        super("forgotten_ice", EnumRarity.RARE);

        // 冰龙龙息全免疫。冰与火的龙息就是 new DamageSource("dragon_ice")，
        // EL 的 EnigmaticEvents 会消费 immunityList，无需自己写事件。
        this.immunityList.add("dragon_ice");
        // resistanceList 按 amount×系数 应用，>1 即易伤。摔落只有一个 damageType，走这里最省事；
        // 火焰与弹射物的 damageType 太多（模组还会自造），统一在事件里按 isFireDamage / isProjectile 判。
        this.resistanceList.put(DamageSource.FALL.damageType, () -> (float) ForgottenIceConfig.fallVulnerability);
    }

    public int getCooldown(EntityPlayer player) {
        return ForgottenIceConfig.cooldown;
    }

    // ============================ 主动技能（按咒石键触发） ============================

    @Override
    public void triggerActiveAbility(World world, EntityPlayerMP player, ItemStack stack) {
        if (IForbiddenConsumed.get(player).getSpellstoneCooldown() > 0) {
            return;
        }

        float damage = (float) (ForgottenIceConfig.abilityDamageBase
                + player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue());

        List<EntityLivingBase> targets = world.getEntitiesWithinAABB(EntityLivingBase.class,
                SuperpositionHandler.getBoundingBoxAroundEntity(player, ForgottenIceConfig.abilityRadius));
        for (EntityLivingBase target : targets) {
            if (target == player) {
                continue;
            }
            FrostHelper.applyFrost(target,
                    FrostHelper.frostTimeFor(target, ForgottenIceConfig.abilityFrostTime));
            target.attackEntityFrom(frostDamage(player), damage);
            spawnFrostParticles(world, target);
        }

        world.playSound(null, player.getPosition(), SoundEvents.BLOCK_GLASS_BREAK,
                SoundCategory.PLAYERS, 1.0F, 0.6F);
        IForbiddenConsumed.get(player).setSpellstoneCooldown(getCooldown(player));
    }

    /** 碎冰视觉。只发粒子包，不读写方块，无区块风险。 */
    private void spawnFrostParticles(World world, EntityLivingBase target) {
        if (!(world instanceof WorldServer)) {
            return;
        }
        float width = target.width / 1.8F;
        IBlockState ice = Blocks.PACKED_ICE.getDefaultState();
        ((WorldServer) world).spawnParticle(EnumParticleTypes.BLOCK_CRACK,
                target.posX, target.posY + target.height * 0.5D, target.posZ,
                16, width, 0.1D, width, 0.0D,
                net.minecraft.block.Block.getStateId(ice));
        ((WorldServer) world).spawnParticle(EnumParticleTypes.SNOWBALL,
                target.posX, target.posY + target.height * 0.5D, target.posZ,
                10, width, 0.1D, width, 0.0D);
    }

    // ============================ 佩戴被动 ============================

    @Override
    public void onWornTick(ItemStack stack, EntityLivingBase living) {
        super.onWornTick(stack, living);
        if (living.world.isRemote || !(living instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) living;

        // 清理层：mixin 已在冰与火的 tickUpdate 入口拦截，这里兜底处理 mixin 未生效（版本漂移）的情况
        if (ModCompat.ICE_AND_FIRE && !FrostHelper.hasFrost(player)) {
            IceAndFireCompat.clearFrost(player);
        }

        applyFrostWalker(player);
    }

    /**
     * 冰霜行者：直接复用原版附魔的 freezeNearby，无需真往靴子上塞附魔。
     * <p>该方法会 setBlockState，按项目规范先确认作用半径覆盖的区块都已在内存中，
     * 否则 getBlockState 会触发同步加载甚至生成，卡住服务端主线程。
     */
    private void applyFrostWalker(EntityPlayer player) {
        int level = ForgottenIceConfig.frostWalkerLevel;
        if (level <= 0 || !player.onGround) {
            return;
        }
        int radius = level + 2;
        int x = MathHelper.floor(player.posX);
        int z = MathHelper.floor(player.posZ);
        for (int cx = (x - radius) >> 4; cx <= (x + radius) >> 4; cx++) {
            for (int cz = (z - radius) >> 4; cz <= (z + radius) >> 4; cz++) {
                Chunk chunk = player.world.getChunkProvider().getLoadedChunk(cx, cz);
                if (chunk == null) {
                    return;
                }
            }
        }
        EnchantmentFrostWalker.freezeNearby(player, player.world, new BlockPos(player), level);
    }

    // ============================ Tooltip ============================

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> list, ITooltipFlag flagIn) {
        list.add("");
        if (GuiScreen.isShiftKeyDown()) {
            list.add(TextFormatting.AQUA + I18n.format("tooltip.eaddons.forgotten_ice.desc1"));
            list.add(TextFormatting.AQUA + I18n.format("tooltip.eaddons.forgotten_ice.desc2"));
            list.add(TextFormatting.AQUA + I18n.format("tooltip.eaddons.forgotten_ice.desc3"));
            list.add(TextFormatting.AQUA + I18n.format("tooltip.eaddons.forgotten_ice.desc4",
                    ForgottenIceConfig.frozenDamageBonus));
            list.add(TextFormatting.AQUA + I18n.format("tooltip.eaddons.forgotten_ice.desc5",
                    ForgottenIceConfig.projectileResistance));
            list.add(TextFormatting.RED + I18n.format("tooltip.eaddons.forgotten_ice.desc6",
                    ForgottenIceConfig.fireVulnerability, ForgottenIceConfig.fallVulnerability));
            list.add(TextFormatting.GOLD + I18n.format("tooltip.eaddons.forgotten_ice.cooldown",
                    ForgottenIceConfig.cooldown / 20.0F));
        } else {
            list.add(TextFormatting.GRAY + I18n.format("tooltip.eaddons.forgotten_ice.brief"));
            list.add(I18n.format("tooltip.eaddons.forgotten_ice.hold_shift"));
        }
        list.add("");
    }
}
