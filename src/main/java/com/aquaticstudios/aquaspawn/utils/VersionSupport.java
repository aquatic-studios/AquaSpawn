package com.aquaticstudios.aquaspawn.utils;

/**
 * Small capability checks derived from the running server version. Keeps version math in one
 * place instead of scattering {@code getBukkitVersion} parsing around the code base.
 */
public final class VersionSupport {

    private static final int MINOR = parseMinor(VersionUtil.getVersion());

    private VersionSupport() {
    }

    /** @return the minor version, e.g. {@code 16} for {@code 1.16.5}, or {@code 0} if unknown. */
    public static int minor() {
        return MINOR;
    }

    /** Hex (RGB) chat colors landed in Minecraft 1.16. */
    public static boolean supportsHexColors() {
        return MINOR >= 16;
    }

    private static int parseMinor(String version) {
        try {
            String[] parts = version.split("\\.");
            return parts.length >= 2 ? Integer.parseInt(parts[1]) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
