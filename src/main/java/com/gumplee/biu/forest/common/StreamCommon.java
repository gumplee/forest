package com.gumplee.biu.forest.common;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import org.apache.http.ConnectionClosedException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.gumplee.biu.forest.thread.MultiThreadDownService;
import com.gumplee.biu.forest.vo.DownInfoVO;
import com.gumplee.biu.forest.vo.StreamReqeustVO;


/**                           _ooOoo_  
 *                           o8888888o  
 *                           88" . "88  
 *                           (| -_- |)  
 *                            O\ = /O  
 *                        ____/`---'\____  
 *                      .   ' \\| |// `.  
 *                       / \\||| : |||// \  
 *                     / _||||| -:- |||||- \  
 *                       | | \\\ - /// | |  
 *                     | \_| ''\---/'' | |  
 *                      \ .-\__ `-` ___/-. /  
 *                   ___`. .' /--.--\ `. . __  
 *                ."" '< `.___\_<|>_/___.' >'"".  
 *               | | : `- \`.;`\ _ /`;.`/ - ` : | |  
 *                 \ \ `-. \_ __\ /__ _/ .-` / /  
 *         ======`-.____`-.___\_____/___.-`____.-'======  
 *                            `=---='  
 *  
 *         .............................................  
 *                  	      佛祖保佑             永无BUG 
*/

@Service("streamCommon")
public class StreamCommon {
	private static Logger logger = LoggerFactory.getLogger(StreamCommon.class);
	@Resource(name="multiThreadDownService")
	MultiThreadDownService multiThreadDownService;
	public static final int BUFSIZE = 1024 * 8;  
	private static HashMap<String, String> sites = new HashMap<String, String>() {
		
		private static final long serialVersionUID = 1359643545160195793L;

		{
			put("cctv", "cntv");
			put("cntv", "cntv");
			put("qq", "qq");
			put("sina", "sina");
			put("sohu", "sohu");
			put("acfun", "acfuntv");
			put("baomihua", "baomihua");
			put("bilibili", "bilibili");
			put("dilidili", "dilidili");
			put("fun", "funshion");
			put("ifeng", "ifeng");
			put("iqilu", "iqilu");
			put("weibo", "miaopai");
			put("163", "netease");
			put("pptv", "pptv");
			put("qianmo", "qianmo");
			put("ted", "ted");
			put("tucao", "tucao");
			put("tudou", "tudou");
			put("xiaokaxiu", "yixia");
			put("zhanqi", "zhanqi");
			put("iqiyi", "iqiyi");
			put("56", "w56");
			put("yinyuetai", "yinYueTai");
			put("ku6", "ku6");
			put("baidu", "baidu");
			put("letv","letv");
			put("le","letv");
			put("youku","youku");
		}
	};

	private static HashMap<String, String> contentType = new HashMap<String, String>() {
		
		private static final long serialVersionUID = 8171321387021292864L;

		{
			put("video/3gpp", "3gp");
			put("video/f4v", "flv");
			put("video/mp4", "mp4");
			put("video/MP2T", "ts");
			put("video/quicktime", "mov");
			put("video/webm", "webm");
			put("video/x-flv", "flv");
			put("video/x-ms-asf", "asf");
			put("audio/mp4", "mp4");
			put("audio/mpeg", "mp3");
			put("image/jpeg", "jpg");
			put("image/png", "png");
			put("image/gif", "gif");
		}
	};

	private static HashMap<String, String> fakeHeader = new HashMap<String, String>() {
		
		private static final long serialVersionUID = -8200221030291691991L;

		{
			put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			put("Accept-Charset", "UTF-8,*;q=0.5");
			put("Accept-Encoding", "gzip,deflate,sdch");
			put("Accept-Language", "en-US,en;q=0.8");
			put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:13.0) Gecko/20100101 Firefox/13.0");
		}
	};

	
	public boolean urlSave(String url,Long fileSize, Long startIndex,Long endIndex,
			String outputFilePath, SimpleProgressBar bar, boolean isPart,StreamReqeustVO srVo)
					throws ConnectionClosedException, SocketTimeoutException, ConnectException 
	{
		
		File file = definedVideoFile(srVo, outputFilePath, fileSize, bar, isPart);
		File tempFile = new File(outputFilePath + ".download");
		Long received = createTmpFile(srVo, tempFile, bar);
		
		if (received < fileSize) {
			StreamReqeustVO newSrvo = setResumePoint(srVo, received,startIndex,endIndex);
			HttpResponse response = byHttpGet(url,newSrvo);
			if (response == null || response.getStatusLine().getStatusCode() > 300){
				logger.info("{} response is illegal",srVo.getUrl());
				return false;
			}
			boolean result = writeVideoFile(newSrvo, response, tempFile, received, bar, fileSize);
			if (!result) return false;
		}
		reName(file, tempFile);
		return true;
	}

	
	public boolean downloadVideo(List<String> urls, String title, String ext, Long size, StreamReqeustVO srVo) {
		SimpleProgressBar simpleProgressBar = new SimpleProgressBar(srVo.isDisplayProgressBar(), size, urls.size());
		String output_filename = title + "." + ext;
		String output_filePath = preDownloadHandler(urls, simpleProgressBar, title, ext, size, srVo,output_filename);
		if(output_filePath.isEmpty())return false;
		
		if (urls.size() == 1) {
			String url = urls.get(0);
			logger.info("{} Downloading {} ",srVo.getUrl(),output_filename);
			
			Long fileSize = urlInfo(url, srVo);
			if(fileSize == 0){
				logger.info("{} get video size failure",srVo.getUrl());
				return false;
			}
			Long blockSize = judgeBlockSize(fileSize);
			boolean flag = blockSize.compareTo(fileSize) > 0;
			String count = String.valueOf(Math.floor(fileSize / blockSize));
			count = count.substring(0,count.indexOf("."));
			int num = Integer.valueOf(count);
			List<DownInfoVO> downInfoVOs = new ArrayList<DownInfoVO>();
			List<String> mergeFiles = new ArrayList<String>();
			String mergedFilePath = srVo.getOutPath() + File.separator + title + "." + ext;
			for (int i = 0; i < num + 1; i++) {

				DownInfoVO downInfoVO = new DownInfoVO();
				String fileNum = String.format("%02d", i);
				output_filename = title + "[" + fileNum + "]." + ext;
				output_filePath = srVo.getOutPath() + File.separator + output_filename;
				mergeFiles.add(output_filePath);
				
				downInfoVO.setUrl(url);
				if (flag && i == 0) {
					downInfoVO.setStartIndex(0l);
					downInfoVO.setEndIndex(fileSize - 1);
				}else if (i == num) {
					downInfoVO.setStartIndex(blockSize * i + 1);
					downInfoVO.setEndIndex(fileSize - 1);
				}else {
					downInfoVO.setStartIndex(blockSize * i + (i == 0 ? 0 : 1));
					downInfoVO.setEndIndex(blockSize * (i + 1));
				}
				downInfoVO.setSize(downInfoVO.getEndIndex() - downInfoVO.getStartIndex() + 1);
				downInfoVO.setAbsFileName(output_filePath);
				downInfoVO.setSrVo(srVo);
				downInfoVO.setSpb(simpleProgressBar);
				downInfoVOs.add(downInfoVO);
			}
			
			List<DownInfoVO> result = multiThreadDownService.down(downInfoVOs);
			for (int i = 0; i < result.size(); i++)
			{
				Integer r = result.get(i).getStatus();
				if (r != 1)return false;
			}
			
			simpleProgressBar.done(output_filePath);
			mergeFile(mergedFilePath, mergeFiles);
			logger.info("{} download video completed! ",output_filePath);
		} else {
			logger.info("{} Downloading {} ",srVo.getUrl(),output_filename);
			
			if (srVo.isMultiThread())
			{
				List<DownInfoVO> downInfoVOs = new ArrayList<DownInfoVO>();
				for (int i = 0; i < urls.size(); i++) {
					DownInfoVO downInfoVO = new DownInfoVO();
					String url = urls.get(i);
					Long fileSize = urlInfo(url, srVo);
					if(fileSize == 0){
						logger.info("{} get video size failure",srVo.getUrl());
						return false;
					}
					String fileNum = String.format("%02d", i);
					output_filename = title + "[" + fileNum + "]." + ext;
					output_filePath = srVo.getOutPath() + File.separator + output_filename;
					
					downInfoVO.setUrl(url);
					downInfoVO.setSize(fileSize);
					downInfoVO.setStartIndex(0l);
					downInfoVO.setEndIndex(0l);
					downInfoVO.setAbsFileName(output_filePath);
					downInfoVO.setSrVo(srVo);
					downInfoVO.setSpb(simpleProgressBar);
					downInfoVOs.add(downInfoVO);
				}
				List<DownInfoVO> result = multiThreadDownService.down(downInfoVOs);
				for (int i = 0; i < result.size(); i++)
				{
					Integer r = result.get(i).getStatus();
					if (r != 1)return false;
				}
				simpleProgressBar.done(output_filePath);
				logger.info("{} download video completed! ",output_filePath);
			}
		}
		return true;
	}
	

	public Long urlInfo(String url, StreamReqeustVO srVo) {

		HttpResponse response = byHttpGet(url, srVo);

		if (response.getStatusLine().getStatusCode() == 200) {
			Header teHeader = response.getFirstHeader("transfer-encoding");
			if (teHeader == null || !teHeader.toString().equals("chunked")) {
				return Long.valueOf(response.getFirstHeader("content-length").getValue());
			}
		}
		return 0l;
	}


	public String getFileExtName(String url, StreamReqeustVO srVo) {
		HttpResponse response = byHttpGet(url, srVo);

		if (response.getStatusLine().getStatusCode() == 200) {
			String type = response.getFirstHeader("content-type").getValue();
			if (type != null && !type.isEmpty()) {
				String ext = contentType.get(type);
				if (ext != null) {
					return ext;
				} else {
					type = null;
					Header header = response.getFirstHeader("content-disposition");
					if (header != null) {
						type = header.getValue();
						if (type != null) {
							String fileName = match2(type, "filename=\"?([^\"]+)\"?");
							try {
								fileName = URLDecoder.decode(fileName, "utf8");
								if (fileName.split(".").length > 1) {
									String[] fileNames = fileName.split(".");
									ext = fileNames[fileNames.length - 1];
									return ext;
								}
							} catch (UnsupportedEncodingException e) {
								return "mp4";
							}
						}
					}

				}
			}
		}
		return "mp4";
	}


	public HttpResponse byHttpGet(String url, StreamReqeustVO srVo) {
		if (srVo.isFaker()) {
			HashMap<String, String> headers = srVo.getHeaders();
			headers.putAll(fakeHeader);
		}
		String refer = srVo.getReferer();

		if (refer != null && !refer.isEmpty()) {
			HashMap<String, String> headers = srVo.getHeaders();
			headers.put("referer", srVo.getReferer());
		}
		if (srVo.getCustomUA() != null) {
			HashMap<String, String> headers = srVo.getHeaders();
			headers.put("User-Agent", srVo.getCustomUA());
		}
		HttpGet get = new HttpGet(url);
		HashMap<String, String> headers = srVo.getHeaders();
		if (headers != null) {
			Iterator<String> iterator = headers.keySet().iterator();
			while (iterator.hasNext()) {
				String name = iterator.next();
				String value = headers.get(name);
				get.addHeader(name, value);
			}
		}

		CloseableHttpClient httpClient = HttpClients.createDefault();
		CloseableHttpResponse response = null;
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(1000 * 60 * 5).setConnectTimeout(1000 * 30).build();//设置请求和传输超时时间
		get.setConfig(requestConfig);
		try {
			response = httpClient.execute(get);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}


	public String urlToModule(String url) {
		url = urlAppendHead(url);
		String video_host = match2(url, "https?://([^/]+)/");

		if (video_host.endsWith(".com.cn")) {
			video_host = video_host.substring(0, video_host.length() - 3);
		}
		String domain = match2(video_host, "(\\.?[^.]+\\.[^.]+)$");
		if (domain == null) {
			domain = video_host;
		}
		String k = match2(domain, "([^.]+)");
		if (sites.get(k) != null) {
			return sites.get(k);
		}
		return "";
	}

	public boolean r1(String pattern, String text) {
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(text);
		if (m.find()) {
			return true;
		}
		return false;
	}


	public List<String> match1(String text, List<String> patterns) {
		ArrayList<String> result = null;
		if (patterns.size() > 0) {
			result = new ArrayList<String>();
			for (int i = 0; i < patterns.size(); i++) {
				String pattern = patterns.get(i);
				Pattern p = Pattern.compile(pattern);
				Matcher m = p.matcher(text);
				if (m.find()) {
					result.add(m.group(1));
				}
			}
		}
		return result;
	}


	public String match2(String text, String pattern) {
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(text);
		if (m.find()) {
			return m.group(1);
		}
		return "";
	}

	public List<String> matchAll(String text, String pattern) {
		List<String> result = new ArrayList<String>();
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(text);
		while (m.find()) {
			int num = m.groupCount();
			for (int i = 1; i <= num; i++) {
				result.add(m.group(i));
			}
		}
		return result;
	}


	public String getHtml(String url, StreamReqeustVO srVo, boolean decoded) {
		HttpResponse response = byHttpGet(url, srVo);
		String content = "";
		if (response != null) {
			HttpEntity entity = response.getEntity();
			BufferedReader br = null;
			InputStream in = null;
			try {
				in = entity.getContent();
				if (decoded)
				{
					String charset = "";
					String contentType = response.getFirstHeader("content-type").getValue();
					if (!contentType.isEmpty()) {
						charset = match2(contentType, "charset=([\\w-]+)");
					}
					if (charset.isEmpty())
					{
						charset = "utf8";
					}
					br = new BufferedReader(new InputStreamReader(in,charset));
				}
				else {
					br = new BufferedReader(new InputStreamReader(in));
				}
				String line;// 循环读取
				while ((line = br.readLine()) != null) {
					content += line;
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return content;
	}


	public String getHtml(String url) {
		return getHtml(url, "UTF-8");
	}


	public String getHtml(String url, String charset) {
		InputStream is = null;
		String dataString = "";
		try {
			URL dataUrlObj = new URL(url);
			URLConnection urlConn = dataUrlObj.openConnection();
			urlConn.setDoOutput(true);
			urlConn.setDoInput(true);
			urlConn.setConnectTimeout(1000);
			urlConn.setReadTimeout(1000);
			urlConn.connect();
			is = urlConn.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(is, charset));
			StringBuffer sb = new StringBuffer();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
			dataString = sb.toString();
			return dataString;
		} catch (Exception e) {
			return "";
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					return "";
				}
			}
		}
	}


	public String urlAppendHead(String url) {
		if (url.startsWith("https://")) {
			url = url.substring(8);
		}
		if (!url.startsWith("http://")) {
			url = "http://" + url;
		}
		return url;
	}


	public String makeMd5Key(String str) {
		if (str == null) {
			return "";
		}
		StringBuilder md5String = new StringBuilder();
		byte[] input = null;
		MessageDigest digester = null;
		byte[] messageDigest = null;
		try {
			input = str.getBytes("UTF-8");
			digester = MessageDigest.getInstance("MD5");
			digester.reset();
			digester.update(input);
			messageDigest = digester.digest();
			String hexStr = null;
			for (int i = 0; i < messageDigest.length; i++) {
				hexStr = Integer.toHexString(0xFF & messageDigest[i]);
				if (hexStr.length() == 1) {
					md5String.append("0").append(hexStr);
				} else {
					md5String.append(hexStr);
				}
			}
		} catch (NoSuchAlgorithmException ex) {
			return "";
		} catch (UnsupportedEncodingException ex) {
			return "";
		} finally {
			input = null;
			digester = null;
			messageDigest = null;
		}
		return md5String.toString();
	}


	public String processTitle(String title) {
		if (title == null || title.isEmpty()) {
			return "";
		}
		String t = title;

		while (t.startsWith(".")) {
			t = t.substring(1);
		}
		t = t.replaceAll("\\s", "-");
		t = t.replaceAll("/", "-");
		t = t.replaceAll("\\\\", "-");
		t = t.replaceAll(":", "-");
		t = t.replaceAll("\\*", "-");
		t = t.replaceAll("<", "-");
		t = t.replaceAll(">", "-");
		t = t.replaceAll("\\?", "-");
		t = t.replaceAll("\\|", "-");
		t = t.replaceAll("\"", "-");
		return t;
	}
	

	private File definedVideoFile(StreamReqeustVO srVo,String outputFilePath,
			Long fileSize,SimpleProgressBar bar,boolean isPart)
	{
		File file = new File(outputFilePath);
		if (file.exists()) {
			if (!srVo.isForce() && fileSize == file.length()) {
				if (!isPart) {
					bar.done(outputFilePath);
					logger.info("{} download video completed! ",outputFilePath);
				} else {
					bar.update_received(fileSize);
				}
				logger.info("Skipping {} : file already exists",outputFilePath);
			} else {
				if (!isPart) {
					bar.done(outputFilePath);
					logger.info("Overwriting {}",outputFilePath);
				}
			}
		} else {
			File dirFile = new File(srVo.getOutPath());
			if (!dirFile.exists()) {
				dirFile.mkdirs();
			}
		}
		return file;
	}
	

	private Long createTmpFile(StreamReqeustVO srVo,File tempFile,SimpleProgressBar bar)
	{
		Long received = 0l;
		if (!srVo.isForce()) {
			if (tempFile.exists()) {
				received += tempFile.length();
				bar.update_received(tempFile.length());
			} else {
				try {
					tempFile.createNewFile();
				} catch (IOException e) {
					logger.info(e.getMessage());
				}
			}
		}
		return received;
	}
	

	private boolean writeVideoFile(StreamReqeustVO srVo,HttpResponse response,File tempFile,
			Long received,SimpleProgressBar bar,Long fileSize)throws ConnectionClosedException,SocketTimeoutException,ConnectException
	{
		HttpEntity entity = response.getEntity();
		InputStream in = null;
		FileOutputStream fout = null;
		try {
			in = entity.getContent();
			fout = new FileOutputStream(tempFile, true);
			int l = -1;
			byte[] tmp = new byte[1024 * 256];
			while ((l = in.read(tmp)) != -1) {
				fout.write(tmp, 0, l);
				received += l;
				bar.update_received(l);
			}
			fout.flush();
			fout.close();
		} catch (ConnectionClosedException e) {
			logger.info(e.getMessage());
			throw new ConnectionClosedException("Premature end of Content-Length delimited message body");
		}catch(SocketTimeoutException e){
			logger.info(e.getMessage());
			throw new SocketTimeoutException("httpclient 传输超时");
		}catch(ConnectException e){
			logger.info(e.getMessage());
			throw new ConnectException("httpclient 链接超时");
		}catch (UnsupportedOperationException e){
			logger.info(e.getMessage());
			e.printStackTrace();
		}catch (IOException e){
			logger.info(e.getMessage());
			e.printStackTrace();
		}
		finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					logger.info(e.getMessage());
					return false;
				}
			}
			if (fout != null){
				try{
					fout.flush();
					fout.close();
				}
				catch (IOException e){
					logger.info(e.getMessage());
					return false;
				}
			}
			if (!fileSize.equals(received)){
				logger.info(" {} 接受文件大小不一致---下载失败",srVo.getUrl());
				return false;
			}
		}
		return true;
	}

	private StreamReqeustVO setResumePoint(StreamReqeustVO srVo,Long received,Long startIndex,Long end)
	{
		StreamReqeustVO newSrvo = null;
		if (received.compareTo(0l) > 0) {//续传的情况
			newSrvo = srVo.clone(srVo.getUrl());
			HashMap<String, String> headers = newSrvo.getHeaders();
			if (received.compareTo(startIndex) > 0){//  这种情况只在第一个文件块才有可能发生
				headers.put("Range", "bytes=" + String.valueOf(received)
						+ "-" + (end.equals(0l) ? "" : String.valueOf(end)));
			}else {//在第二个及以后的文件块中startIndex肯定大于received。
				headers.put("Range", "bytes=" + String.valueOf(startIndex + received)
						+ "-" + (end.equals(0l) ? "" : String.valueOf(end)));
			}
			newSrvo.setHeaders(headers);
		}
		else {//非续传的情况
			newSrvo = srVo.clone(srVo.getUrl());
			if (startIndex.equals(0l) && end.equals(0l)) {
				return newSrvo;
			}
			HashMap<String, String> headers = newSrvo.getHeaders();
			headers.put("Range", "bytes=" + String.valueOf(startIndex)
					+ "-" + (end.equals(0l) ? "" : String.valueOf(end)));
		}
		return newSrvo;
	}
	

	private void reName(File file,File tempFile)
	{
		if (file.exists()) {
			file.delete();
		}
		tempFile.renameTo(file);
	}
	

	private Long judgeBlockSize(Long fileSize)
	{
		if (fileSize.longValue() <= 104857600) {//100M
			return 5242880l;
		}else if (fileSize.longValue() > 104857600 && fileSize.longValue() <= 524288000 ) {
			return 10485760l;
		}else {
			return 20971520l;
		}
	}

	private String preDownloadHandler(List<String> urls,SimpleProgressBar simpleProgressBar,
			String title,String ext,Long size,StreamReqeustVO srVo,String output_filename)
	{
		if (urls == null || urls.size() == 0) {
			return "";
		}
		String output_filePath = srVo.getOutPath() + File.separator + output_filename;
		if (size > 0) {
			File file = new File(output_filePath);
			if (!srVo.isForce() && file.exists() && file.length() >= size * 0.9) {
				logger.info("Skipping {} :file already exists ", output_filePath);
				return output_filePath;
			}
		}
		else {
			logger.info("{} video size is 0");
			return "";
		}
		return output_filePath;
	}
	

	@SuppressWarnings("resource")
	private void mergeFile(String outFile,List<String> files)
	{
		FileChannel outChannel = null;
		try {
			outChannel = new FileOutputStream(outFile).getChannel();
			for(String f : files){
				FileChannel fc = new FileInputStream(f).getChannel(); 
				ByteBuffer bb = ByteBuffer.allocate(BUFSIZE);
				while(fc.read(bb) != -1){
					bb.flip();
					outChannel.write(bb);
					bb.clear();
				}
				fc.close();
				File tmpFile = new File(f);
				if (tmpFile.exists()) {
					tmpFile.delete();
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			try {if (outChannel != null) {outChannel.close();}} catch (IOException ignore) {}
		}
	}
}