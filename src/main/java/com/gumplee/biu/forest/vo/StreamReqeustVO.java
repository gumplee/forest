package com.gumplee.biu.forest.vo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * 
 * @author liyuanjun
 *
 */
public class StreamReqeustVO
{
	private String url;
	private String charset;
	private boolean isDownload;
	private boolean getStream;
	private boolean isMultiThread = true;
	private String outPath = ".";
	private boolean isMerge = false;
	private boolean isForce = false;
	private boolean faker;
	private HashMap<String, String> headers = new HashMap<String, String>(); // 请求头信息
	private ArrayList<HttpCookie> cookies; // 请求Cookie信息
	private String referer;
	private String customUA;
	private boolean isPlaylist=false;   //true:下载列表中的所有视频;false:只下载当前视频
	private boolean isDisplayProgressBar = false;
	private ArrayList<String> videoProfile;//清晰度
	
	
	public boolean isGetStream()
	{
		return getStream;
	}
	public void setGetStream(boolean getStream)
	{
		this.getStream = getStream;
	}
	public String getUrl()
	{
		return url;
	}
	public void setUrl(String url)
	{
		this.url = url;
	}
	public HashMap<String, String> getHeaders()
	{
		return headers;
	}
	public void setHeaders(HashMap<String, String> headers)
	{
		this.headers = headers;
	}
	public ArrayList<HttpCookie> getCookies()
	{
		return cookies;
	}
	public void setCookies(ArrayList<HttpCookie> cookies)
	{
		this.cookies = cookies;
	}
	public String getCharset()
	{
		return charset;
	}
	public void setCharset(String charset)
	{
		this.charset = charset;
	}
	public String getOutPath()
	{
		return outPath;
	}
	public void setOutPath(String outPath)
	{
		this.outPath = outPath;
	}
	public boolean isMerge()
	{
		return isMerge;
	}
	public void setMerge(boolean isMerge)
	{
		this.isMerge = isMerge;
	}
	public boolean isDownload()
	{
		return isDownload;
	}
	public void setDownload(boolean isDownload)
	{
		this.isDownload = isDownload;
	}
	public boolean isForce()
	{
		return isForce;
	}
	public void setForce(boolean isForce)
	{
		this.isForce = isForce;
	}
	public boolean isFaker()
	{
		return faker;
	}
	public void setFaker(boolean faker)
	{
		this.faker = faker;
	}
	public String getReferer()
	{
		return referer;
	}
	public void setReferer(String referer)
	{
		this.referer = referer;
	}
	public String getCustomUA()
	{
		return customUA;
	}
	public void setCustomUA(String customUA)
	{
		this.customUA = customUA;
	}
	public ArrayList<String> getVideoProfile()
	{
		return videoProfile;
	}
	public void setVideoProfile(ArrayList<String> videoProfile)
	{
		this.videoProfile = videoProfile;
	}
	public boolean isPlaylist() {
		return isPlaylist;
	}
	public void setPlaylist(boolean isPlaylist) {
		this.isPlaylist = isPlaylist;
	}
	
	public StreamReqeustVO clone(String url) 
	{
		StreamReqeustVO desc=new StreamReqeustVO();
		
		desc.setUrl(url);
		HashMap<String, String> headers = getHeaders();
		Set<String> set = headers.keySet();
		Iterator<String> iterator = set.iterator();
		HashMap<String, String> newHeaders = new HashMap<String, String>();
		while(iterator.hasNext())
		{
			String name = iterator.next();
			newHeaders.put(name, headers.get(name));
		}
		desc.setHeaders(newHeaders);
		desc.setCharset(getCharset());
		desc.setDownload(isDownload());
		desc.setGetStream(isGetStream());
		desc.setOutPath(getOutPath());
		desc.setMerge(isMerge());
		desc.setFaker(isFaker());
		desc.setForce(isForce());
		desc.setReferer(getReferer());
		desc.setCustomUA(getCustomUA());	
		desc.setPlaylist(isPlaylist());
		desc.setDisplayProgressBar(isDisplayProgressBar());
		
		
		ArrayList<String> videoProfile=null;
		try {
			videoProfile = deepCopy(this.getVideoProfile());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}   
		//清晰度
		desc.setVideoProfile(videoProfile);
		
		return desc;
	}
	 @SuppressWarnings("unchecked")
	public static ArrayList<String> deepCopy(ArrayList<String> src) throws IOException, ClassNotFoundException
	 {   
	        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();   
	        ObjectOutputStream out = new ObjectOutputStream(byteOut);   
	        out.writeObject(src);   
	       
	        ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());   
	        ObjectInputStream in =new ObjectInputStream(byteIn);   
	        ArrayList<String> dest = (ArrayList<String>)in.readObject();   
	        return dest;   
	 }  
	
	public boolean isDisplayProgressBar()
	{
		return isDisplayProgressBar;
	}
	public void setDisplayProgressBar(boolean isDisplayProgressBar)
	{
		this.isDisplayProgressBar = isDisplayProgressBar;
	}
	public boolean isMultiThread()
	{
		return isMultiThread;
	}
	public void setMultiThread(boolean isMultiThread)
	{
		this.isMultiThread = isMultiThread;
	}
}
