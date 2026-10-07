package net.mx.eaddons.primeval;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.mx.eaddons.spellstone.SpellstoneForm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 原初立方（进度物品）上记录了哪些术石。存的是形态名（{@link SpellstoneForm#path()}），不随注册名变动。
 * <p>收集清单直接取共鸣者的形态表：以后新增术石自动纳入。原初立方术石本身将来若成为共鸣形态，要在
 * {@link #required()} 里排除掉它。
 */
public final class PrimevalCubeData {
    private static final String TAG = "PrimevalRecorded";

    private static List<SpellstoneForm> required;

    private PrimevalCubeData() {
    }

    /** 需要集齐的形态。按注册表过滤掉不存在的术石，注册表就绪后缓存。 */
    public static synchronized List<SpellstoneForm> required() {
        if (required == null) {
            List<SpellstoneForm> list = new ArrayList<>();
            for (SpellstoneForm form : SpellstoneForm.values()) {
                // 两颗立方本身是共鸣形态、但不属于要收集的术石，排除掉
                if (form != SpellstoneForm.NONE && !form.isLink() && form.itemId() != null
                        && ForgeRegistries.ITEMS.containsKey(form.itemId())) {
                    list.add(form);
                }
            }
            if (list.isEmpty()) {
                return Collections.emptyList();   // 注册表还没就绪，先不缓存
            }
            required = Collections.unmodifiableList(list);
        }
        return required;
    }

    public static boolean has(ItemStack cube, SpellstoneForm form) {
        NBTTagCompound nbt = cube.getTagCompound();
        if (nbt == null) {
            return false;
        }
        NBTTagList list = nbt.getTagList(TAG, Constants.NBT.TAG_STRING);
        for (int i = 0; i < list.tagCount(); i++) {
            if (form.path().equals(list.getStringTagAt(i))) {
                return true;
            }
        }
        return false;
    }

    public static void add(ItemStack cube, SpellstoneForm form) {
        if (has(cube, form)) {
            return;
        }
        NBTTagCompound nbt = cube.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            cube.setTagCompound(nbt);
        }
        NBTTagList list = nbt.getTagList(TAG, Constants.NBT.TAG_STRING);
        list.appendTag(new NBTTagString(form.path()));
        nbt.setTag(TAG, list);
    }

    /** 已记录且仍在清单里的数量。 */
    public static int count(ItemStack cube) {
        int n = 0;
        for (SpellstoneForm form : required()) {
            if (has(cube, form)) {
                n++;
            }
        }
        return n;
    }

    public static boolean isComplete(ItemStack cube) {
        List<SpellstoneForm> list = required();
        return !list.isEmpty() && count(cube) >= list.size();
    }

    /** 集满的原初立方，供 JEI 展示配方。 */
    public static ItemStack completeStack() {
        ItemStack stack = new ItemStack(ItemPrimevalCube.INSTANCE);
        for (SpellstoneForm form : required()) {
            add(stack, form);
        }
        return stack;
    }

    /** 形态对应的术石物品，用于 tooltip 与提示取名。 */
    public static Item stoneOf(SpellstoneForm form) {
        return ForgeRegistries.ITEMS.getValue(form.itemId());
    }
}
