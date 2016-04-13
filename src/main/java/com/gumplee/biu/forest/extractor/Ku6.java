package com.gumplee.biu.forest.extractor;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.gumplee.biu.forest.common.StreamCommon;
import com.gumplee.biu.forest.common.StreamContext;
import com.gumplee.biu.forest.vo.StreamReqeustVO;
@Service("ku6")
public class Ku6 extends BaseExtractor
{
	private static Logger logger = LoggerFactory.getLogger(Ku6.class);
	public static final String SITE_INFO = "Ku6.com";
	@Resource(name="streamCommon")
	StreamCommon common;

	@Override
	public void process(StreamContext context) {
		StreamReqeustVO srVo = (StreamReqeustVO)context.get(StreamContext.VideoInfo.STREAM_REQUEST_VO);
		execute(srVo,context);
	}

	@SuppressWarnings("serial")
	@Override
	public void execute(StreamReqeustVO srVo, StreamContext context) {
		String url = srVo.getUrl(), id = "";
		List<String> patterns = new ArrayList<String>() {
			{
				add("http://v.ku6.com/special/show_\\d+/(.*)\\.\\.\\.html");
				add("http://v.ku6.com/show/(.*)\\.\\.\\.html");
				add("http://my.ku6.com/watch\\?.*v=(.*)\\.\\..*");
			}
		};
		for (String pattern : patterns) {
			id = common.match2(url, pattern);
			if (!id.isEmpty())
				break;
		}
		ku6DowndoadById(id, srVo, context);
	}

	@SuppressWarnings("unchecked")
	private void ku6DowndoadById(String id, StreamReqeustVO srVo, StreamContext context) {
		if (id.isEmpty()) {
			logger.info("{} not supported",srVo.getUrl());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, -1);
			return;
		}
		try {
			String content = common.getHtml("http://v.ku6.com/fetchVideo4Player/" + id + "...html");
			JSONObject data = JSONObject.parseObject(content).getJSONObject("data");
			String title = data.getString("t");
			assert !title.isEmpty();
			context.put(StreamContext.VideoInfo.TITLE, title);
			
			String f = data.getString("f");
			List<String> urls = new ArrayList<String>();
			for (String str : f.split(",")) {
				urls.add(str);
			}
			if (urls.size() > 0) {
				context.put(StreamContext.VideoInfo.URLS, urls);
				Long size = 0l;
				String ext = common.match2(urls.get(0), "\\.([^.]+)$");
				context.put(StreamContext.VideoInfo.EXT, ext);
				for (int i = 0; i < urls.size(); i++) {
					size += common.urlInfo(urls.get(0), srVo);
				}
				context.put(StreamContext.VideoInfo.SIZE, size);
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
				logger.info("{} the video urls is empty",srVo.getUrl());
				context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
			}
		} catch (Exception e) {
			logger.info(e.getMessage());
			context.put(StreamContext.VideoInfo.DOWNLOAD_STATE, 0);
			return;
		}
	}
}
