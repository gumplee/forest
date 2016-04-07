package com.gumplee.biu.forest.extractor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.gumplee.biu.forest.common.JsonOut;
import com.gumplee.biu.forest.common.StreamCommon;
import com.gumplee.biu.forest.common.StreamContext;
import com.gumplee.biu.forest.vo.StreamJSONResponseVO;
import com.gumplee.biu.forest.vo.StreamReqeustVO;

@Service("yixia")
public class Yixia extends BaseExtractor
{
	private static Logger logger = LoggerFactory.getLogger(Yixia.class);
	public static final String SITE_INFO = "Xiayi.com";
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
		URL u = null;
		try
		{
			u = new URL(url);
		}
		catch (MalformedURLException e)
		{
			logger.info(e.getMessage());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
			return;
		}
		String hostname = u.getHost();
		String scid = "";
		if (hostname.contains("miaopai.com"))
		{
			
			if (common.r1("http://www.miaopai.com/show/channel/\\w+", url))
			{
				scid = common.match2(url, "http://www.miaopai.com/show/channel/(\\w+)");
			}
			else if (common.r1("http://www.miaopai.com/show/\\w+", url)) {
				scid = common.match2(url, "http://www.miaopai.com/show/(\\w+)");
			}
			else if (common.r1("http://m.miaopai.com/show/channel/\\w+", url)) {
				scid = common.match2(url, "http://m.miaopai.com/show/channel/(\\w+)");
			}
			getVedioByIdForMiaoPai(scid, srVo,context);
		}
		else if (hostname.contains("xiaokaxiu.com")) {
			if (common.r1("http://v.xiaokaxiu.com/v/.+\\.html", url))
			{
				scid = common.match2(url, "http://v.xiaokaxiu.com/v/(.+)\\.html");
			}
			if (common.r1("http://m.xiaokaxiu.com/m/.+\\.html", url))
			{
				scid = common.match2(url, "http://m.xiaokaxiu.com/m/(.+)\\.html");
			}
			getVedioByIdForXiaokaxiu(scid, srVo,context);
		}
		else {
			logger.info("{} not supported",srVo.getUrl());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, -1);
			return;
		}
	}
	
	@SuppressWarnings("unchecked")
	public void getVedioByIdForXiaokaxiu(String scid,StreamReqeustVO srVo,StreamContext context)
	{
		if (scid.isEmpty()) {
			logger.info("{} the key id is empty",srVo.getUrl());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
			return;
		}
		String url = String.format("http://api.xiaokaxiu.com/video/web/get_play_video?scid=%1$s", scid);
		String html = common.getHtml(url, srVo, true);
		if (html.isEmpty())
		{
			logger.info("{} the key srcHtml is empty",srVo.getUrl());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
			return;
		}
		try {
			JSONObject root = JSONObject.parseObject(html);
			String videoUrl = root.getJSONObject("data").getString("linkurl");
			String title = root.getJSONObject("data").getString("title");
			Long size = common.urlInfo(videoUrl, srVo);
			String ext = common.getFileExtName(videoUrl, srVo);
			ArrayList<String> urls = new ArrayList<String>();
			urls.add(videoUrl);
			
			context.put(StreamContext.VideoInfo.TITLE, title);
			context.put(StreamContext.VideoInfo.URLS, urls);
			context.put(StreamContext.VideoInfo.SIZE, size);
			context.put(StreamContext.VideoInfo.EXT, ext);
			
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
		} catch (Exception e) {
			logger.info(e.getMessage());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
			return;
		}
	}
	
	@SuppressWarnings("unchecked")
	public void getVedioByIdForMiaoPai(String scid,StreamReqeustVO srVo,StreamContext context)
	{
		if (scid.isEmpty()) {
			logger.info("{} the key id is empty",srVo.getUrl());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
			return;
		}
		String url = String.format("http://api.miaopai.com/m/v2_channel.json?fillType=259&scid=%1$s&vend=miaopai", scid);
		String html = common.getHtml(url, srVo, true);
		if (html.isEmpty())
		{
			logger.info("{} the key srcHtml is empty",srVo.getUrl());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
			return;
		}
		try {
			JSONObject root = JSONObject.parseObject(html);
			String base = root.getJSONObject("result").getJSONObject("stream").getString("base");
			String videoUrl = common.match2(base, "(.+)\\?vend");
			String title = root.getJSONObject("result").getJSONObject("ext").getString("t");
			Long size = common.urlInfo(videoUrl, srVo);
			String ext = common.getFileExtName(videoUrl, srVo);
			ArrayList<String> urls = new ArrayList<String>();
			urls.add(videoUrl);
			
			context.put(StreamContext.VideoInfo.TITLE, title);
			context.put(StreamContext.VideoInfo.URLS, urls);
			context.put(StreamContext.VideoInfo.SIZE, size);
			context.put(StreamContext.VideoInfo.EXT, ext);
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
		} catch (Exception e) {
			logger.info(e.getMessage());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
			return;
		}
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

}
