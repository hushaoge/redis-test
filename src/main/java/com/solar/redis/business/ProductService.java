package com.solar.redis.business;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.solar.redis.cache.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.nio.charset.Charset;
import java.util.Objects;

/**
 * @author hushaoge
 * @date 2024/11/13 10:31
 * @description
 */
@Slf4j
@Service
public class ProductService {
    /**
     * 白名单方式需要提前加载数据到过滤器中，但是新产生的数据需要后续添加进去。但是如果是分布式的，则同步到每一应用中存在问题，可以考虑redis的布隆过滤器了
     * 黑名单方式不需要提前加载，但后续数据产生后，可能存在已经在黑名单中的情况
     *
     */
    private BloomFilter<CharSequence> whiteBloomFilter = BloomFilter.create(Funnels.stringFunnel(Charset.defaultCharset()), 1000000, 0.01);
    private BloomFilter<CharSequence> blackBloomFilter = BloomFilter.create(Funnels.stringFunnel(Charset.defaultCharset()), 1000000, 0.01);

    @Resource
    private RedisService redisService;

    public void initBloom() {
        for (int i = 0; i < 1000; i++) {
            whiteBloomFilter.put(String.valueOf(i));
        }
    }

    /**
     * 采用双检，类似单例写法获取信息，防止缓存击穿
     * 在分布式部署的情况下，虽然每台服务器都需要查询一次（总查询次数可能为10，20），但这样的查询数据库还是可以顶住的
     * @param productId
     * @return
     */
    public Object getProduct(String productId) {
        Object product = redisService.getValueByKey(productId);
        if(product == null){
            // 从数据库获取
            // 可以用分布式锁，根据实际情况
            synchronized (this) {
                // 二次查询，防缓存击穿
                product = redisService.getValueByKey(productId);
                if (!Objects.isNull(product)) {
                    return product;
                }
                // TODO get product or other info from db
                product = "demo product";
                redisService.setKeyValue(productId, product);
            }
        }
        return product;
    }

    /**
     * 布隆过滤器，白名单
     * @param productId
     * @return
     */
    public Object getProductWhiteBloom(String productId) {
        // 加入一层布隆过滤
        if (!whiteBloomFilter.mightContain(productId)) {
            // 布隆过滤器中不存在，则表示数据不存在
            log.warn("数据不存在");
            return null;
        }
        return getProduct(productId);
    }

    /**
     * 布隆过滤器，黑名单
     * @param productId
     * @return
     */
    public Object getProductBlackBloom(String productId) {
        // 加入一层布隆过滤
        if (blackBloomFilter.mightContain(productId)) {
            // 布隆过滤器中存在，则表示数据不存在，ID已经加入黑名单
            log.warn("数据不存在");
            return null;
        }
        Object product = getProduct(productId);
        if(Objects.isNull(product)) {
            // 加入到黑名单
            // 但是这里有个问题，现在没有但是以后有了怎么办，bloom没办法直接删除元素。只能通过重启了
            blackBloomFilter.put(productId);
        }
        return getProduct(productId);
    }
}
