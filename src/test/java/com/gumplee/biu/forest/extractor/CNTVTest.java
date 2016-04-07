package com.gumplee.biu.forest.extractor;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.gumplee.biu.forest.BaseTest;
import com.gumplee.biu.forest.common.StreamContext;
import com.gumplee.biu.forest.vo.StreamReqeustVO;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context.xml")
public class CNTVTest extends BaseTest
{
	
	@Resource(name="cntv")
	CNTV cNTV;
	
	
	@SuppressWarnings("unchecked")
	@Test
	public void test()
	{
		List<String> urls = new ArrayList<String>();
		urls.add("http://news.cntv.cn/2015/12/14/VIDE1450048448227159.shtml");
		//urls.add("http://jingji.cntv.cn/2015/12/10/VIDE1449758880033206.shtml");
		//urls.add("http://news.cntv.cn/2016/01/22/VIDEADgPgFGRheMy5w4pF9zO160122.shtml");
		//urls.add("http://tv.cntv.cn/video/VSET100229263600/d1d949ce736746e99f8569bcfef02466");
		//urls.add("http://tv.cctv.com/2016/03/10/VIDE4B1tEdbVbN5TetPrqKaK160310.shtml");
		for (int i = 0; i < urls.size(); i++)
		{
			StreamReqeustVO srVo = new StreamReqeustVO();
			StreamContext context = new StreamContext();
			context.put(StreamContext.VideoInfo.STREAM_REQUEST_VO, srVo);
			srVo.setOutPath("D:\\workspace\\streamextractordownload");
			srVo.setUrl(urls.get(i));
			srVo.setDownload(true);
			
			cNTV.process(context);
		}
		
	}

}
