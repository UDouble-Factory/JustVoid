package io.mr_w98.justvoid.config;

import java.util.regex.Pattern;

record WorldRule(Pattern name, String dimension) {

    boolean matches(String worldName, String worldDimension) {
        if (dimension != null && !dimension.equals(worldDimension)) return false;
        return name == null || name.matcher(worldName).matches();
    }
}
