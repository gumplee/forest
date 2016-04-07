package com.gumplee.biu.forest.extractor;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gumplee.biu.forest.common.JsonOut;
import com.gumplee.biu.forest.common.StreamCommon;
import com.gumplee.biu.forest.common.StreamContext;
import com.gumplee.biu.forest.vo.StreamJSONResponseVO;
import com.gumplee.biu.forest.vo.StreamReqeustVO;

@Service("tudou")
public class Tudou extends BaseExtractor
{
	private static Logger logger = LoggerFactory.getLogger(Tudou.class);
	public static final String SITE_INFO = "Tudou.com";
	@Resource(name="streamCommon")
	StreamCommon common;
	
	@Resource(name="youku")
	BaseExtractor youku;
	
	@Override
	public void process(StreamContext context)
	{
		StreamReqeustVO srVo = (StreamReqeustVO)context.get(StreamContext.VideoInfo.STREAM_REQUEST_VO);
		srVo.setCustomUA("Python-urllib/3.4");
		execute(srVo,context);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void execute(StreamReqeustVO srVo,StreamContext context)
	{
		String url = srVo.getUrl();
		String id = common.match2(url, "http://www.tudou.com/v/([^/]+)/");
		if (!id.isEmpty())
		{
			getVideoById(id, srVo,context);
		}
		else {
			String srcHtml = common.getHtml(url, srVo,true);
			String title = common.match2(srcHtml, "(?<=,)kw\\s*[:=]\\s*['\"]([^\n]+?)'\\s*");
			if (title == null)return;
			title = StringEscapeUtils.unescapeHtml(title);
			context.put(StreamContext.VideoInfo.TITLE, title);
			String vcode = common.match2(srcHtml, "vcode\\s*[:=]\\s*'([^']+)'");
			if (!vcode.isEmpty())
			{
				/*Youku youku2 = (Youku)youku;
				youku2.downloadByVid(title, vcode, srVo, context);*/
			}
			else {
				String iid = common.match2(srcHtml, "iid\\s*[:=]\\s*(\\d+)");
				if (!iid.isEmpty())
				{
					getVideoByIid(iid,title, srVo,context);
					
				}
				else {
					getVideoForPlaylist(srVo,context);
				}
			}
		}
		
	}
	
	public void getVideoForPlaylist(StreamReqeustVO srVo,StreamContext context)
	{
		String url = srVo.getUrl();
		HashMap<String, String> map = parsePlist(url, srVo);
		Set<String> set = map.keySet();
		Iterator<String> iterator = set.iterator();
		int num = 1;
		while(iterator.hasNext())
		{
			String iid = map.get((String)context.get(StreamContext.VideoInfo.TITLE));
			logger.info("Processing {} of n videos...",String.valueOf(num));
			getVideoByIid(iid,(String) context.get(StreamContext.VideoInfo.TITLE),srVo,context);
			num++;
		}
	}
	
	public HashMap<String, String> parsePlist(String url,StreamReqeustVO srVo)
	{
		HashMap<String, String> result = new HashMap<String, String>();
		String srcHtml = common.getHtml(url, srVo,true);
		String lcode = common.match2(srcHtml, "lcode:\\s*'([^']+)'");
		
		String u = "http://www.tudou.com/crp/plist.action?lcode=" + lcode;
		String content = common.getHtml(u);
		JSONObject root = JSONObject.parseObject(content);
		
		JSONArray items = root.getJSONArray("items");
		for (int i = 0; i < items.size(); i++)
		{
			JSONObject t = items.getJSONObject(i);
			String kw = t.getString("kw");
			String iid = t.getString("iid");
			result.put(kw, iid);
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public boolean getVideoByIid(String iid,String title,StreamReqeustVO srVo,StreamContext context)
	{
		if (iid.isEmpty()) {
			logger.info("{} the key id is empty",srVo.getUrl());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
			return false;
		}
		String url = "http://www.tudou.com/outplay/goto/getItemSegs.action?iid=" + iid;
		String srcHtml = common.getHtml(url, srVo,true);
		if (srcHtml.trim().length() < 10 )
		{
			logger.info("{} the srcHtml is empty",srVo.getUrl());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
			return false;
		}
		
		JSONObject root = JSONObject.parseObject(srcHtml);
		Set<String> set = root.keySet();
		Iterator<String> iterator = set.iterator();
		JSONArray temp = null;
		int sizeTemp = 0;
		while(iterator.hasNext())
		{
			String j = iterator.next();
			JSONArray d = root.getJSONArray(j);
			int sumSize = 0;
			for (int i = 0; i < d.size(); i++)
			{
				JSONObject dd = d.getJSONObject(i);
				String sizeStr = null;
				try
				{
					sizeStr = dd.getString("size");
				}
				catch (Exception e)
				{
					continue;
				}
				if (sizeStr != null)
				{
					int sizeD = dd.getIntValue("size");
					sumSize += sizeD;
				}
			}
			if (sumSize > sizeTemp)
			{
				sizeTemp = sumSize;
				temp = d;
			}
			
		}
		
		List<String> vids = new ArrayList<String>();
		Long size = 0l;
		ArrayList<String> urls = new ArrayList<String>();
		for (int i = 0; i < temp.size(); i++)
		{
			JSONObject t = temp.getJSONObject(i);
			if (t.getString("size") != null)
			{
				size += t.getInteger("size");
				vids.add(t.getString("k"));
			}
		}
		
		for (int i = 0; i < vids.size(); i++)
		{
			String u = "http://ct.v2.tudou.com/f?id=" + vids.get(i);
			//土豆只能以以下方式读入该url的数据
			//因为解析视频url和下载视频url的ua必须一致，如果以后还会遇见这种情况
			//可以把这种下载网页源码的方式写入StreamCommon类中。
			HttpResponse response = common.byHttpGet(u, srVo);
			HttpEntity entity = response.getEntity();
			BufferedReader br = null;
			String content = "";
			InputStream in = null;
			try
			{
				in = entity.getContent();
				br = new BufferedReader(new InputStreamReader(in));
		           String line;//循环读取
		           while ((line = br.readLine()) != null) {
		               content += line;
		           }
			}
			catch (Exception e)
			{
				logger.info(e.getMessage());
				context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
				return false;
			}
			finally{
				try {
					if(in != null)in.close();
					if(br != null)br.close();
				} catch (IOException e) {
					logger.info(e.getMessage());
					context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
					return false;
				}
			}
			Document document = null;
			try
			{
				document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
						.parse(new InputSource(new ByteArrayInputStream(content.getBytes("utf-8"))));
			}
			catch (Exception e)
			{
				logger.info(e.getMessage());
				context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
				return false;
			}
			NodeList ele = document.getElementsByTagName("f");
			String uu = ele.item(0).getFirstChild().getTextContent().trim();
			
			urls.add(uu);
		}
		
		String ext = common.match2(urls.get(0), "http://[\\w.]*/(\\w+)/[\\w.]*");
		context.put(StreamContext.VideoInfo.URLS, urls);
		context.put(StreamContext.VideoInfo.SIZE, size);
		context.put(StreamContext.VideoInfo.EXT, ext);		
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
		return true;
	}
	
	
	@SuppressWarnings("unchecked")
	public void getVideoById(String id ,StreamReqeustVO srVo,StreamContext context)
	{
		String url = "http://www.tudou.com/programs/view/" + id + "/";
		String srcHtml = common.getHtml(url);
		String iid = common.match2(srcHtml, "iid\\s*[:=]\\s*(\\S+)");
		String title = common.match2(srcHtml, "kw\\s*[:=]\\s*['\"]([^\n]+?)'\\s*\n");
		context.put(StreamContext.VideoInfo.TITLE, title);
		getVideoByIid(iid,title, srVo,context);
	}
	
	public boolean getVideoById( String id, String title, StreamReqeustVO srVo,StreamContext context)
	{
		String url = "http://www.tudou.com/programs/view/" + id + "/";
		String srcHtml = common.getHtml(url);
		String iid = common.match2(srcHtml, "iid\\s*[:=]\\s*(\\S+)");
		return getVideoByIid(iid,title, srVo,context);
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
