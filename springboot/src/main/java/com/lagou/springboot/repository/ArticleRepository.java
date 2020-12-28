package com.lagou.springboot.repository;

import com.lagou.springboot.pojo.Article;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Integer> {
}
