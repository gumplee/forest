package com.gumplee.biu.forest.common;

import java.util.HashMap;

@SuppressWarnings({ "rawtypes", "serial" })
public class StreamContext extends HashMap
{
	
	public enum VideoInfo {
		/*
		 * 视频标题
		 */
		TITLE,
		/*
		 * 视频大小
		 */
		SIZE,
		/*
		 * 视频扩展名
		 */
		EXT,
		/*
		 * 视频地址列表
		 */
		URLS,
		/*
		 * 下载状态 0 成功 1 失败 -1不支持
		 */
		DOWNLOAD_STATE,
		/*
		 * 下载视频清晰度
		 */
		VIDEO_PROFILE,
		/*
		 * 清晰度相关的视频信息
		 */
		VIDEO_PROFILE_INFO,
		/*
		 * 视频其他信息
		 */
		VIDEO_EXTEND_INFO,
		/*
		 * 视频信息json格式
		 */
		VIDEO_JSON_INFO,
		/*
		 * 请求对象
		 */
		STREAM_REQUEST_VO
	}
}
