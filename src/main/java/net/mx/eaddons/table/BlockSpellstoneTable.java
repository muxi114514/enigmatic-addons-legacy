package net.mx.eaddons.table;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.mx.eaddons.EAddonsMod;
import net.mx.eaddons.item.AntiqueBagGuiHandler;

import java.util.Random;

/**
 * 术石工作台方块。右键打开 {@link ContainerSpellstoneTable}。
 *
 * <p>1.20.1 那边挂了个纯客户端的 BlockEntity，用来画台上漂浮旋转的术石立方体；
 * 1.12.2 要还原得写 TESR，与合成功能无关，故本移植略去，只保留附魔台同款的外形与粒子。
 */
public class BlockSpellstoneTable extends Block {
    public static final BlockSpellstoneTable INSTANCE = new BlockSpellstoneTable();
    /** 对应的方块物品，与其它物品一起在 Register&lt;Item&gt; 里注册。 */
    public static final net.minecraft.item.ItemBlock ITEM_BLOCK =
            (net.minecraft.item.ItemBlock) new net.minecraft.item.ItemBlock(INSTANCE)
                    .setRegistryName("spellstone_table");

    /** 与附魔台同高，0.75 格。 */
    private static final AxisAlignedBB SHAPE =
            new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.75D, 1.0D);

    public BlockSpellstoneTable() {
        super(Material.ROCK);
        setRegistryName("spellstone_table");
        setUnlocalizedName("spellstone_table");
        setCreativeTab(CreativeTabs.DECORATIONS);
        setHardness(5.0F);
        setResistance(2000.0F);
        setSoundType(SoundType.STONE);
        setLightLevel(0.5F);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, net.minecraft.world.IBlockAccess world, BlockPos pos) {
        return SHAPE;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            player.openGui(EAddonsMod.instance, AntiqueBagGuiHandler.SPELLSTONE_TABLE_GUI_ID,
                    world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        if (rand.nextInt(16) == 0) {
            world.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                    pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 0.0D, 0.0D, 0.0D);
        }
    }
}
