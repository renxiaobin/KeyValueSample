package cn.xuan.kvstore;

import cn.helium.kvstore.common.KvStoreConfig;
import cn.helium.kvstore.processor.Processor;
import cn.helium.kvstore.rpc.RpcClientFactory;
import cn.helium.kvstore.rpc.RpcServer;
import cn.xuan.util.HDFSUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyProcessor implements Processor {
    private String hdfsUrl = null;
    private String storePath = null;
    private String indexPath = null;

    //    private Map<String, String> index = null;
    private Map<String, String> tmpIndex = null;

    private Map<String, Map<String, String>> tmpStore = null;
    private Map<String, Map<String, String>> store = null;

    private HDFSUtil hdfsUtil;

    private Logger logger = Logger.getLogger(MyProcessor.class);

    private String[] storePathList = new String[]{"/store0","/store1","/store2"};
    private String[] indexPathList = new String[]{"/index0","/index1","/index2"};
    private Map<String, String>[] indexList = null;

    private int server_id;

    public MyProcessor() {
        server_id = 1;
        hdfsUrl = "hdfs://localhost:8020";
//        hdfsUrl = KvStoreConfig.getHdfsUrl();
//        server_id = RpcServer.getRpcServerId();

        tmpIndex = new HashMap<String, String>();
        indexList = new HashMap[3];
        for (int i = 0; i < 3; i++) {
            indexList[i] = new HashMap<String, String>();
        }
        tmpStore = new HashMap<String, Map<String, String>>();
        store = new HashMap<String, Map<String, String>>();
        try {
            hdfsUtil = new HDFSUtil(hdfsUrl);
        } catch (IOException e) {
            logger.error(e);
        }
        load_index(server_id);
        new Thread(new WriteDataThread()).start();
    }


    @Override
    public Map<String, String> get(String s) {
        if (this.store.containsKey(s)) {
            return this.store.get(s);
        } else {
            if (indexList[server_id].containsKey(s)) {
                return getValueFromLocal(s);
            } else {
                return getValueFromOtherPod(s);
            }

        }
    }

        @Override
        public boolean put (String s, Map < String, String > map){
            synchronized (tmpStore) {
                tmpStore.put(s, map);
                return true;
            }
        }

        @Override
        public boolean batchPut (Map < String, Map < String, String >> map){
            synchronized (tmpStore) {
                if (tmpStore.size() > 1000000) {
                    try {
                        Thread.sleep(200000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    tmpStore.putAll(map);
                }
                return true;
            }
        }

        @Override
        public int count (Map < String, String > map){
            int count = 0;

            return count;
        }

        @Override
        public Map<Map<String, String>, Integer> groupBy (List < String > list) {
            return null;
        }

        @Override
        public byte[] process ( byte[] bytes){
            String msg = new String(bytes);
            Map<String, String> reply = this.getValueFromLocal(msg);
            if (reply == null) {
                return null;
            } else {
                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                try {
                    ObjectOutputStream out = new ObjectOutputStream(byteOut);
                    out.writeObject(reply);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                byte[] replybyte = byteOut.toByteArray();
                return replybyte;
            }
        }

        private class WriteDataThread implements Runnable {

            @Override
            public void run() {
            indexPath = indexPathList[server_id];
            storePath = storePathList[server_id];

//                storePath = "/store1";
//                indexPath = "/index1";
                Map<String, Map<String, String>> tmp;
                while (true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    synchronized (tmpStore) {
                        if (tmpStore.keySet().size() == 0) {
                            continue;
                        }
                        tmp = (HashMap) ((HashMap) tmpStore).clone();
                        tmpStore.clear();
                    }
//                    System.out.println("store:" + store.size());
//                    System.out.println("tmp:" + tmp.size());
//                    System.out.println("tmpIndex:" + tmpIndex.size());
//                    System.out.println("write");
                    store.putAll(tmp);
                    try {
                        hdfsUtil.getHDFSfFileStream(storePath);
                        StringBuffer buffer = new StringBuffer();
                        for (Map.Entry<String, Map<String, String>> entry : tmp.entrySet()) {
                            buffer.append("{").append(entry.toString()).append("}");
                            Map<String, Long> info = hdfsUtil.write_to_hdfs(buffer.toString());
                            tmpIndex.put(entry.getKey(), String.valueOf(info.get("start_pos")) + "|" + String.valueOf(info.get("length")));
                            buffer.setLength(0);
                        }

                        hdfsUtil.fsdos_close();
                        hdfsUtil.getHDFSfFileStream(indexPath);
                        hdfsUtil.write_index_to_hdfs(tmpIndex);
                        hdfsUtil.fsdos_close();
                        indexList[server_id].putAll(tmpIndex);
                        tmpIndex.clear();
                        logger.info("success :" + tmp.size());
                    } catch (IOException e) {
                        e.printStackTrace();
                        logger.error(e);
                        break;
                    }

                }
            }

        }

    public void load_index(int i) {
        try {
            indexList[i] = hdfsUtil.read_index_from_hdfs(indexPathList[i]);
//            System.out.println("indexlistsize:"+indexList[i].size());
            logger.info("load index" + i + " success!");
        } catch (IOException e) {
            logger.error(e);
        }
    }

    //    public void load_Data() {
//        synchronized (store) {
//            for (int i = 0; i < indexList.length; i++) {
//                for (Map.Entry<String, String> entry : indexList[i].entrySet()) {
//                    long start_pos = Long.valueOf(entry.getValue().split("\\|")[0]);
//                    long length = Long.valueOf(entry.getValue().split("\\|")[1]);
//                    try {
//                        String re=hdfsUtil.read_from_hdfs(storePathList[i], start_pos, length);
//                        Gson gson = new Gson();
//                        Map<String, Map<String, String>> t = gson.fromJson(re, new TypeToken<Map<String, Map<String, String>>>() {}.getType());
//                        store.putAll(t);
//                    } catch (IOException e) {
//                        logger.error(e);
//                    }
//                }
//            }
//        }
//    }
    public Map<String, String> getValueFromLocal(String s) {
        String[] index_re = null;
        String store_path = null;
        if (indexList[server_id].containsKey(s)) {
            index_re = indexList[server_id].get(s).split("\\|");
            store_path = storePathList[server_id];
            String result = null;
            if (index_re != null) {
                try {
                    result = hdfsUtil.read_from_hdfs(store_path, Long.valueOf(index_re[0]), Long.valueOf(index_re[1]));
                } catch (IOException e) {
                    logger.error(e);
                }
            }
            if (result != null) {
                Gson gson = new Gson();
                Map<String, Map<String, String>> t = gson.fromJson(result, new TypeToken<Map<String, Map<String, String>>>() {
                }.getType());
                return t.get(s);
            }
        }
        return null;
    }

    private Map<String, String> getValueFromOtherPod(String s) {
        for (int i = 0; i < 3; i++) {
            if (i == server_id) {
                continue;
            }
            byte[] re;
            try {
                if ((re = RpcClientFactory.inform(i, s.getBytes())) != null) {
                        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(re);
                        ObjectInputStream oi = new ObjectInputStream(byteArrayInputStream);
                        Map<String, String> a = (Map<String, String>) oi.readObject();
                        return a;
                    } else {
                        continue;
                    }
            } catch (IOException e) {
                load_index(i);
                Map<String, String> remap;
                if ((remap = getValueFromLocal(s)) != null) {
                    return remap;
                } else {
                    continue;
                }
            } catch (ClassNotFoundException e) {
                logger.error(e);
            }
        }
        return null;
    }

}





