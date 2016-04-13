package com.gumplee.biu.forest.extractor;

import java.util.HashMap;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gumplee.biu.forest.common.JsonOut;
import com.gumplee.biu.forest.common.StreamCommon;
import com.gumplee.biu.forest.common.StreamContext;
import com.gumplee.biu.forest.vo.StreamJSONResponseVO;
import com.gumplee.biu.forest.vo.StreamReqeustVO;

public class Bandcamp extends BaseExtractor {

	private static Logger logger = LoggerFactory.getLogger(Bandcamp.class);
	public static final String SITE_INFO = "Bandcamp.com";
	@Resource(name="streamCommon")
	StreamCommon common;
	@Override
	public void process(StreamContext context) {
		StreamReqeustVO srVo = (StreamReqeustVO)context.get(StreamContext.VideoInfo.STREAM_REQUEST_VO);
		execute(srVo,context);
	}

	@Override
	public void execute(StreamReqeustVO srVo, StreamContext context) {
		
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean getStreamJsonInfo(StreamReqeustVO srVo, StreamContext context) {
		JsonOut jo = new JsonOut();
		HashMap<String, StreamJSONResponseVO> result = jo.print_info_json(SITE_INFO,context);
		context.put(StreamContext.VideoInfo.VIDEO_JSON_INFO, result);
		return true;
	}

}
