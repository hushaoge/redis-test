package com.solar.redis.bloom;

import java.util.BitSet;
import java.util.Objects;

/**
 * @author hushaoge
 * @date 2024/11/13 11:19
 * @description
 */
public class SolarBloomFilter {
    /**
     * 位数组大小 33554432
     */
    private static final int DEFAULT_SIZE = 2 << 24;

    /**
     * 通过这个数组创建多个Hash函数
     */
    private static final int[] SEEDS = new int[]{7, 19, 61, 89, 129, 179, 241};

    /**
     * 初始化位数组，数组中的元素只能是 0 或者 1
     */
    private BitSet bits = new BitSet(DEFAULT_SIZE);

    /**
     * Hash函数数组
     */
    private SolarHash[] solarHashes = new SolarHash[SEEDS.length];

    /**
     * 初始化多个包含 Hash 函数的类数组，每个类中的 Hash 函数都不一样
     */
    public SolarBloomFilter() {
        // 初始化多个不同的 Hash 函数
        for (int i = 0; i < SEEDS.length; i++) {
            solarHashes[i] = new SolarHash(DEFAULT_SIZE, SEEDS[i]);
        }
    }

    /**
     * 添加元素到位数组
     */
    public void add(Object value) {
        for (SolarHash solarHash : solarHashes) {
            bits.set(solarHash.hash(value), true);
        }
    }

    /**
     * 判断指定元素是否存在于位数组
     */
    public boolean contains(Object value) {
        boolean result = true;
        for (SolarHash solarHash : solarHashes) {
            result = result && bits.get(solarHash.hash(value));
        }
        return result;
    }

    /**
     * 自定义 Hash 函数
     */
    private class SolarHash {
        private int cap;
        private int seed;

        SolarHash(int cap, int seed) {
            this.cap = cap;
            this.seed = seed;
        }

        /**
         * 计算 Hash 值
         */
        int hash(Object obj) {
            return Objects.isNull(obj) ? 0 : Math.abs(seed * (cap - 1) & (obj.hashCode() ^ (obj.hashCode() >>> 16)));
        }
    }

    public static void main(String[] args) {
        long capacity = 10000000L;
        System.out.println(2 << 24);

        SolarBloomFilter solarBloomFilter = new SolarBloomFilter();
        //put值进去
        for (long i = 0; i < capacity; i++) {
            solarBloomFilter.add(i);
        }
        // 统计误判次数
        int count = 0;
        // 我在数据范围之外的数据，测试相同量的数据，判断错误率是不是符合我们当时设定的错误率
        for (long i = capacity; i < capacity * 2; i++) {
            if (solarBloomFilter.contains(i)) {
                count++;
            }
        }
        System.out.println(count);
    }

}
