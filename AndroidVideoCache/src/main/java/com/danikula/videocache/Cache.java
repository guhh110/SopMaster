package com.danikula.videocache;

/**
 * Cache for proxy.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public interface Cache {
	
	/**
	 * 返回可以从缓存  输入流中可无阻塞读取的字节数。 
	 * @return
	 * @throws ProxyCacheException
	 */
	int available() throws ProxyCacheException;

	int read(byte[] buffer, long offset, int length) throws ProxyCacheException;
	
	/**
	 * 
	 * 由 ByteArrayCache 、FileCache 类实现  {@link ByteArrayCache} implementation.
	 * <br />
	 * FileCache append 向 缓存文件写数据
	 * @param data
	 * @param length
	 * @throws ProxyCacheException
	 */
	void append(byte[] data, int length) throws ProxyCacheException;

	void close() throws ProxyCacheException;

	void complete() throws ProxyCacheException;

	/**
	 * 是否完成
	 * @return
	 */
	boolean isCompleted();
}
