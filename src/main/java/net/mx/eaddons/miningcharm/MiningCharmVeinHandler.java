package net.mx.eaddons.miningcharm;

import baubles.api.BaublesApi;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.mx.eaddons.compat.MoreModVeinCompat;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 猎宝者护符连锁挖掘：佩戴护符、按住连锁键挖掉一个方块时，连带挖掉与之相连的同种方块。
 * HIGH：晚于 MX 原点保护（HIGHEST，被取消的不会进来），早于 moremod 范围挖掘（NORMAL），矿先按时运挖走。
 * 起点方块此时还在，它自己的挖掘、扣耐久在本事件之后由原版照常完成。
 */
public class MiningCharmVeinHandler {
    private static final ResourceLocation MINING_CHARM = new ResourceLocation("enigmaticlegacy", "mining_charm");

    private Item charm;
    /** 正在连锁的玩家：连锁里每一块都会再发 BreakEvent，靠它防止递归。 */
    private final Set<UUID> chaining = ConcurrentHashMap.newKeySet();
    /** 连锁期间生成的掉落物与经验球挪到这里；gatherWorld 为 null 表示不在收集。只在服务端主线程读写。 */
    private World gatherWorld;
    private Vec3d gatherPos;

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!MiningCharmConfig.veinMiningEnabled || !(event.getPlayer() instanceof EntityPlayerMP)
                || event.getPlayer() instanceof FakePlayer) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.getPlayer();
        World world = event.getWorld();
        BlockPos start = event.getPos();
        IBlockState state = event.getState();
        if (world.isRemote || this.chaining.contains(player.getUniqueID()) || !VeinKeyState.isDown(player)
                || !this.hasCharm(player) || !this.canStart(player, world, start, state)) {
            return;
        }
        List<BlockPos> targets = VeinSearch.find(world, start, state, player, MiningCharmConfig.veinMaxBlocks);
        if (!targets.isEmpty()) {
            this.chain(player, world, start, targets);
        }
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        World world = event.getWorld();
        if (world.isRemote || world != this.gatherWorld) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity instanceof EntityItem || entity instanceof EntityXPOrb) {
            entity.setPosition(this.gatherPos.x, this.gatherPos.y, this.gatherPos.z);
        }
    }

    /** 起点要能用手上的工具采集（徒手敲铁矿不连锁），且本身不在黑名单。 */
    private boolean canStart(EntityPlayerMP player, World world, BlockPos pos, IBlockState state) {
        if (!VeinSearch.isMineable(world, pos, state) || !VeinHarvester.isChainable(state.getBlock())) {
            return false;
        }
        return player.isCreative() || state.getBlock().canHarvestBlock(world, pos, player);
    }

    private void chain(EntityPlayerMP player, World world, BlockPos start, List<BlockPos> targets) {
        UUID id = player.getUniqueID();
        this.chaining.add(id);
        boolean moremodSuppressed = MoreModVeinCompat.suppress(id);
        World prevWorld = this.gatherWorld;
        Vec3d prevPos = this.gatherPos;
        if (MiningCharmConfig.veinGatherDrops) {
            this.gatherWorld = world;
            this.gatherPos = new Vec3d(start).addVector(0.5, 0.25, 0.5);
        }
        try {
            for (BlockPos pos : targets) {
                // 门、床等联动方块可能已随前一块一起消失
                if (!world.isAirBlock(pos)) {
                    VeinHarvester.harvest(player, world, pos);
                }
            }
        } finally {
            this.gatherWorld = prevWorld;
            this.gatherPos = prevPos;
            MoreModVeinCompat.release(id, moremodSuppressed);
            this.chaining.remove(id);
        }
    }

    private boolean hasCharm(EntityPlayer player) {
        if (this.charm == null) {
            this.charm = ForgeRegistries.ITEMS.getValue(MINING_CHARM);
        }
        // 物品注册表查不到时返回空气而不是 null
        if (this.charm == null || this.charm == Items.AIR) {
            return false;
        }
        return BaublesApi.isBaubleEquipped(player, this.charm) != -1;
    }
}
