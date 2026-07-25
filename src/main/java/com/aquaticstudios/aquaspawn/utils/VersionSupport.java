package com.aquaticstudios.aquaspawn.utils;

public final class VersionSupport {

    private static final int MINOR = parseMinor(VersionUtil.getVersion());

    private VersionSupport() {
    }

    public static int minor() {
        return MINOR;
    }

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
