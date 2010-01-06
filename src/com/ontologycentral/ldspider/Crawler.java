package com.ontologycentral.ldspider;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import org.semanticweb.yars.nx.parser.Callback;
import org.semanticweb.yars.util.Callbacks;

import com.ontologycentral.ldspider.hooks.content.CallbackDummy;
import com.ontologycentral.ldspider.hooks.error.ErrorHandler;
import com.ontologycentral.ldspider.hooks.error.ErrorHandlerDummy;
import com.ontologycentral.ldspider.hooks.fetch.FetchFilter;
import com.ontologycentral.ldspider.hooks.fetch.FetchFilterAllow;
import com.ontologycentral.ldspider.hooks.links.LinkFilter;
import com.ontologycentral.ldspider.hooks.links.LinkFilterDefault;
import com.ontologycentral.ldspider.http.ConnectionManager;
import com.ontologycentral.ldspider.http.LookupThread;
import com.ontologycentral.ldspider.http.robot.Robots;
import com.ontologycentral.ldspider.queue.memory.FetchQueue;
import com.ontologycentral.ldspider.tld.TldManager;

public class Crawler {
	Logger _log = Logger.getLogger(this.getClass().getName());

	Callback _output;
	LinkFilter _links;
	ErrorHandler _eh;
	FetchFilter _ff;
	ConnectionManager _cm;
	
	Robots _robots;
	TldManager _tldm;
	
	//UriSrc _urisrc;
	
	int _threads;
	
	public Crawler() {
		this(CrawlerConstants.DEFAULT_NB_THREADS);
	}
	
	public Crawler(int threads) {
		//_urisrc = new UriSrc();

		_threads = threads;
		
		String phost = null;
		int pport = 0;		
		String puser = null;
		String ppassword = null;
		
		if (System.getProperties().get("http.proxyHost") != null) {
			phost = System.getProperties().get("http.proxyHost").toString();
		}
		if (System.getProperties().get("http.proxyPort") != null) {
			pport = Integer.parseInt(System.getProperties().get("http.proxyPort").toString());
		}
		
		if (System.getProperties().get("http.proxyUser") != null) {
			puser = System.getProperties().get("http.proxyUser").toString();
		}
		if (System.getProperties().get("http.proxyPassword") != null) {
			ppassword = System.getProperties().get("http.proxyPassword").toString();
		}
		
	    _cm = new ConnectionManager(phost, pport, puser, ppassword, threads*CrawlerConstants.MAX_CONNECTIONS_PER_THREAD);
	    _cm.setRetries(CrawlerConstants.RETRIES);
	    
	    try {
		    _tldm = new TldManager(_cm);
		} catch (Exception e) {
			_log.info("cannot get tld file online " + e.getMessage());
			try {
				_tldm = new TldManager();
			} catch (IOException e1) {
				_log.info("cannot get tld file locally " + e.getMessage());
			}
		}

		_eh = new ErrorHandlerDummy();

	    _robots = new Robots(_cm, _eh);
		
		_output = new CallbackDummy();
		_links = new LinkFilterDefault(_eh);
		_ff = new FetchFilterAllow();
	}
	
	public void setFetchFilter(FetchFilter ff) {
		_ff = ff;
	}
	
	public void setErrorHandler(ErrorHandler eh) {
		_eh = eh;
		
		_robots.setErrorHandler(eh);
	}
	
	public void setOutputCallback(Callback cb) {
		_output = cb;
	}
	
	public void setLinkSelectionCallback(LinkFilter links) {
		_links = links;
	}
	
	public void evaluate(Collection<URI> seeds, int rounds) {
		evaluate(seeds, rounds, CrawlerConstants.DEFAULT_NB_URIS);
	}
	
	public void evaluate(Collection<URI> seeds, int rounds, int maxuris) {
		FetchQueue q = new FetchQueue(_tldm);

		for (URI u : seeds) {
			q.addFrontier(u);
		}
		
		q.schedule(maxuris);
		
		for (int curRound = 0 ; curRound <= rounds; curRound++) {
			List<Thread> ts = new ArrayList<Thread>();

			Callbacks cbs = new Callbacks(new Callback[] { _output, _links } );

			for (int j = 0; j < _threads; j++) {
				LookupThread lt = new LookupThread(_cm, q, cbs, _robots, _eh, _ff);
				ts.add(new Thread(lt,"LookupThread-"+j));		
			}

			_log.info("Starting threads round " + curRound + " with " + q.size() + " uris");
			_log.info(q.toString());
			
			for (Thread t : ts) {
				t.start();
			}

			for (Thread t : ts) {
				try {
					t.join();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}

			for (URI u : _links.getLinks()) {
				q.addFrontier(u);
			}

			q.schedule(maxuris);
		}
	}
	
	public void close() {
		_cm.shutdown();
		_eh.close();
	}
}
