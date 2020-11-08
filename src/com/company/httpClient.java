package com.company;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;


public class httpClient {

    public static String doPost(String pathUrl, String data, String token){
        OutputStreamWriter out = null;
        BufferedReader br = null;
        String result = "";
        try {
            URL url = new URL(pathUrl);
            //打开和url之间的连接
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            //请求方式
            conn.setRequestMethod("POST");
            //conn.setRequestMethod("GET");

            //设置通用的请求属性
            conn.setRequestProperty("accept", "application/json, text/plain, */*");
            conn.setRequestProperty("connection", "keep-alive");
            conn.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.135 Safari/537.36");
            conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
//            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            if (token != null){
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }

            //DoOutput设置是否向httpUrlConnection输出，DoInput设置是否从httpUrlConnection读入，此外发送post请求必须设置这两个
            conn.setDoOutput(true);
            conn.setDoInput(true);

            /**
             * 下面的三句代码，就是调用第三方http接口
             */
            //获取URLConnection对象对应的输出流
            out = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
            //发送请求参数即数据
            out.write(data);
            //flush输出流的缓冲
            out.flush();

            /**
             * 下面的代码相当于，获取调用第三方http接口后返回的结果
             */
            //获取URLConnection对象对应的输入流
            InputStream is = conn.getInputStream();
            //构造一个字符流缓存
            br = new BufferedReader(new InputStreamReader(is));
            String str = "";
            while ((str = br.readLine()) != null){
                result += str;
            }
//            System.out.println(result);
            //关闭流
            is.close();
            //断开连接，disconnect是在底层tcp socket链接空闲时才切断，如果正在被其他线程使用就不切断。
            conn.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                if (out != null){
                    out.close();
                }
                if (br != null){
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }


    public static String doGet(String pathUrl, String token){
        OutputStreamWriter out = null;
        BufferedReader br = null;
        String result = "";
        try {
            URL url = new URL(pathUrl);
            //打开和url之间的连接
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            //请求方式
//            conn.setRequestMethod("POST");
            conn.setRequestMethod("GET");

            //设置通用的请求属性
//            conn.setRequestProperty("accept", "application/json, text/plain, */*");
            conn.setRequestProperty("connection", "keep-alive");
//            conn.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.135 Safari/537.36");
//            conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
//            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            conn.setRequestProperty("Authorization", "Bearer " + token);

            //DoOutput设置是否向httpUrlConnection输出，DoInput设置是否从httpUrlConnection读入，此外发送post请求必须设置这两个
            conn.setDoOutput(false);
            conn.setDoInput(true);

            /**
             * 下面的代码相当于，获取调用第三方http接口后返回的结果
             */
            //获取URLConnection对象对应的输入流
            InputStream is = conn.getInputStream();
            //构造一个字符流缓存
            br = new BufferedReader(new InputStreamReader(is));
            String str = "";
            while ((str = br.readLine()) != null){
                result += str;
            }
//            System.out.println(result);
            //关闭流
            is.close();
            //断开连接，disconnect是在底层tcp socket链接空闲时才切断，如果正在被其他线程使用就不切断。
            conn.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                if (out != null){
                    out.close();
                }
                if (br != null){
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }


    /*public static void main(String[] args) {
//        String access_token = doPost("http://kylin.dev.xsyxsc.cn/token/gentestaccesstoken", "{\"clientId\": \"kylin-sso-web\",\"scopes\":[\"sso_admin\"],\"userName\": \"administrator\",\"passWord\": \"admin123\",\"lifetime\": 1800}", null);
//        System.out.println(access_token);
//        String acc_token = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjNBODVERThBQjVDQzk1OTgxNEIzOThCNDZEMzFERDk1QTdDODc1RjciLCJ0eXAiOiJKV1QiLCJ4NXQiOiJPb1hlaXJYTWxaZ1VzNWkwYlRIZGxhZklkZmMifQ.eyJuYmYiOjE1OTkxMjA0ODIsImV4cCI6MTU5OTIwNjg4MiwiaXNzIjoiaHR0cDovLzE5Mi4xNjguNi4yMTk6MTAwNiIsImF1ZCI6WyJodHRwOi8vMTkyLjE2OC42LjIxOToxMDA2L3Jlc291cmNlcyIsInhzeXgtZ2VtaW5pLXdvcmtiZW5jaCJdLCJjbGllbnRfaWQiOiJ4c3l4LWdlbWluaS13b3JrYmVuY2giLCJzdWIiOiIxMjU1MDExMDA2NjQ0MDUwIiwiaWRwIjoibG9jYWwiLCJ0ZW5hbnQiOiIxMjU1MDExMDA2NjQ0MDUwLGRhaXRpbmcsZGFpdGluZywxODU3NTA3NTcxNSxkYWl0aW5nQHhzeXhzYy5jb20sMTI1NDk3OTE4NjMyMzkyNCwxMDAxNSzkupHlrqLmnI0sMCwsMTAwMTUs5LqR5a6i5pyNLCxGYWxzZSIsInRva2VuSWQiOiI1NjMwMGQ1Ny0zYzdiLTRhYzEtOTE5Yy00YzcxNTRkMDRkMmQiLCJ0IjoiZGV2Iiwic2NvcGUiOlsieHN5eC1nZW1pbmktd29ya2JlbmNoIl0sImFtciI6WyJwd2QiXX0.gKuT4V0vw0ZMyrDEw4rLWDEFsiQw-KjFRnFDLU9dW3Rp3O48QWh6BMALoty38B1HTjyKUUoMLl6iOxz8kF12B3xKRHpsMaroirGnYU_sAIS9OvN055gVjApMlUf59mMjkbYZTUgZlx3aHrxRN-dJ5MR6sm4KWkSMsUSx3dOjka814fi2DDd1kxuMo2QgDaWUGM_43Dh9tJ-RwLNoTYLG2ExPUGp1-RadmtAGyDqrbd7PDRatXSH9EAaKbrfZ4kCZAVzoyyUMNcN3ViqwORcnm2tK14H_RfRvEgd1-4_ITZ99JXvziYv_xZqK6uaPLKeZCf5OANrWWudiiBzNEs_8wA";
//        doGet("http://172.21.192.162/kf/customer/currentInfo", acc_token);
        // 获取当前客服基础信息
        String KF_result = httpClient.doPost("http://192.168.6.186:8081/store/msgbox/im/msgs", acc_token);
        JSONObject KFJson = JSON.parseObject(KF_result);
        if (KFJson.getString("message").equals("成功")){
            JSONObject data = KFJson.getJSONObject("data");
            JSONArray skillGroups = data.getJSONArray("skillGroups");
            String[] skillGroupIds = new String[skillGroups.size()];
            System.out.println(skillGroups);
            for(int i=0;i<skillGroups.size();i++){
                JSONObject obj = skillGroups.getJSONObject(i);
                skillGroupIds[i] = obj.getString("skillGroupId");
            }
            System.out.println(Arrays.toString(skillGroupIds));
        }else{
            System.out.println("Error: 获取当前客服基础信息失败， " + KF_result);
            System.exit(-1);
        }
    }*/
}
