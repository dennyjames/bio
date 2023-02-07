package Smutans.bio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class App {

	private static final String INPUT_PATH = "input.txt";

	private static final String OUTPUT_PATH = "output.csv";

	public static String RESULT = "%s;%s;%s"; 

	private static String ANTIGEN = "ANTIGEN";

	public static void main(String[] args) throws Exception {

		List<String> sequenceList = readSequencesFromFile();

		PrintWriter writer = new PrintWriter(OUTPUT_PATH, "UTF-8");
		
		for (String s : sequenceList) {
			requestServer(s, "bacteria", "0.5", writer);
		}
		
		writer.close();
		
		System.out.println("END");
	}

	private static List<String> readSequencesFromFile() throws IOException {

		Path path = Paths.get(INPUT_PATH);
		return Files.readAllLines(path);
	}

	private static void requestServer(String sequence, String target, String threshold, PrintWriter writer) throws ClientProtocolException, IOException  {
		HttpClient httpclient = HttpClients.createDefault();
		HttpPost httppost = new HttpPost("http://www.ddg-pharmfac.net/vaxijen/scripts/VaxiJen_scripts/VaxiJen3.pl");

		// Request parameters and other properties.
		List<NameValuePair> params = new ArrayList<NameValuePair>(2);
		params.add(new BasicNameValuePair("seq", sequence));
		params.add(new BasicNameValuePair("uploaded_file", ""));
		params.add(new BasicNameValuePair("SequenceOnOff", "A"));
		params.add(new BasicNameValuePair("Target", target));
		params.add(new BasicNameValuePair("threshold", threshold));
		params.add(new BasicNameValuePair("submit", "Submit"));

		httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

		HttpResponse response = httpclient.execute(httppost);
		HttpEntity entity = response.getEntity();

		
		
		writeToOutputFile(sequence, writer, entity);

	}

	private static void writeToOutputFile(String sequence, PrintWriter writer, HttpEntity entity) throws IOException {
		if (entity != null) {
			try (InputStream instream = entity.getContent()) {
				String html = new BufferedReader(new InputStreamReader(instream, StandardCharsets.UTF_8)).lines()
						.filter(i -> i.startsWith("<b><h1>")).collect(Collectors.joining("\n"));

				Document doc = Jsoup.parse(html);
				String[] arr = doc.body().text().split(" ");
				String atingenicScore = arr[18];
				String prediction = ANTIGEN.equals(arr[21]) ? "Antigen" : "Non-Antigen";
				System.out.println(String.format(RESULT, sequence, atingenicScore, prediction));
				writer.println(String.format(RESULT, sequence, atingenicScore, prediction));
			}
			
			
		}
	}

}
