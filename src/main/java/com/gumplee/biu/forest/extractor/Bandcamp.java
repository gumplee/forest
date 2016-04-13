package com.gumplee.biu.forest.extractor;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gumplee.biu.forest.common.StreamCommon;
import com.gumplee.biu.forest.common.StreamContext;
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
}
