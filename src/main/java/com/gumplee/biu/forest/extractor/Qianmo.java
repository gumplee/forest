package com.gumplee.biu.forest.extractor;

import java.util.ArrayList;
import java.util.HashMap;

import javax.annotation.Resource;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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

/**
 * 
 * @author: liyuanjun
 * @date: 2016年1月11日
 */
@Service("qianmo")
public class Qianmo extends BaseExtractor
{
	private static Logger logger = LoggerFactory.getLogger(Qianmo.class);
	public static final String SITE_INFO = "Qianmo.com";
	@Resource(name="streamCommon")
	StreamCommon common;
	
	
	@Override
	public void process(StreamContext context)
	{
		StreamReqeustVO srVo = (StreamReqeustVO)context.get(StreamContext.VideoInfo.STREAM_REQUEST_VO);
		srVo.setFaker(true);
		execute(srVo,context);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void execute(StreamReqeustVO srVo,StreamContext context)
	{
		String url = srVo.getUrl();
		if (common.r1("http://qianmo.com/\\w+", url))
		{
			String html = common.getHtml(url,srVo,true);
			Document doc = Jsoup.parse(html);
			String postInfo = "";
			Elements eles = doc.select("script");
			for(Element e : eles)
			{
				String text = e.html();
				if (text.contains("postInfo"))
				{
					postInfo = text;
					break;
				}
			}
			postInfo = postInfo.substring(postInfo.indexOf("=") + 1,postInfo.length() - 1);
			if (!postInfo.isEmpty())
			{
				try {
					JSONObject root = JSONObject.parseObject(postInfo);
					String title = root.getString("title");
					context.put(StreamContext.VideoInfo.TITLE, title);
					String id = root.getString("video_id");
					
					String vUrl = "http://v.qianmo.com/player/" + id;
					String vHtml = common.getHtml(vUrl);
					root = JSONObject.parseObject(vHtml);
					
					JSONObject seg = root.getJSONObject("seg");
					String[] profiles = {"sd" , "hd" , "ssd"};
					ArrayList<String> urls = new ArrayList<String>();
					Long size = 0l;
					for (int i = 0; i < profiles.length; i++)
					{
						JSONArray pp = seg.getJSONArray(profiles[i]);
						if (pp.size() > 0)
						{
							String u = pp.getJSONObject(0).getJSONArray("url").getJSONArray(0).getString(0);
							urls.add(u);
						}
					}
					
					for (int i = 0; i < urls.size(); i++)
					{
						size += common.urlInfo(urls.get(i), srVo);
					}
					String ext = common.getFileExtName(urls.get(0), srVo);
					context.put(StreamContext.VideoInfo.URLS, urls);
					context.put(StreamContext.VideoInfo.SIZE, size);
					context.put(StreamContext.VideoInfo.EXT,ext);
					
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
			else {
				logger.info("{} the key srcHtml is empty",srVo.getUrl());
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
	@Override
	public boolean getStreamJsonInfo(StreamReqeustVO srVo,StreamContext context)
	{
		JsonOut jo = new JsonOut();
		HashMap<String, StreamJSONResponseVO> result = jo.print_info_json(SITE_INFO,context);
		context.put(StreamContext.VideoInfo.VIDEO_JSON_INFO, result);
		return true;
	}


}
