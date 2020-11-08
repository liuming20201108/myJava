package com.company;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ReceiveMsgOnGemini {
    
    static class MyWebSocketClient extends WebSocketClient {
        
        String configStr = concurrentLinkedQueue2.poll();
        
        String[] configList = configStr.split(",");
        
        private String sessionId = configList[0];
        
        AtomicReference<String> userIdAtomic = new AtomicReference<>(configList[1]);
        
        private String deviceId = configList[2];
        
        private String userKey = configList[3];
        
        private String userName = configList[4];
        
        private String bizType = "KF";
        
        private ThreadLocal<String> token = new ThreadLocal<>();
        
        private ThreadLocal<String> msgUserId = new ThreadLocal<>();
        
        
        Map<String, AtomicInteger> cidMap = new ConcurrentHashMap<>();  // 保存当前客服每个会话的收到消息数
        
        public MyWebSocketClient(URI serverURI) {
            super(serverURI);
        }
        
        @Override
        public void onOpen(ServerHandshake handshakedata) {
            String Handshake =
                    "{\"cmd\":2,\"flags\":16,\"sessionId\":\"" + sessionId + "\",\"body\":{\"deviceId\":\"" + deviceId
                            + "\",\"osName\":\"web\",\"osVersion\":\"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.86 Safari/537.36\",\"clientVersion\":\"0.1.0\",\"pkgName\":\"\",\"minHeartbeat\":1000,\"maxHeartbeat\":30000}}";
            send(Handshake);
            //            System.out.println(Handshake);
        }
        
        @Override
        public void onClose(int code, String reason, boolean remote) {
            System.out.println("closed with exit code " + code + " additional info: " + reason);
            
            
        }
        
        @Override
        public void onMessage(String message) {
            
            JSONObject msgJson = JSON.parseObject(message);
            String cmd = msgJson.getString("cmd");
            if (cmd.equals("2")) {
                String sessionId = msgJson.getString("sessionId");
                //            System.out.println("sessionId: " + sessionId + " deviceId: " + deviceId + " 握手成功");
                String Banduser = "{\"cmd\":5,\"flags\":16,\"sessionId\":\"" + sessionId + "\",\"body\":{\"userId\":\""
                        + userIdAtomic.get() + "\",\"userKey\":\"" + userKey + "\",\"bizType\":\"" + bizType + "\"}}";
                //            System.out.println(Banduser);
                send(Banduser);
            } else if (cmd.equals("11")) {
                String sessionId = msgJson.getString("sessionId");
                JSONObject body = msgJson.getJSONObject("body");
                String data = body.getString("data");
                JSONObject dataJson = JSON.parseObject(data);
                token.set(dataJson.getString("token"));
                msgUserId.set(dataJson.getString("msgUserId"));
                System.out.println("msgUserId: " + msgUserId.get() + " token: " + token.get());
                System.out.println("userName: " + userName + " userId: " + userIdAtomic.get() + " 绑定用户成功");
                
                ScheduledExecutorService scheduled = Executors.newSingleThreadScheduledExecutor();
                scheduled.schedule(() -> {
                    long requestId = System.currentTimeMillis();
                    String json = "{\"body\":{\"data\":-33},\"bodyLength\":1,\"cmd\":1,\"flags\":16,\"requestId\":"
                            + requestId + ",\"sessionId\":" + sessionId + "}";
                    send(json);
                }, 5, TimeUnit.SECONDS);
            } else if (cmd.equals("19")) {
                String sessionId2 = msgJson.getString("sessionId");
                String requestId = msgJson.getString("requestId");
                JSONObject body = msgJson.getJSONObject("body");
                Integer msgType = body.getInteger("msgType");
                String msgId = body.getString("msgId");
                String cid = body.getString("cid");
                String toId = body.getString("toId");
                String content = body.getString("content");
                send("{\"cmd\":23,\"flags\":16,\"sessionId\":" + sessionId2 + ",\"requestId\":" + requestId
                        + ",\"body\":{\"data\":\"{" + msgId + "}\"}}");
                if (cid != null && cid.length() != 0) {
                    System.out.println("cid不为空的消息: msgId " + msgId + " cid: " + cid + " 消息内容: " + content);
                    
                    // 拉取历史消息
                    if (content.contains("很高兴为您服务")) {
                        StringBuffer sb = new StringBuffer();
                        sb.append("http://192.168.6.186:8081/store/msgbox/im/msgs?").append("userId=")
                                .append(msgUserId.get()).append("&cid=").append(cid).append("&queryType=HISTORY")
                                .append("&size=50&msgId=").append(msgId);
                        
                        System.out.println("URL: " + sb.toString() + " |token: " + token.get());
                        String json = httpClient.doGet(sb.toString(), token.get());
                        JSONObject jsonObject = JSON.parseObject(json);
                        boolean isSuccess = (boolean) jsonObject.get("success");
                        if (isSuccess) {
                            JSONArray jsonArray = jsonObject.getJSONArray("data");
                            Iterator<Object> iterator = jsonArray.iterator();
                            while (iterator.hasNext()) {
                                JSONObject obj = (JSONObject) iterator.next();
                                String msgId1 = (String) obj.get("msgId");
                                String content1 = (String) obj.get("content");
                                Integer msgType1 = obj.getInteger("msgType");
                                if (msgType1 == 1) {
                                    System.out.println("历史 消息: cid: " + cid + " msgId: " + msgId1 + " 消息内容: " + content1
                                            + " Time: " + System.currentTimeMillis());
                                }
                            }
                        } else {
                            System.err.println("请求失败" + sb.toString());
                        }
                    }
                    
                    //                    if (!content.contains("客服主动结束") && !content.contains("对话结束啦，有问题请直接发送描述或图片") && !content.contains("客服超时未回复访客的提醒")) {
                    if (msgType == 1) {
                        System.out.println(
                                "userName: " + userName + "  cid: " + cid + " msgId: " + msgId + " 消息内容: " + content
                                        + " Time: " + System.currentTimeMillis());
                        //                    send("{\"cmd\":19,\"flags\":24,\"sessionId\":" + System.currentTimeMillis() + ",\"requestId\":" + System.currentTimeMillis() + ",\"body\":{\"msgType\":1,\"cid\":\"" + cid + "\",\"fromId\":\"" + toId + "\",\"toId\":\"\",\"content\":\"客服回复1条消息" + content + "\",\"data\":\"{\\\"nickName\\\":\\\"长沙EE\\\",\\\"bizKey\\\":\\\"KF\\\"}\"}}");
                        //                    send("{\"cmd\":19,\"flags\":24,\"sessionId\":" + System.currentTimeMillis() + ",\"requestId\":" + System.currentTimeMillis() + ",\"body\":{\"msgType\":1,\"cid\":\"" + cid + "\",\"fromId\":\"" + toId + "\",\"toId\":\"\",\"content\":\"客服回复2条消息" + content + "\",\"data\":\"{\\\"nickName\\\":\\\"长沙EE\\\",\\\"bizKey\\\":\\\"KF\\\"}\"}}");
                        if (cidMap.containsKey(cid)) {
                            AtomicInteger msgCount = cidMap.get(cid);
                            
                            if (msgCount.incrementAndGet() >= 9) {
                                String closeConversation = httpClient
                                        .doPost("http://192.168.6.6:8466/customer/oper/closeConversation",
                                                "{\"cid\":\"" + cid + "\"}", userKey);
                                JSONObject closeConversationJson = JSON.parseObject(closeConversation);
                                if (closeConversationJson.getString("message").equals("成功")) {
                                    cidMap.remove(cid);
                                    System.out.println("userName: " + userName + "  cid: " + cid + " 会话结束成功!");
                                } else {
                                    System.out.println("userName: " + userName + "  cid: " + cid + " 会话结束失败! "
                                            + closeConversation);
                                }
                            } else {
                                cidMap.put(cid, msgCount);
                            }
                        } else {
                            cidMap.put(cid, new AtomicInteger(1));
                        }
                    }
                }
                
            } else {
                System.out.println("Error Message: " + message);
            }
        }
        
        @Override
        public void onMessage(ByteBuffer message) {
            System.out.println("received ByteBuffer");
        }
        
        @Override
        public void onError(Exception ex) {
            System.err.println("an error occurred:" + ex);
        }
    }
    
    private static ConcurrentLinkedQueue<String> concurrentLinkedQueue1 = new ConcurrentLinkedQueue<String>();
    
    private static ConcurrentLinkedQueue<String> concurrentLinkedQueue2 = new ConcurrentLinkedQueue<String>();
    
    /**
     * 以行为单位读取文件，常用于读面向行的格式化文件
     */
    public static void readFileByLines(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("Error: " + fileName + "文件不存在!!!");
            System.exit(-1);
        }
        BufferedReader reader = null;
        try {
            //            System.out.println("以行为单位读取文件内容，一次读一整行：");
            reader = new BufferedReader(new FileReader(file));
            String tempString = null;
            // 一次读入一行，直到读入null为文件结束
            while ((tempString = reader.readLine()) != null) {
                // 显示行号
                //                System.out.println(line + "," + tempString);
                concurrentLinkedQueue1.add(tempString);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
    
    static void startWebSocket(URI uri) {
        WebSocketClient client = new MyWebSocketClient(uri);
        client.connect();
    }
    
    public static void main(String[] args) throws URISyntaxException {
        
        if (args.length == 1) {
            String fileName = args[0];
            String webSocketUrl = "ws://192.168.6.172:8008/";
            //            String fileName = "G:\\IdeaProjects\\untitled\\out\\userId.txt";
            ReceiveMsgOnGemini.readFileByLines(fileName);
            String[] skillGroupIds;
            int threadCount = concurrentLinkedQueue1.size();
            Future f = null;
            if (threadCount > 0) {
                int sessionId = 8530000;
                long deviceId = 864000000000000000L;
                ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
                for (int i = 0; i < threadCount; i++) {
                    String configStr = concurrentLinkedQueue1.poll();
                    String[] configList = configStr.split(",");
                    String userName = configList[0];
                    String passWord = configList[1];
                    sessionId++;
                    deviceId++;
                    String token = null;
                    //                    String token = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjNBODVERThBQjVDQzk1OTgxNEIzOThCNDZEMzFERDk1QTdDODc1RjciLCJ0eXAiOiJKV1QiLCJ4NXQiOiJPb1hlaXJYTWxaZ1VzNWkwYlRIZGxhZklkZmMifQ.eyJuYmYiOjE1OTgzMTc5MDAsImV4cCI6MTU5ODQwNDMwMCwiaXNzIjoiaHR0cDovLzE5Mi4xNjguNi4yMTk6MTAwNiIsImF1ZCI6WyJodHRwOi8vMTkyLjE2OC42LjIxOToxMDA2L3Jlc291cmNlcyIsInhzeXgtZ2VtaW5pLXdvcmtiZW5jaCJdLCJjbGllbnRfaWQiOiJ4c3l4LWdlbWluaS13b3JrYmVuY2giLCJzdWIiOiIxMjU1MDExMDA2NjQ0MDUwIiwiYXV0aF90aW1lIjoxNTk4MzE3ODk5LCJpZHAiOiJsb2NhbCIsInRlbmFudCI6IjEyNTUwMTEwMDY2NDQwNTAsZGFpdGluZyxkYWl0aW5nLDE4NTc1MDc1NzE1LGRhaXRpbmdAeHN5eHNjLmNvbSwxMjU0OTc5MTg2MzIzOTI0LDEwMDE1LOS6keWuouacjSwwLCwxMDAxNSzkupHlrqLmnI0sMjAyMC0wOC0yNSAwOToxMTozOSxGYWxzZSIsInRva2VuSWQiOiI0NzhhY2ZkOS1hOGRlLTRjYmUtYjc0NS0zN2IzM2RhNjFhYmUiLCJzY29wZSI6WyJ4c3l4LWdlbWluaS13b3JrYmVuY2giXSwiYW1yIjpbInB3ZCJdfQ.sf1ESUDrSSO8Wd35ADPONfVWJAJ_xDNP-CgV72NKxMCIcZMo-Mpi9DQO73kuoVjHPY1dCgZXlY6eDhym4GlvUVSK411_gmv29kAwllCKMfNwuocgYOtGjw4IAdKnDQYESTwBtH1nxgs4q-tbvpV6BS8Q7JW2vaI5DnJFVyBfbW8KqLKBS6zWv13TjK8BzzgnyLo6Pl_uMhPW-6n_9Fi-XMiM9UBPGEChFKFU8N4K4Vwue1cZjAedO2XRvbt9xgN7bFPQaHB75LNIOz-qBI-WDGlqSsZAsZBrL6ZP7adXWAdEyCcDsaeTw0Shm7VlF0cYw8jwW0qGPZW6n98Fw_FfJw";
                    String userId = null;
                    // 从麒麟系统获取token
                    String token_result = httpClient.doPost("http://192.168.6.219:1006/token/gentestaccesstoken",
                            "{\"clientId\": \"xsyx-gemini-workbench\",\"scopes\":[\"xsyx-gemini-workbench\"],\"userName\": \""
                                    + userName + "\",\"passWord\": \"" + passWord + "\",\"lifetime\": 86400}", null);
                    JSONObject tokenJson = JSON.parseObject(token_result);
                    if (tokenJson.getString("message").equals("success")) {
                        token = tokenJson.getString("data");
                        //                        System.out.println("从麒麟系统获取token成功. ");
                    } else {
                        System.out.println("Error: " + userName + "获取token失败， " + token_result);
                        //                        System.exit(-1);
                        continue;
                    }
                    
                    // 获取当前客服基础信息
                    String KF_result = httpClient.doGet("http://192.168.6.6:8466/customer/currentInfo", token);
                    JSONObject KFJson = JSON.parseObject(KF_result);
                    if (KFJson.getString("message").equals("成功")) {
                        JSONObject data = KFJson.getJSONObject("data");
                        userId = data.getString("userId");
                        JSONArray skillGroups = data.getJSONArray("skillGroups");
                        skillGroupIds = new String[skillGroups.size()];
                        //                         System.out.println(skillGroups);
                        for (int ii = 0; ii < skillGroups.size(); ii++) {
                            JSONObject obj = skillGroups.getJSONObject(ii);
                            skillGroupIds[ii] = obj.getString("skillGroupId");
                        }
                        //                        System.out.println(Arrays.toString(skillGroupIds));
                    } else {
                        System.out.println("Error: " + userName + "获取当前客服基础信息失败， " + KF_result);
                        //                        System.exit(-1);
                        continue;
                    }
                    
                    // 用户状态切换(下线)
                       /* String userStatus_result = httpClient.doPost("http://192.168.6.6:8466/customer/changeStatus", "{\"status\":\"offline\",\"skillGroups\":" + Arrays.toString(skillGroupIds) + "}", token);
                        JSONObject userStatusJson = JSON.parseObject(userStatus_result);
                        if (userStatusJson.getString("message").equals("成功")){
                            System.out.println(userName + " " + Arrays.toString(skillGroupIds) + "用户切换下线状态成功！");
                        }else{
                            System.out.println("Error: " + userName + "用户状态切换(下线)失败， " + userStatus_result);
                            // System.exit(-1);
                            continue;
                        }*/
                    
                    // 用户状态切换(上线)
                    String userStatus_result = httpClient.doPost("http://192.168.6.6:8466/customer/changeStatus",
                            "{\"status\":\"online\",\"skillGroups\":" + Arrays.toString(skillGroupIds) + "}", token);
                    JSONObject userStatusJson = JSON.parseObject(userStatus_result);
                    if (userStatusJson.getString("message").equals("成功")) {
                        System.out.println(userName + " " + Arrays.toString(skillGroupIds) + "用户切换上线状态成功！");
                    } else {
                        System.out.println("Error: " + userName + "用户状态切换(上线)失败， " + userStatus_result);
                        // System.exit(-1);
                        continue;
                    }
                    
                    concurrentLinkedQueue2
                            .add(sessionId + "," + userId + "," + deviceId + "," + token + "," + userName);
                    
                    // 与IM系统建立连接
                    f = executorService.submit(new MyWebSocketClient(new URI(webSocketUrl)));
                    
                }
            } else {
                System.out.println("Error: " + fileName + "文件不能为空!");
            }
            if (f != null) {
                try {
                    f.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        } else {
            String[] jarPath = new ReceiveMsgOnGemini().getClass().getProtectionDomain().getCodeSource().getLocation()
                    .toString().split("/");
            String jarName = jarPath[jarPath.length - 1];
            System.out.println("Error: command error!  Use: java -jar " + jarName + " userName.txt");
        }
        
        //        WebSocketClient client = new ReceiveMsgOnIM.MyWebSocketClient(new URI("ws://192.168.6.172:8008/"));
        //        client.connect();
        //        startWebSocket(new URI("ws://192.168.6.172:8008/"));
        
    }
}
