package com.gumplee.biu.forest.extractor;

import java.util.ArrayList;
import java.util.HashMap;

import javax.annotation.Resource;

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

@Service("ted")
public class Ted extends BaseExtractor
{
	private static Logger logger = LoggerFactory.getLogger(Ted.class);
	public static final String SITE_INFO = "Ted.com";
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
		String html = common.getHtml(url, srVo, true);
		String jsonStr = common.match2(html, "(\\{\"talks\"(.*)\\})\\)</script>");
		
		JSONObject root = JSONObject.parseObject(jsonStr);
		JSONArray talks = root.getJSONArray("talks");
		if (talks.size() > 0)
		{
			try {
				String title = talks.getJSONObject(0).getString("title");
				JSONObject nativeDownloads = talks.getJSONObject(0).getJSONObject("nativeDownloads");
				ArrayList<String> urls = new ArrayList<String>();
				if (nativeDownloads.getString("low") != null)
				{
					String u = nativeDownloads.getString("low");
					Long size = common.urlInfo(u, srVo);
					String ext = common.getFileExtName(u, srVo);
					urls.add(u);
					
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
	@Override
	public boolean getStreamJsonInfo(StreamReqeustVO srVo,StreamContext context)
	{
		JsonOut jo = new JsonOut();
		HashMap<String, StreamJSONResponseVO> result = jo.print_info_json(SITE_INFO,context);
		context.put(StreamContext.VideoInfo.VIDEO_JSON_INFO, result);
		return true;
	}

}
