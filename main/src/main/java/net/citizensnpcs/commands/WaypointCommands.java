package net.citizensnpcs.commands;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.citizensnpcs.Citizens;
import net.citizensnpcs.api.ai.TeleportStuckAction;
import net.citizensnpcs.api.astar.pathfinder.ChunkBlockSource;
import net.citizensnpcs.api.command.Command;
import net.citizensnpcs.api.command.CommandContext;
import net.citizensnpcs.api.command.Requirements;
import net.citizensnpcs.api.command.exception.CommandException;
import net.citizensnpcs.api.hpastar.HPAGraph;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.util.Messaging;
import net.citizensnpcs.editor.Editor;
import net.citizensnpcs.trait.waypoint.LinearWaypointProvider;
import net.citizensnpcs.trait.waypoint.Waypoint;
import net.citizensnpcs.trait.waypoint.WaypointProvider;
import net.citizensnpcs.trait.waypoint.Waypoints;
import net.citizensnpcs.util.Messages;
import net.citizensnpcs.util.Util;

@Requirements(ownership = true, selected = true)
public class WaypointCommands {
    public WaypointCommands(Citizens plugin) {
    }

    @Command(
            aliases = { "waypoints", "waypoint", "wp" },
            usage = "add [x] [y] [z] (world) (--index idx)",
            desc = "Adds a waypoint at a point",
            modifiers = { "add" },
            min = 4,
            max = 5,
            permission = "citizens.waypoints.add")
    public void add(CommandContext args, CommandSender sender, NPC npc) throws CommandException {
        WaypointProvider provider = npc.getOrAddTrait(Waypoints.class).getCurrentProvider();
        if (!(provider instanceof LinearWaypointProvider))
            throw new CommandException();
        List<Waypoint> waypoints = (List<Waypoint>) ((LinearWaypointProvider) provider).waypoints();
        World world = args.argsLength() > 4 ? Bukkit.getWorld(args.getString(4)) : npc.getStoredLocation().getWorld();
        if (world == null)
            throw new CommandException(Messages.WORLD_NOT_FOUND);
        Location loc = new Location(world, args.getInteger(1), args.getInteger(2), args.getInteger(3));
        int index = args.getFlagInteger("index", waypoints.size());
        waypoints.add(index, new Waypoint(loc));
        Messaging.sendTr(sender, Messages.WAYPOINT_ADDED, Util.prettyPrintLocation(loc), index);
    }

    @Command(
            aliases = { "waypoints", "waypoint", "wp" },
            usage = "disableteleport",
            desc = "Disables teleportation when stuck",
            modifiers = { "disableteleport", "dt" },
            min = 1,
            max = 1,
            permission = "citizens.waypoints.disableteleport")
    public void disableTeleporting(CommandContext args, CommandSender sender, NPC npc) throws CommandException {
        npc.data().setPersistent(NPC.DISABLE_DEFAULT_STUCK_ACTION_METADATA,
                !npc.data().get(NPC.DISABLE_DEFAULT_STUCK_ACTION_METADATA, false));
        if (npc.data().get(NPC.DISABLE_DEFAULT_STUCK_ACTION_METADATA, false)) {
            npc.getNavigator().getDefaultParameters().stuckAction(null);
            Messaging.sendTr(sender, Messages.WAYPOINT_TELEPORTING_DISABLED, npc.getName());
        } else {
            npc.getNavigator().getDefaultParameters().stuckAction(TeleportStuckAction.INSTANCE);
            Messaging.sendTr(sender, Messages.WAYPOINT_TELEPORTING_ENABLED, npc.getName());
        }
    }

    @Command(
            aliases = { "waypoints", "waypoint", "wp" },
            usage = "hpa",
            desc = "Debugging command",
            modifiers = { "hpa" },
            min = 1,
            max = 1,
            permission = "citizens.waypoints.hpa")
    public void hpa(CommandContext args, CommandSender sender, NPC npc) throws CommandException {
        if (Messaging.isDebugging() && sender.isOp()) {
            HPAGraph graph = new HPAGraph(new ChunkBlockSource(npc.getStoredLocation(), 16),
                    npc.getStoredLocation().getBlockX(), npc.getStoredLocation().getBlockY(),
                    npc.getStoredLocation().getBlockZ());
            graph.addClusters(npc.getStoredLocation().getBlockX(), npc.getStoredLocation().getBlockZ());
            System.out.println(graph.findPath(new Location(npc.getStoredLocation().getWorld(), 8, 68, -134),
                    new Location(npc.getStoredLocation().getWorld(), 11, 68, -131)));
        }
    }

    @Command(
            aliases = { "waypoints", "waypoint", "wp" },
            usage = "opendoors",
            desc = "Enables opening doors when pathfinding",
            modifiers = { "opendoors", "od" },
            min = 1,
            max = 1,
            permission = "citizens.waypoints.opendoors")
    public void openDoors(CommandContext args, CommandSender sender, NPC npc) throws CommandException {
        boolean opensDoors = !npc.data().get(NPC.PATHFINDER_OPEN_DOORS_METADATA, false);
        npc.data().setPersistent(NPC.PATHFINDER_OPEN_DOORS_METADATA, opensDoors);
        Messaging.sendTr(sender,
                opensDoors ? Messages.PATHFINDER_OPEN_DOORS_ENABLED : Messages.PATHFINDER_OPEN_DOORS_DISABLED,
                npc.getName());
    }

    @Command(
            aliases = { "waypoints", "waypoint", "wp" },
            usage = "provider [provider name]",
            desc = "Sets the current waypoint provider",
            modifiers = { "provider" },
            min = 1,
            max = 2,
            permission = "citizens.waypoints.provider")
    public void provider(CommandContext args, CommandSender sender, NPC npc) throws CommandException {
        Waypoints waypoints = npc.getOrAddTrait(Waypoints.class);
        if (args.argsLength() == 1) {
            Messaging.sendTr(sender, Messages.CURRENT_WAYPOINT_PROVIDER, waypoints.getCurrentProviderName());
            waypoints.describeProviders(sender);
            return;
        }
        if (sender instanceof Player && Editor.hasEditor(((Player) sender))) {
            Editor.leave(((Player) sender));
        }
        boolean success = waypoints.setWaypointProvider(args.getString(1));
        if (!success)
            throw new CommandException("Provider not found.");
        Messaging.sendTr(sender, Messages.WAYPOINT_PROVIDER_SET, args.getString(1));
    }
}
