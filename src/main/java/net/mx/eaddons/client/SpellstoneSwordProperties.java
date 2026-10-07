package net.mx.eaddons.client;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.mx.eaddons.item.ItemSpellstoneSword;
import net.mx.eaddons.spellstone.SpellstoneData;

import javax.annotation.Nullable;

/**
 * 术质共鸣者的模型属性：按共鸣形态换贴图，取值 index/10，与模型 json 的 overrides 对应。
 *
 * <p>单独放在客户端包里，因为 {@code Item#addPropertyOverride} 与 {@code IItemPropertyGetter}
 * 都是 {@code @SideOnly(Side.CLIENT)}，在专用服务器上碰到就会 NoClassDefFound。
 * 由 {@code EAddonsMod#registerModels}（ModelRegistryEvent，本身就只在客户端触发）调用。
 */
@SideOnly(Side.CLIENT)
public final class SpellstoneSwordProperties {

    private SpellstoneSwordProperties() {
    }

    public static void register() {
        ItemSpellstoneSword.INSTANCE.addPropertyOverride(new ResourceLocation("form"),
                new net.minecraft.item.IItemPropertyGetter() {
                    @Override
                    @SideOnly(Side.CLIENT)
                    public float apply(ItemStack stack, @Nullable World world, @Nullable EntityLivingBase entity) {
                        // 用显示形态：原初共鸣切到子形态时，贴图仍然是原初立方
                        return SpellstoneData.getDisplayForm(stack).index() / 10.0F;
                    }
                });
    }
}
