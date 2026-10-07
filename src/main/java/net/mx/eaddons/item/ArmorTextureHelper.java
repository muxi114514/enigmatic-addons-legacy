package net.mx.eaddons.item;

import net.minecraft.inventory.EntityEquipmentSlot;

/**
 * 以太／极恶护甲的动画贴图解析。
 * <p>1.12.2 的护甲层贴图是独立纹理（{@code SimpleTexture}），只读 texture 元数据段、不处理 animation，
 * 因此无法像物品图标那样用 .mcmeta 播放动画。这里改为每帧由 {@code getArmorTexture} 返回不同文件，
 * 帧序复刻以太锭的呼吸曲线（frametime 2，两端各停一拍、最亮处停三拍）。
 * <p>所有路径在类加载时预先拼好，渲染循环中只做数组寻址，不产生临时对象。
 */
public final class ArmorTextureHelper {
    private ArmorTextureHelper() {
    }

    /** 套装贴图前缀。 */
    public static final String SET_ETHERIUM = "etherium";
    public static final String SET_EVIL = "evil";

    private static final int FRAME_COUNT = 6;
    /** 每帧持续的游戏刻，与以太锭的 frametime 一致。 */
    private static final int FRAME_TIME = 2;
    /** 呼吸帧序：亮到顶后停三拍再回落，取自以太锭的 animation.frames。 */
    private static final int[] SEQUENCE = {0, 1, 2, 3, 4, 5, 5, 5, 4, 3, 2, 1, 0, 0};

    // [层(0=layer_1, 1=layer_2)][帧] —— 预先拼好，避免每帧拼字符串
    private static final String[][] ETHERIUM_PATHS = build(SET_ETHERIUM);
    private static final String[][] EVIL_PATHS = build(SET_EVIL);

    private static String[][] build(String set) {
        String[][] paths = new String[2][FRAME_COUNT];
        for (int layer = 0; layer < 2; layer++) {
            for (int frame = 0; frame < FRAME_COUNT; frame++) {
                paths[layer][frame] = "eaddons:textures/models/armor/"
                        + set + "_layer_" + (layer + 1) + "_" + frame + ".png";
            }
        }
        return paths;
    }

    /**
     * 取当前帧的贴图路径。
     *
     * @param set       {@link #SET_ETHERIUM} 或 {@link #SET_EVIL}
     * @param slot      装备槽；护腿走 layer_2，其余走 layer_1
     * @param worldTime 世界时间（用于推进动画）
     */
    public static String getTexture(String set, EntityEquipmentSlot slot, long worldTime) {
        int layer = (slot == EntityEquipmentSlot.LEGS) ? 1 : 0;
        int step = (int) (Math.floorMod(worldTime / FRAME_TIME, SEQUENCE.length));
        int frame = SEQUENCE[step];
        return (SET_EVIL.equals(set) ? EVIL_PATHS : ETHERIUM_PATHS)[layer][frame];
    }
}
