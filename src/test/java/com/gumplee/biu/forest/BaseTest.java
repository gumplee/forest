package com.gumplee.biu.forest;

import java.io.File;

import org.junit.After;
import org.junit.Before;

public class BaseTest {
	
	@Before
	public void setup() {
		try {
			String confPath = "." + File.separator + "src" + File.separator + "test" + File.separator + "resources";
			System.setProperty("stream-extractor-conf", confPath);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@After
	public void tearDown() {
	}

}
