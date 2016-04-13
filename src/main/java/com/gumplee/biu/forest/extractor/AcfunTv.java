package com.gumplee.biu.forest.extractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gumplee.biu.forest.common.StreamCommon;
import com.gumplee.biu.forest.common.StreamContext;
import com.gumplee.biu.forest.vo.StreamReqeustVO;

@Service("acfuntv")
public class AcfunTv extends BaseExtractor
{
	private static Logger logger = LoggerFactory.getLogger(AcfunTv.class);
	public static final String SITE_INFO = "Acfun.tv";
	@Resource(name="streamCommon")
	StreamCommon common;
	@Resource(name="sina")
	BaseExtractor sina;
	@Resource(name="tudou")
	BaseExtractor tudou;
	@Resource(name="qq")
	BaseExtractor qq;
	@Resource(name="le")
	BaseExtractor le;
	
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
		if (common.r1("http://[^\\.]+.acfun.[^\\.]+/\\D/\\D\\D(\\d+)", url))
		{
			String html = common.getHtml(url,srVo,true);
			
			String title = common.match2(html, "<h1 id=\"txt-title-view\">([^<>]+)<");
			title = StringEscapeUtils.unescapeHtml(title);
			title = common.processTitle(title);
			
			List<String> videos = common.matchAll(html, "data-vid=\"(\\d+)\"[^>]+title=\"([^\"]+)\"");
			if (videos.size() > 0)
			{
				for (int i = 0; i < videos.size(); i = i + 2)
				{
					String vid = videos.get(i);
					String pTitle = videos.get(i + 1);
					if (!pTitle.equals("删除标签"))
					{
						pTitle = title + " - " + pTitle;
					}
					context.put(StreamContext.VideoInfo.TITLE, pTitle);
					getVedioById(vid,pTitle, srVo,context);
				}
			}
			else {
				String id = common.match2(html, "src=\"/newflvplayer/player.*id=(\\d+)");
				if (id.isEmpty()) {
					logger.info("the key id is empty!");
					context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
					return;
				}
				Sina sina = new Sina();
				context.put(StreamContext.VideoInfo.TITLE, title);
				sina.downloadVideoByVid(id,title, srVo,context);
			}
			
		}
		else {
			logger.info("not supported {} ",srVo.getUrl());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, -1);
			return;
		}
	}
	
	
	@SuppressWarnings("unchecked")
	public void getVedioById(String id ,String title,StreamReqeustVO srVo,StreamContext context)
	{
		try {
			String videoInfoUrl = "http://www.acfun.tv/video/getVideo.aspx?id=" + id;
			String videoInfoHtml = common.getHtml(videoInfoUrl);
			JSONObject videoInfoRoot = JSONObject.parseObject(videoInfoHtml);
			String sourceType = videoInfoRoot.getString("sourceType");
			String sourceId = videoInfoRoot.getString("sourceId");
			if (sourceId != null && !sourceId.isEmpty())
			{
				if (sourceType.equals("sina"))
				{
					Sina sina2 = (Sina) sina;
					sina2.downloadVideoByVid(sourceId, title,srVo,context);
				}
				else if (sourceType.equals("youku")) {
					/*Youku youku2 = (Youku) youku;
					youku2.downloadByVid(title, sourceId, srVo, context);*/
					
				}
				else if (sourceType.equals("tudou")) {
					Tudou tudou2 = (Tudou) tudou;
					tudou2.getVideoByIid(sourceId,title, srVo,context);
				}
				else if (sourceType.equals("qq")) {
					QQ qq2 = (QQ) qq;
					qq2.getVedioById(sourceId, title, srVo, context);
				}
				else if (sourceType.equals("letv")) {
					/*LETV letv2 = (LETV) letv;
					letv2.downloadLetvCloudByVu(sourceId, "2d8c027396", title, srVo,context);*/
				}
				else if (sourceType.equals("zhuzhan")) {

					String a = "http://api.aixifan.com/plays/" + id + "/realSource";
					HashMap<String, String> headers = srVo.getHeaders();
					headers.put("deviceType", "1");
					String s = common.getHtml(a, srVo,true);
					JSONObject root = JSONObject.parseObject(s);
					JSONObject data = root.getJSONObject("data");
					JSONArray videoList = data.getJSONArray("files");
					JSONArray urlArray = videoList.getJSONObject(videoList.size() - 1).getJSONArray("url");
					ArrayList<String> urls = new ArrayList<String>();
					for (int i = 0; i < urlArray.size(); i++)
					{
						urls.add(urlArray.getString(i));
					}
					

					context.put(StreamContext.VideoInfo.URLS, urls);
					Long size = common.urlInfo(urls.get(0), srVo);
					context.put(StreamContext.VideoInfo.SIZE, size);
					String ext = "mp4";
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
				else {
					logger.info("not supported {} ",srVo.getUrl());
					context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, -1);
					return;
				}
			}
			else {
				logger.info("not supported {} ",srVo.getUrl());
				context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, -1);
				return;
			}
		} catch (Exception e) {
			logger.info(e.getMessage());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
			return;
		}
	}
}
