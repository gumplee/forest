package com.gumplee.biu.forest.common;

import java.text.DecimalFormat;

public class SimpleProgressBar
{
	private int bar_size = 38;
	private boolean displayed;
	private Double size;
	private Integer pieces;
	private Integer currentPiece;
	private double received;
	private String speed;
	private double last_updated;
	DecimalFormat df2 = new DecimalFormat("0.00");
	
	public SimpleProgressBar(boolean displayed,Long size,Integer pieces)
	{
		this.displayed = displayed;
		this.size = Double.valueOf(size);
		this.pieces = pieces;
		this.currentPiece = 1;
		this.received = 0;
		this.speed = "";
		this.last_updated = System.currentTimeMillis();
	}
	
	
	public void update()
	{
		String plusStr = " ";
		double percent = Double.valueOf(df2.format(received * 100 / size));
		if (percent > 100)
		{
			percent = 100;
		}
		int dots = (int) (bar_size * percent / 100);
		double plus = percent - dots / bar_size * 100;
		if (plus > 0.8)
		{
			plusStr = "█";
		}
		else if (plus < 0.8 && plus > 0.4)
		{
			plusStr = ">";
		}
		String localBar = "";
		String localBar2 = "";
		for (int i = 0; i < dots; i++)
		{
			localBar += "█";
		}
		localBar += plusStr;
		for (int i = 0; i <bar_size- localBar.length(); i++)
		{
			localBar2 += "-";
		}
		System.out.println(df2.format(percent) + "% " + "(" + df2.format(received / 1048576) + "/" + 
				df2.format(size / 1048576) + " MB ) ├" + localBar + localBar2 + "┤ [" + currentPiece + "/" + pieces 
				+ "]" + speed);
	}
	
	public void update_received(long receivedSize)
	{
		if (displayed)
		{
			received += receivedSize;
			double curTime = System.currentTimeMillis();
			double timeDiff = curTime - last_updated;
			double bytesPre = 0;
			if (timeDiff > 0)
			{
				bytesPre = receivedSize / timeDiff * 1000;
			}
			else {
				bytesPre = 0;
			}
			
			if (bytesPre >= 1048576)
			{
				speed = df2.format(Double.valueOf(bytesPre / 1048576)) + " MB/s";
			}
			else if (bytesPre >= 1024) {
				speed = df2.format(Double.valueOf(bytesPre / 1024)) + " kB/s";
			}
			else {
				speed = df2.format(Double.valueOf(bytesPre)) + " B/s";
			}
			
			last_updated = System.currentTimeMillis();
			update();
		}
	}
	
	public void update_piece(int n)
	{
		this.currentPiece = n;
	}
	
	public void done(String title)
	{
		if (displayed) {
			System.out.println(title + "  下载完成！");
			this.displayed = false;
		}
	}


	public double getReceived()
	{
		return received;
	}


	public void setReceived(double received)
	{
		this.received = received;
	}
}
