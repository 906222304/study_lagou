package com.lagou.springboot.controller;

import com.lagou.springboot.pojo.Article;
import com.lagou.springboot.service.ArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
public class ArticleController {

    @Autowired
    ArticleService articleService;

    @RequestMapping("/getArticle")
    public String getArticle(Model model, Integer page) {
        if (page == null) {
            page = 1;
        }
        Page<Article> articles = articleService.getArticle(page);
        model.addAttribute("articles", articles);
        return "index";
    }
}
