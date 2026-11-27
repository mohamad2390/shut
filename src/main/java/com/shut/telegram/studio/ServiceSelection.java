package com.shut.telegram.studio;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** The services ticked so far while a package is being made; kept in the conversation as a comma separated text. */
final class ServiceSelection {

    private final Set<Long> ids;

    private ServiceSelection(Set<Long> ids) {
        this.ids = ids;
    }

    static ServiceSelection parse(String raw) {
        Set<Long> ids = new LinkedHashSet<>();
        if (raw != null && !raw.isBlank()) {
            for (String part : raw.split(",")) {
                try {
                    ids.add(Long.parseLong(part.strip()));
                } catch (NumberFormatException ignored) {
                    // a damaged entry is dropped
                }
            }
        }
        return new ServiceSelection(ids);
    }

    /** The selection with this service ticked, or unticked if it was ticked. */
    ServiceSelection toggled(long id) {
        Set<Long> next = new LinkedHashSet<>(ids);
        if (!next.remove(id)) {
            next.add(id);
        }
        return new ServiceSelection(next);
    }

    Set<Long> ids() {
        return Set.copyOf(ids);
    }

    boolean isEmpty() {
        return ids.isEmpty();
    }

    int size() {
        return ids.size();
    }

    boolean contains(long id) {
        return ids.contains(id);
    }

    String encode() {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}
