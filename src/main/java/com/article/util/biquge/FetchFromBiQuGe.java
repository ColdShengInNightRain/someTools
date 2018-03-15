package com.article.util.biquge;

import com.article.constant.WebUri;
import com.article.dto.Content;
import com.article.dto.Title;
import com.article.util.CommonUtil;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * 从笔趣阁获取小说
 * @Author: jiangxinlei
 * @Time: 2018/2/12 14:38
 **/
public class FetchFromBiQuGe {

    public static void main(String[] args) {
        String articleCode = "/20_20368/";
        String articleName = "女帝直播攻略";
        fetchNovel(articleName,articleCode);

    }

    public static boolean fetchNovel(String articleName, String articleCode) {
        //获取桌面路径
        FileSystemView fsv = FileSystemView.getFileSystemView();
        File com=fsv.getHomeDirectory();
        String filePath = com.getPath()+"\\articles";
        String url = WebUri.BIQUGE.concat(articleCode);
        //消除不受信任的HTML(防止XSS攻击)
        url = CommonUtil.transferToSafe(url);

        Document document = CommonUtil.getDocument(url);

        List<Title> titles = getTitles(document);

        List<Content> contents = getContents(titles);

        contents.sort(Comparator.comparing(Content::getTitleId));
        CommonUtil.writeToFile(articleName, contents, filePath);
        return true;
    }

    private static List<Content> getContents(List<Title> titles) {
        List<Content> contents = Lists.newArrayListWithCapacity(titles.size());
        titles.parallelStream().forEach(title -> {
            Content content = getContent(title);
            if (content != null) {
                contents.add(content);
            }
        });
        return contents;
    }

    private static Content getContent(Title title) {
        String titleName = title.getTitleName();
        System.out.println(titleName+" "+(new Date()));
        Integer id = title.getId();
        String uri = title.getUri();
        String url = WebUri.BIQUGE.concat(uri);
        //消除不受信任的HTML(防止XSS攻击)
        url = CommonUtil.transferToSafe(url);

        Document document = CommonUtil.getDocument(url);

        Elements elements = document.getElementsByClass("showtxt");
        List<Node> nodes = elements.get(0).childNodes();
        StringBuilder sb = new StringBuilder();
        sb.append(titleName).append("\n");
        for (Node node : nodes) {
            String s = node.toString();
            s = StringUtils.replaceAll(s, "&nbsp;", " ");
            s = StringUtils.replaceAll(s, "<br>", "\n");
            sb.append(s).append("\n");
        }
        return Content.builder().titleId(id).title(titleName).content(sb.toString()).build();
    }

    private static List<Title> getTitles(Document doc) {
        List<Title> titles = Lists.newArrayList();
        Elements elements = doc.getElementsByClass("listmain").select("a");
        int count = 0;
        Iterator<Element> iterator = elements.iterator();
        while (iterator.hasNext()) {
            Element element = iterator.next();
            String titleName = element.childNodes().get(0).toString();
            String url = element.attributes().get("href");
            titles.add(Title.builder().id(count).titleName(dealTitleName(titleName, count)).uri(url).build());
            count ++;
        }
        return titles;
    }

    private static String dealTitleName(String titleName, int count) {
        if (!StringUtils.contains(titleName, "第") || !StringUtils.contains(titleName, "章")) {
            titleName = "第" + (count+1) + "章 "+ titleName;
        }
        return titleName;
    }


}
