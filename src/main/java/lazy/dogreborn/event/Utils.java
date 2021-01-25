package lazy.dogreborn.event;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.vector.Vector3i;

public class Utils {

    public static Vector3i getPlayerSpawnPos(PlayerEntity entity){
        System.out.println(entity.getPersistentData());
        int x = entity.getPersistentData().getInt("SpawnX");
        int y = entity.getPersistentData().getInt("SpawnY");
        int z = entity.getPersistentData().getInt("SpawnZ");
        System.out.println(String.format("x %s, y %s, z %s", x, y, z));
        return new Vector3i(x, y, z);
    }

    public static Vector3i readVector3i(CompoundNBT nbt){
        return new Vector3i(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"));
    }

    public static CompoundNBT writeVector3i(Vector3i vector3i){
        CompoundNBT nbt = new CompoundNBT();
        nbt.putInt("x", vector3i.getX());
        nbt.putInt("y", vector3i.getY());
        nbt.putInt("z", vector3i.getZ());
        return nbt;
    }
}
