package com.monitoring_gym.utils;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class HttpUtils {

    public static RestTemplate getUTF8Instance()
    {
        RestTemplate restTemplate = new RestTemplate();
        List<HttpMessageConverter<?>> list = restTemplate.getMessageConverters();
        for(HttpMessageConverter<?> httpMessageConverter : list)
        {
            if(httpMessageConverter instanceof StringHttpMessageConverter)
            {
                ((StringHttpMessageConverter)httpMessageConverter).setDefaultCharset(StandardCharsets.UTF_8);
            }
        }
        return restTemplate;
    }

    public static ResponseEntity<JSONObject> doPost(String url, MultiValueMap params, HttpHeaders httpHeaders)
    {
        final RestTemplate restTemplate = new RestTemplate();
        final HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        final HttpClient httpClient = HttpClientBuilder.create()
                .setRedirectStrategy(new LaxRedirectStrategy())
                .build();
        factory.setHttpClient(httpClient);
        restTemplate.setRequestFactory(factory);


        return restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(params,httpHeaders),
                JSONObject.class
        );

    }

    public static ResponseEntity<String> doGetDisableRedirect(String url, MultiValueMap<String, String> params, HttpHeaders httpHeaders)
    {
        final RestTemplate restTemplate = new RestTemplate();
        final HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        final HttpClient httpClient = HttpClientBuilder.create()
                .disableRedirectHandling()
                .build();
        factory.setHttpClient(httpClient);
        restTemplate.setRequestFactory(factory);

        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(params, httpHeaders),
                String.class
        );
    }

    public static ResponseEntity<String> doGet(String url, MultiValueMap<String, String> params, HttpHeaders httpHeaders)
    {
        final RestTemplate restTemplate = new RestTemplate();
        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(params, httpHeaders),
                String.class
        );
    }


}
