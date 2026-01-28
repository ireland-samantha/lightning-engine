package ca.samanthaireland.lightning.engine.util;

import java.util.UUID;

public class IdGeneratorV2 {
    public static long newId() {
        return Math.abs(UUID.randomUUID().getLeastSignificantBits());
    }
}
