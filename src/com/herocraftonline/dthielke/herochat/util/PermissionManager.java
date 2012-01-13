/**
 * Copyright (C) 2011 DThielke <dave.thielke@gmail.com>
 * 
 * This work is licensed under the Creative Commons Attribution-NonCommercial-NoDerivs 3.0 Unported License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/3.0/ or send a letter to
 * Creative Commons, 171 Second Street, Suite 300, San Francisco, California, 94105, USA.
 **/

package com.herocraftonline.dthielke.herochat.util;

import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import de.bananaco.bpermissions.api.Group;
import de.bananaco.bpermissions.api.WorldManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PermissionManager {

    private WorldManager wm = null;
    
    public PermissionManager() {
        try {
        	 wm = WorldManager.getInstance();
        } catch (Exception e) {
        	System.err.println("bPermissions not detected");
        }
    }
    
    public void registerPermissions(JavaPlugin herochat) {
    	PluginManager pm = herochat.getServer().getPluginManager();
    	Map<String, Boolean> children = new HashMap<String, Boolean>();
    	children.put("herochat.admin", true);
    	children.put("herochat.create", true);
    	children.put("herochat.color", true);
    	Permission permission = new Permission("herochat.*", PermissionDefault.OP, children);
    	// Boo superperms
    	pm.addPermission(permission);
    }

    public String[] getGroups(Player p) {
        if (wm != null) {
            try {
            String world = p.getWorld().getName();
            String name = p.getName();
            Set<String> groups = wm.getWorld(world).getUser(name).getGroupsAsString();
            return groups.toArray(new String[groups.size()]);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        return new String[0];
    }

    public boolean anyGroupsInList( Player p, List<String> list ) {
        String[] groups = getGroups(p);
        for (int i = 0; i < groups.length; i++) {
            if (list.contains(groups[i])) return true;
        }
        return false;
    }

    public String getGroup(Player p) {
        if (wm != null) {
            try {
            String group = null;
            String[] groups = getGroups(p);
            
            if(groups.length > 0)
            	group = groups[0];
            
            if (group == null) {
                group = "";
            }
            return group;
            } catch (Exception e) {
                System.out.println(e.getMessage());
                return "";
            }
        } else {
            return "";
        }
    }

    public String getGroupPrefix(Player p) {
        if (wm != null) {
            try {
                String[] groups = getGroups(p);
                for (int i = 0; i < groups.length; i++) {
                    String world = p.getWorld().getName();
                    Group group = wm.getWorld(world).getGroup(groups[i]);
                    
                    if (!group.getEffectiveValue("prefix").equals(""))
                            return group.getEffectiveValue("prefix").replaceAll("&([0-9a-f])", "§$1");
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        return "";
    }
    
    public String getGroupSuffix(Player p) {
        if (wm != null) {
            try {
                String[] groups = getGroups(p);
                for (int i = 0; i < groups.length; i++) {
                	String world = p.getWorld().getName();
                    Group group = wm.getWorld(world).getGroup(groups[i]);
                    if (!group.getEffectiveValue("prefix").equals(""))
                        return group.getEffectiveValue("prefix").replaceAll("&([0-9a-f])", "§$1");
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        return "";
    }

    public String getPrefix(Player p) {
        if (wm != null) {
            try {
                String world = p.getWorld().getName();
                String name = p.getName();
                String prefix = wm.getWorld(world).getUser(name).getEffectiveValue("prefix");
                return prefix.replaceAll("&([0-9a-f])", "§$1");
            } catch (Exception e) {
                System.out.println(e.getMessage());
                return "";
            }
        } else {
            return "";
        }
    }

    public String getSuffix(Player p) {
        if (wm != null) {
            try {
                String world = p.getWorld().getName();
                String name = p.getName();
                String suffix = wm.getWorld(world).getUser(name).getEffectiveValue("suffix");
                return suffix.replaceAll("&([0-9a-f])", "§$1");
            } catch (Exception e) {
                System.out.println(e.getMessage());
                return "";
            }
        } else {
            return "";
        }
    }

    public boolean isAdmin(Player p) {
        return p.hasPermission("herochat.admin");
    }

    public boolean isAllowedColor(Player p) {
            return p.hasPermission("herochat.color");
    }

    public boolean canCreate(Player p) {
            boolean admin = p.hasPermission("herochat.admin");
            boolean create = p.hasPermission("herochat.create");
            return admin || create;
    }

}
