package com.gumplee.biu.forest.extractor;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.gumplee.biu.forest.common.StreamCommon;
import com.gumplee.biu.forest.common.StreamContext;
import com.gumplee.biu.forest.vo.StreamReqeustVO;


@Service("pptv")
public class PPTV extends BaseExtractor
{
	private static Logger logger = LoggerFactory.getLogger(PPTV.class);
	public static final String SITE_INFO = "PPTV.com";
	@Resource(name="streamCommon")
	StreamCommon common;

	@Override
	public void process(StreamContext context)
	{
		StreamReqeustVO srVo = (StreamReqeustVO)context.get(StreamContext.VideoInfo.STREAM_REQUEST_VO);
		srVo.setReferer("http://ccf.pptv.com/0");
		srVo.setFaker(true);
		execute(srVo,context);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void execute(StreamReqeustVO srVo,StreamContext context)
	{
		String url = srVo.getUrl();
		if (common.r1("http://v.pptv.com/show/(\\w+)\\.html$", url))
		{
			String srcHtml = common.getHtml(url);
			String id = common.match2(srcHtml, "webcfg\\s*=\\s*\\{\"id\":\\s*(\\d+)");
			if (!id.isEmpty())
			{
				getVideoById(id, srVo,context);
			}
			else {
				logger.info("{} not supported",srVo.getUrl());
				context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, -1);
				return;
			}
		}
		else {
			logger.info("{} not supported",srVo.getUrl());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, -1);
			return;
		}
	}
	
	@SuppressWarnings("unchecked")
	public void getVideoById(String id,StreamReqeustVO srVo,StreamContext context)
	{
		String url = "http://web-play.pptv.com/webplay3-0-" + id + ".xml?type=web.fpp";
		String xml = common.getHtml(url);
		
		String k = common.match2(xml, "<key expire=[^<>]+>([^<>]+)</key>");
		String rid = common.match2(xml, "rid=\"([^\"]+)\"");
		String title = common.match2(xml, "nm=\"([^\"]+)\"");
		context.put(StreamContext.VideoInfo.TITLE, title);
		String st = common.match2(xml, "<st>([^<>]+)</st>");
		st = st.substring(0,st.length()-4);
		SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM DD HH:mm:ss yyyy",Locale.ENGLISH);
		Date d = null;
		try
		{
			d = sdf.parse(st);
		}
		catch (ParseException e)
		{
			logger.info(e.getMessage());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
			return;
		}
		double time = d.getTime();
		double time2 = time- 60 * 1000 - System.currentTimeMillis();
		time2 = time2 + System.currentTimeMillis();
		double k1 = time2 / 1000;
		BigDecimal bd = new BigDecimal(k1);
		String key  = bd.toString();
		
		key = constructKey(key);
		List<String> pieces = common.matchAll(xml, "<sgm no=\"(\\d+)\"[^<>]+fs=\"(\\d+)\"");
		Long size = 0l;
		ArrayList<String> urls = new ArrayList<String>();
		for (int i = 0; i < pieces.size(); i = i + 2)
		{
			String fs = pieces.get(i + 1);
			size += Integer.valueOf(fs);
			String url2 = null;
			url2 = String.format("http://ccf.pptv.com/%1$s/%2$s?key=%3$s&fpp.ver=1.3.0.4&k=%4$s&type=web.fpp", 
					i,rid,key,k);
			urls.add(url2);
		}
		context.put(StreamContext.VideoInfo.URLS, urls);
		context.put(StreamContext.VideoInfo.SIZE, size);
		context.put(StreamContext.VideoInfo.EXT, "mp4");
		if (rid.endsWith(".mp4"))
		{
			getStreamJsonInfo(SITE_INFO,srVo, context);//封装视频信息json串
			
			if (srVo.isDownload())
			{
				boolean result = common.downloadVideo(urls, title, "mp4", size, srVo);
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
			logger.info("{} this ext is not supported",srVo.getUrl());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, -1);
			return;
		}
	}

	
	public String constructKey(String key)
	{
		int k =(int) Math.round(Double.valueOf(key));
		String hex = Integer.toHexString(k);
		
		int num = 16 - hex.length();
		String str = "";
		for (int i = 0; i < num; i++)
		{
			str += "\\x00";
		}
		
		hex = hex + str;
		
		String SERVER_KEY = "qqqqqww\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00";
		String res = encrypt(hex, SERVER_KEY);
		return str2hex(res);
	}
	
	public String str2hex(String s)
	{
		String r = "";
		char[] c = s.substring(0,8).toCharArray();
		for (int i = 0; i < c.length; i++)
		{
			int ac = c[i];
			String t = Integer.toHexString(ac);
			if (t.length() == 1)
			{
				t = "0" + t;
			}
			r += t;
		}
		for (int i = 0; i < 16; i++)
		{
			String t = Integer.toHexString((int)(15 * Math.random()));
			r += t;
		}
		return r;
	}
	
	public int getKey(String s)
	{
		char[] c = s.toCharArray();
		int l3 = 0;
		int l4 = 0;
		while(l4 < c.length)
		{
			char l5 = c[l4];
			int l6 = l5;
			int l7 = l6 << ((l4 % 4) * 8);
			l3 = l3 ^ l7;
			l4 += 1;
		}
		return l3;
	}
	
	public int rot(int k,int b)
	{
		if(k >= 0)
		{
			return k >> b;
		}
		else {
			return (int)(Math.pow(2, 32) + k) >> b;
		}
	}
	
	public int lot(int k,int b)
	{
		return (int)((k << b) % (Math.pow(2, 32)));
	}
	
	public String encrypt(String s1,String s2)
	{
		long delta = 2654435769L;
		int l4 = getKey(s2);
		char[] c8 = s1.toCharArray();
		int l10 = l4;
		int l5 = lot(l10, 8) | rot(l10, 24);
		int l6 = lot(l10,16) | rot(l10,16);
		int l7 = lot(l10,24) | rot(l10,8);
		
		String l11 = "";
		int l12 = 0;
		
		int l13 = (int)(c8[l12]) << 0;
		int l14 = (int)(c8[l12+1]) << 8;
		int l15 = (int)(c8[l12+2]) << 16;
		int l16 = (int)(c8[l12+3]) << 24;
		int l17 = (int)(c8[l12+4]) << 0;
		int l18 = (int)(c8[l12+5]) << 8;
		int l19 = (int)(c8[l12+6]) << 16;
		int l20 = (int)(c8[l12+7]) << 24;
		
		int l21=(((0|l13)|l14)|l15)|l16;
		int l22=(((0|l17)|l18)|l19)|l20;
		
		int l23 = 0;
		int l24 = 0;
		
		while(l24 < 32)
		{
			l23=(int) ((l23+delta)%(Math.pow(2, 32)));
		    int l33=(int) ((lot(l22,4)+l4)%(Math.pow(2, 32)));
		    int l34=(int) ((l22+l23)%(Math.pow(2, 32)));
		    int l35=(int) ((rot(l22,5)+l5)%(Math.pow(2, 32)));
		    int l36=(l33^l34)^l35;
		    l21=(int) ((l21+l36)%(Math.pow(2, 32)));
		    int l37=(int) ((lot(l21,4)+l6)%(Math.pow(2, 32)));
		    int l38=(int) ((l21+l23)%(Math.pow(2, 32)));
		    int l39=(int) ((rot(l21,5))%(Math.pow(2, 32)));
		    int l40=(int) ((l39+l7)%(Math.pow(2, 32)));
		    int l41=(int) (((l37^l38)%(int)(Math.pow(2, 32))^l40)%(Math.pow(2, 32)));
		    l22=(int) ((l22+l41)%(Math.pow(2, 32)));
		     
		    l24+=1;
		}
		
		l11+=(char)(rot(l21,0)&0xff);
		l11+=(char)(rot(l21,8)&0xff);
		l11+=(char)(rot(l21,16)&0xff);
		l11+=(char)(rot(l21,24)&0xff);
		l11+=(char)(rot(l22,0)&0xff);
		l11+=(char)(rot(l22,8)&0xff);
		l11+=(char)(rot(l22,16)&0xff);
		l11+=(char)(rot(l22,24)&0xff);
		
		return l11;
	}
}
