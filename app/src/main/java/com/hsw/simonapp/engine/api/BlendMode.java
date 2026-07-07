package com.hsw.simonapp.engine.api;

public enum BlendMode {
    ALPHA(0),
    ADDITIVE(1),
    MULTIPLY(2);

    private final int nativeValue;

    BlendMode(int nativeValue) {
        this.nativeValue = nativeValue;
    }

    int nativeValue() {
        return nativeValue;
    }

    static boolean isValidNativeValue(int nativeValue) {
        for (BlendMode blendMode : values()) {
            if (blendMode.nativeValue == nativeValue) {
                return true;
            }
        }
        return false;
    }

    static String describeNativeValues() {
        StringBuilder description = new StringBuilder();
        for (BlendMode blendMode : values()) {
            if (description.length() > 0) {
                description.append(", ");
            }
            description.append(blendMode.nativeValue)
                    .append("=")
                    .append(blendMode.name());
        }
        return description.toString();
    }
}
