package com.gumplee.biu.forest.extractor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import javax.annotation.Resource;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gumplee.biu.forest.common.JsonOut;
import com.gumplee.biu.forest.common.StreamCommon;
import com.gumplee.biu.forest.common.StreamContext;
import com.gumplee.biu.forest.vo.StreamJSONResponseVO;
import com.gumplee.biu.forest.vo.StreamReqeustVO;

@Service("sohu")
public class Sohu extends BaseExtractor
{
	private static Logger logger = LoggerFactory.getLogger(Sohu.class);
	public static final String SITE_INFO = "Sohu.com";
	@Resource(name="streamCommon")
	StreamCommon common;
	
	@Override
	public void process(StreamContext context)
	{
		StreamReqeustVO srVo = (StreamReqeustVO)context.get(StreamContext.VideoInfo.STREAM_REQUEST_VO);
		execute(srVo,context);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void execute(StreamReqeustVO srVo,StreamContext context)
	{
		String vid = "";
		String url = srVo.getUrl();
		String content = "";
		if (common.r1("http://share.vrs.sohu.com", url))
		{
			vid = common.match2(url, "id=(\\d+)");
		}
		else {
			content = common.getHtml(url,srVo,true);
			vid = common.match2(content, "\\Wvid\\s*[:=]\\s*['\"]?(\\d+)['\"]?");
		}
		
		if (vid.isEmpty())
		{
			logger.info("{} not supported",srVo.getUrl());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, -1);
			return;
		}
		
		Long size = 0l;
		ArrayList<String> urls = new ArrayList<String>();
		String title = "";
		String ext = "mp4";
		if (common.r1("http://tv.sohu.com/", url))
		{
			try {
				content = common.getHtml("http://hot.vrs.sohu.com/vrs_flash.action?vid=" + vid, srVo,true);
				JSONObject root = JSONObject.parseObject(content);
				JSONObject data = root.getJSONObject("data");
				if (data != null)
				{
					String[] vids = {"oriVid","superVid","highVid" ,"norVid","relativeId"};
					for (int i = 0; i < vids.length; i++)
					{
						String hqvid = data.getString(vids[i]);
						if (!hqvid.equals("0") && !hqvid.equals(vid))
						{
							content = common.getHtml("http://hot.vrs.sohu.com/vrs_flash.action?vid=" + hqvid, srVo,true);
							root = JSONObject.parseObject(content);
							break;
						}
					}
				}
				
				if (root != null)
				{
					String host = root.getString("allot");
					String tvid = root.getString("tvid");
					data = root.getJSONObject("data");
					title = data.getString("tvName");
					title=DoesTitle(title);
					JSONArray clipsBytes = data.getJSONArray("clipsBytes");
					
					for (int i = 0; i < clipsBytes.size(); i++)
					{
						size += clipsBytes.getInteger(i);
					}
					JSONArray clipsUrl = data.getJSONArray("clipsURL");
					JSONArray su = data.getJSONArray("su");
					JSONArray ck = data.getJSONArray("ck");
					if (ck.size() == clipsUrl.size() &&  clipsUrl.size() == su.size())
					{
						for (int i = 0; i < ck.size(); i++)
						{
							try
							{
								String urlPath = "";
								if (!clipsUrl.getString(i).isEmpty())
								{
									urlPath = new URL(clipsUrl.getString(i)).getPath();
								}
								urls.add(getRealUrl(host, vid, tvid, su.getString(i), urlPath, ck.getString(i)));
							}
							catch (MalformedURLException e)
							{
								logger.info(e.getMessage());
								context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
								return;
							}
						}
					}
					context.put(StreamContext.VideoInfo.TITLE, title);
					context.put(StreamContext.VideoInfo.URLS, urls);
					context.put(StreamContext.VideoInfo.SIZE, size);
					context.put(StreamContext.VideoInfo.EXT, ext);
				}
			} catch (Exception e) {
				logger.info(e.getMessage());
				context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
				return;
			}
		}
		else {
			try {
				content = common.getHtml("http://my.tv.sohu.com/play/videonew.do?vid=" + vid + "&referer=http://my.tv.sohu.com", srVo,false);
				JSONObject root = JSONObject.parseObject(content);
				String host = root.getString("allot");
				String tvid = root.getString("tvid");
				JSONObject data = root.getJSONObject("data");
				title = data.getString("tvName");
				title=DoesTitle(title);
				JSONArray clipsBytes = data.getJSONArray("clipsBytes");
				for (int i = 0; i < clipsBytes.size(); i++)
				{
					size += clipsBytes.getInteger(i);
				}
				JSONArray clipsUrl = data.getJSONArray("clipsURL");
				JSONArray su = data.getJSONArray("su");
				JSONArray ck = data.getJSONArray("ck");
				if (ck.size() == clipsUrl.size() &&  clipsUrl.size() == su.size())
				{
					for (int i = 0; i < ck.size(); i++)
					{
						try
						{
							String urlPath = "";
							if (!clipsUrl.getString(i).isEmpty())
							{
								urlPath = new URL(clipsUrl.getString(i)).getPath();
							}
							urls.add(getRealUrl(host, vid, tvid, su.getString(i), urlPath, ck.getString(i)));
						}
						catch (MalformedURLException e)
						{
							e.printStackTrace();
						}
					}
				}
				context.put(StreamContext.VideoInfo.TITLE, title);
				context.put(StreamContext.VideoInfo.URLS, urls);
				context.put(StreamContext.VideoInfo.SIZE, size);
				context.put(StreamContext.VideoInfo.EXT, ext);
			} catch (Exception e) {
				logger.info(e.getMessage());
				context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
				return;
			}
		}
		
		getStreamJsonInfo(srVo, context);//封装视频信息json串
		
		if (srVo.isDownload())
		{
			boolean result = common.downloadVideo(urls, title, ext, size, srVo);
			if (result)
			{
				logger.info("{} downloadVideo successful",srVo.getUrl());
				context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 1);
			}
			else {
				logger.info("{} downloadVideo failure",srVo.getUrl());
				context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
			}
		}
	}
	

	public String getRealUrl(String host,String vid,String tvid,String ne,String clipURL,String ck)
	{
		String url = "http://"+host+"/?prot=9&prod=flash&pt=1&file="+clipURL+"&new="
					+ne +"&key="+ ck+"&vid="+vid+"&uid="+String.valueOf(System.currentTimeMillis())+"&t="
					+String.valueOf(Math.random())+"&rb=1";
		String content = common.getHtml(url);
		JSONObject root = JSONObject.parseObject(content);
		if (root != null)
		{
			return root.getString("url");
		}
		return "";
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean getStreamJsonInfo(StreamReqeustVO srVo,StreamContext context)
	{
		JsonOut jo = new JsonOut();
		HashMap<String, StreamJSONResponseVO> result = jo.print_info_json(SITE_INFO,context);
		context.put(StreamContext.VideoInfo.VIDEO_JSON_INFO, result);
		return true;
	}
	

	@SuppressWarnings("unchecked")
	public boolean getVideoById(String vid, StreamReqeustVO srVo,StreamContext context) {
		Long size = 0l;
		ArrayList<String> urls = new ArrayList<String>();
		String title = "";
		String ext = "mp4";
		String content = common.getHtml("http://hot.vrs.sohu.com/vrs_flash.action?vid=" + vid, srVo,false);
		JSONObject root = JSONObject.parseObject(content);
		JSONObject data = root.getJSONObject("data");
		if (data != null)
		{
			String[] vids = {"oriVid","superVid","highVid" ,"norVid","relativeId"};
			for (int i = 0; i < vids.length; i++)
			{
				String hqvid = data.getString(vids[i]);
				if (!hqvid.equals("0") && !hqvid.equals(vid))
				{
					content = common.getHtml("http://hot.vrs.sohu.com/vrs_flash.action?vid=" + hqvid, srVo,false);
					root = JSONObject.parseObject(content);
					break;
				}
			}
		}
		
		if (root != null)
		{
			String host = root.getString("allot");
			String tvid = root.getString("tvid");
			data = root.getJSONObject("data");
			title = data.getString("tvName");
			title=DoesTitle(title);
			JSONArray clipsBytes = data.getJSONArray("clipsBytes");
			
			for (int i = 0; i < clipsBytes.size(); i++)
			{
				size += clipsBytes.getInteger(i);
			}
			JSONArray clipsUrl = data.getJSONArray("clipsURL");
			JSONArray su = data.getJSONArray("su");
			JSONArray ck = data.getJSONArray("ck");
			if (ck.size() == clipsUrl.size() &&  clipsUrl.size() == su.size())
			{
				for (int i = 0; i < ck.size(); i++)
				{
					try
					{
						String urlPath = "";
						if (!clipsUrl.getString(i).isEmpty())
						{
							urlPath = new URL(clipsUrl.getString(i)).getPath();
						}
						urls.add(getRealUrl(host, vid, tvid, su.getString(i), urlPath, ck.getString(i)));
					}
					catch (MalformedURLException e)
					{
						logger.info(e.getMessage());
						context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
						return false;
					}
				}
			}
			context.put(StreamContext.VideoInfo.TITLE, title);
			context.put(StreamContext.VideoInfo.URLS, urls);
			context.put(StreamContext.VideoInfo.SIZE, size);
			context.put(StreamContext.VideoInfo.EXT, ext);
		}
		getStreamJsonInfo(srVo, context);//封装视频信息json串
		
		if (srVo.isDownload())
		{
			boolean result = common.downloadVideo(urls, title, ext, size, srVo);
			if (result)
			{
				logger.info("{} downloadVideo successful",srVo.getUrl());
				context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 1);
			}
			else {
				logger.info("{} downloadVideo failure",srVo.getUrl());
				context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
			}
		}
		return true;
	}
	
	private String DoesTitle(String title)
	{
		title = StringEscapeUtils.unescapeHtml(title);
        title = common.processTitle(title);		
		return title;
	}



}
