/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2638
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.shared.patches;

import static java.lang.Boolean.TRUE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.settings.BaseSettings;
import app.morphe.extension.shared.settings.SharedYouTubeSettings;

@SuppressWarnings("unused")
public final class EnableDebuggingPatch {

    /**
     * Only log if debugging is enabled on startup.
     * This prevents enabling debugging
     * while the app is running then failing to restart
     * resulting in an incomplete log.
     */
    private static final boolean LOG_FEATURE_FLAGS = BaseSettings.DEBUG.get();

    private static final ConcurrentMap<Long, Boolean> featureFlags = LOG_FEATURE_FLAGS
            ? new ConcurrentHashMap<>(800, 0.5f, 1)
            : null;

    private static final Set<Long> DISABLED_FEATURE_FLAGS = parseFlags(SharedYouTubeSettings.DISABLED_FEATURE_FLAGS.get());

    private static final Set<Long> FORCED_FEATURE_FLAGS = parseFlags(SharedYouTubeSettings.FORCED_FEATURE_FLAGS.get());

    // Log all overridden flags on app startup.
    static {
        if (LOG_FEATURE_FLAGS) {
            logFlags("Disabled feature flags:", DISABLED_FEATURE_FLAGS);
            logFlags("Forced feature flags:", FORCED_FEATURE_FLAGS);
        }
    }

    private static void logFlags(String header, Set<Long> flags) {
        if (flags.isEmpty()) return;

        StringBuilder sb = new StringBuilder(header);
        sb.append('\n');
        for (Long flag : flags) {
            sb.append("  ").append(flag).append('\n');
        }
        Logger.printDebug(sb::toString);
    }

    /**
     * Injection point.
     */
    public static boolean isBooleanFeatureFlagEnabled(boolean value, long flag) {
        if (flag == 45751092) return true;

        if (LOG_FEATURE_FLAGS) {
            Long flagObj = flag;
            if (DISABLED_FEATURE_FLAGS.contains(flagObj)) {
                return false;
            }
            // A flag the app turns off is never logged, so forcing one on is the only
            // way to test what a flag does when the app did not enable it.
            if (FORCED_FEATURE_FLAGS.contains(flagObj)) {
                return true;
            }
            if (value && featureFlags.putIfAbsent(flagObj, TRUE) == null) {
                Logger.printDebug(() -> "boolean feature is enabled: " + flag);
            }
        }

        return value;
    }

    /**
     * Injection point.
     */
    public static double isDoubleFeatureFlagEnabled(double value, long flag, double defaultValue) {
        if (LOG_FEATURE_FLAGS && defaultValue != value) {
            if (DISABLED_FEATURE_FLAGS.contains(flag)) {
                return defaultValue;
            }
            if (featureFlags.putIfAbsent(flag, true) == null) {
                // Align the log outputs to make post-processing easier.
                Logger.printDebug(() -> " double feature is enabled: " + flag
                        + " value: " + value + (defaultValue == 0 ? "" : " default: " + defaultValue));
            }
        }

        return value;
    }

    /**
     * Injection point.
     */
    public static long isLongFeatureFlagEnabled(long value, long flag, long defaultValue) {
        if (LOG_FEATURE_FLAGS && defaultValue != value) {
            if (DISABLED_FEATURE_FLAGS.contains(flag)) {
                return defaultValue;
            }
            if (featureFlags.putIfAbsent(flag, true) == null) {
                Logger.printDebug(() -> "   long feature is enabled: " + flag
                        + " value: " + value + (defaultValue == 0 ? "" : " default: " + defaultValue));
            }
        }

        return value;
    }

    /**
     * Injection point.
     */
    public static String isStringFeatureFlagEnabled(String value, long flag, String defaultValue) {
        if (LOG_FEATURE_FLAGS && !defaultValue.equals(value)) {
            if (DISABLED_FEATURE_FLAGS.contains(flag)) {
                return defaultValue;
            }
            if (featureFlags.putIfAbsent(flag, true) == null) {
                Logger.printDebug(() -> " string feature is enabled: " + flag
                        + " value: " + value + (defaultValue.isEmpty() ? "" : " default: " + defaultValue));
            }
        }

        return value;
    }

    /**
     * Get all logged feature flags.
     * @return Set of all known flags
     */
    public static Set<Long> getAllLoggedFlags() {
        if (featureFlags != null) {
            return new HashSet<>(featureFlags.keySet());
        }

        return new HashSet<>();
    }

    /**
     * Serializes flags into the format used by the settings.
     * @param flags Flag IDs to serialize
     * @return String containing newline-separated flag IDs
     */
    public static String serializeFlags(Collection<Long> flags) {
        return serializeFlags(flags, "\n");
    }

    /**
     * @param flags     Flag IDs to serialize
     * @param separator Separator to put between the flag IDs
     * @return String containing the separated flag IDs
     */
    public static String serializeFlags(Collection<Long> flags, String separator) {
        StringBuilder builder = new StringBuilder();
        for (Long flag : flags) {
            if (builder.length() != 0) {
                builder.append(separator);
            }
            builder.append(flag);
        }

        return builder.toString();
    }

    /**
     * Public method for parsing flags.
     * @param flags String containing flag IDs separated by commas or whitespace
     * @return Set of parsed flag IDs
     */
    public static Set<Long> parseFlags(String flags) {
        return new HashSet<>(parseFlagList(flags));
    }

    /**
     * @param flags String containing flag IDs separated by commas or whitespace
     * @return Parsed flag IDs, in the order they appear
     */
    public static List<Long> parseFlagList(String flags) {
        List<Long> parsedFlags = new ArrayList<>();
        if (!flags.isBlank()) {
            for (String flag : flags.split("[,\\s]+")) {
                String trimmedFlag = flag.trim();
                if (trimmedFlag.isEmpty()) continue; // Skip empty entries.
                try {
                    parsedFlags.add(Long.parseLong(trimmedFlag));
                } catch (NumberFormatException e) {
                    Logger.printException(() -> "Invalid flag ID: " + flag);
                }
            }
        }

        return parsedFlags;
    }
}
