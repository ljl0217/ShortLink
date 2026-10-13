package com.nageoffer.shortlink.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ShortLinkNotFoundController {
    /**
     * 短链接跳转不存在的页面
     */
    @RequestMapping("/notfound/page")
    public String shortLinkNotFound() {
        return "notfound";
    }
}
