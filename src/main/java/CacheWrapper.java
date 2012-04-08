import com.thimbleware.jmemcached.Cache;
import com.thimbleware.jmemcached.CacheElement;
import com.thimbleware.jmemcached.Key;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class CacheWrapper<T extends CacheElement> implements Cache<T> {

	private Cache<T> delegate;

	public CacheWrapper(Cache<T> delegate) {
		this.delegate = delegate;
	}

	public DeleteResponse delete(Key key, int time) {
		return delegate.delete(key, time);
	}

	public StoreResponse add(T element) {
		return delegate.add(element);
	}

	public StoreResponse replace(T element) {
		return delegate.replace(element);
	}

	public StoreResponse append(T element) {
		return delegate.append(element);
	}

	public StoreResponse prepend(T element) {
		return delegate.prepend(element);
	}

	public StoreResponse set(T element) {
		return delegate.set(element);
	}

	public StoreResponse cas(Long casKey, T element) {
		return delegate.cas(casKey, element);
	}

	public Integer get_add(Key key, int mod) {
		return delegate.get_add(key, mod);
	}

	public T[] get(Key... keys) {
		return delegate.get(keys);
	}

	public boolean flush_all() {
		return delegate.flush_all();
	}

	public boolean flush_all(int expire) {
		return delegate.flush_all(expire);
	}

	public void close() throws IOException {
		delegate.close();
	}

	public long getCurrentItems() {
		return delegate.getCurrentItems();
	}

	public long getLimitMaxBytes() {
		return delegate.getLimitMaxBytes();
	}

	public long getCurrentBytes() {
		return delegate.getCurrentBytes();
	}

	public int getGetCmds() {
		return delegate.getGetCmds();
	}

	public int getSetCmds() {
		return delegate.getSetCmds();
	}

	public int getGetHits() {
		return delegate.getGetHits();
	}

	public int getGetMisses() {
		return delegate.getGetMisses();
	}

	public Map<String, Set<String>> stat(String arg) {
		return delegate.stat(arg);
	}

	public void asyncEventPing() {
		delegate.asyncEventPing();
	}
}
