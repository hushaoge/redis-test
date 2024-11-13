package com.solar.redis.controller;

import com.solar.redis.business.ProductService;
import com.solar.redis.lock.LockService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @author hushaoge
 * @date 2024/11/13 12:27
 * @description
 */
@Controller
public class ProductController {

    @Resource
    private ProductService productCacheService;

    @Resource
    private LockService lockService;


    // http://127.0.0.1:8080/init
    @RequestMapping("/init")
    @ResponseBody
    public String init() {
        productCacheService.initBloom();
        return "成功";
    }

    // http://127.0.0.1:8080/product?productId=lisi
    @RequestMapping("/product")
    @ResponseBody
    public String getProduct(@RequestParam(name = "productId" ) String productId) {
        Object product = productCacheService.getProduct(productId);
        return Objects.toString(product);
    }

    // http://127.0.0.1:8080/product/white?productId=1
    @RequestMapping("/product/white")
    @ResponseBody
    public String getProductWhite(@RequestParam(name = "productId" ) String productId) {
        Object product = productCacheService.getProductWhiteBloom(productId);
        return Objects.toString(product);
    }

    // http://127.0.0.1:8080/product/black?productId=1
    @RequestMapping("/product/black")
    @ResponseBody
    public String getProductBlack(@RequestParam(name = "productId" ) String productId) {
        Object product = productCacheService.getProductBlackBloom(productId);
        return Objects.toString(product);
    }

    // http://127.0.0.1:8080/product/createNx?userId=1
    @RequestMapping("/product/createNx")
    @ResponseBody
    public String createOrderNx(@RequestParam(name = "userId" ) String userId) {
        String result = lockService.createOrderNx(userId, null);
        return result;
    }

    // http://127.0.0.1:8080/product/create?userId=1
    @RequestMapping("/product/create")
    @ResponseBody
    public String createOrder(@RequestParam(name = "userId" ) String userId) {
        String result = lockService.createOrder(userId, null);
        return result;
    }

    // http://127.0.0.1:8080/product/createComplex?userId=1
    @RequestMapping("/product/createComplex")
    @ResponseBody
    public String createOrderComplex(@RequestParam(name = "userId" ) String userId) {
        String result = lockService.createOrderComplex(userId, null);
        return result;
    }
}
