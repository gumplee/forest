package com.gumplee.biu.forest.thread;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.http.ConnectionClosedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.gumplee.biu.forest.common.StreamCommon;
import com.gumplee.biu.forest.vo.DownInfoVO;


@Service(value="multiThreadDownService")
public class MultiThreadDownService {
	
	private static Logger logger = LoggerFactory.getLogger(MultiThreadDownService.class);
	
	@Autowired
	ThreadPoolTaskExecutor threadPoolTaskExecutor;
	
	/**
	 * 下载多个文件
	 * 
	 * @param downInfoVOs 下载信息列表
	 * @return
	 */
	public List<DownInfoVO> down(List<DownInfoVO> downInfoVOs) {
		if((null == downInfoVOs) || downInfoVOs.isEmpty()){
			return null;
		}
		List<DownInfoVO> downInfoResuList = new ArrayList<DownInfoVO>();
		// 多线程根据插件取数据
		Map<Integer, Future<Boolean>> downTaskMap = new HashMap<Integer, Future<Boolean>>();
		for (int i = 0; i < downInfoVOs.size(); i++) {
			DownInfoVO downInfoVO = downInfoVOs.get(i);
			downInfoResuList.add(downInfoVO);
			downTaskMap.put(i, threadPoolTaskExecutor.submit(new DownTask(downInfoVO)));
		}
		
		for (int j = 0; j < downInfoVOs.size(); j++) {
			DownInfoVO downInfoVO = downInfoVOs.get(j);
			Future<Boolean> downTask = downTaskMap.get(j);
			if (downTask != null) {
				try {
					downInfoVO.setStatus(downTask.get() ? 1 : 0);
					if (!downTask.get())
					{
						logger.info(" thread cancel! ");
						downTask.cancel(true);
						break;
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
		}
		return downInfoResuList;
	}
	
	public static class DownTask extends StreamCommon implements Callable<Boolean> {
	
		private DownInfoVO downInfoVO;
		public DownTask(DownInfoVO downInfoVO) {
			super();
			this.downInfoVO = downInfoVO;
		}

		@Override
		public Boolean call() {
			
			boolean result = false;
			int i = 0;
			while(!Thread.currentThread().isInterrupted() && !result && i < 3)
			{
				try
				{
					result = urlSave(downInfoVO.getUrl(), downInfoVO.getSize(),downInfoVO.getStartIndex(),
							downInfoVO.getEndIndex(),downInfoVO.getAbsFileName(), downInfoVO.getSpb(), true, downInfoVO.getSrVo());
				}
				catch (ConnectionClosedException e)
				{
					logger.info("thread catch closed exception {} ",e.getMessage());
					Thread.interrupted();
				}
				catch (SocketTimeoutException e)
				{
					logger.info("thread catch timeout exception {} ",e.getMessage());
					Thread.interrupted();
				}
				catch (ConnectException e)
				{
					logger.info("thread catch connecttimeout exception {} ",e.getMessage());
					Thread.interrupted();
				}
				i++;
			}
			return result;
		}
	}
}
