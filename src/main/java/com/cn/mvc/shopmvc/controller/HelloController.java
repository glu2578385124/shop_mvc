package com.cn.mvc.shopmvc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author: KING
 * @description:
 * @Date:Created in 2020-09-16 20:20
 */
@Controller
public class HelloController {
    @RequestMapping(path = "/hello")
    public String sayHello(){
        System.out.println("---------");
        return "index";
    }
}