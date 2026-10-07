package net.mx.eaddons.core;

import java.util.Map;

import fermiumbooter.FermiumRegistryAPI;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

/**
 * 本模组的 coremod 入口，唯一职责：向前置 FermiumBooter 注册 mixin 配置。
 * <p>不做任何 ASM 变换，也不得引用任何 Minecraft 类——coremod 在 MC 类加载前构造。
 * mixin 目标见 {@code mixins.eaddons.json}。
 */
@IFMLLoadingPlugin.Name("EAddonsCore")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.SortingIndex(1001)
public class EAddonsCorePlugin implements IFMLLoadingPlugin {

    public EAddonsCorePlugin() {
        // false = early mixin（在 MC 类加载前应用）
        FermiumRegistryAPI.enqueueMixin(false, "mixins.eaddons.json");
        // 冰与火接缝（忘却冰晶的冻结）。coremod 阶段还不能用 Loader 判模组，
        // 故该配置设了 required=false，缺冰与火时由 Mixin 自行跳过。
        FermiumRegistryAPI.enqueueMixin(false, "mixins.eaddons.iceandfire.json");
        // 精英怪接缝（非欧领域拦 Ender 词条的自写瞬移）。
        // 这一份必须是 late（第一个参数 true）：early 阶段 Mixin 就会去读 MM_Ender 的字节码，
        // 而此时精英怪的 jar 还没进 LaunchClassLoader，找不到类之后这个类名会被记进
        // invalidClasses，等精英怪自己 preInit 加载它时直接 NoClassDefFoundError 崩服。
        // 同包里的 RLCraftLuckified 打同一个模组时也是走 late。
        FermiumRegistryAPI.enqueueMixin(true, "mixins.eaddons.infernalmobs.json");
        // 打在原版类上的 mixin。本工程不生成 refmap，目标方法名写 SRG，
        // 万一版本不匹配也只是该功能失效（required=false），不致崩游戏。
        FermiumRegistryAPI.enqueueMixin(false, "mixins.eaddons.vanilla.json");
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
