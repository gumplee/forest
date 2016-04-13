package com.gumplee.biu.forest.extractor;

import java.util.ArrayList;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.gumplee.biu.forest.common.StreamCommon;
import com.gumplee.biu.forest.common.StreamContext;
import com.gumplee.biu.forest.vo.StreamReqeustVO;

@Service("baomihua")
public class Baomihua extends BaseExtractor
{
	private static Logger logger = LoggerFactory.getLogger(Baomihua.class);
	public static final String SITE_INFO = "Baomihu.com";
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
		String html = common.getHtml(srVo.getUrl());
		String title = common.match2(html, "<title>(.*)</title>");
		context.put(StreamContext.VideoInfo.TITLE, title);
		if (title.isEmpty())
		{
			logger.info("the title is empty ");
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
			return;
		}
		String id = common.match2(html, "flvid\\s*=\\s*(\\d+)");
		if (id.isEmpty())
		{
			logger.info("the key id is empty");
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
			return;
		}
		
		getVedioById(id, title, srVo,context);
	}
	
	
	@SuppressWarnings("unchecked")
	public void getVedioById(String id,String title,StreamReqeustVO srVo,StreamContext context)
	{
		String html = common.getHtml("http://play.baomihua.com/getvideourl.aspx?flvid=" + id);
		String host = common.match2(html, "host=([^&]*)");
		String type = common.match2(html, "videofiletype=([^&]*)");
		String vid = common.match2(html,"&stream_name=([^&]*)");
		if (host.isEmpty() || type.isEmpty() || vid.isEmpty()) {
			logger.info("the key id is empty");
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
			return;
		}
		
		String url = String.format("http://%1$s/pomoho_video/%2$s.%3$s", host,vid,type);
		
		ArrayList<String> urls = new ArrayList<String>();
		urls.add(url);
		context.put(StreamContext.VideoInfo.URLS, urls);
		Long size = common.urlInfo(url, srVo);
		context.put(StreamContext.VideoInfo.SIZE, size);
		String ext = common.getFileExtName(url, srVo);
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
	}

}
