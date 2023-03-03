package com.monitoring_gym;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.monitoring_gym.DAO.Site;
import com.monitoring_gym.utils.HttpUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import lombok.extern.slf4j.Slf4j;


import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
@EnableScheduling
@Async
public class AutoRemind {
//    public static final String host = "https://reservation.bupt.edu.cn";
//    public static final String cookieUrl = "https://reservation.bupt.edu.cn/index.php/1600?l=zh-cn";
    public static final String getSiteUrl = "https://reservation.bupt.edu.cn/index.php/Wechat/Booking/get_one_day_one_area_state_table_html";
    public static final String confirmBookUrl = "https://reservation.bupt.edu.cn/index.php/Wechat/Booking/confirm_booking";
    public static final String bookUrl = "https://reservation.bupt.edu.cn/index.php/Wechat/Register/register_show";
    public static final Map<String, String> siteName = Maps.newHashMap();
    public static final String cookie = "PHPSESSID=8pe7ncvmp44dm9a2dvse6vueg3"; // 在这里copy爬来的cookie

    static {
        siteName.put("5982",  "羽毛球场");
        siteName.put("5985",  "健身房");
        siteName.put("5983",  "乒乓球场");
        siteName.put("5991",  "三层羽毛球场地");
        siteName.put("5984",  "游泳馆");
    }


    @Scheduled(cron = "0 0 10 ? * *")
    public void remind()  {
        try {
            book("5982", "15424_2022112912,15424_2022112913", "20221129");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Scheduled(cron = "0 0 12 ? * *")
    public void remind1(){
        try {
            book("5985", "15415_2022112905", "20221129");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    @Scheduled(fixedRate = 5000)
//    public void remind2()
//    {
//        try {
//            AutoRemind autoRemind = new AutoRemind();
//            List<Site> list = autoRemind.parseData("5985", "20230228");
//            for(Site site : list)
//            {
//                if(StringUtils.equals(site.getSiteId(), "15415_2023022815"))
//                {
//                    System.out.println(site);
//                    if(!"已约满".equals(site.getStates())){
//                        book("5985", "15415_2023022815", "20230228");
//                    }
//                }
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }


    public static void main(String[] args)
    {
        AutoRemind autoRemind = new AutoRemind();
        autoRemind.parseData("5985", "20230228"); //不知道场地名称可以先调用这个
    }


    /**
     * @author: shenzhaocong
     * @description: 根据场馆ID和日期获取场地ID
     * @date: 2022/11/25 15:39
     * @param areaID 场馆ID
     * @param times 日期 想预约的场地时间，放到list里，格式是20211125 08:00-09:00
     * @param siteName 场地名称 例如羽毛球场2号
     * @return 返回指定的场地Id
     */
    public String getTargetSiteId(String areaID, List<String> times, String siteName)
    {
        String date = times.get(0).split(" ")[0];
        List<Site> siteList = parseData(areaID, date);
        if(siteList.isEmpty())
            return "";

        List<Site> siteInTime = filterSite(siteList, times, siteName);
        if(siteInTime.isEmpty())
            return "";

        StringBuilder siteId = new StringBuilder();
        for(Site site: siteInTime)
        {
            siteId.append(site.getSiteId());
            siteId.append(",");
        }
        siteId.deleteCharAt(siteId.length()-1);
        return siteId.toString();
    }

    /**
     * @author: shenzhaocong
     * @description: 获取某一天场馆下所有场地信息
     * @date: 2022/11/25 16:01
     * @param areaId 场馆ID
     * @param time 日期， 20221126
     * @return
     */
    public List<Site> parseData(String areaId, String time)
    {
        ArrayList<Site> result = Lists.newArrayList();

        MultiValueMap<String, String> params =  new LinkedMultiValueMap<>(); // 参数
        params.add("now_area_id",areaId);
        params.add("query_date", time);
        params.add("first_room_id", "0");
        params.add("start_date", time);
        params.add("the_ajax_execute_times", "1");

        HttpHeaders headers = new HttpHeaders();
        headers.add("User-Agent","Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.5195.102 Safari/537.36 Language/zh wxwork/4.0.19 (MicroMessenger/6.2) WindowsWechat  MailPlugin_Electron WeMail embeddisk");
        headers.add("Cookie", cookie);

        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<JSONObject> response = HttpUtils.doPost(getSiteUrl, params, headers); // 获取场地预约信息
        String webContent = response.getBody().getString("table_html");
        Document document = Jsoup.parse(webContent);
        Elements elements = document.getElementsByAttributeValueContaining("class", "table_btn");

        for(Element element : elements) // 爬取场地信息
        {
            Site site = new Site();
            site.setAreaId(areaId);
            site.setAreaName(siteName.get(areaId));
            site.setSiteId(element.attr("id"));

            Element timeDiv = element.getElementsByAttributeValueContaining("class", "time").get(0);
            site.setTime(time + " " + timeDiv.text().substring(0, timeDiv.text().indexOf("(")));

            Element stateDiv = element.getElementsByAttributeValueContaining("class", "state_text").get(0);
            site.setStates(stateDiv.text());

            Element siteNameDiv = document.getElementsByAttributeValue("roomId", site.getSiteId().substring(0, site.getSiteId().indexOf("_"))).get(0);
            site.setSiteName(siteNameDiv.text());

           // System.out.println(site);
            result.add(site);
        }


        return result;
    }

    /**
     * siteList: 所有场次的信息
     * time: 要需要的时间段, 只有乒乓球和羽毛球能一下约两个时间段，时间格式20221123 18:00-19:00
     * siteNum: 几号场，可以先调用一次parseData查看目标场地名称
     * */
    public List<Site> filterSite(List<Site> siteList, List<String> time, String siteName)
    {
        List<Site> sitesInTime = Lists.newArrayList();
        for(Site site: siteList)
        {
            if(siteName.equals(site.getSiteName()))
            {
                if(time.contains(site.getTime()))
                {
                    sitesInTime.add(site); // 将指定时间的指定场地名称加入list
                    System.out.println("目标场地信息 ———— " + site);
                }
            }
        }
        return sitesInTime;
    }

    public void book(String areaId, String siteId,  String time) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.add("User-Agent","Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.5195.102 Safari/537.36 Language/zh wxwork/4.0.19 (MicroMessenger/6.2) WindowsWechat  MailPlugin_Electron WeMail embeddisk");
        headers.add("Cookie", cookie);

        String getUrl = confirmBookUrl + "?area_id=" + areaId + "&td_id=" + siteId + "&query_date=" + time + "&country_id=0";
        ResponseEntity<String> confirmResponse = HttpUtils.doGet(getUrl, new LinkedMultiValueMap<String, String>(), headers); // 获取预约表单
        String webContent = confirmResponse.getBody();
        Document document = Jsoup.parse(webContent);
        Elements elements = document.getElementsByAttributeValue("id", "form-sub");
        Elements formElements = elements.get(0).getElementsByTag("input");

        String boundary = "----WebKitFormBoundary" + RandomStringUtils.randomAlphanumeric(16);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create()
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE) // 设置浏览器模式
                .setBoundary(boundary)
                .setCharset(StandardCharsets.UTF_8);

        for(Element formData : formElements)
        {
            if(formData.hasAttr("name"))
            {
                String key = formData.attr("id");
                String value = formData.attr("value");
                if(key.equals("total_amount"))
                    value = document.getElementsByAttributeValue("id", "no_card_price").attr("value");
                if(key.equals("mixed_payment_type"))
                    value = "wechat_pay";
                builder.addTextBody(key, value);
            }
        }

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpEntity multipart = builder.build();
        HttpPost httpPost = new HttpPost(bookUrl);
        httpPost.setEntity(multipart);
        httpPost.setHeader("Cookie", cookie);
        CloseableHttpResponse response = httpClient.execute(httpPost);
        HttpEntity responseEntity = response.getEntity();
        String sResponse= EntityUtils.toString(responseEntity, "UTF-8");
        System.out.println("Post 返回结果"+sResponse);
    }
}
