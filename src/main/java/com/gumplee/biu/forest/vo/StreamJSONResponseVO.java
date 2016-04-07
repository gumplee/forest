package com.gumplee.biu.forest.vo;

import com.alibaba.fastjson.JSONObject;



/**
 * 
 * @author: liyuanjun
 * @date: 2015年12月24日
 */
public class StreamJSONResponseVO
{
	private String title;
	private String siteName;
	private String url;
	private JSONObject stream;
	private String sd;   //清晰度
	public String getTitle()
	{
		return title;
	}
	public void setTitle(String title)
	{
		this.title = title;
	}
	public String getSiteName()
	{
		return siteName;
	}
	public void setSiteName(String siteName)
	{
		this.siteName = siteName;
	}
	public JSONObject getStream()
	{
		return stream;
	}
	public void setStream(JSONObject stream)
	{
		this.stream = stream;
	}
	public String getUrl()
	{
		return url;
	}
	public void setUrl(String url)
	{
		this.url = url;
	}
	public String getSd() {
		return sd;
	}
	public void setSd(String sd) {
		this.sd = sd;
	}
}
