package com.monitoring_gym;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.monitoring_gym.DAO.Site;
import com.monitoring_gym.utils.HttpUtils;
import com.monitoring_gym.utils.MailClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@EnableScheduling
@Async
public class AutoRemind {

    @Autowired
    MailClient mailClient;

    public static final String remindEmail = "529540465@qq.com";

    public static final String getSiteUrl = "https://reservation.bupt.edu.cn/index.php/Wechat/Booking/get_one_day_one_area_state_table_html";
    public static final String confirmBookUrl = "https://reservation.bupt.edu.cn/index.php/Wechat/Booking/confirm_booking";
    public static final String bookUrl = "https://reservation.bupt.edu.cn/index.php/Wechat/Register/register_show";
    public static final String loginUrl = "https://reservation.bupt.edu.cn/index.php/Wechat/User/user_is_login";

    public static final Map<String, String> siteName = Maps.newHashMap();
    public static final String cookie = "PHPSESSID=vn4ojven226m2orm52aisqtk21"; // 在这里copy爬来的cookie
    public static final String cookie1 = "PHPSESSID=pngqku5e301dg8bbhqft04gsv4"; // 在这里copy爬来的cookie

    static {
        siteName.put("5982",  "羽毛球场");
        siteName.put("5985",  "健身房");
        siteName.put("5983",  "乒乓球场");
        siteName.put("5991",  "三层羽毛球场地");
        siteName.put("5984",  "游泳馆");
    }


    @Scheduled(cron = "0 0 10 ? * *")
    @Async
    public void bookForBadminton()  {
        String time = "20230606";
//        String siteId1 = "15424_"+time+"14";
//        String siteId2 = "15424_"+time+"15";
        String siteId = "15424_"+time+"14,15424_"+time+"15";

//        System.out.println("查询场地");
//        parseData("5982", time, cookie);
//        System.out.println("查询场地完成");

        try {
            log.info("开始预约");
            book("5982", siteId, time, cookie, "wechat_pay");
//            book("5982", siteId1, time, cookie, "wechat_pay");
//            book("5982", siteId2, time, cookie, "wechat_pay");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

//    @Scheduled(cron = "0 0 12 ? * *")
//    public void bookForGym(){
//        try {
//            book("5985", "15415_2022112905", "20221129", cookie);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    @Scheduled(fixedRate = 3000)
    @Async
    public void remindForBadminton()
    {
        remind("20230606", "5982", 15418, 15426,11, 15, cookie, "wechat_pay");
        remind("20230606", "5982", 15418, 15426,11, 15, cookie, "wechat_pay");
    }



//    @Scheduled(fixedRate = 3000)
//    @Async
//    public void remindForGym() throws IOException {
//        remind("20230601", "5985", 15415, 15415,19, 19, cookie, "package_pay");
//    }
//

    public static void main(String[] args) throws Exception {
        AutoRemind autoRemind = new AutoRemind();
        List<Site> list = autoRemind.parseData("5982", "20230606", cookie); //不知道场地名称可以先调用这个
        autoRemind.isLogin();
        list.forEach(System.out::println);
//        autoRemind.book("5985", "15415_2023060119", "20230601", cookie, "wechat_pay");
    }

    public synchronized void remind(String time, String areaId, int startSiteId, int endSiteId, int startTimeId, int endTimeId, String cookie, String paymentType)
    {
        List<String> siteIds = Lists.newArrayList();

        for(int i = startSiteId; i <= endSiteId; i++) {
            for(int j = startTimeId; j <= endTimeId; j++) {
                StringBuffer sb = new StringBuffer();
                sb.append(i);
                sb.append("_");
                sb.append(time);
                sb.append(j < 10 ? "0"+j : j);
                siteIds.add(sb.toString());
            }
        }

        try {
            AutoRemind autoRemind = new AutoRemind();
            List<Site> list = autoRemind.parseData(areaId, time, cookie);
            for(Site site : list)
            {
                if(siteIds.contains(site.getSiteId()))
                {
//                    System.out.println(site);
                    if(!"已约满".equals(site.getStates())){
                        log.info(site.toString());
                        log.info(cookie);
                        isLogin();
                        String res = book(areaId, site.getSiteId(), time, cookie, paymentType);

                        if(StringUtils.isBlank(res)) {
                            mailClient.sendMail(remindEmail, "提醒："+ siteName.get(areaId) + "捡漏预约成功", "如果是微信支付的话尽快去付款嗷");
                        }

                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @author: shenzhaocong
     * @description: 获取某一天场馆下所有场地信息
     * @date: 2022/11/25 16:01
     * @param areaId 场馆ID
     * @param time 日期， 20221126
     * @return
     */
    public List<Site> parseData(String areaId, String time, String cookie)
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

//            System.out.println(site);
            result.add(site);
        }


        return result;
    }

    public String request(String siteId, String areaId, String price) {

        String devicesUrl = "https://reservation.bupt.edu.cn/index.php/API/Room/get_room_devices";
        String softUrl = "https://reservation.bupt.edu.cn/index.php/API/Room/get_room_softs";
        String balanceUrl = "https://reservation.bupt.edu.cn/index.php/Wechat/MixedPayment/get_balance_and_packages_of_one_user";

        HttpHeaders headers = new HttpHeaders();
        headers.add("User-Agent","Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.5195.102 Safari/537.36 Language/zh wxwork/4.0.19 (MicroMessenger/6.2) WindowsWechat  MailPlugin_Electron WeMail embeddisk");
        headers.add("Cookie", cookie);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params =  new LinkedMultiValueMap<>(); // 参数
        ResponseEntity<JSONObject> response;

        String id = siteId.substring(0, siteId.indexOf("_"));;
        String timeId;
        if(siteId.contains(",")) {

            StringBuffer sb = new StringBuffer();
            String[] siteIds = siteId.split(",");
            for(String site : siteIds) {
                sb.append(site.substring(site.indexOf("_") + 1));
                sb.append(" ");
            }
            timeId = sb.toString().trim();

        } else {
            timeId = siteId.substring(siteId.indexOf("_") + 1);
        }



//        params.clear();
//        params.add("id", id);
//        response = HttpUtils.doPost(devicesUrl, params, headers);
//        System.out.println(response.getBody());
//
//        response = HttpUtils.doPost(softUrl, params, headers);
//        System.out.println(response.getBody());

//        params.clear();
        params.add("room_id", id);
        params.add("device_id", "0");
        params.add("soft_id", "0");
        params.add("time_id", timeId);
        params.add("area_id", areaId);
        params.add("card_id", "0");
        params.add("card_name", "0");
        params.add("card_type", "0");
        params.add("card_discount", "0");
        params.add("finall_price", price);
        params.add("occupy_quota", "1");
        response = HttpUtils.doPost(balanceUrl, params, headers);

        JSONObject balance = response.getBody();
        log.info(balance.toString());

        JSONArray packages = balance.getJSONArray("vip_packages");

        if(packages != null && packages.size() > 0) {
            JSONObject vip = packages.getJSONObject(0);
            return vip.getString("vip_id");
        }

        return "";

    }

    public void isLogin() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("User-Agent","Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.5195.102 Safari/537.36 Language/zh wxwork/4.0.19 (MicroMessenger/6.2) WindowsWechat  MailPlugin_Electron WeMail embeddisk");
        headers.add("Cookie", cookie);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params =  new LinkedMultiValueMap<>(); // 参数

        params.add("{\"is_need_login\":true,\"is_specified_page_login\":false}","");
        ResponseEntity<JSONObject> response = HttpUtils.doPost(loginUrl, params, headers);
        log.info(response.getBody().toString());
    }

    /**
     * @author: shenzhaocong
     * @date: 2023/4/24 15:57
     * @param areaId 场馆id
     * @param siteId 场地id
     * @param time 要预约的时间
     * @param cookie 体育馆cookie
     * @param paymentType 支付方式 wechat_pay & package_pay
     *                    wechat_pay 代表使用微信支付，约好以后要用微信支付
     *                    package_pay 代表使用套餐，例如如果有健身房卡，可以直接用卡支付
     * @return 预约结果
     */
    @Async
    public String book(String areaId, String siteId,  String time, String cookie, String paymentType) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.add("User-Agent","Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.5195.102 Safari/537.36 Language/zh wxwork/4.0.19 (MicroMessenger/6.2) WindowsWechat  MailPlugin_Electron WeMail embeddisk");
        headers.add("Cookie", cookie);

        Elements elements;
        String getUrl;
        Document document;
        do{
            getUrl = confirmBookUrl + "?area_id=" + areaId + "&td_id=" + siteId + "&query_date=" + time + "&country_id=0";
            ResponseEntity<String> confirmResponse = HttpUtils.doGet(getUrl, new LinkedMultiValueMap<String, String>(), headers); // 获取预约表单
            String webContent = confirmResponse.getBody();
            document = Jsoup.parse(webContent);
            elements = document.getElementsByAttributeValue("id", "form-sub");
        }
        while(elements.size() < 1);

        Elements formElements = elements.get(0).getElementsByTag("input");
        String boundary = "----WebKitFormBoundary" + RandomStringUtils.randomAlphanumeric(16);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create()
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .setBoundary(boundary);

        ContentType contentType= ContentType.create("text/plain", "UTF-8");
        String price = document.getElementsByAttributeValue("id", "no_card_price").attr("value");
        String vipId = request(siteId, areaId, price);

        for(Element formData : formElements)
        {
            if(formData.hasAttr("name"))
            {
                String key = formData.attr("id");
                String value = formData.attr("value");

                if(key.equals("total_amount")) {
                    value = price;
                }

                if(key.equals("mixed_payment_type")) {
                    if(paymentType.equals("package_pay") && !vipId.equals("0"))
                        value = paymentType;
                    else
                        value = "wechat_pay";
                }


                if(key.equals(("to_use_vip_id"))) {
                    value = vipId;
                }

                StringBody stringBody=new StringBody(value, contentType);
                builder.addPart(key, stringBody);
//                System.out.println(key + ":" + value);
            }
        }

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpEntity multipart = builder.build();
        HttpPost httpPost = new HttpPost(bookUrl);
        httpPost.setEntity(multipart);

        File file = new File("src/main/java/com/monitoring_gym/header.txt");
        List<String> list = new ArrayList<>();
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file));) {
            list = bufferedReader.lines().collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }

        for(String header : list) {
            httpPost.setHeader(header.substring(0, header.indexOf(":")).trim(), header.substring(header.indexOf(":") + 1).trim());
        }

        httpPost.setHeader("Referer" , getUrl);
        httpPost.setHeader("Cookie", cookie + "; think_language=zh-cn");
        httpPost.setHeader("Content-Type", "multipart/form-data; boundary=" + boundary);

        CloseableHttpResponse response = httpClient.execute(httpPost);
        HttpEntity responseEntity = response.getEntity();
        String sResponse= EntityUtils.toString(responseEntity, "UTF-8");

        if(StringUtils.isBlank(sResponse)) {
            log.info("预约成功！");
        }
        else {
            log.error("Post 返回结果: \n" + sResponse);
        }
        return sResponse;
    }
}
