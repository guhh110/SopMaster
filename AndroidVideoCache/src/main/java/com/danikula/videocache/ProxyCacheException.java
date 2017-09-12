package com.danikula.videocache;

/**
 * Indicates any error in work of {@link ProxyCache}.
 *
 * @author Alexey Danilov
 */
public class ProxyCacheException extends Exception {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ProxyCacheException(String message) {
        super(message);
    }

    public ProxyCacheException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProxyCacheException(Throwable cause) {
        super(cause);
    }
}
