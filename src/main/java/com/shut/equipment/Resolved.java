package com.shut.equipment;

/** A catalog entry found or created from a name a member typed; {@code created} says which. */
public record Resolved<T>(T value, boolean created) {
}
