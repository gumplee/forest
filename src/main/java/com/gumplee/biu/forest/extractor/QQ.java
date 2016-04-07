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

import com.alibaba.fastjson.JSONObject;
import com.gumplee.biu.forest.common.JsonOut;
import com.gumplee.biu.forest.common.StreamCommon;
import com.gumplee.biu.forest.common.StreamContext;
import com.gumplee.biu.forest.vo.StreamJSONResponseVO;
import com.gumplee.biu.forest.vo.StreamReqeustVO;


@Service("qq")
public class QQ extends BaseExtractor
{
	private static Logger logger = LoggerFactory.getLogger(QQ.class);
	public static final String SITE_INFO = "QQ.com";
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
		String content = common.getHtml(url);
		String vid = common.match2(url, "\\?vid=(.*)$");
		Document doc = Jsoup.parse(content);
		Elements eles = doc.select("a");
		String title = "";
		for(Element e : eles)
		{
			if (e.hasAttr("sv"))
			{
				String sv = e.attr("sv");
				if (sv.equals(vid))
				{
					title = e.attr("title");
					break;
				}
			}
		}
		context.put(StreamContext.VideoInfo.TITLE, title);
		getVedioById(vid, title, srVo,context);
	}
	
	
	@SuppressWarnings("unchecked")
	public boolean getVedioById(String vid,String title,StreamReqeustVO srVo,StreamContext context)
	{
		if (vid.isEmpty()) {
			logger.info("{} not supported",srVo.getUrl());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, -1);
			return false;
		}
		try {
			String api = "http://vv.video.qq.com/geturl?otype=json&vid=" + vid;
			String content = common.getHtml(api,srVo,true);
			String JsonStr = common.match2(content, "QZOutputJson=(.*)");
			JsonStr = JsonStr.substring(0, JsonStr.length()-1);
			JSONObject root = JSONObject.parseObject(JsonStr);
			String url = root.getJSONObject("vd").getJSONArray("vi").getJSONObject(0).getString("url");
			ArrayList<String> urls = new ArrayList<String>();
			urls.add(url);
			context.put(StreamContext.VideoInfo.URLS, urls);
			Long size = common.urlInfo(url, srVo);
			context.put(StreamContext.VideoInfo.SIZE, size);
			String ext = common.getFileExtName(url, srVo);
			context.put(StreamContext.VideoInfo.EXT,ext);
			context.put(StreamContext.VideoInfo.TITLE, title);
			
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
			return false;
		}
		return true;
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
