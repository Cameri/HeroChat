/**
 * Copyright (C) 2011 DThielke <dave.thielke@gmail.com>
 * 
 * This work is licensed under the Creative Commons Attribution-NonCommercial-NoDerivs 3.0 Unported License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/3.0/ or send a letter to
 * Creative Commons, 171 Second Street, Suite 300, San Francisco, California, 94105, USA.
 **/

package com.herocraftonline.dthielke.herochat.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.herocraftonline.dthielke.herochat.HeroChat;
import com.herocraftonline.dthielke.herochat.HeroChat.ChatColor;
import com.herocraftonline.dthielke.herochat.channels.Channel;
import com.herocraftonline.dthielke.herochat.channels.ChannelManager;
import com.herocraftonline.dthielke.herochat.channels.LocalChannel;

public class ConfigManager {
	protected HeroChat plugin;
	protected File primaryConfigFile;
	protected File usersConfigFolder;

	public ConfigManager(HeroChat plugin) {
		this.plugin = plugin;
		this.primaryConfigFile = new File(plugin.getDataFolder(), "config.yml");
		this.usersConfigFolder = new File(plugin.getDataFolder(), "users/");
		usersConfigFolder.mkdirs();
	}

	public void reload() throws Exception {
		load();
	}

	public void load() throws Exception {
		checkConfig();
		YamlConfiguration config = new YamlConfiguration();//primaryConfigFile);
		create(primaryConfigFile);
		config.load(primaryConfigFile);
		loadChannels(config);
		loadGlobals(config);
	}

	private void checkConfig() {
		if (!primaryConfigFile.exists()) {
			try {
				primaryConfigFile.getParentFile().mkdir();
				primaryConfigFile.createNewFile();
				OutputStream output = new FileOutputStream(primaryConfigFile, false);
				InputStream input = ConfigManager.class.getResourceAsStream("config.yml");
				byte[] buf = new byte[8192];
				while (true) {
					int length = input.read(buf);
					if (length < 0) {
						break;
					}
					output.write(buf, 0, length);
				}
				input.close();
				output.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void loadGlobals(YamlConfiguration config) {
		String globals = "globals.";
		ChannelManager cm = plugin.getChannelManager();
		String pluginTag = config.getString(globals + "plugin-tag", "[HeroChat] ").replace("&", "§");
		String ircTag = config.getString(globals + "craftIRC-prefix", "#");
		String ircMessageFormat = config.getString(globals + "craftIRC-message-format", "[{nick}] {player}: ");
		String defaultChannel = config.getString(globals + "default-channel", cm.getChannels().get(0).getName());
		String defaultMsgFormat = config.getString(globals + "default-message-format", "{player}: ");
		String incomingTellFormat = config.getString(globals + "incoming-tell-format", "{prefix}{player} &8->&d ");
		String outgoingTellFormat = config.getString(globals + "outgoing-tell-format", "{prefix}{player} &8->&d "); 
		List<String> censors = config.getStringList(globals + "censors");
		boolean separateChatLog = config.getBoolean(globals + "separate-chat-log", false);

		plugin.setTag(pluginTag);
		plugin.setIrcTag(ircTag);
		plugin.setIrcMessageFormat(ircMessageFormat);
		plugin.setCensors(censors);
		plugin.setIncomingTellFormat(incomingTellFormat);
		plugin.setOutgoingTellFormat(outgoingTellFormat);
		cm.setDefaultChannel(cm.getChannel(defaultChannel));
		cm.setDefaultMsgFormat(defaultMsgFormat);
		plugin.setSeparateChatLog(separateChatLog);
	}

	private void loadChannels(YamlConfiguration config) {
		List<Channel> list = new ArrayList<Channel>();
		ConfigurationSection channels = config.getConfigurationSection("channels");
		if(channels != null) {
			Set<String> keys = channels.getKeys(false);
			for (String s : keys) {
				String root = "channels." + s + ".";
				Channel c;
				if (config.getBoolean(root + "options.local", false)) {
					c = new LocalChannel(plugin);
					((LocalChannel) c).setDistance(config.getInt(root + "local-distance", 100));
				} else {
					c = new Channel(plugin);
				}

				c.setName(s);
				c.setNick(config.getString(root + "nickname", "DEFAULT-NICK"));
				c.setPassword(config.getString(root + "password", ""));
				c.setColor(ChatColor.valueOf(config.getString(root + "color", "WHITE")));
				c.setMsgFormat(config.getString(root + "message-format", "{default}"));
				c.setWorlds(config.getStringList(root + "worlds"));

				String craftIRC = root + "craftIRC.";
				c.setIRCToGameTags(config.getStringList(craftIRC + "IRC-to-game"));
				c.setGameToIRCTags(config.getStringList(craftIRC + "game-to-IRC"));

				String options = root + "options.";
				c.setVerbose(config.getBoolean(options + "join-messages", true));
				c.setQuickMessagable(config.getBoolean(options + "shortcut-allowed", false));
				c.setHidden(config.getBoolean(options + "hidden", false));
				c.setAutoJoined(config.getBoolean(options + "auto-join", false));
				c.setForced(config.getBoolean(options + "forced", false));
				c.setCrossWorld(config.getBoolean(options + "cross-world-chat", true));

				String lists = root + "lists.";
				c.setBlacklist(config.getStringList(lists + "bans"));
				c.setModerators(config.getStringList(lists + "moderators"));

				String permissions = root + "permissions.";
				c.setWhitelist(config.getStringList(permissions + "join"));
				c.setVoicelist(config.getStringList(permissions + "speak"));

				list.add(c);
			}
		}
		plugin.getChannelManager().setChannels(list);
	}

	public void loadPlayer(String name) {
		File userConfigFile = new File(usersConfigFolder, name + ".yml");
		try {
			YamlConfiguration config = new YamlConfiguration();
			create(userConfigFile);
			config.load(userConfigFile);
			ChannelManager channelManager = plugin.getChannelManager();
			try {
				String activeChannelName = config.getString("active-channel", channelManager.getDefaultChannel().getName());
				Channel activeChannel = channelManager.getChannel(activeChannelName);
				if (activeChannel != null) {
					channelManager.setActiveChannel(name, activeChannelName);
				} else {
					channelManager.setActiveChannel(name, channelManager.getDefaultChannel().getName());
				}

				List<String> joinedChannels = config.getStringList("joined-channels");
				if (joinedChannels.isEmpty()) {
					channelManager.joinAutoChannels(name);
				} else {
					for (String s : joinedChannels) {
						Channel c = channelManager.getChannel(s);
						if (c != null) {
							List<String> whitelist = c.getWhitelist();
							Player player = plugin.getServer().getPlayer(name);
							if (!c.getBlacklist().contains(name)
									&& (whitelist.isEmpty()
											|| plugin.getPermissionManager().getGroups(player).length == 0
											|| plugin.getPermissionManager().anyGroupsInList(player, whitelist))) {
								c.addPlayer(name);
							}
						}
					}
				}
			} catch (Exception e) {
				channelManager.setActiveChannel(name, channelManager.getDefaultChannel().getName());
				channelManager.joinAutoChannels(name);
				plugin.log(Level.INFO, "Loaded default settings for " + name);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void save() throws Exception {
		YamlConfiguration config = new YamlConfiguration();
		create(primaryConfigFile);
		config.load(primaryConfigFile);
		saveGlobals(config);
		saveChannels(config);
		config.save(primaryConfigFile);

		for (Player player : plugin.getServer().getOnlinePlayers()) {
			savePlayer(player.getName());
		}
	}

	private void saveGlobals(YamlConfiguration config) throws Exception {
		ChannelManager cm = plugin.getChannelManager();
		String globals = "globals.";
		config.set(globals + "plugin-tag", plugin.getTag());
		config.set(globals + "craftIRC-prefix", plugin.getIrcTag());
		config.set(globals + "craftIRC-message-format", plugin.getIrcMessageFormat());
		config.set(globals + "incoming-tell-format", plugin.getIncomingTellFormat());
		config.set(globals + "outgoing-tell-format", plugin.getOutgoingTellFormat());
		config.set(globals + "default-channel", cm.getDefaultChannel().getName());
		config.set(globals + "default-message-format", cm.getDefaultMsgFormat());
		config.set(globals + "censors", plugin.getCensors());
		config.set(globals + "separate-chat-log", plugin.hasSeparateChatLog());
	}

	private void saveChannels(YamlConfiguration config) throws Exception {
		Channel[] channels = plugin.getChannelManager().getChannels().toArray(new Channel[0]);
		for (Channel c : channels) {
			String root = "channels." + c.getName() + ".";
			config.set(root + "nickname", c.getNick());
			config.set(root + "password", c.getPassword());
			config.set(root + "color", c.getColor().toString());
			config.set(root + "message-format", c.getMsgFormat());
			config.set(root + "worlds", c.getWorlds());
			if (c instanceof LocalChannel) {
				config.set(root + "local-distance", ((LocalChannel) c).getDistance());
			}

			String craftIRC = root + "craftIRC.";
			config.set(craftIRC + "IRC-to-game", c.getIRCToGameTags());
			config.set(craftIRC + "game-to-IRC", c.getGameToIRCTags());

			String options = root + "options.";
			config.set(options + "join-messages", c.isVerbose());
			config.set(options + "shortcut-allowed", c.isQuickMessagable());
			config.set(options + "hidden", c.isHidden());
			config.set(options + "auto-join", c.isAutoJoined());
			config.set(options + "local", c instanceof LocalChannel);
			config.set(options + "forced", c.isForced());
			config.set(options + "cross-world-chat", c.isCrossWorld());

			String lists = root + "lists.";
			config.set(lists + "bans", c.getBlacklist());
			config.set(lists + "moderators", c.getModerators());

			String permissions = root + "permissions.";
			config.set(permissions + "join", c.getWhitelist());
			config.set(permissions + "speak", c.getVoicelist());
		}
	}

	public void savePlayer(String name) {
		File userConfigFile = new File(usersConfigFolder, name + ".yml");
		try {
			YamlConfiguration config = new YamlConfiguration();
			create(userConfigFile);
			config.load(userConfigFile);
			ChannelManager configManager = plugin.getChannelManager();
			Channel active = configManager.getActiveChannel(name);
			List<Channel> joinedChannels = configManager.getJoinedChannels(name);
			List<String> joinedChannelNames = new ArrayList<String>();
			for (Channel channel : joinedChannels) {
				joinedChannelNames.add(channel.getName());
			}
			config.set("active-channel", active.getName());
			config.set("joined-channels", joinedChannelNames);
			config.save(userConfigFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void create(File file) {
		if(!file.exists()) {
			try {
				file.getParentFile().mkdirs();
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
