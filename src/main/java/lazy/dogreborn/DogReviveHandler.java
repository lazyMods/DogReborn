package lazy.dogreborn;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DogReviveHandler {

    public static final String TAG_DOG_REVIVE = "dog_revive";
    public static final String TAG_SPAWN_POINT = "SpawnPoint";
    public static final String TAG_LIVES = "NumOfLives";

    public static final Item LIVE_ITEM = ForgeRegistries.ITEMS.getValue(new ResourceLocation(Configs.ITEM_REG_NAME.get()));
    public static final int MAX_LIVES = Configs.MAX_LIVES.get();

    @SubscribeEvent
    public static void onSetSpawnEvent(PlayerSetSpawnEvent e) {
        ServerPlayer playerEntity = (ServerPlayer) e.getEntityLiving();
        ServerLevel world = (ServerLevel) playerEntity.level;

        List<Wolf> tamedWolfs = world.getEntities(EntityType.WOLF, DogReviveHandler::predicate).stream()
                .map(entity -> (Wolf) entity)
                .filter(Wolf -> Wolf.is(playerEntity))
                .filter(Wolf -> Wolf.getPersistentData().contains(TAG_DOG_REVIVE))
                .collect(Collectors.toList());

        for (Wolf tamedWolf : tamedWolfs) {
            CompoundTag nbt = getData(tamedWolf);
            nbt.put(TAG_SPAWN_POINT, Utils.writeBlockPos(e.getNewSpawn()));
            tamedWolf.getPersistentData().put(TAG_DOG_REVIVE, nbt);
        }
    }

    @SubscribeEvent
    public static void onInteractEvent(PlayerInteractEvent.EntityInteractSpecific e) {
        if (e.getSide().isServer()) {
            if (e.getTarget() instanceof Wolf wolf) {
                ServerPlayer playerEntity = (ServerPlayer) e.getPlayer();
                if (wolf.isTame() && wolf.getPersistentData().contains(TAG_DOG_REVIVE)) {
                    if (e.getItemStack().getItem() == LIVE_ITEM) {
                        boolean successInteract = onInteract(wolf);
                        if (successInteract) {
                            e.getItemStack().shrink(1);
                            playerEntity.sendMessage(new TranslatableComponent("message.on_interact_add", getData(wolf).getInt(TAG_LIVES), MAX_LIVES), playerEntity.getUUID());
                            e.setCanceled(true);
                        } else {
                            playerEntity.sendMessage(new TranslatableComponent("message.on_interact", MAX_LIVES), playerEntity.getUUID());
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onTameEvent(AnimalTameEvent e) {
        if (e.getAnimal() instanceof Wolf wolf) {
            ServerPlayer player = (ServerPlayer) e.getTamer();
            player.addAdditionalSaveData(player.getPersistentData());
            BlockPos spawnPos = Utils.getPlayerSpawnPos(player);
            addDataOnTame(wolf, spawnPos);
            String name = new ItemStack(LIVE_ITEM).getDisplayName().getString();
            player.sendMessage(new TranslatableComponent("message.on_tame", name), player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onDeathEvent(LivingDeathEvent e) {
        if (e.getEntityLiving() instanceof Wolf wolf) {
            if (wolf.getPersistentData().contains(TAG_DOG_REVIVE)) {
                CompoundTag data = getData(wolf);
                if (data.getInt(TAG_LIVES) > 0) {
                    e.setCanceled(true);
                    wolf.setHealth(20f);
                    wolf.setTame(true);
                    Vec3 pos = Utils.readVector3i(data.getCompound(TAG_SPAWN_POINT));
                    wolf.setPos(pos.x(), pos.y(), pos.z());
                    onDeath(wolf);
                    String name = new ItemStack(LIVE_ITEM).getDisplayName().getString();
                    wolf.getOwner().sendMessage(new TranslatableComponent("message.on_death", data.getInt(TAG_LIVES), name, pos.x(), pos.y(), pos.z()), wolf.getOwner().getUUID());
                } else {
                    wolf.getOwner().sendMessage(new TranslatableComponent("message.on_death_no_live"), wolf.getOwner().getUUID());
                }
            }
        }
    }

    //Add data on tame
    private static void addDataOnTame(Wolf wolf, BlockPos playerSpawnPoint) {
        if (!wolf.getPersistentData().contains(TAG_DOG_REVIVE)) {
            CompoundTag data = new CompoundTag();
            data.putInt(TAG_LIVES, 0);
            data.put(TAG_SPAWN_POINT, Utils.writeBlockPos(playerSpawnPoint));
            wolf.getPersistentData().put(TAG_DOG_REVIVE, data);
        }
    }

    //Set the revive tag (used when revived and when given a golden apple)
    private static void onDeath(Wolf Wolf) {
        CompoundTag nbt = getData(Wolf);
        int lives = nbt.getInt(TAG_LIVES);
        nbt.putInt(TAG_LIVES, lives - 1);
        Wolf.getPersistentData().put(TAG_DOG_REVIVE, nbt);
    }

    //When given the item described in the config GIVE_LIVE_ITEM add live
    private static boolean onInteract(Wolf Wolf) {
        CompoundTag data = getData(Wolf);
        int lives = data.getInt(TAG_LIVES);
        if ((lives + 1) <= MAX_LIVES) {
            data.putInt(TAG_LIVES, lives + 1);
            Wolf.getPersistentData().put(TAG_DOG_REVIVE, data);
            return true;
        } else {
            return false;
        }
    }

    //Get the data stored in the wolf
    private static CompoundTag getData(Wolf Wolf) {
        return Wolf.getPersistentData().getCompound(TAG_DOG_REVIVE);
    }

    //Filters wolfs that are tamed;
    private static boolean predicate(Entity entity) {
        return entity instanceof TamableAnimal && ((TamableAnimal) entity).isTame();
    }
}
