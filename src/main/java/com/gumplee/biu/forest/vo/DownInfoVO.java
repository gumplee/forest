package com.gumplee.biu.forest.vo;

import com.gumplee.biu.forest.common.SimpleProgressBar;



/**
 * 下载信息类
 * 
 * @author xiaobisong
 *
 */
public class DownInfoVO {
	
	/**
	 * 下载的url
	 */
	private String url;
	
	/**
	 * 标题
	 */
	private String title;
	
	/**
	 * 扩展名
	 */
	private String ext;
	
	/**
	 * 文件大小
	 */
	private Long size;
	
	/**
	 * 单源多线程下载文件块开始索引
	 */
	private Long startIndex;
	
	/**
	 * 单源多线程下载文件块结束索引
	 */
	private Long endIndex;
	
	/**
	 * 下载状态
	 */
	private Integer status = 0;
	
	/**
	 * 文件绝对路径名
	 */
	private String absFileName;
	
	/**
	 * 请求对象
	 */
	private StreamReqeustVO srVo;
	
	/**
	 * 进度条
	 */
	private SimpleProgressBar spb;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getExt() {
		return ext;
	}

	public void setExt(String ext) {
		this.ext = ext;
	}

	public Long getSize() {
		return size;
	}

	public void setSize(Long size) {
		this.size = size;
	}

	public Integer getStatus()
	{
		return status;
	}

	public void setStatus(Integer status)
	{
		this.status = status;
	}

	public String getAbsFileName()
	{
		return absFileName;
	}

	public void setAbsFileName(String absFileName)
	{
		this.absFileName = absFileName;
	}

	public StreamReqeustVO getSrVo()
	{
		return srVo;
	}

	public void setSrVo(StreamReqeustVO srVo)
	{
		this.srVo = srVo;
	}

	public SimpleProgressBar getSpb()
	{
		return spb;
	}

	public void setSpb(SimpleProgressBar spb)
	{
		this.spb = spb;
	}
	
	public Long getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(Long startIndex) {
		this.startIndex = startIndex;
	}

	public Long getEndIndex() {
		return endIndex;
	}

	public void setEndIndex(Long endIndex) {
		this.endIndex = endIndex;
	}

}
