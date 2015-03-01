package com.bytelightning.opensource.pokerface;
/*
The MIT License (MIT)

PokerFace: Asynchronous, streaming, HTTP/1.1, scriptable, reverse proxy.

Copyright (c) 2015 Frank Stock

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;

/**
 * Semi-delegating <code>X509ExtendedKeyManager</code> which always returns it's own 'server' alias, but delegates all other calls.
 */
public class PokerFaceKeyManager extends X509ExtendedKeyManager {
	private X509ExtendedKeyManager delegate;
    private String alias;
    
    /**
     * Primary constructor
     * @param alias	The hard coded server alias which will *always* be returned by this key manager.
     * @param delegate	The delegate that will handle all other key manager methods.
     */
	public PokerFaceKeyManager(String alias, X509ExtendedKeyManager delegate) {
		this.delegate = delegate;
		this.alias = alias;
	}

	/**
	 * {@inheritDoc}
	 * Always returns the hard coded alias specified in the constructor.
	 */
	@Override
	public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        return alias;
	}
	
	/**
	 * {@inheritDoc}
	 * Always returns the hard coded alias specified in the constructor.
	 */
	@Override
	public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
		return alias;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
		return delegate.chooseEngineClientAlias(keyType, issuers, engine);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
		return delegate.chooseClientAlias(keyType, issuers, socket);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getClientAliases(String keyType, Principal[] issuers) {
		return delegate.getClientAliases(keyType, issuers);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public X509Certificate[] getCertificateChain(String alias) {
		return delegate.getCertificateChain(alias);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PrivateKey getPrivateKey(String alias) {
        return delegate.getPrivateKey(alias);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getServerAliases(String keyType, Principal[] issuers) {
        return new String[] { alias };
	}
}
