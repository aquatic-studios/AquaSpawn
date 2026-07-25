package com.aquaticstudios.aquaspawn.action;

import com.aquaticstudios.aquaspawn.utils.Placeholders;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Evaluates simple, human-readable conditions of the form {@code LEFT <operator> RIGHT}, where
 * either side may contain placeholders. Operators (spaces required around them):
 * <ul>
 *   <li>{@code ==} / {@code equals}, {@code !=} — case-insensitive text equality.</li>
 *   <li>{@code >}, {@code >=}, {@code <}, {@code <=} — numeric comparison.</li>
 *   <li>{@code contains} — substring (case-insensitive).</li>
 *   <li>{@code regex} — the left side matches the right side as a regular expression.</li>
 * </ul>
 * A line with no operator passes when it resolves to {@code true}.
 */
public final class Conditions {

    // longer/greedier operators first so ">=" is not mistaken for ">".
    private static final String[] OPERATORS = {" >= ", " <= ", " == ", " != ", " > ", " < ",
            " equals ", " contains ", " regex "};

    public boolean allMatch(Player player, List<String> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }
        for (String condition : conditions) {
            if (!match(player, condition)) {
                return false;
            }
        }
        return true;
    }

    public boolean match(Player player, String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return true;
        }
        String line = Placeholders.apply(player, raw).trim();
        for (String operator : OPERATORS) {
            int index = line.indexOf(operator);
            if (index > 0) {
                String left = line.substring(0, index).trim();
                String right = line.substring(index + operator.length()).trim();
                return compare(left, operator.trim(), right);
            }
        }
        return line.equalsIgnoreCase("true");
    }

    private boolean compare(String left, String operator, String right) {
        switch (operator) {
            case "==":
            case "equals":
                return left.equalsIgnoreCase(right);
            case "!=":
                return !left.equalsIgnoreCase(right);
            case "contains":
                return left.toLowerCase().contains(right.toLowerCase());
            case "regex":
                try {
                    return left.matches(right);
                } catch (RuntimeException e) {
                    return false;
                }
            case ">":
            case ">=":
            case "<":
            case "<=":
                return compareNumbers(left, operator, right);
            default:
                return false;
        }
    }

    private boolean compareNumbers(String left, String operator, String right) {
        try {
            double a = Double.parseDouble(left);
            double b = Double.parseDouble(right);
            switch (operator) {
                case ">":
                    return a > b;
                case ">=":
                    return a >= b;
                case "<":
                    return a < b;
                case "<=":
                    return a <= b;
                default:
                    return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
