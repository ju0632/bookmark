package com.fanxb.bookmark.business.bookmark.service.impl;

import com.alibaba.fastjson.JSON;
import com.fanxb.bookmark.business.bookmark.dao.BookmarkDao;
import com.fanxb.bookmark.business.bookmark.entity.BookmarkEs;
import com.fanxb.bookmark.business.bookmark.entity.MoveNodeBody;
import com.fanxb.bookmark.business.bookmark.entity.redis.VisitNumPlus;
import com.fanxb.bookmark.business.bookmark.service.BookmarkService;
import com.fanxb.bookmark.business.bookmark.service.PinYinService;
import com.fanxb.bookmark.common.constant.EsConstant;
import com.fanxb.bookmark.common.constant.RedisConstant;
import com.fanxb.bookmark.common.entity.Bookmark;
import com.fanxb.bookmark.common.entity.redis.UserBookmarkUpdate;
import com.fanxb.bookmark.common.util.EsUtil;
import com.fanxb.bookmark.common.util.RedisUtil;
import com.fanxb.bookmark.common.util.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 类功能简述：
 * 类功能详述：
 *
 * @author fanxb
 * @date 2019/7/8 15:00
 */
@Service
@Slf4j
public class BookmarkServiceImpl implements BookmarkService {

    @Autowired
    private BookmarkDao bookmarkDao;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private PinYinService pinYinService;

    @Autowired
    private EsUtil esUtil;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void parseBookmarkFile(int userId, InputStream stream, String path) throws Exception {
        Document doc = Jsoup.parse(stream, "utf-8", "");
        Elements elements = doc.select("html>body>dl>dt");
        //获取当前层sort最大值
        Integer sortBase = bookmarkDao.selectMaxSort(userId, path);
        if (sortBase == null) {
            sortBase = 0;
        }
        int count = 0;
        List<Bookmark> bookmarks = new ArrayList<>();
        for (int i = 0, length = elements.size(); i < length; i++) {
            if (i == 0) {
                Elements firstChildren = elements.get(0).child(1).children();
                count = firstChildren.size();
                for (int j = 0; j < count; j++) {
                    dealBookmark(userId, firstChildren.get(j), path, sortBase + j, bookmarks);
                }
            } else {
                dealBookmark(userId, elements.get(i), path, sortBase + count + i - 1, bookmarks);
            }
        }
        updateVersion(userId);
        RedisUtil.addToMq(RedisConstant.BOOKMARK_INSERT_ES, bookmarks);
        RedisUtil.addToMq(RedisConstant.BOOKMARK_PINYIN_CHANGE, bookmarks);

    }

    /**
     * Description: 处理html节点，解析出文件夹和书签
     *
     * @param ele  待处理节点
     * @param path 节点路径，不包含自身
     * @param sort 当前层级中的排序序号
     * @author fanxb
     * @date 2019/7/8 14:49
     */
    private void dealBookmark(int userId, Element ele, String path, int sort, List<Bookmark> bookmarks) {
        if (!DT.equalsIgnoreCase(ele.tagName())) {
            return;
        }
        Element first = ele.child(0);
        if (A.equalsIgnoreCase(first.tagName())) {
            //说明为链接
            Bookmark node = new Bookmark(userId, path, first.ownText(), first.attr("href"), first.attr("icon")
                    , Long.parseLong(first.attr("add_date")) * 1000, sort);
            //存入数据库
            insertOne(node);
            bookmarks.add(node);
        } else {
            //说明为文件夹
            Bookmark node = new Bookmark(userId, path, first.ownText(), Long.parseLong(first.attr("add_date")) * 1000, sort);
            Integer sortBase = 0;
            //同名文件夹将会合并
            if (insertOne(node)) {
                sortBase = bookmarkDao.selectMaxSort(node.getUserId(), path);
                if (sortBase == null) {
                    sortBase = 0;
                }
            }
            String childPath = path + "." + node.getBookmarkId();
            Elements children = ele.child(1).children();
            for (int i = 0, size = children.size(); i < size; i++) {
                dealBookmark(userId, children.get(i), childPath, sortBase + i + 1, bookmarks);
            }
        }
    }

    /**
     * Description: 插入一条书签，如果已经存在同名书签将跳过
     *
     * @param node node
     * @return boolean 如果已经存在返回true，否则false
     * @author fanxb
     * @date 2019/7/8 17:25
     */
    private boolean insertOne(Bookmark node) {
        //先根据name,userId,parentId获取此节点id
        Integer id = bookmarkDao.selectIdByUserIdAndNameAndPath(node.getUserId(), node.getName(), node.getPath());
        if (id == null) {
            bookmarkDao.insertOne(node);
            return false;
        } else {
            node.setBookmarkId(id);
            return true;
        }
    }


    @Override
    public Map<String, List<Bookmark>> getOneBookmarkTree(int userId) {
        List<Bookmark> list = bookmarkDao.getListByUserId(userId);
        Map<String, List<Bookmark>> map = new HashMap<>(50);
        list.forEach(item -> {
            map.computeIfAbsent(item.getPath(), k -> new ArrayList<>());
            map.get(item.getPath()).add(item);
        });
        return map;
    }


    @Override
    public List<Bookmark> getBookmarkListByPath(int userId, String path) {
        return bookmarkDao.getListByUserIdAndPath(userId, path);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(int userId, List<Integer> folderIdList, List<Integer> bookmarkIdList) {
        Set<String> set = new HashSet<>();
        for (Integer item : folderIdList) {
            set.addAll(bookmarkDao.getChildrenBookmarkId(userId, item).stream().map(String::valueOf).collect(Collectors.toSet()));
            bookmarkDao.deleteUserFolder(userId, item);
            bookmarkIdList.add(item);
        }
        if (bookmarkIdList.size() > 0) {
            bookmarkDao.deleteUserBookmark(userId, bookmarkIdList);
        }
        set.addAll(bookmarkIdList.stream().map(String::valueOf).collect(Collectors.toSet()));
        RedisUtil.addToMq(RedisConstant.BOOKMARK_DELETE_ES, set);
        updateVersion(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Bookmark addOne(Bookmark bookmark) {
        int userId = UserContextHolder.get().getUserId();
        Integer sort = bookmarkDao.selectMaxSort(userId, bookmark.getPath());
        bookmark.setSort(sort == null ? 1 : sort + 1);
        bookmark.setUserId(userId);
        bookmark.setCreateTime(System.currentTimeMillis());
        bookmark.setAddTime(bookmark.getCreateTime());
        bookmark.setSearchKey(pinYinService.changeString(bookmark.getName()));
        bookmarkDao.insertOne(bookmark);
        //如果是书签，插入到es中
        if (bookmark.getType() == 0) {
            RedisUtil.addToMq(RedisConstant.BOOKMARK_INSERT_ES, Collections.singleton(bookmark));
        }
        updateVersion(userId);
        return bookmark;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOne(int userId, Bookmark bookmark) {
        bookmark.setUserId(userId);
        bookmark.setSearchKey(pinYinService.changeString(bookmark.getName()));
        bookmarkDao.editBookmark(bookmark);
        if (bookmark.getType() == 0) {
            RedisUtil.addToMq(RedisConstant.BOOKMARK_INSERT_ES, Collections.singleton(bookmark));
        }
        updateVersion(userId);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveNode(int userId, MoveNodeBody body) {
        if (body.getSort() == -1) {
            Integer max = bookmarkDao.selectMaxSort(userId, body.getTargetPath());
            body.setSort(max == null ? 1 : max);
        } else {
            //更新目标节点的sort
            bookmarkDao.sortPlus(userId, body.getTargetPath(), body.getSort());
        }
        //如果目标位置和当前位置不在一个层级中需要更新子节点的path
        if (!body.getTargetPath().equals(body.getSourcePath())) {
            bookmarkDao.updateChildrenPath(userId, body.getSourcePath() + "." + body.getBookmarkId()
                    , body.getTargetPath() + "." + body.getBookmarkId());
        }
        //更新被移动节点的path和sort
        bookmarkDao.updatePathAndSort(userId, body.getBookmarkId(), body.getTargetPath(), body.getSort());
        updateVersion(userId);
    }

    @Override
    public List<BookmarkEs> searchUserBookmark(int userId, String context) {
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        boolQueryBuilder.must(QueryBuilders.termQuery("userId", userId));
        boolQueryBuilder.must(QueryBuilders.multiMatchQuery(context, "name", "url"));
        SearchSourceBuilder builder = new SearchSourceBuilder();
        builder.size(5);
        builder.query(boolQueryBuilder);
        return esUtil.search(EsConstant.BOOKMARK_INDEX, builder, BookmarkEs.class);
    }

    @Override
    public void visitNumPlus(int id) {
        VisitNumPlus item = new VisitNumPlus(UserContextHolder.get().getUserId(), id);
        RedisUtil.addToMq(RedisConstant.BOOKMARK_VISIT_NUM_PLUS, JSON.toJSONString(item));
    }

    /**
     * 功能描述: 向mq发送消息通知，数据更新
     *
     * @param userId userId
     * @author fanxb
     * @date 2020/5/10 12:07
     */
    private void updateVersion(int userId) {
        RedisUtil.addToMq(RedisConstant.BOOKMARK_UPDATE_VERSION, userId);
    }


}
