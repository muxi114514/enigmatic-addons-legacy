package net.mx.eaddons.item;

import com.google.common.collect.Multimap;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemShield;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketWindowProperty;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Map;
import java.util.WeakHashMap;

public class ForgerGemEventHandler {

    private static final EntityEquipmentSlot[] ARMOR_SLOTS = {
            EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST,
            EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET };

    private final Map<EntityPlayer, Integer> lastSetCost = new WeakHashMap<>();

    @SubscribeEvent
    public void onAnvilUpdate(AnvilUpdateEvent event) {
        EntityPlayer player = findAnvilPlayer(event);
        if (player == null) return;
        if (!ItemForgerGem.hasForgerGem(player)) return;
        if (!ItemForgerGem.hasRingEquipped(player)) return;

        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        if (!qualifiesForUnbreakable(player, left, right)) return;

        ItemStack result = left.copy();
        NBTTagCompound existingTag = result.getTagCompound();
        NBTTagCompound tag = existingTag != null ? existingTag.copy() : new NBTTagCompound();
        tag.setBoolean("Unbreakable", true);
        result.setTagCompound(tag);
        result.setRepairCost(result.getRepairCost() + 8);

        String name = event.getName();
        if (name != null && !name.isEmpty()) {
            result.setStackDisplayName(name);
        }

        event.setOutput(result);
        event.setCost(30);
    }

    @SubscribeEvent
    public void onAnvilRepair(AnvilRepairEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player != null && !player.world.isRemote && ItemForgerGem.hasForgerGem(player)) {
            event.setBreakChance(0F);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;
        EntityPlayer player = event.player;

        if (!ItemForgerGem.hasForgerGem(player)) {
            lastSetCost.remove(player);
            return;
        }

        if (!(player.openContainer instanceof ContainerRepair)) {
            lastSetCost.remove(player);
            return;
        }

        ContainerRepair anvil = (ContainerRepair) player.openContainer;
        int currentCost = anvil.maximumCost;

        if (currentCost <= 0) {
            lastSetCost.remove(player);
            return;
        }

        // 仅在"锻造不可破坏"操作时跳过减半（输入物品无Unbreakable → 输出有Unbreakable）
        // 已有Unbreakable的物品做附魔等操作时，仍享受经验减半
        ItemStack output = anvil.getSlot(2).getStack();
        if (!output.isEmpty()) {
            NBTTagCompound outputTag = output.getTagCompound();
            boolean outputUnbreakable = outputTag != null && outputTag.getBoolean("Unbreakable");

            if (outputUnbreakable) {
                ItemStack input = anvil.getSlot(0).getStack();
                NBTTagCompound inputTag = input.getTagCompound();
                boolean inputUnbreakable = !input.isEmpty() && inputTag != null && inputTag.getBoolean("Unbreakable");

                // 输入没有Unbreakable但输出有 → 正在执行锻造不可破坏，跳过减半
                if (!inputUnbreakable) {
                    lastSetCost.remove(player);
                    return;
                }
                // 输入已经有Unbreakable → 是对已有不可破坏物品的附魔操作，继续减半
            }
        }

        Integer lastSet = lastSetCost.get(player);
        if (lastSet == null || currentCost != lastSet) {
            int halved = (currentCost + 1) / 2;
            anvil.maximumCost = halved;
            lastSetCost.put(player, halved);

            if (player instanceof EntityPlayerMP) {
                ((EntityPlayerMP) player).connection.sendPacket(
                        new SPacketWindowProperty(anvil.windowId, 0, halved)
                );
            }
        }
    }

    /**
     * Finds the player whose open anvil container holds the same left input stack (by reference)
     * as the event. Works on both server (via player list) and client (via ForgerGemClientHelper).
     */
    private EntityPlayer findAnvilPlayer(AnvilUpdateEvent event) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null) {
            ItemStack eventLeft = event.getLeft();
            for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
                if (player.openContainer instanceof ContainerRepair) {
                    ContainerRepair anvil = (ContainerRepair) player.openContainer;
                    if (anvil.getSlot(0).getStack() == eventLeft) {
                        return player;
                    }
                }
            }
            return null;
        }

        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            return ForgerGemClientHelper.findPlayerForAnvil(event.getLeft());
        }
        return null;
    }

    private boolean qualifiesForUnbreakable(EntityPlayer player, ItemStack left, ItemStack right) {
        if (left.isEmpty() || right.isEmpty()) return false;
        if (left.getItem() != right.getItem()) return false;
        if (!left.isItemStackDamageable()) return false;
        if (left.getItemDamage() != 0 || right.getItemDamage() != 0) return false;
        if (left.isItemEnchanted() || right.isItemEnchanted()) return false;

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(left.getItem());
        if (itemId != null && ForgerGemConfig.isBlacklisted(itemId)) return false;

        if (ForgerGemConfig.strictUnbreakableForge && !player.capabilities.isCreativeMode) {
            if (!isForgeableEquipment(left)) return false;
        }

        return true;
    }

    /**
     * 判断物品是否属于"装备"（工具/武器/护甲）。
     * 原先用 instanceof ItemTool/ItemSword/ItemArmor 判定，模组武器大多继承自己的基类而非这三个，
     * 会被直接拒掉——这才是"锻造者宝石对模组武器无效"的根因。
     * 改为按能力判定：先走原版类型快速通道，再看 Forge 工具类别，最后看属性修饰符里有没有攻击力/护甲。
     */
    private static boolean isForgeableEquipment(ItemStack stack) {
        Item item = stack.getItem();

        // 1) 原版及其子类：工具、剑、护甲、弓、盾
        if (item instanceof ItemTool || item instanceof ItemSword || item instanceof ItemArmor
                || item instanceof ItemBow || item instanceof ItemShield) {
            return true;
        }

        // 2) Forge 工具类别（pickaxe/axe/shovel 等），模组工具基本都会注册
        if (!item.getToolClasses(stack).isEmpty()) {
            return true;
        }

        // 3) 属性修饰符：主手带攻击力的算武器，任一护甲位带护甲值/韧性的算护甲
        if (hasModifier(stack, EntityEquipmentSlot.MAINHAND, SharedMonsterAttributes.ATTACK_DAMAGE.getName())) {
            return true;
        }
        for (EntityEquipmentSlot slot : ARMOR_SLOTS) {
            if (hasModifier(stack, slot, SharedMonsterAttributes.ARMOR.getName())
                    || hasModifier(stack, slot, SharedMonsterAttributes.ARMOR_TOUGHNESS.getName())) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasModifier(ItemStack stack, EntityEquipmentSlot slot, String attributeName) {
        Multimap<String, AttributeModifier> modifiers = stack.getAttributeModifiers(slot);
        return modifiers != null && !modifiers.get(attributeName).isEmpty();
    }
}
