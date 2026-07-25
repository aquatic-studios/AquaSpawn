package com.aquaticstudios.aquaspawn.utils;

import org.bukkit.permissions.Permissible;

public final class Permissions {

    public static final String ADMIN = "aquaspawn.admin";

    public static boolean has(Permissible who, String node) {
        return who.hasPermission(ADMIN) || who.hasPermission(node);
    }
}
