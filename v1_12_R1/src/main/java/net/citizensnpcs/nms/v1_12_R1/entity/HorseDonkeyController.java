package net.citizensnpcs.nms.v1_12_R1.entity;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_12_R1.CraftServer;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftDonkey;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftEntity;
import org.bukkit.entity.Donkey;
import org.bukkit.util.Vector;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.nms.v1_12_R1.util.NMSBoundingBox;
import net.citizensnpcs.nms.v1_12_R1.util.NMSImpl;
import net.citizensnpcs.npc.CitizensNPC;
import net.citizensnpcs.npc.ai.NPCHolder;
import net.citizensnpcs.trait.Controllable;
import net.citizensnpcs.trait.HorseModifiers;
import net.citizensnpcs.util.NMS;
import net.citizensnpcs.util.Util;
import net.minecraft.server.v1_12_R1.AxisAlignedBB;
import net.minecraft.server.v1_12_R1.BlockPosition;
import net.minecraft.server.v1_12_R1.DamageSource;
import net.minecraft.server.v1_12_R1.Entity;
import net.minecraft.server.v1_12_R1.EntityHorseDonkey;
import net.minecraft.server.v1_12_R1.GenericAttributes;
import net.minecraft.server.v1_12_R1.IBlockData;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import net.minecraft.server.v1_12_R1.SoundEffect;
import net.minecraft.server.v1_12_R1.World;

public class HorseDonkeyController extends MobEntityController {
    public HorseDonkeyController() {
        super(EntityHorseDonkeyNPC.class);
    }

    @Override
    public void create(Location at, NPC npc) {
        npc.addTrait(HorseModifiers.class);
        super.create(at, npc);
    }

    @Override
    public Donkey getBukkitEntity() {
        return (Donkey) super.getBukkitEntity();
    }

    public static class EntityHorseDonkeyNPC extends EntityHorseDonkey implements NPCHolder {
        private double baseMovementSpeed;

        private final CitizensNPC npc;

        private boolean riding;

        public EntityHorseDonkeyNPC(World world) {
            this(world, null);
        }

        public EntityHorseDonkeyNPC(World world, NPC npc) {
            super(world);
            this.npc = (CitizensNPC) npc;
            if (npc != null) {
                ((Donkey) getBukkitEntity()).setDomestication(((Donkey) getBukkitEntity()).getMaxDomestication());
                baseMovementSpeed = this.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).getValue();
            }
        }

        @Override
        public void a(AxisAlignedBB bb) {
            super.a(NMSBoundingBox.makeBB(npc, bb));
        }

        @Override
        public void a(boolean flag) {
            float oldw = width;
            float oldl = length;
            super.a(flag);
            if (oldw != width || oldl != length) {
                this.setPosition(locX - 0.01, locY, locZ - 0.01);
                this.setPosition(locX + 0.01, locY, locZ + 0.01);
            }
        }

        @Override
        protected void a(double d0, boolean flag, IBlockData block, BlockPosition blockposition) {
            if (npc == null || !npc.isFlyable()) {
                super.a(d0, flag, block, blockposition);
            }
        }

        @Override
        public void a(Entity entity, float strength, double dx, double dz) {
            NMS.callKnockbackEvent(npc, strength, dx, dz, (evt) -> super.a(entity, (float) evt.getStrength(),
                    evt.getKnockbackVector().getX(), evt.getKnockbackVector().getZ()));
        }

        @Override
        public void a(float f, float f1, float f2) {
            if (npc == null || !npc.isFlyable()) {
                super.a(f, f1, f2);
            } else {
                NMSImpl.flyingMoveLogic(this, f, f1, f2);
            }
        }

        @Override
        protected SoundEffect cf() {
            return NMSImpl.getSoundEffect(npc, super.cf(), NPC.Metadata.DEATH_SOUND);
        }

        @Override
        public void collide(net.minecraft.server.v1_12_R1.Entity entity) {
            // this method is called by both the entities involved - cancelling
            // it will not stop the NPC from moving.
            super.collide(entity);
            if (npc != null) {
                Util.callCollisionEvent(npc, entity.getBukkitEntity());
            }
        }

        @Override
        public boolean cV() {
            if (npc != null && riding) {
                return true;
            }
            return super.cV();
        }

        @Override
        protected SoundEffect d(DamageSource damagesource) {
            return NMSImpl.getSoundEffect(npc, super.d(damagesource), NPC.Metadata.HURT_SOUND);
        }

        @Override
        public boolean d(NBTTagCompound save) {
            return npc == null ? super.d(save) : false;
        }

        @Override
        public void e(float f, float f1) {
            if (npc == null || !npc.isFlyable()) {
                super.e(f, f1);
            }
        }

        @Override
        public void enderTeleportTo(double d0, double d1, double d2) {
            NMS.enderTeleportTo(npc, () -> super.enderTeleportTo(d0, d1, d2));
        }

        @Override
        public void f(double x, double y, double z) {
            Vector vector = Util.callPushEvent(npc, x, y, z);
            if (vector != null) {
                super.f(vector.getX(), vector.getY(), vector.getZ());
            }
        }

        @Override
        protected SoundEffect F() {
            return NMSImpl.getSoundEffect(npc, super.F(), NPC.Metadata.AMBIENT_SOUND);
        }

        @Override
        public CraftEntity getBukkitEntity() {
            if (npc != null && !(bukkitEntity instanceof NPCHolder)) {
                bukkitEntity = new HorseDonkeyNPC(this);
            }
            return super.getBukkitEntity();
        }

        @Override
        public NPC getNPC() {
            return npc;
        }

        @Override
        public boolean isLeashed() {
            return NMSImpl.isLeashed(npc, super::isLeashed, this);
        }

        @Override
        protected void L() {
            if (npc == null) {
                super.L();
            }
        }

        @Override
        public void M() {
            super.M();
            if (npc != null) {
                if (npc.hasTrait(Controllable.class) && npc.getOrAddTrait(Controllable.class).isEnabled()) {
                    riding = getBukkitEntity().getPassengers().size() > 0;
                    getAttributeInstance(GenericAttributes.MOVEMENT_SPEED)
                            .setValue(baseMovementSpeed * npc.getNavigator().getDefaultParameters().speedModifier());
                } else {
                    riding = false;
                }
                if (riding) {
                    c(4, true); // datawatcher method
                }
                NMS.setStepHeight(getBukkitEntity(), 1);
                npc.update();
            }
        }

        @Override
        public boolean m_() {
            if (npc == null || !npc.isFlyable()) {
                return super.m_();
            } else {
                return false;
            }
        }
    }

    public static class HorseDonkeyNPC extends CraftDonkey implements NPCHolder {
        private final CitizensNPC npc;

        public HorseDonkeyNPC(EntityHorseDonkeyNPC entity) {
            super((CraftServer) Bukkit.getServer(), entity);
            this.npc = entity.npc;
        }

        @Override
        public NPC getNPC() {
            return npc;
        }
    }
}