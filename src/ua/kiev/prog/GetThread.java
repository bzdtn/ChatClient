package ua.kiev.prog;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author Viktor Bezditnyi
 */
public class GetThread extends Thread{
    private int lastReadMessage;
    private String cookie;

    public GetThread(String cookies){
        this.cookie = cookies;
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                URL url = new URL("http://localhost:8080/get?from=" + lastReadMessage);
                HttpURLConnection http = (HttpURLConnection) url.openConnection();
                http.setRequestProperty("Cookie", cookie);
                int res = http.getResponseCode();
                if (res != 200) {
                    String errorInfo = http.getHeaderField("errorInfo");
                    System.out.println("HTTP error: " + res + ", info: " + errorInfo);
                    Thread.currentThread().interrupt();
                    break;
                }
                try (InputStream is = http.getInputStream()){
                    int sz = http.getContentLength(); //is.available();
                    if (sz > 0) {
                        byte[] buf = new byte[sz];//[is.available()];
                        is.read(buf);
                        Gson gson = new GsonBuilder().create();
                        Message[] list = gson.fromJson(new String(buf), Message[].class);
                        for (Message m : list) {
                            System.out.println(m);
//                            lastReadMessage++;
                        }
                        lastReadMessage = Integer.parseInt(http.getHeaderField("messageNumber"));
//                        System.out.println("last message: " + lastReadMessage);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
    }
}
