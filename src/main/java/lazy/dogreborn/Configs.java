package lazy.dogreborn;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.minecraftforge.common.ForgeConfigSpec;

import java.nio.file.Path;

public class Configs {

    public static final String CATEGORY_GENERAL = "dogreborn";

    private static final ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();

    public static ForgeConfigSpec COMMON_CONFIG;

    public static ForgeConfigSpec.IntValue MAX_LIVES;
    public static ForgeConfigSpec.ConfigValue<String> ITEM_REG_NAME;

    static {
        COMMON_BUILDER.comment("Dog Reborn Configs").push(CATEGORY_GENERAL);

        MAX_LIVES = COMMON_BUILDER.comment("Max lives that each dog can get.").defineInRange("max_lives", 5, 1, Integer.MAX_VALUE);
        ITEM_REG_NAME = COMMON_BUILDER.comment("Item registry name given to the dog to add more lives.").define("item_registry_name", "minecraft:diamond");

        COMMON_BUILDER.pop();

        COMMON_CONFIG = COMMON_BUILDER.build();
    }

    public static void load(ForgeConfigSpec spec, Path path) {
        final CommentedFileConfig configData = CommentedFileConfig.builder(path).sync().autosave().writingMode(WritingMode.REPLACE).build();
        configData.load();
        spec.setConfig(configData);
    }
}
