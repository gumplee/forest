package com.gumplee.biu.forest.extractor;

import java.util.ArrayList;
import java.util.HashMap;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.gumplee.biu.forest.common.JsonOut;
import com.gumplee.biu.forest.common.StreamCommon;
import com.gumplee.biu.forest.common.StreamContext;
import com.gumplee.biu.forest.vo.StreamJSONResponseVO;
import com.gumplee.biu.forest.vo.StreamReqeustVO;


@Service("iqilu")
public class Iqilu extends BaseExtractor
{
	private static Logger logger = LoggerFactory.getLogger(Iqilu.class);
	public static final String SITE_INFO = "Iqilu.com";
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
		if (common.r1("http://v.iqilu.com/\\w+", url))
		{
			String html = common.getHtml(url);
			String vu = common.match2(html, "<input type='hidden' id='playerId' url='([^']+)'");
			String title = common.match2(html, "<meta name=\"description\" content=\"(.*?)\"\\W");
			context.put(StreamContext.VideoInfo.TITLE, title);
			if (!vu.isEmpty())
			{
				ArrayList<String> urls = new ArrayList<String>();
				urls.add(vu);
				context.put(StreamContext.VideoInfo.URLS, urls);
				Long size = common.urlInfo(vu, srVo);
				context.put(StreamContext.VideoInfo.SIZE, size);
				String ext = common.getFileExtName(vu, srVo);
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
			}
			else {
				logger.info("{} the video urls is empty",srVo.getUrl());
				context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
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
