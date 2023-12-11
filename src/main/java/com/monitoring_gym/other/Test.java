package com.monitoring_gym.other;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.monitoring_gym.utils.HttpUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Test {
    static class Blog {
        Date createTime;
        String text;
    }

    public static HttpHeaders headers = new HttpHeaders();
    public static String sinceId = "";
    public static int total = 0;
    public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    static {
        headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Safari/537.36 Edg/111.0.1661.44");
        headers.add("Cookie", "SINAGLOBAL=6654267848342.768.1626610659247; XSRF-TOKEN=WVbR4rT0RCV3wWtmeeXaTaim; PC_TOKEN=b7bb7f19d9; login_sid_t=fcd79b9cbb8a1d428ea567b0ca18a0b1; cross_origin_proto=SSL; _s_tentry=passport.weibo.com; UOR=,,login.sina.com.cn; Apache=4044310458691.778.1679240417311; ULV=1679240417313:10:1:1:4044310458691.778.1679240417311:1669093808706; SCF=AlQp7T7kFw1HfsLvBLAklUFo_NiCb5809GmgXLTU6j3i-3b0k-Reyv8TO7JHuG7Id1oq8Gmgbbql16cguxzqx00.; SUB=_2A25JE1yjDeRhGeNI7loU-SrNzDmIHXVqaclrrDV8PUNbmtANLUujkW9NSETX5h6wHv1lBYq7u8DZ8GylnxuZDpAO; SUBP=0033WrSXqPxfM725Ws9jqgMF55529P9D9WF9g1ncxqolnXX9zVrguVnP5JpX5KzhUgL.Fo-cSKnf1KBpS0-2dJLoI7_8wsSydsLbUPiKgBtt; ALF=1710776435; SSOLoginState=1679240435; WBPSESS=aOlYKBgsbLBvVZ2Aj8Lt4jM601Yt0whdRk2uHun_xr9DiLEciVft50XkUOAbHp2Ioc4u-vcFCZ_6m5Vhrqzwyb5mlmb0UAkM0tQ7DL8MwyEkFjdT_UTYzIlatx4xVjmO2L5GUkBHWGQaBOBLLuKujg==; UPSTREAM-V-WEIBO-COM=b09171a17b2b5a470c42e2f713edace0");
        headers.add("X-XSRF-TOKEN", "30vgurRnNjMNXHY9CELGnZQ_");
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    public static void main(String[] args) throws ParseException, IOException, InterruptedException {

        String beginTime = "2022-03-01";
        String endTime = "2022-05-31";
        Date beginDate = sdf.parse(beginTime);
        Date endDate = sdf.parse(endTime);
        List<String> dateList = getDateByTimeRange(beginDate, endDate);


        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("src/main/java/com/monitoring_gym/data.txt"));

        for(int page = 300; page < 390; page++) {
            String url = "https://weibo.com/ajax/statuses/mymblog?uid=2539961154&page=" + page + "&feature=0";
            if(StringUtils.isNotBlank(sinceId))
                url += "&since_id=" + sinceId;

//            String url = "https://weibo.com/ajax/statuses/searchProfile?uid=2539961154&page=" + page + "&feature=4&starttime=1646064000&endtime=1654012800";

            Thread.sleep(1000);
            JSONArray blogList = getData(url);
            System.out.println("第" + page +"页 :" +  blogList.size());



            for(int i = 0; i < blogList.size(); i++) {
                JSONObject blog = blogList.getJSONObject(i);
                Blog blogData = new Blog();
                String timeStr = blog.getString("created_at");
                timeStr = timeStr.replace("+0800", "CST");
                SimpleDateFormat df = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy",Locale.US);
                //设置对应时区
                df.setTimeZone(new SimpleTimeZone(0, "GMT"));
                blogData.createTime = df.parse(timeStr);

                System.out.println(timeStr);


                if(contains(dateList, blogData.createTime)) {
                    blogData.text = blog.getString("text_raw");
                    if(blog.getString("text").contains("...<span class=\"expand\">展开"))
                    {
                        String getAllTextUrl = "https://weibo.com/ajax/statuses/longtext?id=";
                        getAllTextUrl += blog.getString("mblogid");
                        ResponseEntity<String> allTextRes = HttpUtils.doGet(getAllTextUrl, new LinkedMultiValueMap<String, String>(), headers);
                        String longString =  JSON.parseObject(allTextRes.getBody()).getJSONObject("data").getString("longTextContent");
                        blogData.text = longString;
                    }

                    System.out.println(blogData.text);
                    bufferedWriter.write(blogData.createTime.toString());
                    bufferedWriter.write(blogData.text);
                    bufferedWriter.write("\n");
                }

            }

//            nums += blogList.size();
//            if(nums >= total)
//                break;
        }

        bufferedWriter.close();
    }


    public static JSONArray getData(String url) {
        ResponseEntity<String> response = HttpUtils.doGet(url, new LinkedMultiValueMap<String, String>(), headers);

        JSONObject data = JSON.parseObject(response.getBody());

        if(data.size() > 0) {
            data = data.getJSONObject("data");
            sinceId = data.getString("since_id");
 //           total = data.getIntValue("total");
            return data.getJSONArray("list");
        }

        return new JSONArray();
    }

    public static List<String> getDateByTimeRange(Date beginDate, Date endDate) {
        List<String> dateList =Lists.newArrayList();
        dateList.add(sdf.format(beginDate));
        Calendar beginCalendar = Calendar.getInstance();
        // 使用给定的 Date 设置此 Calendar 的时间
        beginCalendar.setTime(beginDate);
        Calendar endCalendar = Calendar.getInstance();
        // 使用给定的 Date 设置此 Calendar 的时间
        endCalendar.setTime(endDate);
        // 测试此日期是否在指定日期之后
        while (endDate.after(beginCalendar.getTime())) {
            // 根据日历的规则,为给定的日历字段添加或减去指定的时间量
            beginCalendar.add(Calendar.DAY_OF_MONTH, 1);
            dateList.add(sdf.format(beginCalendar.getTime()));
        }
        return dateList;
    }

    public static boolean contains(List<String> list, Date nowDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String dateString = sdf.format(nowDate);
        return list.contains(dateString);
    }
}
