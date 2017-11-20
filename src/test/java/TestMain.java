import cn.xuan.kvstore.MyProcessor;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jettison.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TestMain {

    public static void main(String[] args) {
        MyProcessor processor = new MyProcessor();
//       writeTest(processor);

        readTest(processor);
//        byte[] re =processor.process("679913".getBytes());
//        System.out.println("retostring:"+re.toString());
//        Gson gson = new Gson();
//        try {
//            ByteArrayInputStream byteArrayInputStream=new ByteArrayInputStream(re);
//            ObjectInputStream oi = new ObjectInputStream(byteArrayInputStream);
//            Map<String,String> a= (Map<String, String>) oi.readObject();
//            System.out.println("a="+a);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }
    }

    public static void writeTest(MyProcessor processor) {
        Map<String,Map<String,String>> mapList = TestData.getMapList();
        System.out.println("start-put");
        long t1=new Date().getTime();
        for(Map.Entry<String, Map<String,String>> entry:mapList.entrySet()) {
            processor.put(entry.getKey(), entry.getValue());
        }
        long t2=new Date().getTime();
        System.out.println("cost:"+(t2-t1));
    }



    public static void readTest(MyProcessor processor) {
        System.out.println(processor.get("7413"));

    }
}
