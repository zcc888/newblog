package com.myblog.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.myblog.lucene.BlogIndex;
import com.myblog.model.Blog;
import com.myblog.model.Category;
import com.myblog.model.KeyAndValue;
import com.myblog.service.IBlogService;
import com.myblog.service.ICategoryService;
import com.myblog.util.JedisUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Zephery on 2016/8/5.
 */

@Controller
public class IndexController {
    private static final Logger logger = LoggerFactory.getLogger(IndexController.class);
    @Resource
    private IBlogService blogService;
    @Resource
    private ICategoryService categoryService;
    private BlogIndex blogIndex = new BlogIndex();

    @RequestMapping("index")
    public ModelAndView index(@RequestParam(value = "typeId", required = false) String typeId,
                              @RequestParam(value = "releaseDateStr", required = false) String releaseDateStr,
                              HttpServletRequest request)
            throws Exception {
        String page = request.getParameter("pagenum");
        Integer pagenum;
        if (StringUtils.isEmpty(page)) {
            pagenum = 1;
        } else {
            pagenum = Integer.parseInt(page);
        }
        PageHelper.startPage(pagenum, 15);
        ModelAndView mav = new ModelAndView();
        List<Blog> lists = blogService.getAllBlog();
        List<Blog> banners = blogService.getBanner();
        PageInfo<Blog> blogs = new PageInfo<>(lists);
        Integer startpage, endpage;
        if (blogs.getPages() < 6) {
            startpage = 1;
            endpage = blogs.getPages();
        } else {
            if (pagenum > 3) {
                startpage = blogs.getPageNum() - 3;
                endpage = blogs.getPageNum() + 3 > blogs.getPages() ? blogs.getPages() : blogs.getPageNum() + 3;
            } else {
                startpage = 1;
                endpage = blogs.getPageNum() + 4;
            }
        }
        List<Blog> hotblogs = blogService.getByHits();
        mav.addObject("startpage", startpage);
        mav.addObject("endpage", endpage);
        mav.addObject("hotblogs", hotblogs);
        mav.addObject("blogs", blogs.getList());
        mav.addObject("totalpages", blogs.getPages());
        mav.addObject("pageNum", blogs.getPageNum());
        mav.addObject("banners", banners);
        mav.setViewName("index");
        return mav;
    }

    @RequestMapping("sidebar")
    public ModelAndView sidebar() {
        JedisUtil jedis = JedisUtil.getInstance();
        ModelAndView modelAndView = new ModelAndView();
        JsonParser parser = new JsonParser();
        String str = jedis.get("biaoqian");
        JsonArray jsonArray = (JsonArray) parser.parse(str);
        Iterator iterator = jsonArray.iterator();
        List<KeyAndValue> biaoqian = new ArrayList<>();
        while (iterator.hasNext()) {
            Gson gson = new Gson();
            KeyAndValue keyAndValue = gson.fromJson((JsonObject) iterator.next(), KeyAndValue.class);
            biaoqian.add(keyAndValue);
        }
        List<Category> categories = categoryService.getAllCategory();
        List<Blog> blogbyhits = blogService.getByHits();
        modelAndView.addObject("blogbyhits", blogbyhits);
        modelAndView.addObject("tags", biaoqian);
        modelAndView.addObject("categories", categories);
        modelAndView.setViewName("sidebar");
        return modelAndView;
    }


    @RequestMapping(value = "lucene")
    public void jfoe(HttpServletResponse response) throws Exception {
        List<Blog> blogs = blogService.getAllBlog();
        for (Blog blog : blogs) {
            try {
                blogIndex.addIndex(blog);
            } catch (IOException e) {
                logger.error("jofijo", e);
            }
        }
        response.getWriter().write("success");
    }
}
