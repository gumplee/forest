package com.gumplee.biu.forest.extractor;

import java.util.ArrayList;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.gumplee.biu.forest.common.StreamCommon;
import com.gumplee.biu.forest.common.StreamContext;
import com.gumplee.biu.forest.vo.StreamReqeustVO;

@Service("netease")
public class Netease extends BaseExtractor
{
	private static Logger logger = LoggerFactory.getLogger(Netease.class);
	public static final String SITE_INFO = "163.com";
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
		try {
			String url = srVo.getUrl();
			String srcHtml = common.getHtml(url, srVo,true);
			String title = common.match2(srcHtml, "movieDescription=\'([^\']+)\'");
			if (title.isEmpty())
			{
				title = common.match2(srcHtml, "<title>(.+)</title>");
			}
			if (!title.isEmpty())
			{
				title = title.trim();
			}
			context.put(StreamContext.VideoInfo.TITLE, title);
			String src = common.match2(srcHtml, "<source src=\"([^\"]+)\"");
			if (src.isEmpty())
			{
				src = common.match2(srcHtml, "<source type=\"[^\"]+\" src=\"([^\"]+)\"");
			}
			ArrayList<String> urls = new ArrayList<String>();
			Long size = 0l;
			String ext = "";
			
			
			if (src.isEmpty())
			{
				url = common.match2(srcHtml, "[\"'](.+)-list.m3u8[\"']");
				if (url == null)
				{
					url = common.match2(srcHtml, "[\"'](.+).m3u8[\"']");
				}
				size = common.urlInfo(url, srVo);
				ext = "mp4";
				urls.add(url);
			}
			else {
				url = src;
				size = common.urlInfo(url, srVo);
				ext = common.getFileExtName(url, srVo);
				urls.add(url);
			}
			
			if (urls.size() == 0) {
				logger.info("{} not supported",srVo.getUrl());
				context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, -1);
				return;
			}
			
			context.put(StreamContext.VideoInfo.URLS, urls);
			context.put(StreamContext.VideoInfo.SIZE, size);
			context.put(StreamContext.VideoInfo.EXT,ext);
			
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
}
