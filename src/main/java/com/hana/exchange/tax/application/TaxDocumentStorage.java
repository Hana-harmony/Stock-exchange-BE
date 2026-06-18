package com.hana.exchange.tax.application;

public interface TaxDocumentStorage {

	void store(String storageKey, byte[] content);
}
