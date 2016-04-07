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


@Service("sina")
public class Sina extends BaseExtractor
{
	private static Logger logger = LoggerFactory.getLogger(Sina.class);
	public static final String SITE_INFO = "Sina.com";
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
		String vid = common.match2(url, "vid=(\\d+)");
		String content = "";
		if (vid.isEmpty())
		{
			content = common.getHtml(url);
			String hd_vid = common.match2(content, "hd_vid\\s*:\\s*\'([^\']+)\'");
			if (hd_vid.equals("0"))
			{
				vid = common.match2(content, "[^\\w]vid\\s*:\\s*\'([^\']+)\'");
			}
		}
		if (vid.isEmpty())
		{
			vid = common.match2(content, "vid:(\\d+)");
		}
		if (!vid.isEmpty())
		{
			String title = common.match2(content, "title\\s*:\\s*\'([^\']+)\'");
			context.put(StreamContext.VideoInfo.TITLE, title);
			downloadVideoByVid(vid,title, srVo,context);
		}
		else {
			String vkey = common.match2(content, "vkey\\s*:\\s*\"([^\"]+)\"");
			String title = common.match2(content, "title\\s*:\\s*\"([^\"]+)\"");
			if (vkey.isEmpty())
			{
				logger.info("{} not supported",srVo.getUrl());
				context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, -1);
				return;
			}
			context.put(StreamContext.VideoInfo.TITLE, title);
			downloadVideoByVkey(vkey,title, srVo,context);
		}
	}
	

	public void downloadVideoByVid(String vid,String title,StreamReqeustVO srVo,StreamContext context)
	{
		String xmlContent = getVideoInfoXml(vid, srVo);
		if (!xmlContent.isEmpty())
		{
			downloadVideoByXml(xmlContent, title,srVo,context);
		}
	}
	

	@SuppressWarnings("unchecked")
	public void downloadVideoByXml(String xmlContent,String title,StreamReqeustVO srVo,StreamContext context)
	{
		if (title.isEmpty())
		{
			title = common.match2(xmlContent, "<vname>(?:<!\\[CDATA\\[)?(.+?)(?:\\]\\]>)?</vname>");
		}
		ArrayList<String> urls = (ArrayList<String>)common.matchAll(xmlContent, "<url>(?:<!\\[CDATA\\[)?(.*?)(?:\\]\\]>)?</url>");
		Long size = 0l;
		if(urls.size() > 0)
		{
			for (int i = 0; i < urls.size(); i++)
			{
				size = size + common.urlInfo(urls.get(i), srVo);
			}
			String ext = "flv";
			context.put(StreamContext.VideoInfo.URLS, urls);
			context.put(StreamContext.VideoInfo.SIZE, size);
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
			logger.info("{} the video urls is empty ",srVo.getUrl());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
		}
	}
	

	public String getVideoInfoXml(String vid,StreamReqeustVO srVo)
	{
		String num1 = String.valueOf((int)(9990000 * Math.random() + 10000));
		String num2 = String.valueOf((int)(9990000 * Math.random() + 10000));
		String rand = "0." + num1 + num2;
		
		String realUrl = "http://v.iask.com/v_play.php?vid="+ vid +"&ran="+ rand +"&p=i&k=" + getK(vid, rand);
		
		
		return common.getHtml(realUrl);
	}
	

	public String getK(String vid,String rand)
	{
		int time = (int)(System.currentTimeMillis() / 1000);
		String binary = Integer.toBinaryString(time);
		binary = binary.substring(binary.length() - 6);
		String str = vid + "Z6prk18aWxP278cVAH"	+ binary + rand;
		str = common.makeMd5Key(str);
		str = str.substring(0,str.length()-16) + binary;
		return str;
	}
	
	
	@SuppressWarnings("unchecked")
	public void downloadVideoByVkey(String vkey,String title,StreamReqeustVO srVo,StreamContext context)
	{
		String url = "http://video.sina.com/v/flvideo/"+ vkey +"_0.flv";
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
