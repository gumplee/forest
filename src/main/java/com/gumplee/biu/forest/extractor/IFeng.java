package com.gumplee.biu.forest.extractor;

import java.util.ArrayList;
import java.util.HashMap;

import javax.annotation.Resource;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.gumplee.biu.forest.common.JsonOut;
import com.gumplee.biu.forest.common.StreamCommon;
import com.gumplee.biu.forest.common.StreamContext;
import com.gumplee.biu.forest.vo.StreamJSONResponseVO;
import com.gumplee.biu.forest.vo.StreamReqeustVO;


@Service("ifeng")
public class IFeng extends BaseExtractor
{
	private static Logger logger = LoggerFactory.getLogger(IFeng.class);
	public static final String SITE_INFO = "ifeng.com";
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
		String id = common.match2(url,
				"/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\\.shtml$");
		if (!id.isEmpty())
		{
			getVideoById(id, srVo,context);
		}
		else {
			String srcHtml = common.getHtml(url);
			id = common.match2(srcHtml, 
					"var vid=\"([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\"");
			if (id.isEmpty())
			{
				logger.info("{} not supported",srVo.getUrl());
				context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, -1);
				return;
			}
			else {
				getVideoById(id, srVo,context);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public void getVideoById(String id ,StreamReqeustVO srVo,StreamContext context)
	{
		try {
			char id1 = id.charAt(id.length() - 2);
			String id2 = id.substring(id.length() - 2);
			String url = "http://v.ifeng.com/video_info_new/" + id1 + "/" + id2 + "/" + id + ".xml";
			String xml = common.getHtml(url, "utf8");
			String title = common.match2(xml, "Name=\"([^\"]+)\"");
			title = StringEscapeUtils.unescapeHtml(title);
			context.put(StreamContext.VideoInfo.TITLE, title);
			url = common.match2(xml, "VideoPlayUrl=\"([^\"]+)\"");
			String num =String.valueOf((int)(9 *  Math.random()) + 10);
			url = url.replaceAll("http://video.ifeng.com/", "http://video " + num + ".ifeng.com/");
			ArrayList<String> urls = new ArrayList<String>();
			urls.add(url);
			context.put(StreamContext.VideoInfo.URLS, urls);
			Long size = common.urlInfo(url, srVo);
			context.put(StreamContext.VideoInfo.SIZE, size);
			String ext = common.getFileExtName(url, srVo);
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
