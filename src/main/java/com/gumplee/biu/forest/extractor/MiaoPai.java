package com.gumplee.biu.forest.extractor;

import java.util.ArrayList;
import java.util.HashMap;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.gumplee.biu.forest.common.StreamCommon;
import com.gumplee.biu.forest.common.StreamContext;
import com.gumplee.biu.forest.vo.StreamReqeustVO;

@Service("miaopai")
public class MiaoPai extends BaseExtractor
{
	private static Logger logger = LoggerFactory.getLogger(MiaoPai.class);
	public static final String SITE_INFO = "Miaopai.com";
	@SuppressWarnings("serial")
	private static HashMap<String, String> fakeHeader = new HashMap<String, String>(){
		{
			put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			put("Accept-Charset", "UTF-8,*;q=0.5");
			put("Accept-Encoding", "gzip,deflate,sdch");
			put("Accept-Language", "en-US,en;q=0.8");
			put("User-Agent", "Mozilla/5.0 (Linux; Android 4.4.2; Nexus 4 Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.114 Mobile Safari/537.36");
		}
	};
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
		if (common.r1("http://video.weibo.com/show\\?fid=(\\d{4}:\\w{32})\\w*", url))
		{
			try {
				String webPageUrl = common.match2(url, "(http://video.weibo.com/show\\?fid=\\d{4}:\\w{32})\\w*");
				webPageUrl = webPageUrl + "&type=mp4";
				HashMap<String, String> header = srVo.getHeaders();
				header.putAll(fakeHeader);
				String srcHtml = common.getHtml(webPageUrl, srVo, true);
				url = common.match2(srcHtml, "<video src=\"(.*?)\"\\W");
				String srcHtmlB = common.getHtml(webPageUrl);
				String title = common.match2(srcHtmlB, "<meta name=\"description\" content=\"(.*?)\"\\W");
				title = common.processTitle(title);
				context.put(StreamContext.VideoInfo.TITLE, title);
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
}
