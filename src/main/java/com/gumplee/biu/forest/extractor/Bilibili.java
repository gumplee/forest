package com.gumplee.biu.forest.extractor;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Resource;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.gumplee.biu.forest.common.JsonOut;
import com.gumplee.biu.forest.common.StreamCommon;
import com.gumplee.biu.forest.common.StreamContext;
import com.gumplee.biu.forest.vo.StreamJSONResponseVO;
import com.gumplee.biu.forest.vo.StreamReqeustVO;

@Service("bilibili")
public class Bilibili extends BaseExtractor
{
	private static Logger logger = LoggerFactory.getLogger(Bilibili.class);
	public static final String SITE_INFO = "Bilibili.com";
	public static final String appkey = "8e9fc618fbd41e28";
	@SuppressWarnings("serial")
	private static HashMap<String, String> fakeHeader = new HashMap<String, String>(){
		{
			put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			put("Accept-Charset", "UTF-8,*;q=0.5");
			put("Accept-Encoding", "gzip,deflate,sdch");
			put("Accept-Language", "en-US,en;q=0.8");
			put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.94 Safari/537.36");
		}
	};
	@Resource(name="streamCommon")
	StreamCommon common;
	@Resource(name="sina")
	BaseExtractor sina;
	@Resource(name="tudou")
	BaseExtractor tudou;
	@Resource(name="youku")
	BaseExtractor youku;

	
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
		String srcHtml = common.getHtml(url,srVo,true);
		
		String title = common.match2(srcHtml, "<meta name=\"title\" content=\"([^<>]{1,999})\" />");
		if (title.isEmpty())
		{
			title = common.match2(srcHtml, "<h1[^>]*>([^<>]+)</h1>");
		}
		
		title = StringEscapeUtils.unescapeHtml(title);
		context.put(StreamContext.VideoInfo.TITLE, title);
		
		String flashvars = common.match2(srcHtml, "(cid=\\d+)");
		if (flashvars.isEmpty())
		{
			flashvars = common.match2(srcHtml, "(cid: \\d+)");
			if (flashvars.isEmpty())
			{
				flashvars = common.match2(srcHtml, "flashvars=\"([^\"]+)\"");
				if (flashvars.isEmpty())
				{
					flashvars = common.match2(srcHtml, "\"https://[a-z]+\\.bilibili\\.com/secure,(cid=\\d+)(?:&aid=\\d+)?\"");
				}
			}
		}
		
		if (flashvars.isEmpty())
		{
			logger.info("{} not supported",srVo.getUrl());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, -1);
			return;
		}
		
		String t = "";
		String cid = "";
		try {
			flashvars = flashvars.replaceAll(": ", "=");
			String[] flashVarss = flashvars.split("=", 2);
			t = flashVarss[0];
			cid = flashVarss[1].split("&")[0];
		} catch (Exception e) {
			logger.info(e.getMessage());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
			return;
		}
		
		if (t.equals("cid"))
		{
			if (title.isEmpty())
			{
				title = common.match2(srcHtml, "<option value=.* selected>(.+)</option>");
			}
			getVedioById(cid, srVo,context);
		}
		else if (t.equals("vid")) {
			Sina sina2 = (Sina) sina;
			sina2.downloadVideoByVid(t,title, srVo,context);
		}
		else if (t.equals("ykid")) {
			/*Youku youku2 = (Youku) youku;
			youku2.downloadByVid(title, t, srVo, context);*/
		}
		else if (t.equals("uid")) {
			Tudou tudou2 = (Tudou) tudou;
			tudou2.getVideoById(t, srVo,context);
		}
		else {
			logger.info("{} not supported",srVo.getUrl());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, -1);
			return;
		}
	}
	
	
	@SuppressWarnings("unchecked")
	public void getVedioById(String id,StreamReqeustVO srVo,StreamContext context)
	{
		String url = "http://interface.bilibili.com/playurl?appkey=" + appkey + "&cid=" + id;
		HashMap<String, String> header = srVo.getHeaders();
		header.putAll(fakeHeader);
		String srcHtml = common.getHtml(url, srVo, false);
		if (srcHtml.isEmpty()) {
			logger.info("{} get video url failure",srVo.getUrl());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
		}
		
		List<String> uu = parseXml(srcHtml,context);
		
		ArrayList<String> urls = new ArrayList<String>();
		for (int i = 0; i < uu.size(); i++)
		{
			String tempUrl = uu.get(i);
			if (!common.r1(".*\\.qqvideo\\.tc\\.qq\\.com",tempUrl))
			{
				urls.add(tempUrl);
			}
			else {
				tempUrl = tempUrl.replaceAll(".*\\.qqvideo\\.tc\\.qq\\.com", "http://vsrc.store.qq.com");
				urls.add(tempUrl);
			}
		}
		
		context.put(StreamContext.VideoInfo.URLS, urls);
		Long size = 0l;
		for (int i = 0; i < urls.size(); i++)
		{
			size += common.urlInfo(urls.get(i), srVo);
		}
		context.put(StreamContext.VideoInfo.SIZE, size);
		String ext = common.getFileExtName(urls.get(0), srVo);
		context.put(StreamContext.VideoInfo.EXT, ext);
		String title = (String)context.get(StreamContext.VideoInfo.TITLE);
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
	public List<String> parseXml(String xml,StreamContext context)
	{
		List<String> uu = new ArrayList<String>();
		Document document = null;
		try
		{
			document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
					.parse(new InputSource(new ByteArrayInputStream(xml.getBytes("utf-8"))));
		}
		catch (Exception e)
		{
			logger.info(e.getMessage());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
			return uu;
		}
		NodeList ele = document.getElementsByTagName("durl");
		for (int i = 0; i < ele.getLength(); i++)
		{
			String u = ((Element)ele.item(i)).getElementsByTagName("url").item(0).getFirstChild().getTextContent();
			if (u != null && !u.isEmpty())
			{
				uu.add(u);
			}
		}
		return uu;
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
