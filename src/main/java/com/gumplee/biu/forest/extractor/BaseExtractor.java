package com.gumplee.biu.forest.extractor;

import com.gumplee.biu.forest.common.StreamContext;
import com.gumplee.biu.forest.vo.StreamReqeustVO;

public abstract class BaseExtractor
{
	public abstract void process(StreamContext context);
	public abstract void execute(StreamReqeustVO srVo,StreamContext context);
	public abstract boolean getStreamJsonInfo(StreamReqeustVO srVo,StreamContext context);
}
