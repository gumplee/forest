package com.gumplee.biu.forest.extractor;

import java.util.ArrayList;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gumplee.biu.forest.common.StreamCommon;
import com.gumplee.biu.forest.common.StreamContext;
import com.gumplee.biu.forest.vo.StreamReqeustVO;

@Service("cntv")
public class CNTV extends BaseExtractor
{
	private static Logger logger = LoggerFactory.getLogger(CNTV.class);
	public static final String SITE_INFO = "CNTV.com";
	
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
		String id = "";
		String url = srVo.getUrl();
		if (url.matches("http://tv\\.c[n|c]tv\\.cn/video/(\\w+)/(\\w+)"))
		{
			String pattern = "http://tv\\.c[n|c]tv\\.cn/video/\\w+/(\\w+)";
			id = common.match2(url,pattern);
		}
		else if (url.matches("http://\\w+\\.c[n|c]tv\\.cn/(\\w+/\\w+/(classpage/video/)?)?\\d+/\\d+\\.shtml")
				|| url.matches("http://\\w+.c[n|c]tv.cn/(\\w+/)*VIDE\\d+.shtml")) {
			id = common.match2(common.getHtml(url),"videoCenterId\",\"(\\w+)\"");
		}
		else if (url.matches("http://xiyou.c[n|c]tv.cn/v-[\\w-]+\\.html")) {
			id = common.match2(url,"http://xiyou.c[n|c]tv.cn/v-([\\w-]+)\\.html");
		}
		
		if (id.isEmpty())
		{
			id = common.match2(common.getHtml(url), "videoCenterId\",\"(\\w+)\"");
			if (id.isEmpty())
			{
				id = common.match2(common.getHtml(url), "guid\\s=\\s\"(\\w+)\"");
			}
		}
		if (id.isEmpty())
		{
			logger.info("{} not supported",srVo.getUrl());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, -1);
			return;
		}
		
		downloadVideoById(id, srVo,context);
	}
	

	@SuppressWarnings("unchecked")
	public void downloadVideoById(String id,StreamReqeustVO srVo,StreamContext context)
	{
		try {
			String srcHtml = common.getHtml("http://vdn.apps.cntv.cn/api/getHttpVideoInfo.do?pid=" + id);
			JSONObject root = JSONObject.parseObject(srcHtml);
			String title = root.getString("title");
			context.put(StreamContext.VideoInfo.TITLE, title);
			JSONObject videoEle = root.getJSONObject("video");
			JSONArray chapters = videoEle.getJSONArray("chapters");
			if (chapters == null || chapters.size() == 0)
			{
				chapters = videoEle.getJSONArray("lowChapters");
			}
			ArrayList<String> urls = new ArrayList<String>();
			
			for (int i = 0; i < chapters.size(); i++)
			{
				JSONObject chapter = chapters.getJSONObject(i);
				String url = chapter.getString("url");
				if (url != null && !url.isEmpty())
				{
					urls.add(url);
				}
			}
			if (urls.size() > 0)
			{
				context.put(StreamContext.VideoInfo.URLS, urls);
				Long size = 0l;
				String ext = common.match2(urls.get(0),"\\.([^.]+)$");
				context.put(StreamContext.VideoInfo.EXT,ext);
				for (int i = 0; i < urls.size(); i++)
				{
					size += common.urlInfo(urls.get(i),srVo);
				}
				context.put(StreamContext.VideoInfo.SIZE, size);
				
				getStreamJsonInfo(SITE_INFO,srVo, context);
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
}
