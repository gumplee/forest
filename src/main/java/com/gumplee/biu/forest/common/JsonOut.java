package com.gumplee.biu.forest.common;

import java.util.ArrayList;
import java.util.HashMap;

import com.alibaba.fastjson.JSONObject;
import com.gumplee.biu.forest.vo.StreamJSONResponseVO;
import com.gumplee.biu.forest.vo.StreamReqeustVO;


public class JsonOut
{
	@SuppressWarnings("unchecked")
	public HashMap<String, StreamJSONResponseVO> print_info_json(String siteInfo,StreamContext context)
	{
		HashMap<String, StreamJSONResponseVO> result = new HashMap<String, StreamJSONResponseVO>();
		StreamReqeustVO srVo = (StreamReqeustVO)context.get(StreamContext.VideoInfo.STREAM_REQUEST_VO);
		Object videoExtendInfo = context.get(StreamContext.VideoInfo.VIDEO_EXTEND_INFO);
		StreamJSONResponseVO info = new StreamJSONResponseVO();
		ArrayList<String> videoProfile = srVo.getVideoProfile();		
		String title=null;
		title= (String)context.get(StreamContext.VideoInfo.TITLE);
		if(title==null||title.equals(""))
		{
			result.put("result",null);
			return result;
		}
		if (videoProfile == null||videoProfile.isEmpty())
		{
			ArrayList<String> urls =(ArrayList<String>) context.get(StreamContext.VideoInfo.URLS);
			String type = (String)context.get(StreamContext.VideoInfo.EXT);
			long size =(Long) context.get(StreamContext.VideoInfo.SIZE);
			info.setSiteName(siteInfo);
			info.setTitle(title);
			info.setUrl(srVo.getUrl());
			JSONObject stream = new JSONObject();
			JSONObject __default = new JSONObject();
			__default.put("container", type);
			__default.put("size",size);
			__default.put("urls", urls);
			stream.put("__default__", __default);
			stream.put("video_profile", "__default");
			if (videoExtendInfo!= null)
			{
				stream.put("extend",videoExtendInfo);
			}
			info.setStream(stream);
		}
		else {
			
			JSONObject videoProfileInfo = (JSONObject) context.get(StreamContext.VideoInfo.VIDEO_PROFILE_INFO);	
			String videoProfileKey=(String) context.get(StreamContext.VideoInfo.VIDEO_PROFILE);
			info.setSiteName(siteInfo);
			info.setTitle(title);
			info.setUrl(srVo.getUrl());
			info.setSd(videoProfileKey);
			JSONObject stream = new JSONObject();
			stream.put("streams",videoProfileInfo);
			if (videoExtendInfo!= null)
			{
				stream.put("extend",videoExtendInfo);
			}
			info.setStream(stream);
		}	
		result.put("result", info);
		return result;
	}
}
