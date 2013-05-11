package com.ontologycentral.ldspider.hooks.sink;

import org.semanticweb.yars.nx.parser.Callback;

/**
 * A Sink accepts datasets and writes them to an output.
 * 
 * @author RobertIsele
 */
public interface Sink {
	
	/**
	 * Creates a new dataset
	 * 
	 * @param provenance The provenance of the dataset
	 * @return A callback used to write statements into the datset
	 */
	Callback newDataset(Provenance provenance);

  /**
   * Frees all resources.
   */
  public void shutdown();
}
