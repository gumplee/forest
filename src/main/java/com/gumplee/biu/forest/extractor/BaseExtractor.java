package com.gumplee.biu.forest.extractor;

import java.util.HashMap;

import com.gumplee.biu.forest.common.JsonOut;
import com.gumplee.biu.forest.common.StreamContext;
import com.gumplee.biu.forest.vo.StreamJSONResponseVO;
import com.gumplee.biu.forest.vo.StreamReqeustVO;

public abstract class BaseExtractor
{
	public abstract void process(StreamContext context);
	public abstract void execute(StreamReqeustVO srVo,StreamContext context);
	@SuppressWarnings("unchecked")
	public boolean getStreamJsonInfo(final String SITE_INFO,StreamReqeustVO srVo,StreamContext context)
	{
		JsonOut jo = new JsonOut();
		HashMap<String, StreamJSONResponseVO> result = jo.print_info_json(SITE_INFO,context);
		context.put(StreamContext.VideoInfo.VIDEO_JSON_INFO, result);
		return true;
	}
}
