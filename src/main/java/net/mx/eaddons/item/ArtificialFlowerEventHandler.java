package net.mx.eaddons.item;

import com.google.common.collect.Multimap;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.NonNullList;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

public class ArtificialFlowerEventHandler {

    public static final Map<EntityPlayerMP, Map<Multimap<String, AttributeModifier>, Integer>> PLAYER_ATTRIBUTE_MAP =
            new HashMap<>();

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            PLAYER_ATTRIBUTE_MAP.remove((EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public void onPotionApplicable(PotionEvent.PotionApplicableEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        PotionEffect effect = event.getPotionEffect();

        List<ItemStack> flowers = getAllFlowers(player);
        for (ItemStack flower : flowers) {
            Potion immune = ItemArtificialFlower.Helper.getEffect(flower, 1);
            if (immune != null && immune == effect.getPotion()) {
                event.setResult(Event.Result.DENY);
                return;
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Iterator<Map.Entry<EntityPlayerMP, Map<Multimap<String, AttributeModifier>, Integer>>> playerIter =
                PLAYER_ATTRIBUTE_MAP.entrySet().iterator();
        while (playerIter.hasNext()) {
            Map.Entry<EntityPlayerMP, Map<Multimap<String, AttributeModifier>, Integer>> playerEntry = playerIter.next();
            EntityPlayerMP player = playerEntry.getKey();
            Map<Multimap<String, AttributeModifier>, Integer> tickMap = playerEntry.getValue();

            if (tickMap.isEmpty()) continue;

            Iterator<Map.Entry<Multimap<String, AttributeModifier>, Integer>> attrIter = tickMap.entrySet().iterator();
            while (attrIter.hasNext()) {
                Map.Entry<Multimap<String, AttributeModifier>, Integer> entry = attrIter.next();
                Multimap<String, AttributeModifier> attrMap = entry.getKey();
                int tick = entry.getValue();

                if (tick <= 1) {
                    removeAttributeModifiers(player, attrMap);
                    attrIter.remove();
                } else {
                    applyAttributeModifiers(player, attrMap);
                    entry.setValue(tick - 1);
                }
            }
        }
    }

    private void applyAttributeModifiers(EntityPlayer player, Multimap<String, AttributeModifier> attrMap) {
        for (Map.Entry<String, AttributeModifier> entry : attrMap.entries()) {
            IAttributeInstance inst = player.getAttributeMap().getAttributeInstanceByName(entry.getKey());
            if (inst != null && inst.getModifier(entry.getValue().getID()) == null) {
                inst.applyModifier(entry.getValue());
            }
        }
    }

    private void removeAttributeModifiers(EntityPlayer player, Multimap<String, AttributeModifier> attrMap) {
        for (Map.Entry<String, AttributeModifier> entry : attrMap.entries()) {
            IAttributeInstance inst = player.getAttributeMap().getAttributeInstanceByName(entry.getKey());
            if (inst != null) {
                inst.removeModifier(entry.getValue());
            }
        }
    }

    private List<ItemStack> getAllFlowers(EntityPlayer player) {
        List<ItemStack> flowers = new ArrayList<>();
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack.getItem() instanceof ItemArtificialFlower) {
                flowers.add(stack);
            }
        }
        for (ItemStack stack : player.inventory.offHandInventory) {
            if (stack.getItem() instanceof ItemArtificialFlower) {
                flowers.add(stack);
            }
        }
        if (ItemAntiqueBag.hasBag(player)) {
            NonNullList<ItemStack> bagInv = ItemAntiqueBag.getInventory(player);
            for (int i = 0; i < 2 && i < bagInv.size(); i++) {
                ItemStack stack = bagInv.get(i);
                if (stack.getItem() instanceof ItemArtificialFlower) {
                    flowers.add(stack);
                }
            }
        }
        return flowers;
    }
}
