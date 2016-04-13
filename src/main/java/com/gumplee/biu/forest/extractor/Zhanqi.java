package com.gumplee.biu.forest.extractor;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.gumplee.biu.forest.common.StreamCommon;
import com.gumplee.biu.forest.common.StreamContext;
import com.gumplee.biu.forest.vo.StreamReqeustVO;


@Service("zhanqi")
public class Zhanqi extends BaseExtractor
{
	private static Logger logger = LoggerFactory.getLogger(Zhanqi.class);
	public static final String SITE_INFO = "Zhanqi.tv";
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
		String html = common.getHtml(url);
		
		String type = common.match2(html, "VideoType\":\"([^\"]+)\"");
		String vodM3U8IdPat = "VideoID\":\"([^\"]+)\"";
		String titlePat = "<p class=\"title-name\" title=\"[^\"]+\">([^<]+)</p>";
		String titlePat2 = "<title>([^<]{1,9999})</title>";
		
		String title = common.match2(html, titlePat);
		if (title.isEmpty())
		{
			title = common.match2(html, titlePat2);
		}
		title = StringEscapeUtils.unescapeHtml(title);
		context.put(StreamContext.VideoInfo.TITLE, title);
		
		String vodBase = "http://dlvod.cdn.zhanqi.tv";
		
		if (type.equals("LIVE"))
		{
			logger.info("{} not supported",srVo.getUrl());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, -1);
			return;
		}
		else if (type.equals("VOD")) {
			String vodm3u8Url = vodBase + common.match2(html, vodM3U8IdPat).replaceAll("\\\\", "");
			String vodm3u8Html = common.getHtml(vodm3u8Url);
			
			List<String> vus = common.matchAll(vodm3u8Html, "(/[^#]+)\\.ts");
			ArrayList<String> urls = new ArrayList<String>();
			Long size = 0l;
			String ext = "";
			for (int i = 0; i < vus.size(); i++)
			{
				String u = vodBase + vus.get(i)	+ ".ts";
				urls.add(u);
			}
			for (int i = 0; i < urls.size(); i++)
			{
				size += common.urlInfo(urls.get(i), srVo);
			}
			ext = common.getFileExtName(urls.get(0), srVo);
			
			context.put(StreamContext.VideoInfo.URLS, urls);
			context.put(StreamContext.VideoInfo.SIZE, size);
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
	}
}
