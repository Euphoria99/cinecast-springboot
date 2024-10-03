package com.stream.app;

import com.stream.app.service.VideoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class VideoStreamingSpringApplicationTests {

	@Autowired
	VideoService videoService;

	@Test
	void contextLoads() {
		videoService.processVideo("831b6998-279a-4e38-a548-c22a1d4e5fb2");
	}

}
