package net.citizensnpcs.nms.v1_19_R2.entity.nonliving;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_19_R2.CraftServer;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftTrident;
import org.bukkit.entity.Trident;
import org.bukkit.util.Vector;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.nms.v1_19_R2.entity.MobEntityController;
import net.citizensnpcs.nms.v1_19_R2.util.ForwardingNPCHolder;
import net.citizensnpcs.nms.v1_19_R2.util.NMSBoundingBox;
import net.citizensnpcs.nms.v1_19_R2.util.NMSImpl;
import net.citizensnpcs.npc.CitizensNPC;
import net.citizensnpcs.npc.ai.NPCHolder;
import net.citizensnpcs.util.Util;
import net.minecraft.core.PositionImpl;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;

public class ThrownTridentController extends MobEntityController {
    public ThrownTridentController() {
        super(EntityThrownTridentNPC.class);
    }

    @Override
    public Trident getBukkitEntity() {
        return (Trident) super.getBukkitEntity();
    }

    public static class EntityThrownTridentNPC extends ThrownTrident implements NPCHolder {
        @Override
        public boolean isPushable() {
            return npc == null ? super.isPushable()
                    : npc.data().<Boolean> get(NPC.Metadata.COLLIDABLE, !npc.isProtected());
        }

        private final CitizensNPC npc;

        public EntityThrownTridentNPC(EntityType<? extends ThrownTrident> types, Level level) {
            this(types, level, null);
        }

        public EntityThrownTridentNPC(EntityType<? extends ThrownTrident> types, Level level, NPC npc) {
            super(types, level);
            this.npc = (CitizensNPC) npc;
        }

        @Override
        public CraftEntity getBukkitEntity() {
            if (npc != null && !(super.getBukkitEntity() instanceof NPCHolder)) {
                NMSImpl.setBukkitEntity(this, new ThrownTridentNPC(this));
            }
            return super.getBukkitEntity();
        }

        @Override
        public NPC getNPC() {
            return npc;
        }

        @Override
        protected AABB makeBoundingBox() {
            return NMSBoundingBox.makeBB(npc, super.makeBoundingBox());
        }

        @Override
        public void push(double x, double y, double z) {
            Vector vector = Util.callPushEvent(npc, x, y, z);
            if (vector != null) {
                super.push(vector.getX(), vector.getY(), vector.getZ());
            }
        }

        @Override
        public void push(Entity entity) {
            // this method is called by both the entities involved - cancelling
            // it will not stop the NPC from moving.
            super.push(entity);
            if (npc != null) {
                Util.callCollisionEvent(npc, entity.getBukkitEntity());
            }
        }

        @Override
        public boolean save(CompoundTag save) {
            return npc == null ? super.save(save) : false;
        }

        @Override
        public Entity teleportTo(ServerLevel worldserver, PositionImpl location) {
            if (npc == null)
                return super.teleportTo(worldserver, location);
            return NMSImpl.teleportAcrossWorld(this, worldserver, location);
        }

        @Override
        public void tick() {
            if (npc != null) {
                npc.update();
            } else {
                super.tick();
            }
        }

        @Override
        public boolean updateFluidHeightAndDoFluidPushing(TagKey<Fluid> tagkey, double d0) {
            return NMSImpl.fluidPush(npc, this, () -> super.updateFluidHeightAndDoFluidPushing(tagkey, d0));
        }
    }

    public static class ThrownTridentNPC extends CraftTrident implements ForwardingNPCHolder {
        public ThrownTridentNPC(EntityThrownTridentNPC entity) {
            super((CraftServer) Bukkit.getServer(), entity);
        }
    }
}
