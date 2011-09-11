/*
 * This file is part of Spout (http://wiki.getspout.org/).
 * 
 * Spout is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Spout is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.getspout.spout.player;

import gnu.trove.TIntObjectHashMap;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.getspout.spout.entity.SpoutCraftEntity;
import org.getspout.spout.inventory.SimpleItemManager;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.entity.SpoutEntity;
import org.getspout.spoutapi.player.GlobalManager;
import org.getspout.spoutapi.player.PlayerInformation;
import org.getspout.spoutapi.player.SpoutPlayer;

public class SpoutGlobalManager implements GlobalManager{
	
	HashMap<String, PlayerInformation> infoMap = new HashMap<String, PlayerInformation>();
	PlayerInformation globalInfo = new SimplePlayerInformation();
	TIntObjectHashMap entityIdMap = new TIntObjectHashMap();
	Map<UUID, WeakReference<Entity>> entityUniqueIdMap = new HashMap<UUID, WeakReference<Entity>>();

	@Override
	public SpoutPlayer getPlayer(Player player) {
		return SpoutCraftPlayer.getPlayer(player);
	}

	@Override
	public SpoutPlayer getPlayer(UUID id) {
		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			if (player.getUniqueId().equals(id)) {
				return getPlayer(player);
			}
		}
		return null;
	}

	@Override
	public SpoutPlayer getPlayer(int entityId) {
		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			if (player.getEntityId() == entityId) {
				return getPlayer(player);
			}
		}
		return null;
	}

	@Override
	public PlayerInformation getPlayerInfo(Player player) {
		return infoMap.get(player.getName());
	}
	
	public void onPlayerJoin(Player player) {
		if (getPlayerInfo(player) == null) {
			infoMap.put(player.getName(), new SimplePlayerInformation());
		}
	}
	
	public void onPluginEnable() {
		for(Player player : Bukkit.getServer().getOnlinePlayers()) {
			infoMap.put(player.getName(), new SimplePlayerInformation());
		}
	}
	
	public void onPluginDisable() {
		infoMap.clear();
	}

	@Override
	public PlayerInformation getGlobalInfo() {
		return globalInfo;
	}

	@Override
	public SpoutPlayer[] getOnlinePlayers() {
		Player[] online = Bukkit.getServer().getOnlinePlayers();
		SpoutPlayer[] spoutPlayers = new SpoutPlayer[online.length];
		for (int i = 0; i < online.length; i++) {
			spoutPlayers[i] = getPlayer(online[i]);
		}
		return spoutPlayers;
	}
	
	@Override
	public void setVersionString(int playerId, String versionString) {
		
		SpoutPlayer sp = getPlayer(playerId);
		if (sp instanceof SpoutCraftPlayer) {
			SpoutCraftPlayer scp = (SpoutCraftPlayer)sp;
			scp.setVersionString(versionString);
			System.out.println("[Spout] Successfully authenticated " + scp.getName() + "'s Spoutcraft client. Running client version: " + scp.getVersionString());
			((SimpleItemManager)SpoutManager.getItemManager()).updateCustomClientData(scp);
			((SimpleItemManager)SpoutManager.getItemManager()).updateAllCustomBlockDesigns(scp);
			SimpleItemManager im = (SimpleItemManager)SpoutManager.getItemManager();
			im.sendBlockOverrideToPlayers(new Player[] {sp}, sp.getWorld());
		}
		
	}

	@Override
	public SpoutEntity getEntity(UUID id) {
		WeakReference<Entity> result = entityUniqueIdMap.get(id);
		Entity found = null;
		if (result != null && result.get() != null) {
			found = result.get();
		}
		else {
loop:		for (World world : Bukkit.getServer().getWorlds()){
				for (Entity e : world.getEntities()) {
					if (e.getUniqueId().equals(id)) {
						found = e;
						break loop;
					}
				}
			}
		}
		found = getEntity(found);
		if (found != null) {
			result = new WeakReference<Entity>(found);
			entityUniqueIdMap.put(id, result);
		}
		return (SpoutEntity)found;
	}

	@SuppressWarnings("unchecked")
	@Override
	public SpoutEntity getEntity(int entityId) {
		WeakReference<Entity> result = (WeakReference<Entity>) entityIdMap.get(entityId);
		Entity found = null;
		if (result != null && result.get() != null) {
			found = result.get();
		}
		else {
loop:		for (World world : Bukkit.getServer().getWorlds()){
				for (Entity e : world.getEntities()) {
					if (e.getEntityId() == entityId) {
						found = e;
						break loop;
					}
				}
			}
		}
		found = getEntity(found);
		if (found != null) {
			result = new WeakReference<Entity>(found);
			entityIdMap.put(entityId, result);
		}
		return (SpoutEntity)found;
	}

	@Override
	public SpoutEntity getEntity(Entity entity) {
		if (entity == null) {
			return null;
		}
		CraftEntity ce = (CraftEntity)entity;
		CraftEntity result = SpoutCraftEntity.getUpdatedEntity(((CraftServer)ce.getServer()), ce.getHandle(), ce);
		return (SpoutEntity)result;
	}
}
