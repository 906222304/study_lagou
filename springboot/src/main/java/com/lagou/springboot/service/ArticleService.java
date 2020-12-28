package com.lagou.springboot.service;

import com.lagou.springboot.pojo.Article;
import com.lagou.springboot.repository.ArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArticleService {

    @Autowired
    ArticleRepository articleRepository;

    public Page<Article> getArticle(int page) {
        Page<Article> articles = articleRepository.findAll(PageRequest.of(page - 1, 3));
        return articles;
    }

}
