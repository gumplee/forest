package com.gumplee.biu.forest.extractor;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.gumplee.biu.forest.common.JsonOut;
import com.gumplee.biu.forest.common.StreamCommon;
import com.gumplee.biu.forest.common.StreamContext;
import com.gumplee.biu.forest.vo.StreamJSONResponseVO;
import com.gumplee.biu.forest.vo.StreamReqeustVO;

public class Le extends BaseExtractor{
	
	private static Logger logger = LoggerFactory.getLogger(Le.class);
	public static final String SITE_INFO = "Le.com";
	private static final String signKey = "2f9d6924b33a165a6d8b5d3d42f4f987";
	@Resource(name="streamCommon")
	StreamCommon common;
	
	@Override
	public void process(StreamContext context) {
		StreamReqeustVO srVo = (StreamReqeustVO)context.get(StreamContext.VideoInfo.STREAM_REQUEST_VO);
		execute(srVo, context);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void execute(StreamReqeustVO srVo, StreamContext context) {
		String url = srVo.getUrl();
		if (common.r1("http://yuntv.letv.com/", url)) {
			letvCloudDownload(srVo, context);
		}else {
			String html = common.getHtml(url);
			String vid = common.match2(url, "http://www.letv.com/ptv/vplay/(\\d+).html");
			if (vid.isEmpty()) {
				vid = common.match2(url, "http://www.le.com/ptv/vplay/(\\d+).html");
			}
			if (vid.isEmpty()) {
				vid = common.match2(html, "vid=\"(\\d+)\"");
			}
			if (vid.isEmpty()) {
				logger.info("not supported {} ",srVo.getUrl());
				context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, -1);
				return;
			}
			String title = common.match2(html, "name=\"irTitle\" content=\"(.*?)\"");
			context.put(StreamContext.VideoInfo.TITLE, title);
			letvDownloadById(vid,srVo, context);
		}
	}
	
	
	@SuppressWarnings("unchecked")
	private void letvDownloadById(String vid,StreamReqeustVO srVo,StreamContext context)
	{
		String url = String.format("http://api.letv.com/mms/out/video/playJson?id=%1$s&platid=1&splatid=101&format=1&tkey=%2$s&domain=www.letv.com",
				vid,calcTimeKey((long)System.currentTimeMillis() / 1000));
		String jsonStr = common.getHtml(url, srVo, false);
		try {
			jsonStr = new String(jsonStr.getBytes(),"utf8");
		} catch (UnsupportedEncodingException e) {
			logger.info("charset cast error {} ",srVo.getUrl());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
			return;
		}
		
		JSONObject root = JSONObject.parseObject(jsonStr);
		JSONObject playUrl = root.getJSONObject("playurl");
		JSONObject dispatch = playUrl.getJSONObject("dispatch");
		Set<String> supportId = dispatch.keySet();
		String streamId = "";
		if (supportId.contains("1080p")) {
			streamId = "1080p";
		}else if (supportId.contains("720p")) {
			streamId = "720P";
		}else {
			//排序......................................................
		}
		String videoUrl = playUrl.getJSONArray("domain").getString(0) 
				+ dispatch.getJSONArray(streamId).getString(0);
		String ext = root.getJSONObject("playurl")
				.getJSONObject("dispatch").getJSONArray(streamId).getString(1);
		
		videoUrl += String.format("&ctv=pc&m3v=1&termid=1&format=1&hwtype=un&ostype=Linux&tag=letv&sign=letv&expect=3&tn=%1$s&pay=0&iscpn=f9051&rateid=%2$s", new Random().nextDouble(),streamId);
		String jsonStr2 = common.getHtml(videoUrl, srVo, false);
		try {
			jsonStr2 = new String(jsonStr2.getBytes(),"utf8");
		} catch (UnsupportedEncodingException e) {
			logger.info("charset cast error {} ",srVo.getUrl());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
			return;
		}
		JSONObject root2 = JSONObject.parseObject(jsonStr2);
		String m3u8 = common.getHtml(root2.getString("location"), srVo, false);
		String m3u8List = decode(m3u8);
		List<String> urls = common.matchAll(m3u8List,"^[^#][^\r]*");
		Long size = 0l;
		for (int i = 0; i < urls.size(); i++) {
			size += common.urlInfo(urls.get(i), srVo);
		}
		context.put(StreamContext.VideoInfo.URLS, urls);
		context.put(StreamContext.VideoInfo.SIZE, size);
		context.put(StreamContext.VideoInfo.EXT,ext);
		getStreamJsonInfo(srVo, context);//封装视频信息json串
		
		if (srVo.isDownload())
		{
			boolean result = common.downloadVideo(urls, (String)context.get(StreamContext.VideoInfo.TITLE), ext, size, srVo);
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
	private void letvCloudDownload(StreamReqeustVO srVo, StreamContext context)
	{
		String url = srVo.getUrl();
		URI uri = null;
		try {
			uri = new URI(url);
			String query = uri.getQuery();
			String vu = common.match2(query, "vu=([\\w]+)");
			String uu = common.match2(query, "uu=([\\w]+)");
			String title = "LETV-"+vu;
			downloadLetvCloudByVu( vu, uu,title,srVo,context );
		} catch (URISyntaxException e) {
			logger.info("{} parse url error",srVo.getUrl());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
			return;
		}
	}
	
	public void downloadLetvCloudByVu(final String vu,final String uu,String title,StreamReqeustVO srVo,StreamContext context)
	{
		HashMap<String, String> argument = new HashMap<String,String>(){
			private static final long serialVersionUID = 8553213322235510938L;
			{
				put("cf", "flash");
				put("format", "json");
				put("rn", String.valueOf(System.currentTimeMillis() / 1000));
				put("uu", uu);
				put("ver", "2.2");
				put("vu", vu);
			}
		};
		List<String> listTmp = new ArrayList<>();
		Iterator<String> iterator = argument.keySet().iterator();
		while(iterator.hasNext())
		{
			listTmp.add(iterator.next());
		}
		Collections.sort(listTmp);
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < listTmp.size(); i++) {
			sb = sb.append(listTmp.get(i)).append(argument.get(listTmp.get(i)));
		}
		sb = sb.append(signKey);
		String md5Str = common.makeMd5Key(sb.toString());
		StringBuffer sb2 = new StringBuffer();
		for (int i = 0; i < listTmp.size(); i++) {
			sb2 = sb2.append(listTmp.get(i)).append("=").append(argument.get(listTmp.get(i)));
		}
		String str2HashWithoutSign = sb2.toString();
		String url = "http://api.letvcloud.com/gpc.php?&" + str2HashWithoutSign + "&sign=" + md5Str;
		String html = common.getHtml(url, srVo, false);
		JSONObject root = JSONObject.parseObject(html);
		JSONObject infodata = root.getJSONObject("data");
		
        if(! infodata.containsKey("video_info"))return;
       
        JSONObject media = infodata.getJSONObject("video_info").getJSONObject("media");
        Iterator<?> entrySet =media.entrySet().iterator(); 
        HashMap<String,JSONObject> returnInfo = new HashMap< String, JSONObject>();
        while(entrySet.hasNext() )
        {
        	String type=  entrySet.next().toString();
        	String[] typesplit = type.split("=");
        	String videoType = typesplit[0];
        	JSONObject content = new JSONObject();
        	JSONObject theCommon =media.getJSONObject(videoType).getJSONObject("play_url");
        	int videoQuality = theCommon.getIntValue("vtype");
        	String videoUrl =theCommon.getString("main_url");
        	String definition = theCommon.getString("definition");
        	content.put("vtype", videoQuality);
        	content.put("mainUrl",videoUrl);
        	content.put("definition",definition);
        	returnInfo.put(videoType, content);
        }
	
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean getStreamJsonInfo(StreamReqeustVO srVo, StreamContext context) {
		JsonOut jo = new JsonOut();
		HashMap<String, StreamJSONResponseVO> result = jo.print_info_json(SITE_INFO,context);
		context.put(StreamContext.VideoInfo.VIDEO_JSON_INFO, result);
		return true;
	}
	
	private long rot( long val , int r_bits)
	{
		int temp = r_bits%32;
		long tempk = (long) (Math.pow(2, 32)-1);
		long temp1 = (val & tempk) >> temp;
		long temp2 = ( val << ( 32 - temp)) & tempk;
		return temp1|temp2;
	}
	

	private long calcTimeKey(long time)
	{
	    int r_bits = 773625421 % 13;
	    long ror = rot( time, r_bits)^773625421;
	    r_bits = 773625421 % 17;
	    ror = rot( ror,r_bits);
		return ror;
		
	}
	
	private String decode(String srcHtml)
	{
		byte[] srcByte;
		try {
			srcByte = srcHtml.getBytes("iso-8859-1");
			byte[] btn = new byte[5];
		    for( int i = 0; i < 5; i++){
		       btn[i]=srcByte[i];
		    }
			String version = new String(btn) ;
			if( version.toLowerCase().equals("vc_01")){
				int[] loc2 = new int[srcByte.length - 5];
				int length = loc2.length;
				for( int i = 0; i < length; i++){
					loc2[i] = srcByte[i+5]& 0xff;
				}
				
				int[] loc4 = new int[ 2*length];
				for( int i = 0; i < length; i++){
					loc4[2*i] =  loc2[i] >> 4;
					loc4[2*i+1]=  loc2[i] & 15;
				}
				int[] loc6 = new int[loc4.length];
				int k = 0;
				int site = loc4.length-11;
				for( int i = site; i <loc4.length; i++ ){
					loc6[k] = loc4[i];
					k++;
				}
				for( int j = 0; j < site; j++){
					loc6[k] = loc4[j];
					k++;
				}
				byte [] loc7 = new byte[length];
				for( int n = 0; n < length; n++){ 
					int temp = loc6[2*n]<<4;
					loc7[n] = (byte) (temp+(+loc6[2*n+1]));
				}
				String result = null;
				try {
					result = new String(loc7,"utf-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				return result;
			}else{
				return srcHtml;
			}
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		return srcHtml;
	}

}
