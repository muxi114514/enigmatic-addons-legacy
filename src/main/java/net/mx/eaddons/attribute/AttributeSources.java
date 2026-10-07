package net.mx.eaddons.attribute;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.ToDoubleFunction;

import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 属性来源登记表：每个来源 = 「某属性上的一个修饰符 + 计算其数值的函数」。
 * <p>服务端每隔 {@code interval} tick 对每名玩家求值并对账（见 {@link ModifierSync}），
 * 客户端经属性同步拿到结果。附属模组可调用 {@link #register} 追加自己的来源。
 */
public final class AttributeSources {

    private static final Logger LOG = LogManager.getLogger("eaddons");
    private static final List<Source> SOURCES = new CopyOnWriteArrayList<>();
    /** 已报过错的来源，同一来源只打一次日志，避免每 tick 刷屏 */
    private static final Set<String> FAILED = ConcurrentHashMap.newKeySet();

    private AttributeSources() {
    }

    /**
     * 登记一个来源。
     *
     * @param key       全局唯一的来源名（用于生成修饰符 UUID，改名会让旧修饰符残留到下次重算）
     * @param operation 0 加值 / 1 乘基础值 / 2 乘总值
     * @param interval  求值间隔（tick），≥1
     * @param amount    返回该玩家此刻应有的修饰值，0 表示没有
     */
    public static void register(String key, IAttribute attribute, int operation, int interval,
                                ToDoubleFunction<EntityPlayer> amount) {
        UUID id = UUID.nameUUIDFromBytes(("eaddons:attribute_source/" + key).getBytes(StandardCharsets.UTF_8));
        SOURCES.add(new Source(key, attribute, id, operation, Math.max(1, interval), amount));
    }

    /** 服务端玩家 tick 末尾求值 */
    public static final class Ticker {
        @SubscribeEvent
        public void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
                return;
            }
            EntityPlayer player = event.player;
            int tick = player.ticksExisted;
            for (Source source : SOURCES) {
                if (tick % source.interval == 0) {
                    source.update(player);
                }
            }
        }
    }

    private static final class Source {
        final String key;
        final IAttribute attribute;
        final UUID id;
        final int operation;
        final int interval;
        final ToDoubleFunction<EntityPlayer> amount;

        Source(String key, IAttribute attribute, UUID id, int operation, int interval,
               ToDoubleFunction<EntityPlayer> amount) {
            this.key = key;
            this.attribute = attribute;
            this.id = id;
            this.operation = operation;
            this.interval = interval;
            this.amount = amount;
        }

        void update(EntityPlayer player) {
            double value;
            try {
                value = amount.applyAsDouble(player);
            } catch (Throwable t) {
                // 单个来源出错不能拖垮整个 tick；该来源视为 0
                if (FAILED.add(key)) {
                    LOG.error("Attribute source '" + key + "' failed, treated as 0", t);
                }
                value = 0;
            }
            ModifierSync.apply(player, attribute, id, "eaddons:" + key, value, operation);
        }
    }
}
