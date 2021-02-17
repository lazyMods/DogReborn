package lazy.dogreborn;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
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
        ServerPlayerEntity playerEntity = (ServerPlayerEntity) e.getEntityLiving();
        ServerWorld world = (ServerWorld) playerEntity.world;

        List<WolfEntity> tamedWolfs = world.getEntities(EntityType.WOLF, DogReviveHandler::predicate).stream()
                .map(entity -> (WolfEntity) entity)
                .filter(wolfEntity -> wolfEntity.isOwner(playerEntity))
                .filter(wolfEntity -> wolfEntity.getPersistentData().contains(TAG_DOG_REVIVE))
                .collect(Collectors.toList());

        for (WolfEntity tamedWolf : tamedWolfs) {
            CompoundNBT nbt = getData(tamedWolf);
            nbt.put(TAG_SPAWN_POINT, Utils.writeVector3i(e.getNewSpawn()));
            tamedWolf.getPersistentData().put(TAG_DOG_REVIVE, nbt);
        }
    }

    @SubscribeEvent
    public static void onInteractEvent(PlayerInteractEvent.EntityInteractSpecific e) {
        if (e.getSide().isServer()) {
            if (e.getTarget() instanceof WolfEntity) {
                WolfEntity wolf = (WolfEntity) e.getTarget();
                ServerPlayerEntity playerEntity = (ServerPlayerEntity) e.getPlayer();
                if (wolf.isTamed() && wolf.getPersistentData().contains(TAG_DOG_REVIVE)) {
                    if (e.getItemStack().getItem() == LIVE_ITEM) {
                        boolean successInteract = onInteract(wolf);
                        if (successInteract) {
                            e.getItemStack().shrink(1);
                            playerEntity.sendMessage(new TranslationTextComponent("message.on_interact_add", getData(wolf).getInt(TAG_LIVES), MAX_LIVES), playerEntity.getUniqueID());
                            e.setCanceled(true);
                        } else {
                            playerEntity.sendMessage(new TranslationTextComponent("message.on_interact", MAX_LIVES), playerEntity.getUniqueID());
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onTameEvent(AnimalTameEvent e) {
        if (e.getAnimal() instanceof WolfEntity) {
            WolfEntity wolf = (WolfEntity) e.getEntityLiving();
            ServerPlayerEntity player = (ServerPlayerEntity) e.getTamer();
            player.writeAdditional(player.getPersistentData());
            Vector3i spawnPos = Utils.getPlayerSpawnPos(player);
            addDataOnTame(wolf, spawnPos);
            String name = new ItemStack(LIVE_ITEM).getDisplayName().getString();
            player.sendMessage(new TranslationTextComponent("message.on_tame", name), player.getUniqueID());
        }
    }

    @SubscribeEvent
    public static void onDeathEvent(LivingDeathEvent e) {
        if (e.getEntityLiving() instanceof WolfEntity) {
            WolfEntity wolf = (WolfEntity) e.getEntityLiving();
            if (wolf.getPersistentData().contains(TAG_DOG_REVIVE)) {
                CompoundNBT data = getData(wolf);
                if (data.getInt(TAG_LIVES) > 0) {
                    e.setCanceled(true);
                    wolf.setHealth(20f);
                    wolf.func_233687_w_(true);
                    Vector3i pos = Utils.readVector3i(data.getCompound(TAG_SPAWN_POINT));
                    wolf.setPosition(pos.getX(), pos.getY(), pos.getZ());
                    onDeath(wolf);
                    String name = new ItemStack(LIVE_ITEM).getDisplayName().getString();
                    wolf.getOwner().sendMessage(new TranslationTextComponent("message.on_death", data.getInt(TAG_LIVES), name, pos.getX(), pos.getY(), pos.getZ()), wolf.getOwner().getUniqueID());
                } else {
                    wolf.getOwner().sendMessage(new TranslationTextComponent("message.on_death_no_live"), wolf.getOwner().getUniqueID());
                }
            }
        }
    }

    //Add data on tame
    private static void addDataOnTame(WolfEntity wolf, Vector3i playerSpawnPoint) {
        if (!wolf.getPersistentData().contains(TAG_DOG_REVIVE)) {
            CompoundNBT data = new CompoundNBT();
            data.putInt(TAG_LIVES, 0);
            data.put(TAG_SPAWN_POINT, Utils.writeVector3i(playerSpawnPoint));
            wolf.getPersistentData().put(TAG_DOG_REVIVE, data);
        }
    }

    //Set the revive tag (used when revived and when given a golden apple)
    private static void onDeath(WolfEntity wolfEntity) {
        CompoundNBT nbt = getData(wolfEntity);
        int lives = nbt.getInt(TAG_LIVES);
        nbt.putInt(TAG_LIVES, lives - 1);
        wolfEntity.getPersistentData().put(TAG_DOG_REVIVE, nbt);
    }

    //When given the item described in the config GIVE_LIVE_ITEM add live
    private static boolean onInteract(WolfEntity wolfEntity) {
        CompoundNBT data = getData(wolfEntity);
        int lives = data.getInt(TAG_LIVES);
        if ((lives + 1) <= MAX_LIVES) {
            data.putInt(TAG_LIVES, lives + 1);
            wolfEntity.getPersistentData().put(TAG_DOG_REVIVE, data);
            return true;
        } else {
            return false;
        }
    }

    //Get the data stored in the wolf
    private static CompoundNBT getData(WolfEntity wolfEntity) {
        return wolfEntity.getPersistentData().getCompound(TAG_DOG_REVIVE);
    }

    //Filters wolfs that are tamed;
    private static boolean predicate(Entity entity) {
        return entity instanceof TameableEntity && ((TameableEntity) entity).isTamed();
    }
}
