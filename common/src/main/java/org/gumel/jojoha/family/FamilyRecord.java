package org.gumel.jojoha.family;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class FamilyRecord extends SavedData {
    private static final String NAME = "jojoha_family";

    private static final Factory<FamilyRecord> FACTORY = new Factory<>(FamilyRecord::new, FamilyRecord::load, null);

    public static final class Member {
        @Nullable UUID parent;
        final List<UUID> heirs = new ArrayList<>();
        String name = "";
        boolean bodyThief;
        long feverUntil;
        boolean pendingStir;
    }

    private final Map<UUID, Member> members = new HashMap<>();

    public static FamilyRecord of(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    private static FamilyRecord load(CompoundTag tag, HolderLookup.Provider registries) {
        FamilyRecord record = new FamilyRecord();
        for (Tag entry : tag.getList("members", Tag.TAG_COMPOUND)) {
            CompoundTag saved = (CompoundTag) entry;
            Member member = new Member();
            member.parent = saved.hasUUID("parent") ? saved.getUUID("parent") : null;
            if (saved.hasUUID("heir")) {
                member.heirs.add(saved.getUUID("heir"));
            }
            for (Tag heir : saved.getList("heirs", Tag.TAG_INT_ARRAY)) {
                member.heirs.add(NbtUtils.loadUUID(heir));
            }
            member.name = saved.getString("name");
            member.bodyThief = saved.getBoolean("thief");
            member.feverUntil = saved.getLong("fever");
            member.pendingStir = saved.getBoolean("stir");
            record.members.put(saved.getUUID("id"), member);
        }
        return record;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        members.forEach((id, member) -> {
            CompoundTag saved = new CompoundTag();
            saved.putUUID("id", id);
            if (member.parent != null) {
                saved.putUUID("parent", member.parent);
            }
            ListTag heirs = new ListTag();
            for (UUID heir : member.heirs) {
                heirs.add(NbtUtils.createUUID(heir));
            }
            saved.put("heirs", heirs);
            saved.putString("name", member.name);
            saved.putBoolean("thief", member.bodyThief);
            saved.putLong("fever", member.feverUntil);
            saved.putBoolean("stir", member.pendingStir);
            list.add(saved);
        });
        tag.put("members", list);
        return tag;
    }

    @Nullable
    public Member get(UUID id) {
        return members.get(id);
    }

    public Member member(UUID id, String name) {
        Member member = members.computeIfAbsent(id, key -> new Member());
        if (name != null && !name.isEmpty() && !name.equals(member.name)) {
            member.name = name;
            setDirty();
        }
        return member;
    }

    public String nameOf(@Nullable UUID id) {
        Member member = id == null ? null : members.get(id);
        return member == null || member.name.isEmpty() ? "?" : member.name;
    }

    @Nullable
    public UUID parentOf(UUID id) {
        Member member = members.get(id);
        return member == null ? null : member.parent;
    }

    public List<UUID> heirsOf(UUID id) {
        Member member = members.get(id);
        return member == null ? List.of() : List.copyOf(member.heirs);
    }

    public List<UUID> childrenOf(UUID id) {
        List<UUID> children = new ArrayList<>();
        members.forEach((other, member) -> {
            if (id.equals(member.parent)) {
                children.add(other);
            }
        });
        return children;
    }

    public Set<UUID> family(UUID id) {
        Set<UUID> seen = new LinkedHashSet<>();
        if (!members.containsKey(id)) {
            return seen;
        }
        Deque<UUID> open = new ArrayDeque<>();
        open.add(id);
        while (!open.isEmpty()) {
            UUID next = open.poll();
            if (!seen.add(next)) {
                continue;
            }
            UUID parent = parentOf(next);
            if (parent != null) {
                open.add(parent);
            }
            open.addAll(childrenOf(next));
        }
        if (seen.size() == 1 && parentOf(id) == null && childrenOf(id).isEmpty()) {
            seen.clear();
        }
        return seen;
    }

    public Set<UUID> descendants(UUID id) {
        Set<UUID> found = new LinkedHashSet<>();
        Deque<UUID> open = new ArrayDeque<>(childrenOf(id));
        while (!open.isEmpty()) {
            UUID next = open.poll();
            if (found.add(next)) {
                open.addAll(childrenOf(next));
            }
        }
        found.remove(id);
        return found;
    }

    public boolean related(UUID a, UUID b) {
        return !a.equals(b) && family(a).contains(b);
    }

    public void bindHeir(UUID parent, String parentName, UUID child, String childName) {
        member(parent, parentName).heirs.add(child);
        member(child, childName).parent = parent;
        setDirty();
    }

    public void bindBody(UUID thief, String thiefName, UUID body) {
        Member member = member(thief, thiefName);
        if (member.parent == null) {
            member.parent = body;
        }
        member.bodyThief = true;
        setDirty();
    }

    @Nullable
    public UUID releaseHeir(UUID parent, @Nullable UUID which) {
        Member member = members.get(parent);
        if (member == null || member.heirs.isEmpty() || which != null && !member.heirs.contains(which)) {
            return null;
        }
        UUID heir = which != null ? which : member.heirs.get(member.heirs.size() - 1);
        member.heirs.remove(heir);
        Member child = members.get(heir);
        if (child != null && parent.equals(child.parent)) {
            child.parent = null;
        }
        setDirty();
        return heir;
    }

    public boolean bodyThief(UUID id) {
        Member member = members.get(id);
        return member != null && member.bodyThief;
    }

    public long feverUntil(UUID id) {
        Member member = members.get(id);
        return member == null ? 0L : member.feverUntil;
    }

    public void setFever(UUID id, String name, long until) {
        member(id, name).feverUntil = until;
        setDirty();
    }

    public boolean takeStir(UUID id) {
        Member member = members.get(id);
        if (member == null || !member.pendingStir) {
            return false;
        }
        member.pendingStir = false;
        setDirty();
        return true;
    }

    public void markStir(UUID id) {
        Member member = members.get(id);
        if (member != null && !member.pendingStir) {
            member.pendingStir = true;
            setDirty();
        }
    }
}
