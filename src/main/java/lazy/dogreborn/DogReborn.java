package lazy.dogreborn;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod("dogreborn")
public class DogReborn {

    public DogReborn() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Configs.COMMON_CONFIG);
        Configs.load(Configs.COMMON_CONFIG, FMLPaths.CONFIGDIR.get().resolve("dogreborn-common.toml"));
    }
}
