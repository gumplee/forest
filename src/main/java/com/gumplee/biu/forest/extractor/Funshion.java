package com.gumplee.biu.forest.extractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gumplee.biu.forest.common.StreamCommon;
import com.gumplee.biu.forest.common.StreamContext;
import com.gumplee.biu.forest.vo.StreamReqeustVO;


@Service("funshion")
public class Funshion extends BaseExtractor
{
	private static Logger logger = LoggerFactory.getLogger(Funshion.class);
	public static final String SITE_INFO = "Funshion.tv";
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
		String url = srVo.getUrl();
		
		if (common.r1("http://www.fun.tv/vplay/v-(\\w+)", url))
		{
			getVideoById(srVo,context);
		}
		else if (common.r1("http://www.fun.tv/vplay/g-(\\w+)", url)) {
			getVideoByDraId(srVo,context);
		}
		else {
			logger.info("{} not supported",srVo.getUrl());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, -1);
			return;
		}
	}
	
	@SuppressWarnings("unchecked")
	public void getVideoByDraId(StreamReqeustVO srVo,StreamContext context)
	{
		List<String> idList = common.matchAll(srVo.getUrl(), "http://www.fun.tv/vplay/g-(\\d+)(?:\\.v-)(\\d+)");
		String id = "";
		String curId = "";
		if (idList.size() == 0)
		{
			id = common.match2(srVo.getUrl(), "http://www.fun.tv/vplay/g-(\\w+)");
			String html = common.getHtml(srVo.getUrl(), srVo, true);
			curId = common.match2(html, "vplay.videoid\\s*=\\s*(\\d+)");
		}
		else if (idList.size() == 2) {
			id = idList.get(0);
			curId = idList.get(1);
		}
		
		String vUrl = String.format("http://pm.funshion.com/v5/media/episode?id=%1$s&cl=aphone&uc=5", id);
		String vHtml = common.getHtml(vUrl);
		if (!vHtml.isEmpty())
		{
			try {
				JSONObject root = JSONObject.parseObject(vHtml);
				JSONArray episodes = root.getJSONArray("episodes");
				for (int i = 0; i < episodes.size(); i++)
				{
					JSONObject j = episodes.getJSONObject(i);
					String vid = j.getString("id");
					if (vid.equals(curId))
					{
						getVideoByDraVid(vid, id, srVo,context);
						break;
					}
				}
			} catch (Exception e) {
				logger.info(e.getMessage());
				context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
				return;
			}
		}
		else {
			logger.info("{} not supported",srVo.getUrl());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, -1);
			return;
		}
	}
	
	@SuppressWarnings("unchecked")
	public void getVideoByDraVid(String vid,String id, StreamReqeustVO srVo,StreamContext context)
	{
		String tUrl = String.format("http://pm.funshion.com/v5/media/episode?id=%1$s&cl=aphone&uc=5", id);
		String tHtml = common.getHtml(tUrl);
		
		JSONObject root = JSONObject.parseObject(tHtml);
		JSONArray eArray = root.getJSONArray("episodes");
		String title = "";
		for (int i = 0; i < eArray.size(); i++)
		{
			JSONObject j = eArray.getJSONObject(i);
			if (j.getString("id").equals(vid))
			{
				title = root.getString("name") + "-" + j.getString("name");
			}
		}
		context.put(StreamContext.VideoInfo.TITLE, title);
		String vUrl = String.format("http://pm.funshion.com/v5/media/play/?id=%1$s&cl=aphone&uc=5", vid);
		String vHtml = common.getHtml(vUrl);
		ArrayList<String> urls = selectUrlsFromVideoAPI(vHtml);
		context.put(StreamContext.VideoInfo.URLS, urls);
		Long size = 0l;
		for (int i = 0; i < urls.size(); i++)
		{
			size += common.urlInfo(urls.get(i), srVo);
		}
		context.put(StreamContext.VideoInfo.SIZE, size);
		
		String ext = common.getFileExtName(urls.get(0), srVo);
		context.put(StreamContext.VideoInfo.EXT, ext);
		
		getStreamJsonInfo(SITE_INFO,srVo, context);//封装视频信息json串
		
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
	
	@SuppressWarnings("unchecked")
	public void getVideoById(StreamReqeustVO srVo,StreamContext context)
	{
		String url = srVo.getUrl();
		String vid = common.match2(url, "http://www.fun.tv/vplay/v-(\\d+)(.?)");
		if (!vid.isEmpty())
		{
			getVideoByVid(vid, srVo,context);
		}
		else {
			logger.info("{} not supported",srVo.getUrl());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, -1);
			return;
		}
	}

	@SuppressWarnings("unchecked")
	public void getVideoByVid(String vid,StreamReqeustVO srVo,StreamContext context)
	{
		String tUrl = String.format("http://pv.funshion.com/v5/video/profile?id=%1$s&cl=aphone&uc=5", vid);
		String tHtml = common.getHtml(tUrl);
		if (!tHtml.isEmpty())
		{
			try {
				JSONObject root = JSONObject.parseObject(tHtml);
				String title = root.getString("name");
				context.put(StreamContext.VideoInfo.TITLE, title);
				String vUrl = String.format("http://pv.funshion.com/v5/video/play/?id=%1$s&cl=aphone&uc=5", vid);
				String vHtml = common.getHtml(vUrl);
				ArrayList<String> urls = selectUrlsFromVideoAPI(vHtml);
				context.put(StreamContext.VideoInfo.URLS, urls);
				Long size = 0l;
				for (int i = 0; i < urls.size(); i++)
				{
					size += common.urlInfo(urls.get(i), srVo);
				}
				context.put(StreamContext.VideoInfo.SIZE, size);
				String ext = common.getFileExtName(urls.get(0), srVo);
				context.put(StreamContext.VideoInfo.EXT, ext);
				
				getStreamJsonInfo(SITE_INFO,srVo, context);//封装视频信息json串
				
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
			} catch (Exception e) {
				logger.info(e.getMessage());
				context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
				return;
			}
		}
		else {
			logger.info("{} the key srcHtml is empty",srVo.getUrl());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
			return;
		}
	}
	
	public ArrayList<String> selectUrlsFromVideoAPI(String html)
	{
		JSONObject root = JSONObject.parseObject(html);
		JSONArray mp4Info = root.getJSONArray("mp4");
		HashMap<String, String> mp4Map = new HashMap<String, String>();
		for (int i = 0; i < mp4Info.size(); i++)
		{
			String code = mp4Info.getJSONObject(i).getString("code");
			String http = mp4Info.getJSONObject(i).getString("http");
			mp4Map.put(code, http);
		}
		
		String[] qualityArray = {"sdvd", "hd","dvd","sd"};
		String vUrl = "";
		for (int i = 0; i < qualityArray.length; i++)
		{
			String quality = qualityArray[i];
			if (mp4Map.get(quality) != null)
			{
				vUrl = mp4Map.get(quality);
				break;
			}
		}
		
		String vHtml = common.getHtml(vUrl);
		root = JSONObject.parseObject(vHtml);
		JSONArray playlist = root.getJSONArray("playlist");
		ArrayList<String> result = new ArrayList<String>();
		for (int i = 0; i < playlist.size(); i++)
		{
			JSONArray urlsJ = playlist.getJSONObject(i).getJSONArray("urls");
			if (urlsJ.size() > 0)
			{
				result.add(urlsJ.getString(0));
			}
		}
		return result;
	}
}
