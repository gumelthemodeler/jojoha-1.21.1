package org.gumel.jojoha.dream;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.gumel.jojoha.data.Discipline;
import org.gumel.jojoha.data.DreamMemory;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StatPerk;
import org.gumel.jojoha.fortune.Fortune;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class DreamLines {

    private static final String P = "message.jojoha.dream.";

    private static final long SOON_NIGHTS = 4;

    private static final long LONG_NIGHTS = 100;

    private static final float MENTIONS_OFFERING = 0.5F;

    private static final float MENTIONS_STAND = 0.35F;

    private static final int[] REMARKABLE_VISITS = {5, 10, 25, 50};

    static final int LORE = 4;

    private DreamLines() {
    }

    static Component line(String key, Object... args) {
        return Component.translatable(P + key, args);
    }

    static Component standName(JojohaPlayerData data) {
        if (!data.stand.isPresent()) {
            return null;
        }
        return standName(data.stand.standId());
    }

    static Component standName(ResourceLocation id) {
        return Component.translatable("stand." + id.getNamespace() + "." + id.getPath());
    }

    static Component wantsAligned(ResourceLocation standId) {
        return line("wants.align", standName(standId));
    }

    static int leaningBand(float odds) {
        return odds < 0.6F ? 0 : odds < 0.8F ? 1 : 2;
    }

    static List<Component> greeting(ServerPlayer player, DreamMemory before, long today) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        RandomSource random = player.getRandom();
        Component stand = standName(data);
        List<Component> lines = new ArrayList<>();

        if (before.stranger()) {
            lines.add(line("meet.0"));
            lines.add(line("meet.1"));
            if (stand != null) {
                lines.add(line("stand.outside", stand));
            }
            lines.add(line("meet.2"));
            return lines;
        }

        long nights = before.lastDay() < 0 ? LONG_NIGHTS : today - before.lastDay();
        if (nights <= SOON_NIGHTS) {
            lines.add(line("again"));
        } else if (nights < LONG_NIGHTS) {
            lines.add(line("again.nights", nights));
        } else {
            lines.add(line("again.long"));
        }

        int visit = before.visits() + 1;
        for (int remarkable : REMARKABLE_VISITS) {
            if (visit == remarkable) {
                lines.add(line("visits", visit));
            }
        }

        switch (before.lastOutcome()) {
            case DreamMemory.GRANTED -> {
                if (!before.lastWish().isEmpty()) {
                    lines.add(line("last.granted." + before.lastWish()));
                }
                if (!before.lastOffering().isEmpty() && random.nextFloat() < MENTIONS_OFFERING) {
                    Component paid = itemName(before.lastOffering());
                    if (paid != null) {
                        lines.add(line("last.offering", paid));
                    }
                }
            }
            case DreamMemory.WOKE -> lines.add(line("last.woke"));
            case DreamMemory.IGNORED -> lines.add(line("last.ignored"));
            default -> {
            }
        }

        String nowStand = data.stand.isPresent() ? data.stand.standId().toString() : "";
        if (stand != null && !nowStand.equals(before.lastStand())) {
            lines.add(line("stand.new", stand));
        } else if (stand == null && !before.lastStand().isEmpty()) {
            lines.add(line("stand.gone"));
        } else if (stand != null && random.nextFloat() < MENTIONS_STAND) {
            lines.add(line("stand.outside", stand));
        }

        lines.add(line("ask"));
        return lines;
    }

    static Component lore(int heard) {
        return heard < LORE ? line("lore." + heard) : line("lore.done");
    }

    static Component wants(DreamOffering.Wish wish, JojohaPlayerData data) {
        Component stand = standName(data);
        return stand != null && namesTheStand(wish) ? line("wants." + wish.id(), stand) : line("wants." + wish.id());
    }

    static Component reply(DreamOffering.Wish wish, JojohaPlayerData data) {
        Component stand = standName(data);
        return stand != null && namesTheStand(wish) ? line("reply." + wish.id(), stand) : line("reply." + wish.id());
    }

    private static boolean namesTheStand(DreamOffering.Wish wish) {
        return wish == DreamOffering.Wish.STAND || wish == DreamOffering.Wish.HOLLOW;
    }

    static Component replyFor(DreamOffering.Wish wish, JojohaPlayerData data) {
        if (wish == DreamOffering.Wish.FORGET) {
            Discipline named = data.discipline == Discipline.NONE ? Discipline.ANUBIS : data.discipline;
            return line("reply.forget",
                    Component.translatable("discipline.jojoha." + named.name().toLowerCase(Locale.ROOT)));
        }
        return reply(wish, data);
    }

    static Component price(ServerPlayer player, DreamOffering.Wish wish) {
        int diamond = Worth.perItem(new ItemStack(Items.DIAMOND));
        if (!wish.scales()) {
            return line("price", inYourCoin(player, DreamOffering.price(player, wish)));
        }
        int price = DreamOffering.price(player, wish);
        return price < diamond ? line("price.any") : line("price.least", inYourCoin(player, price));
    }

    static Component notEnough(ServerPlayer player, DreamOffering.Wish wish, int offered) {
        return line("not_enough", inYourCoin(player, Math.max(1, DreamOffering.price(player, wish) - offered)));
    }

    static Component inYourCoin(ServerPlayer player, int missing) {
        ItemStack under = ItemStack.EMPTY, over = ItemStack.EMPTY;
        for (ItemStack stack : player.getInventory().items) {
            int each = Worth.perItem(stack);
            if (each <= 0 || stack.getCount() * each < Math.min(missing, each)) {
                continue;
            }
            if (each <= missing && (under.isEmpty() || each > Worth.perItem(under))) {
                under = stack;
            } else if (each > missing && (over.isEmpty() || each < Worth.perItem(over))) {
                over = stack;
            }
        }
        ItemStack coin = !under.isEmpty() ? under : !over.isEmpty() ? over : new ItemStack(Items.DIAMOND);
        int each = Worth.perItem(coin);
        int count = Math.max(1, (missing + each - 1) / each);
        return line("gift.some", count, coin.getItem().getDescription());
    }

    static Component wantsStat(DreamOffering.Wish wish, JojohaPlayerData data, int stat) {
        String side = wish == DreamOffering.Wish.STAND ? "stand." : "player.";
        Component part = line("part." + side + stat);
        Component stand = standName(data);
        return stand != null && wish == DreamOffering.Wish.STAND
                ? line("wants.stand", stand, part) : line("wants.self", part);
    }

    static Component taken(ItemStack offering, DreamMemory before) {
        String id = BuiltInRegistries.ITEM.getKey(offering.getItem()).toString();
        Component name = offering.getHoverName();
        return id.equals(before.lastOffering()) ? line("taken.same", name) : line("taken", name);
    }

    static List<Component> told(DreamOffering.Result result) {
        DreamOffering.Raised raised = result.raised();
        List<Component> lines = new ArrayList<>();

        if (!raised.gifts().isEmpty()) {
            lines.add(line("gift", listed(raised.gifts())));
            if (!result.message().endsWith("." + DreamOffering.Wish.NIGHT.id())) {
                lines.add(gamble(DreamOffering.worth(raised.gifts()), result.paid()));
            }
        }

        if (raised.forgot() != null) {
            lines.add(line("forget.settled", Component.translatable(raised.forgot())));
        }

        if (raised.trait() != null) {
            if (raised.trait().legendary()) {
                lines.add(line("fate.rare"));
            }
            lines.add(line("fate.settled", Component.translatable(raised.trait().nameKey())));
            lines.add(Component.translatable(raised.trait().descriptionKey()));
        }

        if (raised.fortune() != null) {
            Fortune fortune = Fortune.byId(raised.fortune());
            if (fortune != null) {
                lines.add(line("fortune.settled", Component.translatable(fortune.nameKey())));
                lines.add(Component.translatable(fortune.descriptionKey()));
            }
        }

        if (raised.aligned() != null) {
            lines.add(line("align.settled", standName(raised.aligned())));
            lines.add(line("align.odds." + leaningBand(raised.odds())));
        }

        if (raised.stat() >= 0) {
            lines.add(line("boost." + (raised.stand() ? "stand." : "player.") + raised.stat()));
            lines.add(line("boost.amount." + band(raised.levels())));
            for (StatPerk perk : raised.perks()) {
                lines.add(line("perk", Component.translatable(perk.nameKey())));
                lines.add(Component.translatable(perk.descriptionKey()));
            }
            if (raised.trust() != null) {
                lines.add(line("trust", Component.translatable(raised.trust())));
            }
            if (raised.rank() != null) {
                lines.add(line("rank", Component.translatable(raised.rank())));
            }
        }
        return lines;
    }

    static int band(int levels) {
        if (levels <= 0) {
            return 0;
        }
        return levels <= 2 ? 1 : levels <= 4 ? 2 : levels <= 6 ? 3 : 4;
    }

    static Component gamble(int got, int paid) {
        if (got * 2 <= paid) {
            return line("gamble.robbed");
        }
        if (got < paid) {
            return line("gamble.lost");
        }
        if (got == paid) {
            return line("gamble.even");
        }
        return got < paid * 2 ? line("gamble.won") : line("gamble.jackpot");
    }

    private static Component listed(List<ItemStack> gifts) {
        List<Component> each = new ArrayList<>();
        for (ItemStack gift : gifts) {
            each.add(gift.getCount() > 1 ? line("gift.some", gift.getCount(), gift.getHoverName()) : gift.getHoverName());
        }
        if (each.size() == 1) {
            return each.get(0);
        }
        Component all = each.get(0);
        for (int i = 1; i < each.size(); i++) {
            all = line(i == each.size() - 1 ? "gift.and" : "gift.then", all, each.get(i));
        }
        return all;
    }

    private static Component itemName(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null || !BuiltInRegistries.ITEM.containsKey(key)) {
            return null;
        }
        return BuiltInRegistries.ITEM.get(key).getDescription();
    }
}
