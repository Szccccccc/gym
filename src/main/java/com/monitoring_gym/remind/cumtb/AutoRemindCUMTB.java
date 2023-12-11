//package com.monitoring_gym.remind.cumtb;
//
//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.JSONArray;
//import com.alibaba.fastjson.JSONObject;
//import com.google.common.collect.Maps;
//import com.monitoring_gym.utils.HttpUtils;
//import com.monitoring_gym.utils.MailClient;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.lang3.StringUtils;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import java.util.Map;
//
//@Slf4j
//@Component
//@EnableScheduling
//@Async
//public class AutoRemindCUMTB {
//    @Autowired
//    MailClient mailClient;
//
//    public static final String remindEmail = "529540465@qq.com";
//    public static final String code = "051w9xFa1JhKEF0ozmIa1AyfUs1w9xFi·";
//    public static final String ROOT = "http://cgyy.cumtb.edu.cn/wx/gym/";
//    public static String token = "";
//    public static boolean isRun = false;
//    private Map<String, String> userInfo = Maps.newHashMap();
//
//    @Scheduled(fixedRate = 10000)
//    public void remindForBadminton()
//    {
//        String siteInfo = ROOT + "selectSiteReservation";
//
//        if(!isRun)
//           userInfo = getUserInfo();
//
//        String token = userInfo.get("Token");
//        String date = "2023-07-16";
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
//        headers.set("Referer", "http://cgyy.cumtb.edu.cn/sire/index.html?code="+code);
//        headers.set("Authorization", token);
//
//        JSONObject param = new JSONObject();
//        param.put("siteTypeInnerId", 1);
//        param.put("courseDT", date);
//
//        String res = HttpUtils.doJsonPost(siteInfo, param.toJSONString(),headers);
//        JSONObject siteInfoJson = JSON.parseObject(res);
//
//        if(!StringUtils.equals(siteInfoJson.getString("code"), "0")) {
//            log.error("获取场地信息失败！");
//            return;
//        }
//
//        JSONArray sites = siteInfoJson.getJSONArray("result");
//
//        for(int i = 0; i < sites.size(); i++) {
//            JSONObject site = sites.getJSONObject(i);
//            String name = site.getString("SiteName");
//            JSONArray time = site.getJSONArray("Check");
//            int status = time.getInteger(time.size() - 2);
//
//            log.info(name + ": " + status);
//
//            if(status == 0)
//                mailClient.sendMail(remindEmail, "提醒："+ name + "有人退啦", "快去快去");
//        }
//
//    }
//
//    public Map<String, String> getUserInfo()
//    {
//        String getUserInfo = ROOT + "selectUserInfo";
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
//        JSONObject param = new JSONObject();
//        param.put("code", code);
//
//        String res = HttpUtils.doJsonPost(getUserInfo, param.toJSONString(),headers);
//        JSONObject userInfo = JSON.parseObject(res);
//
//        if(!StringUtils.equals(userInfo.getString("code"), "0")) {
//            log.error("获取用户信息失败！");
//            return Maps.newHashMap();
//        }
//
//        Map<String, String> userInfoMap = JSON.parseObject(userInfo.getJSONObject("result").toJSONString(), Map.class);
//        log.info("用户信息获取成功！"+userInfoMap);
//        isRun = true;
//
//        return userInfoMap;
//    }
//
//
//
//    public static void main(String[] args) {
//        AutoRemindCUMTB autoRemind = new AutoRemindCUMTB();
//        autoRemind.remindForBadminton();
//    }
//}
