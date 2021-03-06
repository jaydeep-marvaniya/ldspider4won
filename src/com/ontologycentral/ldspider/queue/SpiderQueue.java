package com.ontologycentral.ldspider.queue;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.semanticweb.yars.tld.TldManager;
import org.semanticweb.yars.util.LRUMapCache;

import com.ontologycentral.ldspider.CrawlerConstants;
import com.ontologycentral.ldspider.frontier.DiskFrontier;
import com.ontologycentral.ldspider.frontier.Frontier;




public abstract class SpiderQueue implements Serializable{
	private static final long serialVersionUID = 1L;
	private static final Logger _log = Logger.getLogger(SpiderQueue.class.getName());

	public abstract URI poll();
	public abstract int size();
	
	protected Set<URI> _seen;
	
	LRUMapCache<URI, Integer> _redirsCache = new LRUMapCache<URI, Integer>(2 * CrawlerConstants.NB_THREADS);

	//protected Set<URI> _seenRound = null;
	//protected Set<URI> _redirsRound = null;

	//protected Set<URI> _urisRound = null;

	protected TldManager _tldm;
	protected Redirects _redirs;
	
	public SpiderQueue(TldManager tldm, Redirects redirs) {
		_tldm = tldm;
		
		_redirs = redirs;
		
		_seen = Collections.synchronizedSet(new HashSet<URI>());
		
//		_urisRound = new HashSet<URI>();
	}
	
	/**
	 * Schedule URIs in Frontier (i.e. put URIs in Frontier into the queue for the next round)
	 */
	public abstract void schedule(Frontier f);// {
//		if (_seenRound != null) {
//			if (!(f instanceof DiskFrontier)) {
//				f.removeAll(_seenRound);
//			}
//		}
//		if (_urisRound != null) {
//			uris
//		}
		
//		_seenRound = Collections.synchronizedSet(new HashSet<URI>());
//		_redirsRound = Collections.synchronizedSet(new HashSet<URI>());
	//}
	
	/**
	 * Set a redirect (303)
	 * @param from
	 * @param to
	 * @param status
	 */
	public void setRedirect(URI from, URI to, int status) {
		try {
			to = Frontier.normalise(to);
		} catch (URISyntaxException e) {
			_log.info(to +  " not parsable, skipping " + to);
			return;
		}
		
		if (from.equals(to)) {
			_log.info("redirected to same uri " + from);
			return;
		}
		
		_redirs.put(from, to);
//		_redirsRound.add(to);
		
		Integer i = null;
		if ((i = _redirsCache.get(from)) != null) {
			_redirsCache.remove(from);
			_redirsCache.put(to, i = Integer.valueOf(i.intValue() + 1));
		} else {
			_redirsCache.put(to, i = Integer.valueOf(0));
		}
		
		if (i.intValue() >= CrawlerConstants.MAX_REDIRECTS) {
			_log.info("Too many redirects on path to: " + to + " ; previous on path: " + from + " .");
			return;
		}
		
		if (checkSeen(to) == false) {
			_log.info("adding " + to + " directly to queue");
			addRedirect(to);
		}
	}

	void add(URI u) {
		add(u, false);
	}

	abstract void add(URI u, boolean uriHasAlreadyBeenProcessed);

	abstract void addRedirect(URI u);

	/**
	 * Return redirected URI (if there's a redirect)
	 * otherwise return original URI.
	 * 
	 * @param from
	 * @return
	 */
	URI obtainRedirect(URI from) {
		URI to = _redirs.getRedirect(from);
		if (from != to) {
			_log.info("redir from " + from + " to " + to);
			return to;
		}
		
		return from;
	}
	
	public Redirects getRedirects() {
		return _redirs;
	}

	public void setRedirects(Redirects redirs) {
		_redirs = redirs;		
	}

	public void addSeen(URI u) {
		if (u != null)
			_seen.add(u);
	}
	
	public Set<URI> getSeen() {
		return _seen;
	}
	
	public void setSeen(Set<URI> seen) {
		_seen = seen;
	}
	
	public boolean checkSeen(URI u) {
		if (u == null) {
			throw new NullPointerException("u cannot be null");
		}
		
		return _seen.contains(u);
	}
	
	void setSeen(URI u) {
		addSeen(u);
//		if (u != null) {
//			_seen.add(u);
//			_seenRound.add(u);
//		}
	}
}
