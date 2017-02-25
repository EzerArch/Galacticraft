package micdoodle8.mods.galacticraft.planets.venus.dimension;

import micdoodle8.mods.galacticraft.api.vector.Vector3;
import micdoodle8.mods.galacticraft.api.world.ITeleportType;
import micdoodle8.mods.galacticraft.core.entities.player.GCPlayerStats;
import micdoodle8.mods.galacticraft.planets.venus.entities.EntityEntryPodVenus;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.Random;

public class TeleportTypeVenus implements ITeleportType
{
    @Override
    public boolean useParachute()
    {
        return false;
    }

    @Override
    public Vector3 getPlayerSpawnLocation(WorldServer world, EntityPlayerMP player)
    {
        if (player != null)
        {
            GCPlayerStats stats = GCPlayerStats.get(player);
            return new Vector3(stats.coordsTeleportedFromX, 900.0, stats.coordsTeleportedFromZ);
        }

        return null;
    }

    @Override
    public Vector3 getEntitySpawnLocation(WorldServer world, Entity entity)
    {
        return new Vector3(entity.posX, 900.0, entity.posZ);
    }

    @Override
    public Vector3 getParaChestSpawnLocation(WorldServer world, EntityPlayerMP player, Random rand)
    {
        return null;
    }

    @Override
    public void onSpaceDimensionChanged(World newWorld, EntityPlayerMP player, boolean ridingAutoRocket)
    {
        if (!ridingAutoRocket && player != null)
        {
            GCPlayerStats stats = GCPlayerStats.get(player);

            if (stats.teleportCooldown <= 0)
            {
                if (player.capabilities.isFlying)
                {
                    player.capabilities.isFlying = false;
                }

                if (!newWorld.isRemote)
                {
                    EntityEntryPodVenus entryPod = new EntityEntryPodVenus(player);

                    newWorld.spawnEntityInWorld(entryPod);
                }

                stats.teleportCooldown = 10;
            }
        }
    }

    @Override
    public void setupAdventureSpawn(EntityPlayerMP player)
    {
        // TODO Auto-generated method stub

    }
}
