package com.elytradev.movingworld.common.experiments.network.messages.client;

import com.elytradev.concrete.network.Message;
import com.elytradev.concrete.network.NetworkContext;
import com.elytradev.concrete.network.annotation.field.MarshalledAs;
import com.elytradev.concrete.network.annotation.type.ReceivedOn;
import com.elytradev.concrete.reflect.accessor.Accessor;
import com.elytradev.concrete.reflect.accessor.Accessors;
import com.elytradev.movingworld.common.experiments.MovingWorldExperimentsMod;
import com.elytradev.movingworld.common.experiments.entity.EntityMobileRegion;
import com.elytradev.movingworld.common.experiments.interact.MWPlayerInteractionManager;
import com.elytradev.movingworld.common.experiments.network.MovingWorldExperimentsNetworking;
import com.elytradev.movingworld.common.experiments.network.messages.server.MessageBlockChange;
import com.elytradev.movingworld.common.network.marshallers.EntityMarshaller;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.server.SPacketChat;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Created by darkevilmac on 3/6/2017.
 */
@ReceivedOn(Side.SERVER)
public class MessageTryUseItemOnBlock extends Message {

    private transient Accessor<Vec3d> targetPos = Accessors.findField(NetHandlerPlayServer.class, "targetPos", "field_184362_y");

    @MarshalledAs("int")
    private int dimension;
    @MarshalledAs(EntityMarshaller.MARSHALLER_NAME)
    private EntityMobileRegion regionInteractedWith;

    private BlockPos regionPos;
    private EnumFacing placedBlockDirection;
    private EnumHand hand;
    @MarshalledAs("float")
    private float facingX, facingY, facingZ;

    public MessageTryUseItemOnBlock(NetworkContext ctx) {
        super(ctx);
    }

    public MessageTryUseItemOnBlock(EntityMobileRegion regionInteractedWith, BlockPos regionPos, EnumFacing placedBlockDirection, EnumHand hand, float facingX, float facingY, float facingZ) {
        super(MovingWorldExperimentsNetworking.networkContext);
        this.dimension = regionInteractedWith.region.dimension;
        this.regionInteractedWith = regionInteractedWith;
        this.regionPos = regionPos;
        this.placedBlockDirection = placedBlockDirection;
        this.hand = hand;
        this.facingX = facingX;
        this.facingY = facingY;
        this.facingZ = facingZ;
    }

    @Override
    protected void handle(EntityPlayer senderIn) {
        EntityPlayerMP player = (EntityPlayerMP) senderIn;
        WorldServer worldserver = (WorldServer) MovingWorldExperimentsMod.modProxy.getCommonDB().getWorldFromDim(dimension);
        BlockPos realWorldPos = regionInteractedWith.region.convertRegionPosToRealWorld(regionPos);
        ItemStack itemstack = player.getHeldItem(hand);

        if (!MWPlayerInteractionManager.MANAGERS.containsKey(player)) {
            MWPlayerInteractionManager.MANAGERS.put(player, new MWPlayerInteractionManager(regionInteractedWith, player));
        } else {
            MWPlayerInteractionManager.MANAGERS.get(player).setRegionEntity(regionInteractedWith);
        }

        MWPlayerInteractionManager interactionManager = MWPlayerInteractionManager.MANAGERS.get(player);

        player.markPlayerActive();

        if (regionPos.getY() < player.getServerWorld().getMinecraftServer().getBuildLimit() - 1 || placedBlockDirection != EnumFacing.UP && regionPos.getY() < player.getServerWorld().getMinecraftServer().getBuildLimit()) {
            double dist = player.interactionManager.getBlockReachDistance() + 3;
            dist *= dist;
            boolean withinRange = player.getDistanceSq((double) realWorldPos.getX() + 0.5D, (double) realWorldPos.getY() + 0.5D, (double) realWorldPos.getZ() + 0.5D) < dist;
            if (this.targetPos.get(player.connection) == null && withinRange && regionInteractedWith.region.isPosWithinBounds(regionPos)) {
                interactionManager.processRightClickBlock(player, worldserver, itemstack, hand, regionPos, placedBlockDirection, facingX, facingY, facingZ);
            }
        } else {
            TextComponentTranslation textcomponenttranslation = new TextComponentTranslation("build.tooHigh", Integer.valueOf(player.getServerWorld().getMinecraftServer().getBuildLimit()));
            textcomponenttranslation.getStyle().setColor(TextFormatting.RED);
            player.connection.sendPacket(new SPacketChat(textcomponenttranslation, ChatType.GAME_INFO));
        }
        new MessageBlockChange(worldserver, regionPos).sendTo(player);
        new MessageBlockChange(worldserver, regionPos.offset(placedBlockDirection)).sendTo(player);
    }
}
