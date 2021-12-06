package lazy.dogreborn;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class Utils {

    public static BlockPos getPlayerSpawnPos(Player entity) {
        int x = entity.getPersistentData().getInt("SpawnX");
        int y = entity.getPersistentData().getInt("SpawnY");
        int z = entity.getPersistentData().getInt("SpawnZ");
        return new BlockPos(x, y, z);
    }

    public static Vec3 readVector3i(CompoundTag nbt) {
        return new Vec3(nbt.getDouble("x"), nbt.getDouble("y"), nbt.getDouble("z"));
    }

    public static CompoundTag writeBlockPos(BlockPos blockPos) {
        CompoundTag nbt = new CompoundTag();
        nbt.putDouble("x", blockPos.getX());
        nbt.putDouble("y", blockPos.getY());
        nbt.putDouble("z", blockPos.getZ());
        return nbt;
    }
}
