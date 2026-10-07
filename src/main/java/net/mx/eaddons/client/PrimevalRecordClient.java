package net.mx.eaddons.client;

import net.minecraft.client.Minecraft;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 原初立方记录界面的客户端入口。消息处理器只调这里，客户端类不会出现在服务端会加载的类里。 */
@SideOnly(Side.CLIENT)
public final class PrimevalRecordClient {

    private PrimevalRecordClient() {
    }

    public static void open(List<String> ids, List<Integer> bases) {
        Minecraft mc = Minecraft.getMinecraft();
        // 网络线程收到，切回客户端主线程再开界面
        mc.addScheduledTask(() -> {
            Map<Potion, Integer> record = new LinkedHashMap<>();
            for (int i = 0; i < ids.size(); i++) {
                Potion potion = ForgeRegistries.POTIONS.getValue(new ResourceLocation(ids.get(i)));
                if (potion != null) {
                    record.put(potion, bases.get(i));
                }
            }
            mc.displayGuiScreen(new GuiPrimevalRecord(record));
        });
    }
}
