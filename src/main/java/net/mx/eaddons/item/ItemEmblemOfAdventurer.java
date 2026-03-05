package net.mx.eaddons.item;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import keletu.enigmaticlegacy.EnigmaticLegacy;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

@Optional.Interface(iface = "baubles.api.IBauble", modid = "baubles")
public class ItemEmblemOfAdventurer extends Item implements IBauble {

    public static final ItemEmblemOfAdventurer INSTANCE = new ItemEmblemOfAdventurer();
    private static final UUID DAMAGE_UUID = UUID.fromString("6ec66ba8-46e7-487a-8bd1-82eeac5dd4ab");
    private static final UUID SPEED_UUID = UUID.fromString("131a7a69-ba19-47a7-9ad5-7ce2965a8d6b");

    public ItemEmblemOfAdventurer() {
        setMaxDamage(0);
        maxStackSize = 1;
        setUnlocalizedName("emblem_of_adventurer");
        setRegistryName("emblem_of_adventurer");
        setCreativeTab(EnigmaticLegacy.tabEnigmaticLegacy);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.UNCOMMON;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> list, ITooltipFlag flagIn) {
        list.add(TextFormatting.GRAY + net.minecraft.client.resources.I18n.format("tooltip.eaddons.emblem_adventurer.line1"));
        list.add(TextFormatting.GRAY + net.minecraft.client.resources.I18n.format("tooltip.eaddons.emblem_adventurer.line2"));
        if (worldIn != null && worldIn.getWorldInfo().isHardcoreModeEnabled()) {
            list.add(TextFormatting.DARK_PURPLE + net.minecraft.client.resources.I18n.format("tooltip.eaddons.emblem_adventurer.hardcore"));
            list.add(net.minecraft.client.resources.I18n.format("tooltip.eaddons.emblem_adventurer.howto"));
        }
    }

    @Override
    @Optional.Method(modid = "baubles")
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.TRINKET;
    }

    @Override
    @Optional.Method(modid = "baubles")
    public boolean canEquip(ItemStack itemstack, EntityLivingBase player) {
        if (!(player instanceof EntityPlayer)) return true;
        return BaublesApi.isBaubleEquipped((EntityPlayer) player, ItemInsigniaOfDespair.INSTANCE) == -1;
    }

    @Override
    @Optional.Method(modid = "baubles")
    public void onEquipped(ItemStack itemstack, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;
        if (entity.world.isRemote) return;
        if (!player.world.getWorldInfo().isHardcoreModeEnabled()) return;
        // Replace on next tick: Baubles may call onEquipped before the stack is in the slot.
        net.minecraft.server.MinecraftServer server = player.world.getMinecraftServer();
        if (server != null)
            server.addScheduledTask(() -> replaceWithInsigniaIfStillEquipped(player));
    }

    /** Runs next tick: find slot with this emblem and replace with Insignia of Despair. */
    private static void replaceWithInsigniaIfStillEquipped(EntityPlayer player) {
        if (player == null || player.world == null || player.world.isRemote) return;
        if (!player.world.getWorldInfo().isHardcoreModeEnabled()) return;
        int slot = BaublesApi.isBaubleEquipped(player, ItemEmblemOfAdventurer.INSTANCE);
        if (slot < 0) return;
        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        if (handler == null) return;
        handler.setStackInSlot(slot, new ItemStack(ItemInsigniaOfDespair.INSTANCE));
    }

    @Override
    @Optional.Method(modid = "baubles")
    public void onUnequipped(ItemStack itemstack, EntityLivingBase entity) {
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            removeModifier(player, SharedMonsterAttributes.ATTACK_DAMAGE, DAMAGE_UUID);
            removeModifier(player, SharedMonsterAttributes.ATTACK_SPEED, SPEED_UUID);
        }
    }

    @Override
    @Optional.Method(modid = "baubles")
    public void onWornTick(ItemStack stack, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer) || entity.world.isRemote) return;
        EntityPlayer player = (EntityPlayer) entity;
        // In Hardcore, replace this slot with Insignia of Despair (Baubles may not call onEquipped on server).
        if (player.world.getWorldInfo().isHardcoreModeEnabled()) {
            int slot = BaublesApi.isBaubleEquipped(player, INSTANCE);
            if (slot >= 0) {
                IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
                if (handler != null) {
                    handler.setStackInSlot(slot, new ItemStack(ItemInsigniaOfDespair.INSTANCE));
                    return; // slot now has insignia, no need to apply emblem modifiers
                }
            }
        }
        double damage = EmblemAdventurerConfig.adventurerAttackDamage;
        double speedMult = EmblemAdventurerConfig.adventurerAttackSpeedPercent / 100.0;
        ensureModifier(player, SharedMonsterAttributes.ATTACK_DAMAGE, DAMAGE_UUID, "Emblem Adventurer Damage", damage, 0);
        ensureModifier(player, SharedMonsterAttributes.ATTACK_SPEED, SPEED_UUID, "Emblem Adventurer Speed", speedMult, 2);
    }

    private static void ensureModifier(EntityPlayer player, net.minecraft.entity.ai.attributes.IAttribute attr, UUID uuid, String name, double amount, int operation) {
        IAttributeInstance inst = player.getEntityAttribute(attr);
        if (inst == null) return;
        AttributeModifier existing = inst.getModifier(uuid);
        if (existing == null) {
            inst.applyModifier(new AttributeModifier(uuid, name, amount, operation));
        }
    }

    private static void removeModifier(EntityPlayer player, net.minecraft.entity.ai.attributes.IAttribute attr, UUID uuid) {
        IAttributeInstance inst = player.getEntityAttribute(attr);
        if (inst != null) inst.removeModifier(uuid);
    }
}
