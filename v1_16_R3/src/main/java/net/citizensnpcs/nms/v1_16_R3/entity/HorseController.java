package net.citizensnpcs.nms.v1_16_R3.entity;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftHorse;
import org.bukkit.entity.Horse;
import org.bukkit.util.Vector;

import net.citizensnpcs.api.event.NPCEnderTeleportEvent;
import net.citizensnpcs.api.event.NPCKnockbackEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.nms.v1_16_R3.util.ForwardingNPCHolder;
import net.citizensnpcs.nms.v1_16_R3.util.NMSImpl;
import net.citizensnpcs.npc.CitizensNPC;
import net.citizensnpcs.npc.ai.NPCHolder;
import net.citizensnpcs.trait.Controllable;
import net.citizensnpcs.trait.HorseModifiers;
import net.citizensnpcs.util.NMS;
import net.citizensnpcs.util.Util;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.DamageSource;
import net.minecraft.server.v1_16_R3.DataWatcherObject;
import net.minecraft.server.v1_16_R3.Entity;
import net.minecraft.server.v1_16_R3.EntityBoat;
import net.minecraft.server.v1_16_R3.EntityHorse;
import net.minecraft.server.v1_16_R3.EntityMinecartAbstract;
import net.minecraft.server.v1_16_R3.EntityTypes;
import net.minecraft.server.v1_16_R3.FluidType;
import net.minecraft.server.v1_16_R3.GenericAttributes;
import net.minecraft.server.v1_16_R3.IBlockData;
import net.minecraft.server.v1_16_R3.NBTTagCompound;
import net.minecraft.server.v1_16_R3.SoundEffect;
import net.minecraft.server.v1_16_R3.Tag;
import net.minecraft.server.v1_16_R3.Vec3D;
import net.minecraft.server.v1_16_R3.World;

public class HorseController extends MobEntityController {
    public HorseController() {
        super(EntityHorseNPC.class);
    }

    @Override
    public Horse getBukkitEntity() {
        return (Horse) super.getBukkitEntity();
    }

    @Override
    public void spawn(Location at, NPC npc) {
        npc.getOrAddTrait(HorseModifiers.class);
        super.spawn(at, npc);
    }

    public static class EntityHorseNPC extends EntityHorse implements NPCHolder {
        private double baseMovementSpeed;

        private boolean calledNMSHeight = false;

        private final CitizensNPC npc;
        private boolean riding;

        public EntityHorseNPC(EntityTypes<? extends EntityHorse> types, World world) {
            this(types, world, null);
        }

        public EntityHorseNPC(EntityTypes<? extends EntityHorse> types, World world, NPC npc) {
            super(types, world);
            this.npc = (CitizensNPC) npc;
            if (npc != null) {
                Horse horse = (Horse) getBukkitEntity();
                horse.setDomestication(horse.getMaxDomestication());
                baseMovementSpeed = this.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).getValue();
            }
        }

        @Override
        public void a(DataWatcherObject<?> datawatcherobject) {
            if (npc != null && !calledNMSHeight) {
                calledNMSHeight = true;
                NMSImpl.checkAndUpdateHeight(this, datawatcherobject);
                calledNMSHeight = false;
            }

            super.a(datawatcherobject);
        }

        @Override
        protected void a(double d0, boolean flag, IBlockData block, BlockPosition blockposition) {
            if (npc == null || !npc.isFlyable()) {
                super.a(d0, flag, block, blockposition);
            }
        }

        @Override
        public void a(float strength, double dx, double dz) {
            NPCKnockbackEvent event = new NPCKnockbackEvent(npc, strength, dx, dz);
            Bukkit.getPluginManager().callEvent(event);
            Vector kb = event.getKnockbackVector();
            if (!event.isCancelled()) {
                super.a((float) event.getStrength(), kb.getX(), kb.getZ());
            }
        }

        @Override
        public boolean a(Tag<FluidType> tag, double d0) {
            Vec3D old = getMot().add(0, 0, 0);
            boolean res = super.a(tag, d0);
            if (!npc.isPushableByFluids()) {
                this.setMot(old);
            }
            return res;
        }

        @Override
        public boolean b(float f, float f1) {
            if (npc == null || !npc.isFlyable()) {
                return super.b(f, f1);
            }
            return false;
        }

        @Override
        public void checkDespawn() {
            if (npc == null) {
                super.checkDespawn();
            }
        }

        @Override
        public void collide(net.minecraft.server.v1_16_R3.Entity entity) {
            // this method is called by both the entities involved - cancelling
            // it will not stop the NPC from moving.
            super.collide(entity);
            if (npc != null) {
                Util.callCollisionEvent(npc, entity.getBukkitEntity());
            }
        }

        @Override
        public boolean cs() { // horse boolean
            if (npc != null && riding) {
                return true;
            }
            return super.cs();
        }

        @Override
        public boolean d(NBTTagCompound save) {
            return npc == null ? super.d(save) : false;
        }

        @Override
        public void enderTeleportTo(double d0, double d1, double d2) {
            if (npc == null) {
                super.enderTeleportTo(d0, d1, d2);
                return;
            }
            NPCEnderTeleportEvent event = new NPCEnderTeleportEvent(npc);
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                super.enderTeleportTo(d0, d1, d2);
            }
        }

        @Override
        public boolean er() {
            return npc != null && npc.getNavigator().isNavigating() ? false : super.er();
        }

        @Override
        public void g(Vec3D vec3d) {
            if (npc == null || !npc.isFlyable()) {
                super.g(vec3d);
            } else {
                NMSImpl.flyingMoveLogic(this, vec3d);
            }
        }

        @Override
        public CraftEntity getBukkitEntity() {
            if (npc != null && !(super.getBukkitEntity() instanceof NPCHolder)) {
                NMSImpl.setBukkitEntity(this, new HorseNPC(this));
            }
            return super.getBukkitEntity();
        }

        @Override
        public NPC getNPC() {
            return npc;
        }

        @Override
        protected SoundEffect getSoundAmbient() {
            return NMSImpl.getSoundEffect(npc, super.getSoundAmbient(), NPC.Metadata.AMBIENT_SOUND);
        }

        @Override
        protected SoundEffect getSoundDeath() {
            return NMSImpl.getSoundEffect(npc, super.getSoundDeath(), NPC.Metadata.DEATH_SOUND);
        }

        @Override
        protected SoundEffect getSoundHurt(DamageSource damagesource) {
            return NMSImpl.getSoundEffect(npc, super.getSoundHurt(damagesource), NPC.Metadata.HURT_SOUND);
        }

        @Override
        public void i(double x, double y, double z) {
            Vector vector = Util.callPushEvent(npc, x, y, z);
            if (vector != null) {
                super.i(vector.getX(), vector.getY(), vector.getZ());
            }
        }

        @Override
        public boolean isClimbing() {
            if (npc == null || !npc.isFlyable()) {
                return super.isClimbing();
            } else {
                return false;
            }
        }

        @Override
        public boolean isLeashed() {
            if (npc == null)
                return super.isLeashed();
            boolean protectedDefault = npc.isProtected();
            if (!protectedDefault || !npc.data().get(NPC.Metadata.LEASH_PROTECTED, protectedDefault))
                return super.isLeashed();
            if (super.isLeashed()) {
                unleash(true, false); // clearLeash with client update
            }
            return false; // shouldLeash
        }

        @Override
        public void mobTick() {
            super.mobTick();
            if (npc == null)
                return;
            NMSImpl.updateMinecraftAIState(npc, this);
            if (npc.hasTrait(Controllable.class) && npc.getOrAddTrait(Controllable.class).isEnabled()) {
                riding = getBukkitEntity().getPassengers().size() > 0;
                getAttributeInstance(GenericAttributes.MOVEMENT_SPEED)
                        .setValue(baseMovementSpeed * npc.getNavigator().getDefaultParameters().speedModifier());
            } else {
                riding = false;
            }
            if (riding) {
                if (npc.getNavigator().isNavigating()) {
                    org.bukkit.entity.Entity basePassenger = passengers.get(0).getBukkitEntity();
                    NMS.look(basePassenger, yaw, pitch);
                }
                d(4, true); // datawatcher method
            }
            NMS.setStepHeight(getBukkitEntity(), 1);
            npc.update();

        }

        @Override
        protected boolean n(Entity entity) {
            if (npc != null && (entity instanceof EntityBoat || entity instanceof EntityMinecartAbstract)) {
                return !npc.isProtected();
            }
            return super.n(entity);
        }
    }

    public static class HorseNPC extends CraftHorse implements ForwardingNPCHolder {
        public HorseNPC(EntityHorseNPC entity) {
            super((CraftServer) Bukkit.getServer(), entity);
        }
    }
}
