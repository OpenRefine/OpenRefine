public class EditDistance {

    /**
     * Calculates the edit distance between two strings using dynamic programming.
     *
     * @param s1 First string.
     * @param s2 Second string.
     * @return The edit distance between the two strings.
     */
    public static int calculateEditDistance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();

        // Create a 2D array to store results of subproblems
        int[][] dp = new int[m + 1][n + 1];

        // Fill dp[][] bottom up manner
        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                // If first string is empty, only option is to insert all characters of second string
                if (i == 0) {
                    dp[i][j] = j; // Min. operations = j
                }
                // If second string is empty, only option is to remove all characters of first string
                else if (j == 0) {
                    dp[i][j] = i; // Min. operations = i
                }
                // If last characters are the same, ignore the last character and recur for the remaining substring
                else if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                }
                // If the last character is different, consider all possibilities and find the minimum
                else {
                    dp[i][j] = 1 + Math.min(dp[i][j - 1], // Insert
                                            Math.min(dp[i - 1][j], // Remove
                                                     dp[i - 1][j - 1])); // Replace
                }
            }
        }

        return dp[m][n];
    }
