package com.neutron.usermatchbackend.util;

import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author zzs
 * @date 2023/3/31 16:48
 */
public class AlgorithmUtil {

    /**
     * 编辑距离算法
     *
     * @param word1 字符串1
     * @param word2 字符串2
     * @return 相似度，返回值越小，相似度越大
     */
    public static int minDistance(List<String> word1, List<String> word2) {
        word1 = word1.stream().sorted().collect(Collectors.toList());
        word2 = word2.stream().sorted().collect(Collectors.toList());
        int n = word1.size();
        int m = word2.size();

        if (n * m == 0) {
            return n + m;
        }

        int[][] d = new int[n + 1][m + 1];
        for (int i = 0; i < n + 1; i++) {
            d[i][0] = i;
        }

        for (int j = 0; j < m + 1; j++) {
            d[0][j] = j;
        }

        for (int i = 1; i < n + 1; i++) {
            for (int j = 1; j < m + 1; j++) {
                int left = d[i - 1][j] + 1;
                int down = d[i][j - 1] + 1;
                int left_down = d[i - 1][j - 1];
                if (!Objects.equals(word1.get(i - 1), word2.get(j - 1))) {
                    left_down += 1;
                }
                d[i][j] = Math.min(left, Math.min(down, left_down));
            }
        }
        return d[n][m];
    }

}
