package org.gumel.jojoha.client;

import net.minecraft.Util;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClientQuests {
    public record Entry(String id, int done, int needed, long mark) {
        public boolean marked() {
            return mark != Long.MIN_VALUE;
        }

        public boolean ready() {
            return done >= needed;
        }
    }

    private static List<Entry> entries = List.of();
    private static final Map<String, Long> changedAt = new HashMap<>();
    private static final Map<String, Long> addedAt = new HashMap<>();
    @Nullable
    private static String tracked;
    private static boolean primed;
    private static List<String> offering = List.of();

    private ClientQuests() {
    }

    public static void set(List<String> ids, List<Integer> done, List<Integer> needed, List<Long> marks,
                           List<String> givers) {
        offering = List.copyOf(givers);
        Map<String, Entry> before = new HashMap<>();
        for (Entry entry : entries) {
            before.put(entry.id(), entry);
        }
        long now = Util.getMillis();
        List<Entry> fresh = new ArrayList<>();
        for (int i = 0; i < ids.size() && i < done.size() && i < needed.size() && i < marks.size(); i++) {
            Entry entry = new Entry(ids.get(i), done.get(i), needed.get(i), marks.get(i));
            Entry old = before.get(entry.id());
            if (old == null) {
                addedAt.put(entry.id(), primed ? now : 0L);
                if (primed) {
                    tracked = entry.id();
                }
            } else if (old.done() != entry.done()) {
                changedAt.put(entry.id(), now);
            }
            fresh.add(entry);
        }
        entries = List.copyOf(fresh);
        primed = true;
        if (tracked != null && entries.stream().noneMatch(entry -> entry.id().equals(tracked))) {
            tracked = null;
        }
    }

    public static boolean offering(String giver) {
        return offering.contains(giver);
    }

    public static boolean anyReady() {
        return entries.stream().anyMatch(Entry::ready);
    }

    public static List<Entry> entries() {
        return entries;
    }

    @Nullable
    public static Entry tracked() {
        for (Entry entry : entries) {
            if (entry.id().equals(tracked)) {
                return entry;
            }
        }
        return entries.isEmpty() ? null : entries.get(0);
    }

    public static boolean isTracked(Entry entry) {
        Entry current = tracked();
        return current != null && current.id().equals(entry.id());
    }

    public static void track(String id) {
        tracked = id;
    }

    public static long changedAt(String id) {
        return changedAt.getOrDefault(id, 0L);
    }

    public static long addedAt(String id) {
        return addedAt.getOrDefault(id, 0L);
    }
}
