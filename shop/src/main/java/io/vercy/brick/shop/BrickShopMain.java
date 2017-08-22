package io.vercy.brick.shop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

public class BrickShopMain {

    private static final Logger log = LoggerFactory.getLogger(BrickShopMain.class);

    public static void main(String[] args) throws Exception {
        BrickSetFactory factory = new BrickSetFactory();
        while(true) {
            sendBrickSet(factory.nextBrickSet());
            Thread.sleep(1000);
        }
    }

    private static void sendBrickSet(byte[] brickSet) {
        try {
            URL url = new URL("http://localhost:8080/brick?color=red&length=20");
            HttpURLConnection cn = (HttpURLConnection) url.openConnection();
            cn.setRequestProperty("Content-Type", "text/plain");
            cn.setRequestMethod("GET");
            cn.setDoInput(true);
            String response = readFully(cn.getInputStream());
            int responseCode = cn.getResponseCode();
            if(responseCode != 200) {
                log.warn("< Failed: {}, status: {}", url, responseCode);
            } else {
                log.info("> {}", response);
            }
        } catch(IOException ex) {
            log.warn("Request failed", ex);
        }
    }

    private static String readFully(InputStream in) throws IOException {
        try(InputStreamReader reader = new InputStreamReader(in, Charset.forName("UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            int c;
            while((c = reader.read()) != -1) {
                sb.append((char)c);
            }
            return sb.toString();
        }
    }
}