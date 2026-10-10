
package com.emailintel.backend.dsa;

public class Similarity {

    public static int lcs(String a, String b) {

        String[] x = a.toLowerCase().split("\\W+");
        String[] y = b.toLowerCase().split("\\W+");

        int[][] dp = new int[x.length + 1][y.length + 1];

        for (int i = 1; i <= x.length; i++) {
            for (int j = 1; j <= y.length; j++) {

                if (x[i - 1].equals(y[j - 1])) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        return dp[x.length][y.length];
    }

    public static double score(String a, String b) {

        if (a == null || b == null || a.isBlank() || b.isBlank()) {
            return 0;
        }

        String[] x = a.toLowerCase().split("\\W+");
        String[] y = b.toLowerCase().split("\\W+");

        int lcsLength = lcs(a, b);

        int maxLength = Math.max(x.length, y.length);

        if (maxLength == 0) {
            return 0;
        }

        /*
         * LCS-based similarity:
         *
         * Example:
         * Email A: important exam tomorrow check instructions
         * Email B: important reminder exam tomorrow check instructions
         *
         * Most words are common, so the similarity score becomes high.
         */
        double similarity = (100.0 * lcsLength) / maxLength;

        return Math.round(similarity * 100.0) / 100.0;
    }
}

