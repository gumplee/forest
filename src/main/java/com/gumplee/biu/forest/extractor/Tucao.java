package com.gumplee.biu.forest.extractor;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;

import javax.annotation.Resource;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.gumplee.biu.forest.common.StreamCommon;
import com.gumplee.biu.forest.common.StreamContext;
import com.gumplee.biu.forest.vo.StreamReqeustVO;

@Service("tucao")
public class Tucao extends BaseExtractor
{
	private static Logger logger = LoggerFactory.getLogger(Tucao.class);
	public static final String SITE_INFO = "Tucao.cc";
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
			String html = common.getHtml(url);
			String title = common.match2(html, "<h1 class=\"show_title\">(.*?)<\\w");
			context.put(StreamContext.VideoInfo.TITLE, title);
			String rawList = common.match2(html, "<li>(type=.+?)</li>");
			String[] raws = rawList.split("\\*\\*");
			if (raws.length == 1)
			{
				String formatLink = raws[0];
				if (formatLink.endsWith("|"))
				{
					formatLink = formatLink.substring(0,formatLink.length() - 1);
				}
				getVideoById(formatLink, title, srVo,context);
			}
			else if (raws.length > 1) 
			{
				String iid = common.match2(url, "http://www.tucao.tv/\\w+/\\w+/#(\\d+)");
				Integer curId = 0;
				for (int i = 0; i < raws.length;)
				{
					if (!iid.isEmpty())
					{
						curId = Integer.valueOf(iid);
					}
					String[] rr = raws[curId - 1].split("\\|");
					String formatLink = rr[0];
					String subTitle = rr[1];
					getVideoById(formatLink, title + "-" + subTitle, srVo,context);
					break;
				}
			}
			else {
				logger.info("{} not supported",srVo.getUrl());
				context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, -1);
				return;
			}
		} catch (Exception e) {
			logger.info(e.getMessage());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
			return;
		}
	}
	
	@SuppressWarnings("unchecked")
	public void getVideoById(String id,String title,StreamReqeustVO srVo,StreamContext context)
	{
		if (id.isEmpty()){
			logger.info("{} the key id is empty",srVo.getUrl());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
			return;
		};
		
		if (id.contains("file"))
		{
			ArrayList<String> urls = new ArrayList<String>();
			String url = id.substring(id.indexOf("file=") + 5);
			Long size = common.urlInfo(url, srVo);
			String ext = common.getFileExtName(url, srVo);
			urls.add(url);
			
			context.put(StreamContext.VideoInfo.TITLE, title);
			context.put(StreamContext.VideoInfo.URLS, urls);
			context.put(StreamContext.VideoInfo.SIZE, size);
			context.put(StreamContext.VideoInfo.EXT, ext);
			title = common.processTitle(title);
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
			try {
				String url = String.format("http://www.tucao.tv/api/playurl.php?%1$s&key=tucao%2$s.cc&r=%3$s"
						, id,Long.toHexString(Math.round(Math.random() * 268435456)),System.currentTimeMillis());
				String xmlStr = common.getHtml(url, srVo, true);
				
				Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
						.parse(new InputSource(new ByteArrayInputStream(xmlStr.getBytes("utf-8"))));
				
				NodeList uu = document.getElementsByTagName("url");
				String ext = "";
				Long size = 0l;
				ArrayList<String> urls = new ArrayList<String>();
				for (int i = 0; i < uu.getLength(); i++)
				{
					Element e = (Element) uu.item(i);
					url = e.getFirstChild().getTextContent();
					urls.add(url);
					size = common.urlInfo(url, srVo);
					ext = common.getFileExtName(url, srVo);
				}
				context.put(StreamContext.VideoInfo.TITLE, title);
				context.put(StreamContext.VideoInfo.URLS, urls);
				context.put(StreamContext.VideoInfo.SIZE, size);
				context.put(StreamContext.VideoInfo.EXT, ext);
				title = common.processTitle(title);
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
}
