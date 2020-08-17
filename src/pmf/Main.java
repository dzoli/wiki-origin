package pmf;


import static java.util.stream.Collectors.toList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;

import kong.unirest.Unirest;

public class Main {

	static final CharsetEncoder ENCODER = Charset.forName("Windows-1252").newEncoder();
	static final Charset UTF_8 = Charset.forName("UTF-8");
	static final String URL = "https://en.wikipedia.org/w/api.php?format=json&action=query&prop=revisions&rvlimit=1&rvprop=timestamp&rvdir=newer&titles=";
	static File OUT_OK;
	static File OUT_ERR;
	static FileWriter fw_ok;
	static FileWriter fw_err;
	static List<String> testLineList = new ArrayList<String>();
	
	static {
		RequestConfig globalConfig = RequestConfig.custom()
                .setCookieSpec(CookieSpecs.STANDARD)
                .build();
        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(globalConfig)
                .build();
		Unirest.config().httpClient(httpClient);
		Unirest.config().setDefaultHeader("cache-control", "no-cache");
	}
	
	public static void main(String[] args) {  
		OUT_OK = new File(Paths.get(System.getProperty("user.home"), "Downloads", "ok", "VSortedOriginDateOK.csv").toString());
		OUT_ERR = new File(Paths.get(System.getProperty("user.home"), "Downloads", "err", "VSortedOriginDateERR.csv").toString());
		
		try (InputStream in =  new FileInputStream("/home/novica/workspace-visoke/Wiki_intro/res/tableVSortedOrderedByAttack.csv"); BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			OUT_OK.createNewFile(); fw_ok = new FileWriter(OUT_OK); 
			OUT_ERR.createNewFile(); fw_err = new FileWriter(OUT_ERR);
			
			testLineList = reader.lines().collect(toList());
			testLineList.stream().forEach(Main::api);
			
			fw_ok.flush(); fw_ok.close();
			fw_err.flush(); fw_err.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private static String date(String d) {
		String a [] = d.split("-");
		String day = a[2].substring(0, a[2].indexOf("T"));
		LocalDate ld = LocalDate.of(Integer.valueOf(a[0]), Integer.valueOf(a[1]), Integer.valueOf(day));
		if (ld.isAfter(LocalDate.of(2015, 1, 1))) {
			return d.substring(0, d.indexOf("T"));
		} else {
			return "2015-01-01";
		}
	}

	private static void api(String txt) {
		JSONObject page;
		String pName = null;
		try {
			pName = txt.substring(0, txt.indexOf("\t"));

			if (pName.startsWith("\"") && pName.endsWith("\"")) {
				pName = pName.substring(1);
				pName = pName.substring(0, pName.length() - 1);
			} 

			// utf 8
			ByteBuffer bytes = ENCODER.encode(CharBuffer.wrap(pName.toCharArray()));
			pName = new String(bytes.array(), UTF_8);
			
			page =  Unirest.get(URL + pName).asJson().getBody().getObject().getJSONObject("query").getJSONObject("pages");
			String name = JSONObject.getNames(page)[0];

			System.out.println(page);
			
			if (!(Integer.parseInt(name) == -1)) { 
				String tstamp = page.getJSONObject(name).getJSONArray("revisions").getJSONObject(0).getString("timestamp");
				txt = txt.substring(txt.indexOf("\t"), txt.length()); //rest
				txt = pName + " - " + date(tstamp) + "\t" + txt;
  
				fw_ok.append(txt).append("\t").append("\n");
			} else {
				fw_ok.append(txt).append("\t").append("\n");
				
				fw_err.append(pName).append("\t")
				   .append("page not found")
				   .append("\t")
				   .append("\n");
			} 
		} catch (Exception e) {
			try { 
				fw_ok.append(txt).append("\t").append("\n");
				
				fw_err.append(pName).append("\t")
					   .append(e.getMessage())
					   .append("\t")
					   .append("\n");
			} catch (IOException e1) {System.err.println(" == err1: " + e.getMessage() + " ==");} 
		}
	}
	
}
