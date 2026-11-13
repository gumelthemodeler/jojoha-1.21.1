package org.gumel.jojoha.dream;

import java.util.Arrays;

final class Change {

    private Change() {
    }

    static int[] charged(int[] each, int[] count, int price) {
        int kinds = each.length;
        int dearest = 0;
        long total = 0;
        for (int k = 0; k < kinds; k++) {
            dearest = Math.max(dearest, each[k]);
            total += (long) each[k] * count[k];
        }
        if (price <= 0) {
            return new int[kinds];
        }
        if (total < price) {
            return count.clone();
        }

        Integer[] order = new Integer[kinds];
        for (int k = 0; k < kinds; k++) {
            order[k] = k;
        }
        Arrays.sort(order, (a, b) -> Integer.compare(each[a], each[b]));

        int ceiling = price + dearest;
        boolean[][] reach = new boolean[kinds + 1][ceiling + 1];
        int[][] used = new int[kinds + 1][ceiling + 1];
        reach[0][0] = true;
        for (int i = 1; i <= kinds; i++) {
            int kind = order[i - 1];
            int worth = each[kind];
            int most = (int) Math.min(count[kind], (price + worth - 1L) / worth);
            for (int s = 0; s <= ceiling; s++) {
                for (int n = 0; n <= most && (long) n * worth <= s; n++) {
                    if (reach[i - 1][s - n * worth]) {
                        reach[i][s] = true;
                        used[i][s] = n;
                        break;
                    }
                }
            }
        }

        int best = -1;
        for (int s = price; s <= ceiling; s++) {
            if (reach[kinds][s]) {
                best = s;
                break;
            }
        }
        int[] take = new int[kinds];
        if (best < 0) {
            return count.clone();
        }
        for (int i = kinds, s = best; i >= 1; i--) {
            int kind = order[i - 1];
            take[kind] = used[i][s];
            s -= used[i][s] * each[kind];
        }
        return take;
    }
}
