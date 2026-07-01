package com.hana.exchange.market.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.hana.exchange.market.client.OmniLensMarketIndex;
import com.hana.exchange.market.domain.MarketIndexSnapshot;

@Service
public class MarketIndexStreamPublisher {

	private final SimpMessagingTemplate messagingTemplate;
	private final MarketIndexService marketIndexService;
	private final Clock clock;

	@Autowired
	public MarketIndexStreamPublisher(
			SimpMessagingTemplate messagingTemplate,
			MarketIndexService marketIndexService) {
		this(messagingTemplate, marketIndexService, Clock.systemUTC());
	}

	MarketIndexStreamPublisher(
			SimpMessagingTemplate messagingTemplate,
			MarketIndexService marketIndexService,
			Clock clock) {
		this.messagingTemplate = messagingTemplate;
		this.marketIndexService = marketIndexService;
		this.clock = clock;
	}

	public void publish(OmniLensMarketIndex index) {
		MarketIndexSnapshot.Index message = marketIndexService.toIndex(index);
		for (String topic : topics(index)) {
			messagingTemplate.convertAndSend(topic, message);
		}
	}

	private List<String> topics(OmniLensMarketIndex index) {
		return List.of(
				"/topic/market/indices",
				"/topic/market/indices/" + index.indexCode());
	}

	Instant now() {
		return Instant.now(clock);
	}
}
